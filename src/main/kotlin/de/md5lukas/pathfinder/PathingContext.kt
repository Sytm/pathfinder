package de.md5lukas.pathfinder

import de.md5lukas.pathfinder.world.BlockPosition

interface PathingContext {

  val start: BlockPosition
  val goal: BlockPosition
  val examinedPositions: Set<BlockPosition>

  val iterations: Int
}
