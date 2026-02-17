package core.config

import arc.files.Fi
import arc.util.serialization.JsonReader

data class Config(val botToken: String, val apiKey: String, val model: String)


fun loadConfig(file: Fi): Config = JsonReader().parse(file).let {
    Config(it.getString("botToken"), it.getString("apiKey"), it.getString("model"))
}