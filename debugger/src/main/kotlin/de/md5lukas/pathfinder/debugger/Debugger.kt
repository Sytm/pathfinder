package de.md5lukas.pathfinder.debugger

import org.bukkit.plugin.java.JavaPlugin

class Debugger : JavaPlugin() {

  override fun onEnable() {
    server.commandMap.register(pluginMeta.name, PathCommand(this))
  }
}
