package de.md5lukas.pathfinder

import de.md5lukas.pathfinder.world.BlockAccessor
import de.md5lukas.pathfinder.world.Offset
import de.md5lukas.pathfinder.world.PathLocation
import java.util.*
import java.util.concurrent.CompletableFuture
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.event.HandlerList

class Pathfinder(
    private val options: PathfinderOptions,
) {

  private val accessor = BlockAccessor(options)
  private val listener =
      if (options.setupChunkInvalidationListener) {
        ChunkInvalidationListener(accessor).also {
          Bukkit.getPluginManager().registerEvents(it, options.plugin)
        }
      } else null

  fun unregisterListener() {
    listener?.let { HandlerList.unregisterAll(it) }
  }

  fun findPath(start: Location, goal: Location) = findPath(PathLocation(start), PathLocation(goal))

  fun findPath(start: PathLocation, goal: PathLocation): CompletableFuture<PathResult> =
      CompletableFuture.supplyAsync(
          { findPath0(ActivePathingContext(options, start, goal)) }, options.executor)

  private fun findPath0(context: ActivePathingContext): PathResult {
    context.examinedPositions.add(context.start)
    val startNode = Node(context, context.start, 0.0, null)
    context.frontier.add(startNode)

    var bestNode = startNode

    while (context.frontier.isNotEmpty() && ++context.iterations < options.maxIterations) {
      val next = context.frontier.poll()

      if (next.f <= bestNode.f) {
        bestNode = next

        if (bestNode.depth >= options.maxLength) {
          return PathSuccess(context, PathStatus.PARTIAL, bestNode.retracePath())
        }
      }

      if (next.location == context.goal) {
        return PathSuccess(context, PathStatus.COMPLETE, next.retracePath())
      }

      if (expandNode(context, next)) {
        return PathSuccess(context, PathStatus.PARTIAL, next.retracePath())
      }
    }

    if (options.allowIncompletePathing) {
      return PathSuccess(context, PathStatus.INCOMPLETE, bestNode.retracePath())
    }

    return PathFailure(
        context,
        if (context.frontier.isEmpty()) {
          FailureReason.EXHAUSTED_OPTIONS
        } else if (context.iterations >= options.maxIterations) {
          FailureReason.MAX_ITERATIONS
        } else {
          FailureReason.UNKNOWN
        })
  }

  private fun expandNode(context: ActivePathingContext, node: Node): Boolean {
    // TODO find fix for diagonal moves going through blocks
    if (options.debugTime > 0) node.location.broadcastBlockChange(Material.GLOWSTONE)
    Offset.diagonalOffsets.forEach { offset ->
      if (examineNewLocation(context, node, node.location + offset) ===
          ExaminationResult.CHUNK_EDGE) {
        return true
      }
    }
    if (options.debugTime > 0) node.location.broadcastBlockChange(Material.GLASS)

    return false
  }

  private fun examineNewLocation(
      context: ActivePathingContext,
      node: Node,
      location: PathLocation
  ): ExaminationResult {
    if (location in context.examinedPositions) return ExaminationResult.INVALID

    if (location.isOutOfBounds()) return ExaminationResult.INVALID

    val isBlockAvailable = accessor.isBlockAvailable(location)

    // If chunk loading is disabled and the option is on, the Pathfinder will attempt to find an
    // Path until the edge of available chunks and finish
    if (!isBlockAvailable && options.partialPathOnUnloadedChunks) {
      return ExaminationResult.CHUNK_EDGE
    }

    context.examinedPositions += location

    if (isBlockAvailable && options.pathingStrategy.isValid(accessor, node.parent, location)) {
      context.frontier +=
          Node(
              context,
              location,
              options.pathingStrategy.getCost(accessor, node.parent, location) *
                  node.location.octileDistance(location),
              node,
          )
      if (options.debugTime > 0) {
        location.broadcastBlockChange(Material.LIME_STAINED_GLASS)
        Thread.sleep(options.debugTime)
      }
      return ExaminationResult.VALID
    }
    if (options.debugTime > 0) {
      location.broadcastBlockChange(Material.PINK_STAINED_GLASS)
      Thread.sleep(options.debugTime)
    }
    return ExaminationResult.INVALID
  }

  private fun PathLocation.broadcastBlockChange(material: Material) {
    Bukkit.getOnlinePlayers().forEach { it.sendBlockChange(asBukkit(), material.createBlockData()) }
  }

  internal class ActivePathingContext(
      val options: PathfinderOptions,
      override val start: PathLocation,
      override val goal: PathLocation,
  ) : PathingContext {

    override val examinedPositions: MutableSet<PathLocation> = HashSet()
    val frontier = PriorityQueue<Node>()
    override var iterations: Int = 0
  }

  private enum class ExaminationResult {
    VALID,
    INVALID,
    CHUNK_EDGE,
  }
}
