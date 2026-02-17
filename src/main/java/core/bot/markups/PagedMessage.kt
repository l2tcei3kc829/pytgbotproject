import core.Vars
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText
import org.telegram.telegrambots.meta.api.objects.Update
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton
import java.util.UUID

class PagedMessage {
    private var chatId: Long = 0L
    constructor(chatId: Long) {
        this.chatId = chatId
    }
    constructor(){
    }

    private var title: String = ""
    private val pages = mutableListOf<String>()
    private var currentPage = 0
    private val instanceId = UUID.randomUUID().toString()

    companion object {
        const val PAGED_PREFIX = "PAGED_"
        const val PREV_PAGE = "PREV"
        const val NEXT_PAGE = "NEXT"
        const val CLOSE = "CLOSE"
    }


    fun setChatId(chatId: Long) {
        this.chatId = chatId
    }

    fun clone() = PagedMessage().also {
        it.chatId = this.chatId
        it.title = this.title
        it.pages.clear()
        it.pages.addAll(this.pages)
    }
    private fun createCallback(action: String): String {
        return "${PAGED_PREFIX}${action}_${instanceId}_$currentPage"
    }

    private fun parseCallback(callbackData: String): Triple<String, String, Int>? {
        if (!callbackData.startsWith(PAGED_PREFIX)) return null

        val parts = callbackData.removePrefix(PAGED_PREFIX).split("_")
        if (parts.size < 3) return null

        val action = parts[0]
        val instanceId = parts[1]
        val page = parts[2].toIntOrNull() ?: 0

        return Triple(action, instanceId, page)
    }

    fun addPage(content: String): PagedMessage {
        pages.add(content)
        return this
    }

    fun addPages(vararg contents: String): PagedMessage {
        pages.addAll(contents)
        return this
    }

    fun setTitle(newTitle: String): PagedMessage {
        title = newTitle
        return this
    }


    fun editWithCallback(content: String, messageId: Int) {
        val keyboard = mutableListOf<List<InlineKeyboardButton>>()
        keyboard.add(listOf(InlineKeyboardButton.builder()
            .text("◀️ Назад")
            .callbackData("EDIT_BACK_CALLBACK")
            .build()))
        val markup = InlineKeyboardMarkup.builder().keyboard(keyboard).build()
        val editMessage = EditMessageText.builder()
            .chatId(chatId.toString())
            .messageId(messageId)
            .text(content)
            .replyMarkup(markup)
            .parseMode("Markdown")
            .build()
        Vars.bot.execute(editMessage)
    }

    fun getPages(): List<String> = pages.toList()

    fun getCurrentPage(): Int = currentPage

    fun getTotalPages(): Int = pages.size

    fun navigateToPage(page: Int): Boolean {
        if (page in 0 until pages.size) {
            currentPage = page
            return true
        }
        return false
    }

    private fun createNavigationKeyboard(): InlineKeyboardMarkup {
        val keyboard = mutableListOf<List<InlineKeyboardButton>>()
        val navigationRow = mutableListOf<InlineKeyboardButton>()
        if (currentPage > 0) {
            navigationRow.add(
                InlineKeyboardButton.builder()
                    .text("◀️ Назад")
                    .callbackData(createCallback(PREV_PAGE))
                    .build()
            )
        }
        if (pages.size > 1) {
            navigationRow.add(
                InlineKeyboardButton.builder()
                    .text("${currentPage + 1}/${pages.size}")
                    .callbackData("${PAGED_PREFIX}INFO_$instanceId")
                    .build()
            )
        }
        if (currentPage < pages.size - 1) {
            navigationRow.add(
                InlineKeyboardButton.builder()
                    .text("Вперед ▶️")
                    .callbackData(createCallback(NEXT_PAGE))
                    .build()
            )
        }
        if (navigationRow.isNotEmpty()) {
            keyboard.add(navigationRow)
        }
        keyboard.add(listOf(
            InlineKeyboardButton.builder()
                .text("❌ Закрыть")
                .callbackData(createCallback(CLOSE))
                .build()
        ))
        return InlineKeyboardMarkup.builder().keyboard(keyboard).build()
    }

    private fun getFormattedMessage(): String {
        val pageContent = pages.getOrNull(currentPage) ?: "Содержимое недоступно"
        return if (title.isNotBlank()) {
            "**$title**\n\n$pageContent"
        } else {
            pageContent
        }
    }

    fun sendTo(chatId: Long, messageId: Int? = null): Int? {
        return try {
            val messageText = getFormattedMessage()
            val keyboard = createNavigationKeyboard()
            if (messageId != null) {
                val editMessage = EditMessageText.builder()
                    .chatId(chatId.toString())
                    .messageId(messageId)
                    .text(messageText)
                    .replyMarkup(keyboard)
                    .parseMode("Markdown")
                    .build()
                val result = Vars.bot.execute(editMessage)
                editMessage.messageId
            } else {
                val sendMessage = SendMessage(chatId.toString(), messageText)
                sendMessage.replyMarkup = keyboard
                sendMessage.parseMode = "Markdown"
                val result = Vars.bot.execute(sendMessage)
                result.messageId
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    
    object Handler {
        private val activeInstances = mutableMapOf<String, PagedMessage>()

        fun handleCallback(update: Update): Boolean {
            val callbackData = update.callbackQuery.data

            if (!callbackData.startsWith(PAGED_PREFIX)) {
                return false
            }

            val parsed = parseCallbackStatic(callbackData) ?: return false
            val (action, instanceId, page) = parsed

            val pagedMessage = activeInstances[instanceId] ?: return false

            try {
                when (action) {
                    "EDIT_BACK_CALLBACK" -> {
                        pagedMessage.sendTo(
                            update.callbackQuery.message.chatId,
                            update.callbackQuery.message.messageId
                        )
                    }
                    PREV_PAGE -> {
                        pagedMessage.currentPage = maxOf(page - 1, 0)
                        pagedMessage.sendTo(
                            update.callbackQuery.message.chatId,
                            update.callbackQuery.message.messageId
                        )
                    }
                    NEXT_PAGE -> {
                        pagedMessage.currentPage = minOf(page + 1, pagedMessage.pages.size - 1)
                        pagedMessage.sendTo(
                            update.callbackQuery.message.chatId,
                            update.callbackQuery.message.messageId
                        )
                    }
                    CLOSE -> {
                        activeInstances.remove(instanceId)
                        Vars.manager.showMenu(update.callbackQuery.message.chatId, update.callbackQuery.message.messageId, "root")
                    }
                }

                
                Vars.bot.execute(
                    AnswerCallbackQuery.builder()
                        .callbackQueryId(update.callbackQuery.id)
                        .build()
                )

            } catch (e: Exception) {
                e.printStackTrace()
            }

            return true
        }

        private fun parseCallbackStatic(callbackData: String): Triple<String, String, Int>? {
            if (!callbackData.startsWith(PAGED_PREFIX)) return null

            val parts = callbackData.removePrefix(PAGED_PREFIX).split("_")
            if (parts.size < 3) return null

            val action = parts[0]
            val instanceId = parts[1]
            val page = parts[2].toIntOrNull() ?: 0
            return Triple(action, instanceId, page)
        }

        fun registerInstance(pagedMessage: PagedMessage) {
            activeInstances[pagedMessage.instanceId] = pagedMessage
        }

        fun cleanupOldInstances() {
        }
    }

    init {
        Handler.registerInstance(this)
    }
}

fun handlePagedMessageCallback(update: Update): Boolean {
    return PagedMessage.Handler.handleCallback(update)
}

