package de.md5lukas.pathfinder

import de.md5lukas.pathfinder.world.PathLocation
import kotlin.math.sign
import org.bukkit.Location

class Node
internal constructor(
    context: Pathfinder.ActivePathingContext,
    val location: PathLocation,
    cost: Double,
    val parent: Node?
) : Comparable<Node> {

  val depth: Int = (parent?.depth ?: 0) + 1
  private val g: Double = (parent?.g ?: 0.0) + cost

  private val h: Double =
      location.octileDistance(context.goal) * context.options.heuristicWeight

  val f = g + h

  override fun compareTo(other: Node): Int = (f - other.f).sign.toInt()

  fun retracePath(): List<Location> {
    var current: Node? = this
    val path = mutableListOf<Location>()

    while (current != null) {
      path.add(current.location.asBukkit())
      current = current.parent
    }

    path.reverse()

    return path
  }

  override fun toString(): String {
    return "Node(depth=$depth, g=$g, h=$h, f=$f)"
  }
}
