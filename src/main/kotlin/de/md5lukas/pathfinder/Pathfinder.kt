package de.md5lukas.pathfinder

import de.md5lukas.pathfinder.behaviour.BasicPlayerPathingStrategy
import de.md5lukas.pathfinder.behaviour.ConstantFWeigher
import de.md5lukas.pathfinder.behaviour.FWeigher
import de.md5lukas.pathfinder.behaviour.PathingStrategy
import de.md5lukas.pathfinder.world.BlockAccessor
import de.md5lukas.pathfinder.world.BlockPosition
import de.md5lukas.pathfinder.world.Offset
import java.time.Duration
import java.util.*
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executor
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.event.HandlerList
import org.bukkit.event.Listener
import org.bukkit.plugin.Plugin

class Pathfinder(
    private val plugin: Plugin,
    private val executor: Executor,
    private val maxIterations: Int,
    private val maxLength: Int = 0,
    private val pathingStrategy: PathingStrategy = BasicPlayerPathingStrategy(false, 1.0),
    private val allowIncompletePathing: Boolean = true,
    internal val allowChunkLoading: Boolean = false,
    internal val allowChunkGeneration: Boolean = true,
    private val partialPathOnUnloadedChunks: Boolean = true,
    internal val cacheRetention: Duration = Duration.ofMinutes(5),
    internal val weigher: FWeigher = ConstantFWeigher(),
    private val debugTime: Long = 0,
) {

  private val accessor = BlockAccessor(this)
  private var invalidationListener: Listener? = null

  fun registerInvalidationListener() {
    invalidationListener =
        ChunkInvalidationListener(accessor).also {
          Bukkit.getPluginManager().registerEvents(it, plugin)
        }
  }

  fun unregisterListener() {
    invalidationListener?.let { HandlerList.unregisterAll(it) }
  }

  fun findPath(start: Location, goal: Location) =
      findPath(BlockPosition(start), BlockPosition(goal))

  fun findPath(start: BlockPosition, goal: BlockPosition): CompletableFuture<PathResult> =
      CompletableFuture.supplyAsync({ findPath0(ActivePathingContext(start, goal)) }, executor)

  private fun findPath0(context: ActivePathingContext): PathResult {
    context.examinedPositions.add(context.start)
    val startNode = Node(context, context.start, 0.0, null)
    context.frontier.add(startNode)

    var bestNode = startNode

    while (context.frontier.isNotEmpty() && ++context.iterations < maxIterations) {
      if (!plugin.isEnabled) {
        return PathFailure(context, FailureReason.PLUGIN_DISABLED)
      }

      val next = context.frontier.poll()

      if (next.position == context.goal) {
        return PathSuccess(context, PathStatus.COMPLETE, next.retracePath())
      }

      if (next.f <= bestNode.f) {
        bestNode = next

        if (maxLength > 0 && bestNode.depth >= maxLength) {
          return PathSuccess(context, PathStatus.PARTIAL, bestNode.retracePath())
        }
      }

      if (expandNode(context, next)) {
        return PathSuccess(context, PathStatus.PARTIAL, next.retracePath())
      }
    }

    if (allowIncompletePathing) {
      return PathSuccess(context, PathStatus.INCOMPLETE, bestNode.retracePath())
    }

    return PathFailure(
        context,
        if (context.frontier.isEmpty()) {
          FailureReason.EXHAUSTED_OPTIONS
        } else if (context.iterations >= maxIterations) {
          FailureReason.MAX_ITERATIONS
        } else {
          FailureReason.UNKNOWN
        })
  }

  private fun expandNode(context: ActivePathingContext, node: Node): Boolean {
    // TODO find fix for diagonal moves going through blocks
    if (debugTime > 0) node.position.broadcastBlockChange(Material.GLOWSTONE)
    Offset.diagonal.forEach { offset ->
      if (examineNewLocation(context, node, node.position + offset) ===
          ExaminationResult.CHUNK_EDGE) {
        return true
      }
    }
    if (debugTime > 0) node.position.broadcastBlockChange(Material.GLASS)

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
    if (!isBlockAvailable && partialPathOnUnloadedChunks) {
      return ExaminationResult.CHUNK_EDGE
    }

    context.examinedPositions += position

    if (isBlockAvailable && pathingStrategy.isValid(accessor, node.parent, position)) {
      context.frontier +=
          Node(
              context,
              position,
              pathingStrategy.getCost(accessor, node.parent, position) *
                  node.position.octileDistance(position),
              node,
          )
      if (debugTime > 0) {
        position.broadcastBlockChange(Material.LIME_STAINED_GLASS)
        Thread.sleep(debugTime)
      }
      return ExaminationResult.VALID
    }
    if (debugTime > 0) {
      position.broadcastBlockChange(Material.PINK_STAINED_GLASS)
      Thread.sleep(debugTime)
    }
    return ExaminationResult.INVALID
  }

  private fun BlockPosition.broadcastBlockChange(material: Material) {
    Bukkit.getOnlinePlayers().forEach { it.sendBlockChange(asBukkit(), material.createBlockData()) }
  }

  internal inner class ActivePathingContext(
      override val start: BlockPosition,
      override val goal: BlockPosition,
  ) : PathingContext {

    val pathfinder
      get() = this@Pathfinder

    override val examinedPositions: MutableSet<BlockPosition> = HashSet()
    val frontier = PriorityQueue<Node>()
    override var iterations: Int = 0

    override fun toString(): String {
      return "ActivePathingContext(start=$start, goal=$goal, examinedPositionsN=${examinedPositions.size}, frontierN=${frontier.size}, iterations=$iterations)"
    }
  }

  private enum class ExaminationResult {
    VALID,
    INVALID,
    CHUNK_EDGE,
  }
}
