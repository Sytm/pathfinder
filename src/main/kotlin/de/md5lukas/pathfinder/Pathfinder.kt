package de.md5lukas.pathfinder

import de.md5lukas.pathfinder.world.BlockAccessor
import de.md5lukas.pathfinder.world.Offset
import de.md5lukas.pathfinder.world.BlockPosition
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

  fun findPath(start: Location, goal: Location) = findPath(BlockPosition(start), BlockPosition(goal))

  fun findPath(start: BlockPosition, goal: BlockPosition): CompletableFuture<PathResult> =
      CompletableFuture.supplyAsync(
          { findPath0(ActivePathingContext(options, start, goal)) }, options.executor)

  private fun findPath0(context: ActivePathingContext): PathResult {
    context.examinedPositions.add(context.start)
    val startNode = Node(context, context.start, 0.0, null)
    context.frontier.add(startNode)

    var bestNode = startNode

    while (context.frontier.isNotEmpty() && ++context.iterations < options.maxIterations) {
      val next = context.frontier.poll()

      if (next.position == context.goal) {
        return PathSuccess(context, PathStatus.COMPLETE, next.retracePath())
      }

      if (next.f <= bestNode.f) {
        bestNode = next

        if (options.maxLength > 0 && bestNode.depth >= options.maxLength) {
          return PathSuccess(context, PathStatus.PARTIAL, bestNode.retracePath())
        }
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
    if (options.debugTime > 0) node.position.broadcastBlockChange(Material.GLOWSTONE)
    Offset.diagonal.forEach { offset ->
      if (examineNewLocation(context, node, node.position + offset) ===
          ExaminationResult.CHUNK_EDGE) {
        return true
      }
    }
    if (options.debugTime > 0) node.position.broadcastBlockChange(Material.GLASS)

    return false
  }

  private fun examineNewLocation(
    context: ActivePathingContext,
    node: Node,
    position: BlockPosition
  ): ExaminationResult {
    if (position in context.examinedPositions) return ExaminationResult.INVALID

    if (position.isOutOfBounds()) return ExaminationResult.INVALID

    val isBlockAvailable = accessor.isBlockAvailable(position)

    // If chunk loading is disabled and the option is on, the Pathfinder will attempt to find an
    // Path until the edge of available chunks and finish
    if (!isBlockAvailable && options.partialPathOnUnloadedChunks) {
      return ExaminationResult.CHUNK_EDGE
    }

    context.examinedPositions += position

    if (isBlockAvailable && options.pathingStrategy.isValid(accessor, node.parent, position)) {
      context.frontier +=
          Node(
              context,
              position,
              options.pathingStrategy.getCost(accessor, node.parent, position) *
                  node.position.octileDistance(position),
              node,
          )
      if (options.debugTime > 0) {
        position.broadcastBlockChange(Material.LIME_STAINED_GLASS)
        Thread.sleep(options.debugTime)
      }
      return ExaminationResult.VALID
    }
    if (options.debugTime > 0) {
      position.broadcastBlockChange(Material.PINK_STAINED_GLASS)
      Thread.sleep(options.debugTime)
    }
    return ExaminationResult.INVALID
  }

  private fun BlockPosition.broadcastBlockChange(material: Material) {
    Bukkit.getOnlinePlayers().forEach { it.sendBlockChange(asBukkit(), material.createBlockData()) }
  }

  internal class ActivePathingContext(
    val options: PathfinderOptions,
    override val start: BlockPosition,
    override val goal: BlockPosition,
  ) : PathingContext {

    override val examinedPositions: MutableSet<BlockPosition> = HashSet()
    val frontier = PriorityQueue<Node>()
    override var iterations: Int = 0
  }

  private enum class ExaminationResult {
    VALID,
    INVALID,
    CHUNK_EDGE,
  }
}
