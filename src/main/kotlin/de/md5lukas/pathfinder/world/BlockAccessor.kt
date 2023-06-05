package de.md5lukas.pathfinder.world

import com.google.common.cache.Cache
import com.google.common.cache.CacheBuilder
import com.google.common.cache.CacheLoader
import com.google.common.cache.LoadingCache
import de.md5lukas.pathfinder.Pathfinder
import java.util.*
import org.bukkit.ChunkSnapshot
import org.bukkit.Material
import org.bukkit.World

class BlockAccessor internal constructor(private val pathfinder: Pathfinder) {

  private val worlds: LoadingCache<UUID, Cache<Long, ChunkSnapshot>>

  init {
    val cacheBuilder = CacheBuilder.newBuilder().expireAfterAccess(pathfinder.cacheRetention)
    worlds =
        cacheBuilder.build(
            object : CacheLoader<UUID, Cache<Long, ChunkSnapshot>>() {
              override fun load(key: UUID): Cache<Long, ChunkSnapshot> = cacheBuilder.build()
            })
  }

  fun isBlockAvailable(position: BlockPosition): Boolean {
    return worlds[position.world.uid].asMap().containsKey(position.chunkKey) ||
        canLoadChunk(position)
  }

  fun getBlock(position: BlockPosition): Material? {
    if (position.isOutOfBounds()) return Material.VOID_AIR

    return getChunkSnapshot(position)
        ?.getBlockType(
            position.chunkLocalX,
            position.y,
            position.chunkLocalZ,
        )
  }

  fun invalidate(world: World, chunkKey: Long) {
    worlds.getIfPresent(world.uid)?.invalidate(chunkKey)
  }

  private fun getChunkSnapshot(position: BlockPosition): ChunkSnapshot? {
    val worldCache = worlds[position.world.uid]

    return if (canLoadChunk(position)) {
      worldCache.get(position.chunkKey) {
        position.world
            .getChunkAtAsyncUrgently(position.chunkX, position.chunkZ)
            .join()
            // Don't need height map
            .getChunkSnapshot(false, false, false)
      }
    } else {
      worldCache.getIfPresent(position.chunkKey)
    }
  }

  private fun canLoadChunk(position: BlockPosition) =
      (pathfinder.allowChunkLoading ||
          position.world.isChunkLoaded(position.chunkX, position.chunkZ)) &&
          (pathfinder.allowChunkGeneration ||
              position.world.isChunkGenerated(position.chunkX, position.chunkZ))
}
