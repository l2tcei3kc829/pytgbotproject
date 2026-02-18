package core.bot.listeners

import arc.util.Time
import core.Vars
import core.info.findTermByText
import core.utils.ChatCompetition
import core.utils.*
import org.telegram.telegrambots.meta.api.objects.Message
import kotlin.concurrent.thread

val competitions = mutableMapOf<Long, ChatCompetition>()
val cache = mutableMapOf<String, String>()
fun onMessage(message: Message) {
    if (message.text.startsWith("/start", ignoreCase = true)) {
        if (message.text.length > 6 && message.text.contains("getinfo_")) {
            val targetId = message.text.substringAfter("getinfo_")
            Vars.dicts?.forEach { e ->
                val target = e.entries.find { it.second == targetId }
                prepareCallback(message, target?.first, e.name, targetId)
            }
            message.deleteMessage()
        }
        else Vars.manager.showMenu(message.chatId, null, "root")
    } else if(!message.text.startsWith("/")) {
        var flag = false
        Vars.dicts?.forEach { e ->
            if (flag) return@forEach
            val target = e.entries.find { it.first == message.text }
            target ?: return@forEach
            prepareCallback(message, target.first, e.name)
            flag = true
        }
        if (!flag){
            val text = message.text
            val term = findTermByText(text)
            val resp = if (term != null) "*${term.name}*\n\n${term.desc}\nПример:\n```\n${term.example}\n```" else
                "Не найдено. Возможно вы имели в виду что-либо из этого:\n${
                    mutableListOf<String>().apply { Vars.dicts?.each { it.entries.forEach { pair -> add(pair.first) }} }
                        .apply { sortBy { levenshteinDst(it, text) } }
                        .subList(0, 6)
                        .joinToString(separator = ", ") { "`$it`" }
                    
                }"
            message.reply(resp)
        }
    }
}

private fun prepareCallback(message: Message, target: String?, module: String, id: String? = null) {
    val cached = cache["$module:$target"]
    if (cached != null) {
        message.replyWithClose(cached)
        return
    }
    if (target != null) {
        val competition = competitions.getOrPut(message.chatId) {
            ChatCompetition(
                Vars.config.apiKey,
                Vars.config.model
            ) // maybe i should add context here
        }
        val msg = message.replyWithClose("⏳ Ожидайте ответа...")
        thread(isDaemon = true) {
            val resp = try {
                val start = Time.millis()
                competition.getResponse(msg("Опиши функцию (или класс) \"$target\" из Python из модуля \"$module\", приведи недлинный пример кода. Лимит 2000 символов. Ответь только недлинным описанием функции, без вводных слов. Используй Markdown только для блоков кода и названия функции. Разрешено использовать искключительно следующий Markdown: `text`\n```python\ntext\n```\n На русском языке."), withContext = false)[0]
                    .also {
                        cache["$module:$target"] = it
                        val elapsed = Time.millis() - start
                        Vars.log.info("AI request completed for {} ms, chatId={}", elapsed, message.chatId)
                    }
            } catch (e: Exception) {
                Vars.log.error("AI request failed", e)
                if (id != null) Vars.id2desc.get(id)
                else "Не найдено"
            }
            msg?.editMessage(resp)
        }
    }
}