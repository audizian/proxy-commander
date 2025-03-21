package com.xalitoria.commander

import com.google.common.io.ByteStreams
import com.google.common.io.ByteStreams.newDataInput
import com.google.common.io.ByteStreams.newDataOutput
import com.google.inject.Inject
import com.velocitypowered.api.event.Subscribe
import com.velocitypowered.api.event.connection.PluginMessageEvent
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent
import com.velocitypowered.api.plugin.Plugin
import com.velocitypowered.api.proxy.ProxyServer
import com.velocitypowered.api.proxy.ServerConnection
import com.velocitypowered.api.proxy.messages.ChannelIdentifier
import com.velocitypowered.api.proxy.messages.LegacyChannelIdentifier
import com.xalitoria.commander.Constants.PLUGIN_CHANNEL
import org.slf4j.Logger

@Plugin(id = "proxycommander", name = "ProxyCommander", version = "0.1")
class VelocityPlugin @Inject constructor(private val server: ProxyServer, private val logger: Logger) {

    @Subscribe
    fun onProxyInitialize(event: ProxyInitializeEvent) {
        logger.info("Registering server channel: $PLUGIN_CHANNEL")
        server.channelRegistrar.register(pluginChannelIdentifier)
    }

    @Subscribe
    fun onPluginMessage(event: PluginMessageEvent) {
        if (!event.identifier.equals(pluginChannelIdentifier)) return

        val source = event.source as? ServerConnection
        if (source == null) {
            logger.warn("Plugin message received from non-server source: $source")
            return
        }

        val input = newDataInput(event.data)

        try {
            val sourceName = source.serverInfo.name
            val subChannel = input.readUTF()

            logger.info("$subChannel received from server: $sourceName")
            when (subChannel) {
                "ProxyTypeRequest" -> {
                    val output = newDataOutput()
                    output.writeUTF("ProxyTypeResponse")
                    output.writeUTF(ProxyType.VELOCITY.name)
                    source.sendPluginMessage(pluginChannelIdentifier, output.toByteArray())
                }
                "ServerListRequest" -> {
                    val servers = server.allServers
                    val output = newDataOutput()
                    output.writeUTF("ServerListResponse")
                    output.writeInt(servers.size)
                    for (server in servers) {
                        output.writeUTF(server.serverInfo.name)
                    }
                    source.sendPluginMessage(pluginChannelIdentifier, output.toByteArray())
                }
                "command" -> {
                    val command = input.readUTF()
                    logger.info("$sourceName issued proxy command: /$command")
                    try {
                        server.commandManager.executeAsync(server.consoleCommandSource, command).join()
                    } catch (ex: Exception) {
                        logger.error("Error executing command: $command", ex)
                    }
                }
                else -> logger.warn("Unknown sub-channel: $subChannel")
            }
        } catch (ex: Exception) {
            logger.error("Error processing plugin message", ex)
        }
    }

    companion object {
        private val pluginChannelIdentifier: ChannelIdentifier = LegacyChannelIdentifier(PLUGIN_CHANNEL)
    }
}