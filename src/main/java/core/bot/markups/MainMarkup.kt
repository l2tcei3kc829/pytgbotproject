package core.bot.markups

import PagedMessage
import core.Vars
import core.info.DictPart
import core.info.dataset
import core.utils.getChatId
import org.checkerframework.checker.units.qual.m
import java.util.UUID
import kotlin.collections.chunked

fun load(manager: MarkupManager) {
    val embedStorage = mutableMapOf<DictPart, PagedMessage>()
    val botUserName = Vars.bot.me.userName
    Vars.dicts.each { module ->
        val paged = PagedMessage()
            .setTitle("Словарь\n\n*Функции модуля ${module.name.substringAfter(" ")}*")
        module.entries.chunked(4).forEach {
            paged.addPage(it.joinToString("\n") { p ->
                "${p.first}      ([Узнать больше](https://t.me/$botUserName?start=getinfo_${p.second}))"
            })
        }
        embedStorage[module] = paged
        Vars.log.info("Processed module \"{}\"", module.name.substringAfter(' '))
    }
    manager.addMenu("root") {
        Vars.dicts.chunked(2).forEach { chunk ->
            row {
                chunk.forEach { module ->
                    button(module.name, UUID.randomUUID().toString()) { update ->
                        val paged = embedStorage[module]?.clone() ?: return@button // should never happen
                        val id = update.getChatId()!!
                        paged.setChatId(id)
                        paged.sendTo(id, update.callbackQuery.message.messageId)
                    }
                }
            }
        }
        row {
            button("Основные понятия Python",UUID.randomUUID().toString()) { update ->
                val paged = PagedMessage(update.getChatId()!!)
                    .setTitle("Словарь")
                dataset.chunked(3).forEach { chunk ->
                    paged.addPage(chunk.joinToString("\n") {
                        "*${it.name}*\n${it.desc}\n```python\n${it.example}\n```"
                    })
                }
                paged.sendTo(update.getChatId()!!, update.callbackQuery.message.messageId)
            }
        }
    }
}