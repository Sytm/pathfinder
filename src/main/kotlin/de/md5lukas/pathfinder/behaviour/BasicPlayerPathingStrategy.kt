package de.md5lukas.pathfinder.behaviour

import de.md5lukas.pathfinder.Node
import de.md5lukas.pathfinder.world.BlockAccessor
import de.md5lukas.pathfinder.world.BlockPosition
import org.bukkit.Material

class BasicPlayerPathingStrategy(
    private var allowSwimming: Boolean,
    private var swimmingPenalty: Double
) : PathingStrategy {

  override fun isValid(
      accessor: BlockAccessor,
      previousNode: Node?,
      position: BlockPosition,
  ): Boolean {
    if (!PathingStrategy.fitsPlayer(accessor, position)) return false
    val ground = accessor.getBlock(position.plus(0, -1, 0))
    if (ground === null) return false

    return (allowSwimming && ground === Material.WATER) || PathingStrategy.isValidGround(ground)
  }

  override fun getCost(
      accessor: BlockAccessor,
      previousNode: Node?,
      position: BlockPosition,
  ): Double {
    val baseCost = super.getCost(accessor, previousNode, position)
    if (allowSwimming && accessor.getBlock(position.plus(0, -1, 0)) === Material.WATER) {
      return baseCost * swimmingPenalty
    }
    return baseCost
  }
}
