package com.xalitoria.commander

import dev.idot.text.color.convertColorsAndFormat as color

object Constants {
    const val PLUGIN_CHANNEL = "proxycmd:message"
    val FAILED_SYNC_PROXY_MAX_ATTEMPTS = "&cFailed to sync proxy type. Max attempts reached.".color()
    val SEND_COMMAND_NO_PROXY = "&cUnknown proxy type. Cannot send command.".color()
    val SEND_COMMAND_NO_PLAYERS = "&cNo players online. Cannot send command.".color()
    val SERVER_LIST_NO_PROXY = "&cNo proxy detected. Cannot request server list.".color()
    val SERVER_LIST_NO_PLAYERS = "&cNo players online. Cannot request server list.".color()
    val NO_PERMISSION_COMMAND = "&cYou do not have permission to execute this command.".color()
    val CONSOLE_COMMAND_SENDER = "&cThis command can only be executed from the console.".color()
}