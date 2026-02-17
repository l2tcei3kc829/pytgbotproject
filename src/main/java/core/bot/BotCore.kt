package core.bot

import core.Vars
import core.bot.listeners.onMessage
import core.utils.*
import handlePagedMessageCallback
import org.telegram.telegrambots.bots.TelegramLongPollingBot
import org.telegram.telegrambots.meta.api.objects.Update

class BotCore: TelegramLongPollingBot() {
    override fun getBotToken(): String = Vars.config.botToken

    override fun onUpdateReceived(update: Update) {
        if (update.hasMessage()) {
            onMessage(update.message)
            return
        }
        if (preHandleCallback(update)) return
        if (update.hasCallbackQuery() && handlePagedMessageCallback(update)) {
            return
        }
        Vars.manager?.handleCallback(update)
    }

    override fun getBotUsername() = "Python WordList Bot"
}