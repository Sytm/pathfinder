package de.md5lukas.pathfinder

import de.md5lukas.pathfinder.world.PathLocation

interface PathingContext {

  val start: PathLocation
  val goal: PathLocation
  val examinedPositions: Set<PathLocation>

  val iterations: Int
}
