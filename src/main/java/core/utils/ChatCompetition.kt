package core.utils

import arc.util.serialization.JsonReader
import org.json.JSONObject
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse

class ChatCompetition(
    private val apiKey: String,
    private val model: String
) {
    private val conversation = mutableListOf<MutableMap<String, String>>()
    private val client = HttpClient.newBuilder()
        .build()

    fun getResponse(messages: List<MutableMap<String, String>>, withContext: Boolean = true): List<String> {
        val target = if (withContext) {
            conversation.addAll(messages)
            conversation
        } else {
            messages
        }
        val data = JSONObject(mapOf(
            "model" to model,
            "messages" to target
        )).toString()
        val request = HttpRequest.newBuilder()
            .uri("https://openrouter.ai/api/v1/chat/completions".uri())
            .header("Content-Type", "application/json")
            .header("Authorization", "Bearer $apiKey")
            .POST(data.toBody())
            .build()
        val response = client.send(request, HttpResponse.BodyHandlers.ofString())?.body()
        val jsonResponse = JsonReader().parse(response)
        val message = jsonResponse["choices"][0]["message"]
        val content = message["content"].asString()
        val reasoning = message["reasoning"]?.asString() ?: ""
        if (withContext) {
            conversation.add(
                mutableMapOf("role" to "assistant", "content" to content)
            )
        }
        return listOf(content, reasoning)
    }
}