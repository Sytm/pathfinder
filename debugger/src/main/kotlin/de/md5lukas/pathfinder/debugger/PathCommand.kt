package de.md5lukas.pathfinder.debugger

import de.md5lukas.pathfinder.*
import de.md5lukas.pathfinder.strategy.BasicPlayerPathingStrategy
import de.md5lukas.pathfinder.world.BlockPosition
import io.papermc.paper.math.Position
import java.util.*
import net.kyori.adventure.text.Component
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.block.data.BlockData
import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import org.bukkit.command.PluginIdentifiableCommand
import org.bukkit.entity.Player
import org.bukkit.plugin.Plugin
import org.bukkit.util.StringUtil

class PathCommand(
    private val plugin: Debugger,
) : Command("path"), PluginIdentifiableCommand {

  override fun getPlugin(): Plugin = plugin

  private val pathfinder =
      Pathfinder(
              { Bukkit.getScheduler().runTaskAsynchronously(plugin, it) },
              5000,
              heuristicWeight = 1.2,
              debugTime = 0,
              allowIncompletePathing = false,
              pathingStrategy = BasicPlayerPathingStrategy(true, 5.0),
          )
          .apply { registerInvalidationListener(plugin) }

  private val playerStates = mutableMapOf<UUID, PlayerState>()

  private val examinedData = Bukkit.createBlockData(Material.LIGHT_GRAY_STAINED_GLASS)
  private val startData = Bukkit.createBlockData(Material.LIME_STAINED_GLASS)
  private val pathData = Bukkit.createBlockData(Material.YELLOW_STAINED_GLASS)
  private val endData = Bukkit.createBlockData(Material.RED_STAINED_GLASS)

  override fun tabComplete(
      sender: CommandSender,
      alias: String,
      args: Array<String>
  ): MutableList<String> {
    return StringUtil.copyPartialMatches(
        args.lastOrNull() ?: "",
        listOf("pos1", "pos2", "start", "exam"),
        mutableListOf(),
    )
  }

  override fun execute(sender: CommandSender, commandLabel: String, args: Array<String>): Boolean {
    if (sender !is Player) {
      sender.sendMessage(Component.text("You must be a player to execute this command"))
      return true
    }

    if (args.isEmpty()) {
      sender.sendMessage(Component.text("You must provide an argument to execute this command"))
      return true
    }

    val playerState = playerStates.computeIfAbsent(sender.uniqueId) { PlayerState() }

    when (args[0].lowercase()) {
      "pos1" -> {
        playerState.pos1 = BlockPosition(sender.location)
        sender.sendMessage(Component.text("Position 1 set"))
      }
      "pos2" -> {
        playerState.pos2 = BlockPosition(sender.location)
        sender.sendMessage(Component.text("Position 2 set"))
      }
      "start" -> {
        val pos1 = playerState.pos1
        val pos2 = playerState.pos2
        if (pos1 !== null && pos2 !== null) {
          pathfinder.findPath(pos1, pos2).thenAccept {
            playerState.pathResult = it
            when (it) {
              is PathSuccess -> {
                sender.sendMessage(
                    Component.text(
                        "Pathing completed with ${it.status}, path length ${it.path.size}, iterations ${it.context.iterations}"))
                val map = mutableMapOf<Position, BlockData>()
                val last = it.path.lastIndex
                it.path.forEachIndexed { index, location ->
                  map[location] =
                      when (index) {
                        0 -> startData
                        last -> endData
                        else -> pathData
                      }
                }
                sender.sendMultiBlockChange(map, true)
              }
              is PathFailure -> {
                sender.sendMessage(
                    Component.text(
                        "Pathing failed because ${it.reason}, iterations ${it.context.iterations}"))
              }
            }
          }
        } else {
          sender.sendMessage(Component.text("You must set pos1 and pos2 first"))
        }
      }
      "exam" -> {
        val result = playerState.pathResult
        if (result === null) {
          sender.sendMessage(Component.text("You must have previously attempted to find a path"))
        } else {
          val map = mutableMapOf<Position, BlockData>()
          result.context.examinedPositions.forEach {
            map[Position.block(it.x, it.y, it.z)] = examinedData
          }
          sender.sendMultiBlockChange(map, true)
        }
      }
      else -> sender.sendMessage(Component.text("Couldn't find subcommand"))
    }

    return true
  }

  private class PlayerState {
    var pos1: BlockPosition? = null
    var pos2: BlockPosition? = null
    var pathResult: PathResult? = null
  }
}
