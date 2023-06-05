package de.md5lukas.pathfinder

import de.md5lukas.pathfinder.world.BlockAccessor
import de.md5lukas.pathfinder.world.BlockPosition
import org.bukkit.block.Block
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.block.*

internal class ChunkInvalidationListener(
    private val accessor: BlockAccessor,
) : Listener {

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  fun on(e: BlockBreakEvent) {
    e.block.invalidate()
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  fun on(e: BlockBurnEvent) {
    e.block.invalidate()
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  fun on(e: BlockExplodeEvent) {
    e.block.invalidate()
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  fun on(e: BlockFadeEvent) {
    e.block.invalidate()
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  fun on(e: BlockFormEvent) {
    e.block.invalidate()
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  fun on(e: BlockFromToEvent) {
    e.block.invalidate()
    e.toBlock.invalidate()
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  fun on(e: BlockGrowEvent) {
    e.block.invalidate()
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  fun on(e: BlockMultiPlaceEvent) {
    e.block.invalidate()
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  fun on(e: BlockPistonExtendEvent) {
    e.blocks.forEach { it.invalidate() }
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  fun on(e: BlockPistonRetractEvent) {
    e.blocks.forEach { it.invalidate() }
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  fun on(e: BlockPlaceEvent) {
    e.block.invalidate()
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  fun on(e: BlockSpreadEvent) {
    e.block.invalidate()
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  fun on(e: LeavesDecayEvent) {
    e.block.invalidate()
  }

  private fun Block.invalidate() {
    accessor.invalidate(world, BlockPosition.getChunkKey(x shr 4, z shr 4))
  }
}
