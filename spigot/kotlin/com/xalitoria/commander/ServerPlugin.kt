package com.xalitoria.commander

import com.google.common.io.ByteStreams.newDataInput
import com.google.common.io.ByteStreams.newDataOutput
import com.xalitoria.commander.Constants.CONSOLE_COMMAND_SENDER
import com.xalitoria.commander.Constants.FAILED_SYNC_PROXY_MAX_ATTEMPTS
import com.xalitoria.commander.Constants.NO_PERMISSION_COMMAND
import com.xalitoria.commander.Constants.SEND_COMMAND_NO_PLAYERS
import com.xalitoria.commander.Constants.SERVER_LIST_NO_PLAYERS
import com.xalitoria.commander.Constants.SEND_COMMAND_NO_PROXY
import com.xalitoria.commander.Constants.SERVER_LIST_NO_PROXY
import com.xalitoria.commander.Constants.PLUGIN_CHANNEL
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.bukkit.ChatColor
import org.bukkit.command.*
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.plugin.java.JavaPlugin
import org.bukkit.plugin.messaging.PluginMessageListener
import java.util.logging.*
import dev.idot.text.color.convertColorsAndFormat as color

@Suppress("SameReturnValue", "UnstableApiUsage")
class ServerPlugin : JavaPlugin() {

    override fun onEnable() {
        instance = this

        logger.info("Registering server channel: $PLUGIN_CHANNEL")

        server.messenger.run {
            registerOutgoingPluginChannel(instance, "BungeeCord")
            registerOutgoingPluginChannel(instance, PLUGIN_CHANNEL)
            registerIncomingPluginChannel(instance, PLUGIN_CHANNEL, pluginMessenger)
        }

        server.pluginManager.registerEvents(object: Listener {
            @EventHandler fun playerJoinEvent(event: PlayerJoinEvent) {
                val instant = System.currentTimeMillis()
                if (instant <= syncDebounce) return
                pluginScope.launch {
                    logger.info("Syncing data...")
                    delay(1000)
                    syncData()
                    syncDebounce = instant + 30000
                }
            }
        }, instance)

        fun commandError(command: String) = logger.warning("Command \"$command\" could not be registered.")

        getCommand("proxycommander")?.setExecutor(proxyCommanderCommand)
            ?: commandError("proxycommander")

        getCommand("serveralias")?.run {
            executor = serverAliasCommand
            tabCompleter = serverAliasTabComplete
        } ?: commandError("serveralias")
    }

    override fun onDisable() {
        server.scheduler.cancelTasks(instance)
    }

    companion object {
        lateinit var instance: ServerPlugin
            private set

        private var proxyType: ProxyType = ProxyType.NONE

        private var cachedServerList: List<String> = emptyList()
        private var syncDebounce: Long = System.currentTimeMillis()

        private val pluginScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

        private val pluginMessenger = PluginMessageListener { channel, player, message ->
            if (channel != PLUGIN_CHANNEL) return@PluginMessageListener
            try {
                val input = newDataInput(message)
                when (val subChannel = input.readUTF() ) {
                    "ProxyTypeResponse" -> {
                        val proxyType = try {
                            ProxyType.valueOf(input.readUTF())
                        } catch (_: IllegalArgumentException) {
                            ProxyType.NONE
                        }
                        setProxyType(proxyType)
                    }
                    "ServerListResponse" -> {
                        val serverList = mutableListOf<String>()
                        repeat(input.readInt()) { serverList.add(input.readUTF()) }
                        setServerList(serverList)
                    }

                    else -> instance.logger.warning("Received unknown sub-channel: $subChannel")
                }
            } catch (ex: Exception) {
                instance.logger.log(Level.WARNING, "Error processing plugin message.", ex)
            }
        }

        private val proxyCommanderCommand = CommandExecutor { sender, _, alias, args ->
            if (sender !is ConsoleCommandSender) {
                sender.sendMessage(CONSOLE_COMMAND_SENDER)
                return@CommandExecutor true
            }

            if (args.isEmpty()) {
                sender.sendMessage("&eUsage: &r/$alias <command>".color())
                return@CommandExecutor true
            }

            val proxyCommand = args.joinToString(" ")

            instance.logger.info(ChatColor.YELLOW.toString() + "/$proxyCommand")
            if (!sendProxyCommand(proxyCommand)) instance.logger.warning("&cCommand failed: &r/$proxyCommand".color())
            return@CommandExecutor true
        }

        private val serverAliasCommand = CommandExecutor { sender, _, _, args ->
            if (!sender.hasPermission("proxycmd.server")) {
                sender.sendMessage(NO_PERMISSION_COMMAND)
                return@CommandExecutor true
            }

            if (args.isEmpty()) {
                sender.sendMessage("&eUsage: &r/serveralias <server> [player]".color())
                return@CommandExecutor true
            }

            if (cachedServerList.isEmpty()) {
                sender.sendMessage("&cServer list is not available yet.".color())
                return@CommandExecutor true
            }

            if (!cachedServerList.contains(args.first())) {
                val msg = "&cInvalid server. Available servers:" + cachedServerList.joinToString("&r, &e", "&e")
                sender.sendMessage(msg.color())
                return@CommandExecutor true
            }

            val playerQuery = args.getOrNull(1)
            if (sender !is Player && playerQuery == null) {
                sender.sendMessage("&cYou must specify a player.".color())
                return@CommandExecutor true
            }

            val player: String = if (sender is Player) sender.name
            else {
                var player: String? = null
                for (pl in instance.server.onlinePlayers) {
                    if (pl.name.equals(playerQuery, true)) {
                        player = pl.name
                        break
                    }
                }
                player ?: return@CommandExecutor true
            }

            sendProxyCommand("send $player ${args.first()}")
            return@CommandExecutor true
        }

        private val serverAliasTabComplete = TabCompleter { sender, _, _, args ->
            when (args.lastIndex) {
                0 -> cachedServerList.stringFilter(args[0])
                1 -> instance.server.onlinePlayers.stringFilter(args[1]) { it.name }
                else -> emptyList<String>()
            }
        }

        private fun syncData() {
            pluginScope.launch {
                requestProxyType()
                delay(1000)
                pluginScope.launch {
                    requestServerList()
                }
            }
        }

        private suspend fun requestProxyType() {
            var attempts = 0
            while (attempts <= 5) {
                try {
                    val output = newDataOutput()
                    output.writeUTF("ProxyTypeRequest")
                    instance.server.sendPluginMessage(instance, PLUGIN_CHANNEL, output.toByteArray())

                    delay(1000)

                    if (proxyType != ProxyType.NONE) return // Success

                    attempts++
                    instance.logger.warning("Retrying proxy type request ($attempts/5)")
                } catch (e: Exception) {
                    instance.logger.log(Level.WARNING, "Error requesting proxy type", e)
                }
            }

            instance.logger.warning(FAILED_SYNC_PROXY_MAX_ATTEMPTS)
        }

        private fun setProxyType(type: ProxyType) {
            if (proxyType != type) instance.logger.info("Proxy type set to: $proxyType")
            proxyType = type
        }

        private suspend fun requestServerList() {
            if (proxyType == ProxyType.NONE) {
                instance.logger.warning(SERVER_LIST_NO_PROXY)
                return
            }

            val player: Player = instance.server.onlinePlayers.firstOrNull() ?: run {
                instance.logger.warning(SERVER_LIST_NO_PLAYERS)
                return
            }

            val output = newDataOutput()
            output.writeUTF("ServerListRequest")
            player.sendPluginMessage(instance, PLUGIN_CHANNEL, output.toByteArray())

            delay(1000)
        }

        private fun setServerList(serverList: List<String>) {
            instance.server.scheduler.runTask(instance) { cachedServerList = serverList }
        }

        private fun sendProxyCommand(command: String): Boolean {
            if (proxyType == ProxyType.NONE) {
                instance.logger.warning(SEND_COMMAND_NO_PROXY)
                return false
            }

            val output = newDataOutput().apply {
                writeUTF("command")
                writeUTF(command)
            }

            val player = instance.server.onlinePlayers.firstOrNull() ?: run {
                instance.logger.warning(SEND_COMMAND_NO_PLAYERS)
                return false
            }

            val channel = proxyType.channel ?: run {
                instance.logger.warning("")
                return false
            }

            try {
                player.sendPluginMessage(instance, channel, output.toByteArray())
                return true
            } catch (ex: Exception) {
                instance.logger.log(Level.WARNING, "Failed to send command to proxy via channel '$channel'", ex)
                return false
            }
        }

        private fun Collection<String>.stringFilter(query: String): List<String> = filter {
            if (query.length <= 1) it.startsWith(query, true)
            else it.contains(query, true)
        }

        private inline fun <T> Collection<T>.stringFilter(query: String, task: (T) -> String?): List<String> {
            val result = mutableListOf<String>()
            for (it in this) {
                val it = task(it) ?: continue
                if (query.length <= 1) { if (it.startsWith(query, true)) result.add(it) }
                else { if (it.contains(query, true)) result.add(it) }
            }
            return result
        }
    }
}