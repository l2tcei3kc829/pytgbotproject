package core

import arc.mock.MockFiles
import arc.util.Time
import core.bot.markups.MarkupManager
import core.bot.BotCore
import core.bot.markups.load
import core.config.loadConfig
import core.info.loadFromFile
import core.info.loadFromString
import core.utils.getFunctionsJson
import org.telegram.telegrambots.meta.TelegramBotsApi
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession

fun main() {
    Vars.log.info("app started")
    try {
        val start = Time.millis()
        val files = MockFiles()
        Vars.config = loadConfig(files.internal("config.json"))
        val api = TelegramBotsApi(DefaultBotSession::class.java)
        val bot = BotCore()
        api.registerBot(bot)
        Vars.bot = bot
        Vars.manager = MarkupManager()
        try {
            loadFromString(getFunctionsJson())
        } catch (why: Exception) {
            loadFromFile(files.internal("dictonary.json"))
        }
        Vars.manager.setupDefaultCallbacks()
        load(Vars.manager)
        Vars.log.info("bot started at {}", Time.millis())
        Vars.log.info("Total time to launch: {}ms", Time.millis() - start)
    } catch (err: Exception) {
        err.printStackTrace()
    }
}