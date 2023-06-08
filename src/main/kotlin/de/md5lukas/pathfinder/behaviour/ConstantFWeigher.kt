package de.md5lukas.pathfinder.behaviour

import de.md5lukas.pathfinder.PathingContext
import de.md5lukas.pathfinder.world.BlockPosition

class ConstantFWeigher(private val weight: Double = 1.0) : FWeigher {

  override fun calculateF(
      context: PathingContext,
      position: BlockPosition,
      g: Double,
      h: Double,
  ) = g + h * weight
}
