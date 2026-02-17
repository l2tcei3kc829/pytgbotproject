package core.utils

import arc.files.Fi
import core.bot.markups.MarkupManager
import arc.util.Time
import arc.util.serialization.Base64Coder
import arc.util.serialization.JsonReader
import core.Vars
import org.json.JSONObject
import org.telegram.telegrambots.meta.api.methods.ParseMode
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText
import org.telegram.telegrambots.meta.api.objects.Message
import org.telegram.telegrambots.meta.api.objects.Update
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest

fun Update.getChatId(): Long? =
    callbackQuery?.message?.chatId ?: message?.chatId

fun Update.sendMessage(text: String) {
    val chatId = getChatId() ?: return
    try {
        Vars.bot.execute(SendMessage(chatId.toString(), text))
    } catch (e: Exception) {
        Vars.log.error("Exception", e)
    }
}

fun Update.editMessage(text: String) {
    val chatId = callbackQuery?.message?.chatId ?: return
    val messageId = callbackQuery?.message?.messageId ?: return
    try {
        Vars.bot.execute(
            EditMessageText.builder()
                .chatId(chatId.toString())
                .messageId(messageId)
                .text(text)
                .build()
        )
    } catch (e: Exception) {
        Vars.log.error("Exception", e)
    }
}

fun Update.showMenu(manager: MarkupManager, parent: String) {
    manager.showMenu(this, parent)
}

fun Message.reply(text: String) {
    val chatId = chatId ?: return
    try {
        Vars.bot.execute(SendMessage(chatId.toString(), text).apply { parseMode = ParseMode.MARKDOWN })
    } catch (e: Exception) {
        Vars.log.error("Exception", e)
    }
}

fun preHandleCallback(update: Update): Boolean {
    if (update.hasCallbackQuery() && update.callbackQuery.data.startsWith("SNGMSGDLT_")) {
        val recv = update.callbackQuery.data.removePrefix("SNGMSGDLT_")
        val data = singleStorage[recv]
        data ?: return false
        singleStorage.remove(recv)
        Vars.bot.execute(DeleteMessage.builder()
            .chatId(data[0])
            .messageId(data[1].toInt())
            .build())
        return true
    }
    return false
}


val singleStorage = mutableMapOf<String, List<String>>()
fun Message.replyWithClose(text: String): Message? {
    val chatId = chatId ?: return null
    val keyboard = mutableListOf<List<InlineKeyboardButton>>()
    val uuid = Time.millis().toString() + "L"
    keyboard.add(listOf(InlineKeyboardButton.builder()
        .text("❌ Закрыть")
        .callbackData("SNGMSGDLT_$uuid")
        .build()))
    val markup = InlineKeyboardMarkup.builder().keyboard(keyboard).build()
    try {
        val msg = Vars.bot.execute(SendMessage.builder()
            .text(text)
            .chatId(chatId.toString())
            .replyMarkup(markup)
            .parseMode("Markdown")
            .build())
        singleStorage[uuid] = listOf(msg.chatId.toString(), msg.messageId.toString())
        return msg
    } catch (e: Exception) {
        Vars.log.error("Exception", e)
    }
    return null
}
fun Message.deleteMessage() {
    val chatId = chatId ?: return
    Vars.bot.execute(DeleteMessage.builder().chatId(chatId.toString()).messageId(messageId).build())
}
fun Message.editMessage(text: String) {
    val chatId = chatId ?: return
    val getBuilder = { EditMessageText.builder().chatId(chatId.toString()).messageId(messageId).replyMarkup(this.replyMarkup).text(text) }
    try {
        Vars.bot.execute(getBuilder().parseMode("Markdown").build())
    }catch (_: Exception) {
        Vars.bot.execute(getBuilder().build())
    }
}

fun String.atob(): ByteArray = Base64Coder.decode(this)

fun ByteArray.utf8(): String = String(this, Charsets.UTF_8)
fun msg(text: String): List<MutableMap<String, String>> = listOf(mutableMapOf("role" to "user", "content" to text))
fun String.uri(): URI = URI.create(this)
fun String.toBody(): HttpRequest.BodyPublisher = HttpRequest.BodyPublishers.ofString(this)