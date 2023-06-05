package de.md5lukas.pathfinder.strategy

import de.md5lukas.pathfinder.Node
import de.md5lukas.pathfinder.world.BlockAccessor
import de.md5lukas.pathfinder.world.BlockPosition
import org.bukkit.Material

interface PathingStrategy {

  companion object {
    // Blocks that are marked as noCollision in the block registry that are undesirable to walk
    // through
    private val disallowedNonCollidable =
        arrayOf(
                Material.WATER,
                Material.LAVA,
                Material.COBWEB,
                Material.FIRE,
                Material.SOUL_FIRE,
                Material.TRIPWIRE,
                Material.SWEET_BERRY_BUSH,)
            .toHashSet()

    fun fitsPlayer(accessor: BlockAccessor, position: BlockPosition): Boolean {
      val block = accessor.getBlock(position)
      val above = accessor.getBlock(position.plus(0, 1, 0))
      return block !== null &&
          above !== null &&
          !block.isCollidable &&
          !above.isCollidable &&
          block !in disallowedNonCollidable &&
          above !in disallowedNonCollidable
    }

    fun isValidGround(material: Material) =
       material.isSolid && material.isCollidable
  }

  fun isValid(accessor: BlockAccessor, previousNode: Node?, position: BlockPosition): Boolean

  fun getCost(accessor: BlockAccessor, previousNode: Node?, position: BlockPosition): Double =
      BlockPosition.octileD1
}
