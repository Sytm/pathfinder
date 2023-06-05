package de.md5lukas.pathfinder.world

class Offset(
    val x: Int,
    val y: Int,
    val z: Int,
) {

  companion object {

    val diagonal: Array<Offset>

    init {
      val mutOffsets = mutableListOf<Offset>()

      for (x in -1..1) {
        for (y in -1..1) {
          for (z in -1..1) {
            if (x == 0 && y == 0 && z == 0) continue
            mutOffsets += Offset(x, y, z)
          }
        }
      }

      diagonal = mutOffsets.toTypedArray()
    }
  }

  override fun toString(): String {
    return "Offset(x=$x, y=$y, z=$z)"
  }
}
