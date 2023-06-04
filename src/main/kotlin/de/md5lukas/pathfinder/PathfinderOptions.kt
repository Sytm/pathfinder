package de.md5lukas.pathfinder

import de.md5lukas.pathfinder.strategy.BasicPlayerPathingStrategy
import de.md5lukas.pathfinder.strategy.PathingStrategy
import java.time.Duration
import java.util.concurrent.Executor
import org.bukkit.plugin.Plugin

data class PathfinderOptions(
    val plugin: Plugin,
    val executor: Executor,
    val maxLength: Int,
    val maxIterations: Int,
    val pathingStrategy: PathingStrategy = BasicPlayerPathingStrategy(false, 1.0),
    val allowIncompletePathing: Boolean = true,
    val allowChunkLoading: Boolean = false,
    val allowChunkGeneration: Boolean = true,
    val partialPathOnUnloadedChunks: Boolean = true,
    val cacheRetention: Duration = Duration.ofMinutes(5),
    val setupChunkInvalidationListener: Boolean = false,
    val debugTime: Long = 0,
    val heuristicWeight: Double = 1.0,
)