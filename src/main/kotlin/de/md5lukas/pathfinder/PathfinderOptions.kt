package de.md5lukas.pathfinder

import de.md5lukas.pathfinder.strategy.BasicPlayerPathingStrategy
import de.md5lukas.pathfinder.strategy.PathingStrategy
import java.time.Duration
import java.util.concurrent.Executor

data class PathfinderOptions(
    val executor: Executor,
    val maxIterations: Int,
    val maxLength: Int = 0,
    val pathingStrategy: PathingStrategy = BasicPlayerPathingStrategy(false, 1.0),
    val allowIncompletePathing: Boolean = true,
    val allowChunkLoading: Boolean = false,
    val allowChunkGeneration: Boolean = true,
    val partialPathOnUnloadedChunks: Boolean = true,
    val cacheRetention: Duration = Duration.ofMinutes(5),
    val heuristicWeight: Double = 1.0,
    val debugTime: Long = 0,
)
