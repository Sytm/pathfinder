package de.md5lukas.pathfinder.strategy

import de.md5lukas.pathfinder.Node
import de.md5lukas.pathfinder.world.BlockAccessor
import de.md5lukas.pathfinder.world.PathLocation
import org.bukkit.Material

class BasicPlayerPathingStrategy(
    private val allowSwimming: Boolean,
    private val swimmingPenalty: Double
) : PathingStrategy {

  override fun isValid(
    accessor: BlockAccessor,
    previousNode: Node?,
    location: PathLocation,
  ): Boolean {
    if (!PathingStrategy.fitsPlayer(accessor, location)) return false
    val ground = accessor.getBlock(location.plus(0, -1, 0))
    if (ground === null) return false

    return (allowSwimming && ground === Material.WATER) || PathingStrategy.isValidGround(ground)
  }

  override fun getCost(
    accessor: BlockAccessor,
    previousNode: Node?,
    location: PathLocation,
  ): Double {
    val baseCost = super.getCost(accessor, previousNode, location)
    if (allowSwimming && accessor.getBlock(location.plus(0, -1, 0)) === Material.WATER) {
      return baseCost * swimmingPenalty
    }
    return baseCost
  }
}
