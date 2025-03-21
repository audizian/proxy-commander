package com.xalitoria.commander

import com.xalitoria.commander.Constants.PLUGIN_CHANNEL

enum class ProxyType(val channel: String?) {
    NONE(null),
    BUNGEECORD("BungeeCord"),
    VELOCITY(PLUGIN_CHANNEL)
}