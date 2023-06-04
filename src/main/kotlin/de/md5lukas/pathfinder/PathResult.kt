package de.md5lukas.pathfinder

import org.bukkit.Location

sealed class PathResult(val context: PathingContext)

class PathFailure
internal constructor(
    context: PathingContext,
    val reason: FailureReason,
) : PathResult(context)

enum class FailureReason {
  EXHAUSTED_OPTIONS,
  MAX_ITERATIONS,
  UNKNOWN,
}

class PathSuccess
internal constructor(
    context: PathingContext,
    val status: PathStatus,
    val path: List<Location>,
) : PathResult(context)

enum class PathStatus {
  COMPLETE,
  PARTIAL,
  INCOMPLETE,
}
