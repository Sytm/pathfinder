package de.md5lukas.pathfinder.world

import kotlin.math.abs
import kotlin.math.sqrt
import org.bukkit.Location
import org.bukkit.World

class BlockPosition(
    val x: Int,
    val y: Int,
    val z: Int,
    val world: World,
) {

  companion object {
    const val octileD1 = 1.0
    private val octileD2 = sqrt(2.0)
    private val octileD3 = sqrt(3.0)

    fun getChunkKey(chunkX: Int, chunkZ: Int) =
        (chunkX.toLong() and 0xFFFFFFFFL) or (chunkZ.toLong() shl 32)
  }

  constructor(
      bukkitLocation: Location
  ) : this(
      bukkitLocation.blockX,
      bukkitLocation.blockY,
      bukkitLocation.blockZ,
      bukkitLocation.world,
  )

  val chunkLocalX = x and 0xF
  val chunkLocalZ = z and 0xF
  val chunkX = x shr 4
  val chunkZ = z shr 4
  val chunkKey = getChunkKey(chunkX, chunkZ)

  fun asBukkit() = Location(world, x.toDouble(), y.toDouble(), z.toDouble())

  fun octileDistance(other: BlockPosition): Double {
    if (world != other.world) {
      throw IllegalArgumentException("Locations must be in the same world")
    }
    val dx = abs(x - other.x)
    val dy = abs(y - other.y)
    val dz = abs(z - other.z)

    val min = minOf(dx, dy, dz)
    val median = medianOf(dx, dy, dz)
    val max = maxOf(dx, dy, dz)

    return octileD1 * max + (octileD2 - octileD1) * median + (octileD3 - octileD2) * min
  }

  fun isOutOfBounds() = y !in world.minHeight until world.maxHeight

  operator fun plus(offset: Offset) = plus(offset.x, offset.y, offset.z)

  fun plus(dx: Int, dy: Int, dz: Int) = BlockPosition(x + dx, y + dy, z + dz, world)

  operator fun minus(other: BlockPosition) = Offset(x - other.x, y - other.y, z - other.z)

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (javaClass != other?.javaClass) return false

    other as BlockPosition

    if (x != other.x) return false
    if (y != other.y) return false
    if (z != other.z) return false
    return world == other.world
  }

  override fun hashCode(): Int {
    var result = x
    result = 31 * result + y
    result = 31 * result + z
    result = 31 * result + world.hashCode()
    return result
  }

  override fun toString(): String {
    return "PathLocation(x=$x, y=$y, z=$z, world=$world)"
  }

  private fun medianOf(x: Int, y: Int, z: Int) = maxOf(minOf(x, y), minOf(maxOf(x, y), z))
}
