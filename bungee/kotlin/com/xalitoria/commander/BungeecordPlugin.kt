package com.xalitoria.commander

import com.google.common.io.ByteStreams
import com.xalitoria.commander.Constants.PLUGIN_CHANNEL
import net.md_5.bungee.api.ProxyServer
import net.md_5.bungee.api.connection.Server
import net.md_5.bungee.api.event.PluginMessageEvent
import net.md_5.bungee.api.plugin.Listener
import net.md_5.bungee.api.plugin.Plugin
import net.md_5.bungee.event.EventHandler

class BungeecordPlugin : Plugin(), Listener {

    override fun onEnable() {
        logger.info("Registering server channel: $PLUGIN_CHANNEL")
        proxy.registerChannel(PLUGIN_CHANNEL)
        proxy.pluginManager.registerListener(this, CommanderListener())
    }

    inner class CommanderListener(): Listener {
        @EventHandler fun onPluginMessage(event: PluginMessageEvent) {
            if (event.tag != PLUGIN_CHANNEL) return

            val source = event.sender
            if (source !is Server) {
                logger.warning("Plugin message received from non-server source: $source")
                return
            }

            val input = ByteStreams.newDataInput(event.data)
            when (val subChannel = input.readUTF()) {
                "ProxyTypeRequest" -> {
                    val output = ByteStreams.newDataOutput()
                    output.writeUTF("ProxyTypeResponse")
                    output.writeUTF("BungeeCord")
                    source.sendData(PLUGIN_CHANNEL, output.toByteArray())
                }
                "ServerListRequest" -> {
                    val servers = proxy.servers.values
                    val output = ByteStreams.newDataOutput()
                    output.writeUTF("ServerListResponse")
                    output.writeInt(servers.size)
                    for (server in servers) {
                        output.writeUTF(server.name)
                    }
                    source.sendData(PLUGIN_CHANNEL, output.toByteArray())
                }
                "command" -> {
                    val command = input.readUTF()
                    @Suppress("DEPRECATION")
                    proxy.console.sendMessage("/$command")
                    try {
                        proxy.pluginManager.dispatchCommand(proxy.console, command)
                    } catch (ex: Exception) {
                        ex.printStackTrace()
                    }
                }
                else -> logger.warning("Unknown sub-channel: $subChannel")
            }
        }
    }

}