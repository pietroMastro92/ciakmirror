package dev.imirror.receiver.airplay

import dev.imirror.receiver.util.Logger
import java.io.InputStream

/**
 * RtspRequestReader parses RTSP/HTTP-style requests from the AirPlay control socket.
 *
 * The reader consumes bytes directly from the socket so the caller can safely switch
 * to binary RTP interleaved frames after RTSP RECORD without losing buffered data.
 */
internal class RtspRequestReader(
    private val maxMessageBytes: Int,
    private val maxPhotoBytes: Int
) {
    /** Optional diagnostic sink (wired to RtspHandler's file log). */
    var onDiagnostic: ((String) -> Unit)? = null

    /**
     * Reads one complete RTSP or AirPlay photo request from [inputStream].
     *
     * Returns null on clean EOF, malformed input, or payloads over the configured limits.
     */
    fun read(inputStream: InputStream): RtspRequest? {
        val requestLine = readRequestLine(inputStream) ?: return null
        if (requestLine.isBlank()) return read(inputStream)

        val parts = requestLine.split(" ")
        if (parts.size < 3) {
            val printable = requestLine.take(120).map { if (it.code in 32..126) it else '.' }.joinToString("")
            val hex = requestLine.toByteArray(Charsets.ISO_8859_1).take(48)
                .joinToString("") { "%02x".format(it) }
            onDiagnostic?.invoke("MALFORMED line='$printable' hex=$hex")
            Logger.w("Malformed RTSP request line: '$printable'")
            return null
        }

        val headers = readHeaders(inputStream, requestLine.length) ?: return null
        val method = parts[0]
        val uri = parts[1]
        val protocol = parts[2]
        val bodyBytes = readBody(inputStream, method, uri, headers) ?: return null

        return RtspRequest(
            method = method,
            uri = uri,
            headers = headers,
            body = String(bodyBytes, Charsets.UTF_8),
            bodyBytes = bodyBytes,
            protocol = protocol
        )
    }

    private fun readHeaders(inputStream: InputStream, requestLineBytes: Int): Map<String, String>? {
        val headers = mutableMapOf<String, String>()
        var totalBytes = requestLineBytes

        while (true) {
            val line = readLine(inputStream) ?: return null
            if (line.isEmpty()) return headers

            totalBytes += line.length
            if (totalBytes > maxMessageBytes) {
                Logger.w("RTSP message too large — rejecting")
                return null
            }

            val colonIndex = line.indexOf(':')
            if (colonIndex > 0) {
                headers[line.substring(0, colonIndex).trim()] =
                    line.substring(colonIndex + 1).trim()
            }
        }
    }

    private fun readBody(
        inputStream: InputStream,
        method: String,
        uri: String,
        headers: Map<String, String>
    ): ByteArray? {
        val contentLength = headers["Content-Length"]?.toIntOrNull() ?: 0
        val bodyLimit = if (method == "PUT" && uri.substringBefore("?") == PhotoHandler.PHOTO_PATH) {
            maxPhotoBytes
        } else {
            maxMessageBytes
        }

        if (contentLength > bodyLimit) {
            Logger.w("Request body too large ($contentLength bytes) — rejecting")
            return null
        }
        if (contentLength <= 0) return ByteArray(0)

        val buf = ByteArray(contentLength)
        var read = 0
        while (read < contentLength) {
            val n = inputStream.read(buf, read, contentLength - read)
            if (n == -1) return null
            read += n
        }
        return buf
    }

    private fun readLine(inputStream: InputStream): String? {
        val sb = StringBuilder()
        while (true) {
            val b = inputStream.read()
            if (b == -1) return if (sb.isEmpty()) null else sb.toString()
            if (b == '\r'.code) continue
            if (b == '\n'.code) return sb.toString()
            sb.append(b.toChar())
            if (sb.length > maxMessageBytes) return null
        }
    }

    /**
     * Reads the request line, logging the very first byte so a bare TCP probe (no data) can be
     * told apart from a TLS ClientHello / binary garbage that the RTSP parser rejects.
     */
    private fun readRequestLine(inputStream: InputStream): String? {
        val first = inputStream.read()
        if (first == -1) {
            onDiagnostic?.invoke("EOF (no bytes)")
            return null
        }
        onDiagnostic?.invoke("FIRST_BYTE=0x%02x".format(first))
        val sb = StringBuilder()
        var b = first
        while (true) {
            if (b == '\n'.code) return sb.toString()
            if (b != '\r'.code) sb.append(b.toChar())
            if (sb.length > maxMessageBytes) return null
            b = inputStream.read()
            if (b == -1) return sb.toString()
        }
    }
}
