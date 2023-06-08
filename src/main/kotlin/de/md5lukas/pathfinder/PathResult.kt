package de.md5lukas.pathfinder

import org.bukkit.Location

sealed class PathResult(val context: PathingContext)

class PathFailure
internal constructor(
    context: PathingContext,
    val reason: FailureReason,
) : PathResult(context) {

  override fun toString(): String {
    return "PathFailure(context=$context, reason=$reason)"
  }
}

enum class FailureReason {
  EXHAUSTED_OPTIONS,
  MAX_ITERATIONS,
  PLUGIN_DISABLED,
  UNKNOWN,
}

class PathSuccess
internal constructor(
    context: PathingContext,
    val status: PathStatus,
    val path: List<Location>,
) : PathResult(context) {
  override fun toString(): String {
    return "PathSuccess(context=$context, status=$status, pathN=${path.size})"
  }
}

enum class PathStatus {
  COMPLETE,
  PARTIAL,
  INCOMPLETE,
}
