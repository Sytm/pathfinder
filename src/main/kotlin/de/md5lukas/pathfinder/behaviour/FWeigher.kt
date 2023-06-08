package de.md5lukas.pathfinder.behaviour

import de.md5lukas.pathfinder.PathingContext
import de.md5lukas.pathfinder.world.BlockPosition

fun interface FWeigher {

  fun calculateF(
      context: PathingContext,
      position: BlockPosition,
      g: Double,
      h: Double,
  ): Double
}
