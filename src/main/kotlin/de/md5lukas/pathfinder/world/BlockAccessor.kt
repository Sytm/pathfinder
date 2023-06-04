package de.md5lukas.pathfinder.world

import com.google.common.cache.Cache
import com.google.common.cache.CacheBuilder
import com.google.common.cache.CacheLoader
import com.google.common.cache.LoadingCache
import de.md5lukas.pathfinder.PathfinderOptions
import java.util.*
import org.bukkit.ChunkSnapshot
import org.bukkit.Material
import org.bukkit.World

class BlockAccessor internal constructor(private val options: PathfinderOptions) {

  private val worlds: LoadingCache<UUID, Cache<Long, ChunkSnapshot>>

  init {
    val cacheBuilder = CacheBuilder.newBuilder().expireAfterAccess(options.cacheRetention)
    worlds =
        cacheBuilder.build(
            object : CacheLoader<UUID, Cache<Long, ChunkSnapshot>>() {
              override fun load(key: UUID): Cache<Long, ChunkSnapshot> = cacheBuilder.build()
            })
  }

  fun isBlockAvailable(location: PathLocation): Boolean {
    return worlds[location.world.uid].asMap().containsKey(location.chunkKey) ||
        canLoadChunk(location)
  }

  fun getBlock(location: PathLocation): Material? {
    if (location.isOutOfBounds()) return Material.VOID_AIR

    return getChunkSnapshot(location)?.getBlockType(
      location.chunkLocalX,
      location.y,
      location.chunkLocalZ,
    )
  }

  fun invalidate(world: World, chunkKey: Long) {
    worlds.getIfPresent(world.uid)?.invalidate(chunkKey)
  }

  private fun getChunkSnapshot(location: PathLocation): ChunkSnapshot? {
    val worldCache = worlds[location.world.uid]

    return if (canLoadChunk(location)) {
      worldCache.get(location.chunkKey) {
        location.world
            .getChunkAtAsyncUrgently(location.chunkX, location.chunkZ)
            .join()
            // Don't need height map
            .getChunkSnapshot(false, false, false)
      }
    } else {
      worldCache.getIfPresent(location.chunkKey)
    }
  }

  private fun canLoadChunk(location: PathLocation) =
      (options.allowChunkLoading ||
          location.world.isChunkLoaded(location.chunkX, location.chunkZ)) &&
          (options.allowChunkGeneration ||
              location.world.isChunkGenerated(location.chunkX, location.chunkZ))
}
