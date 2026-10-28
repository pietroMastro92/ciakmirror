package dev.imirror.receiver.history

/** Kind of AirPlay session shown in the local history. */
enum class SessionKind { MIRRORING, VIDEO, AUDIO }

/** How a session ended. */
enum class SessionOutcome { COMPLETED, ERROR }

/**
 * One locally-stored connection session.
 *
 * PRIVACY: only the sender's display name (already shown on screen), the session type,
 * start time and duration are stored. No IP addresses, no media URLs, no content.
 */
data class SessionRecord(
    val id: Long,
    val deviceName: String,
    val kind: SessionKind,
    val startedAt: Long,
    val durationMs: Long,
    val outcome: SessionOutcome,
) {
    val durationMinutes: Long get() = durationMs / 60_000L
    val durationSeconds: Long get() = durationMs / 1_000L
}
