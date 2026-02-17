package core.bot.markups

import core.Vars
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText
import org.telegram.telegrambots.meta.api.objects.Update
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton
import core.utils.showMenu

const val BACK_CALLBACK = "NAV_BACK"
const val HOME_CALLBACK = "NAV_HOME"
class MarkupManager {

    private val callbackHandlers = mutableMapOf<String, (Update) -> Unit>()
    private val menuStructure = mutableMapOf<String, List<MenuItem>>()

    fun setupDefaultCallbacks() {

        onCallback(BACK_CALLBACK) { update ->
            val current = getCurrentMenuFromUpdate(update) ?: "root"
            var parent = current.substringBeforeLast(":", "root")
            if (parent !in menuStructure.keys) parent = "root"
            update.showMenu(this, parent)
        }

        onCallback(HOME_CALLBACK) { update ->
            update.showMenu(this, "root")
        }
    }
    fun showMenu(chatId: Long, messageId: Int?, menuKey: String) {

        val keyboard = createKeyboard(menuKey)
        val title = getMenuTitle(menuKey)

        val text = "$title\n\nВыберите действие:"

        try {
            if (messageId != null) {
                val editMessage = EditMessageText.builder()
                    .chatId(chatId.toString())
                    .messageId(messageId)
                    .text(text)
                    .replyMarkup(keyboard)
                    .build()
                Vars.bot.execute(editMessage)
            } else {
                val sendMessage = SendMessage(chatId.toString(), text)
                sendMessage.replyMarkup = keyboard
                Vars.bot.execute(sendMessage)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    fun showMenu(update: Update, menuKey: String) {
        val chatId = update.callbackQuery?.message?.chatId
            ?: update.message?.chatId
            ?: return

        val messageId = update.callbackQuery?.message?.messageId


        showMenu(chatId, messageId, menuKey)
    }
    fun handleCallback(update: Update) {
        val callbackData = update.callbackQuery.data
        try {
            Vars.bot.execute(
                AnswerCallbackQuery.builder()
                    .callbackQueryId(update.callbackQuery.id)
                    .build()
            )
        } catch (_: Exception) {}
        callbackHandlers[callbackData]?.invoke(update) ?: run {
            try {
                val alert = AnswerCallbackQuery();
                alert.callbackQueryId = update.callbackQuery.id;
                alert.text = "unknown interaction provided: $callbackData";
                Vars.bot.execute(alert)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun onCallback(callbackData: String, handler: (Update) -> Unit) {
        callbackHandlers[callbackData] = handler
    }

    fun addMenu(menuKey: String, builder: MenuBuilder.() -> Unit) {
        val menuBuilder = MenuBuilder()
        menuBuilder.builder()
        menuStructure[menuKey] = menuBuilder.items
    }

    fun createKeyboard(menuKey: String): InlineKeyboardMarkup {
        val items = menuStructure[menuKey] ?: emptyList()
        val rows = mutableListOf<List<InlineKeyboardButton>>()
        items.chunked(2).forEach { chunk ->
            rows.add(
                chunk.map { item ->
                    InlineKeyboardButton.builder()
                        .text(item.text)
                        .callbackData(item.callbackData)
                        .build()
                }
            )
        }
        return InlineKeyboardMarkup.builder().keyboard(rows).build()
    }
    fun getMenuTitle(menuKey: String): String = when (menuKey) {
        "root" -> "Главное меню"
        else -> "Меню"
    }

    private fun getCurrentMenuFromUpdate(update: Update): String? {
        val markup = update.callbackQuery?.message?.replyMarkup
            ?: return null
        return markup.keyboard.flatten().firstOrNull()?.callbackData
    }

    class MenuBuilder {
        val items = mutableListOf<MenuItem>()
        fun row(block: RowBuilder.() -> Unit) {
            val rowBuilder = RowBuilder()
            rowBuilder.block()
            items.addAll(rowBuilder.buttons)
        }
    }

    class RowBuilder {
        val buttons = mutableListOf<MenuItem>()
        fun button(text: String, callbackData: String, action: ((Update) -> Unit)? = null) {
            buttons.add(MenuItem(text, callbackData))
            if (action != null) {
                Vars.manager.onCallback(callbackData, action)
            }
        }
    }

    data class MenuItem(val text: String, val callbackData: String)

}