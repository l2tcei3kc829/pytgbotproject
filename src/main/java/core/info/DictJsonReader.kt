package core.info

import arc.files.Fi;
import arc.util.serialization.JsonReader
import arc.util.serialization.JsonValue
import core.Vars
import java.util.UUID

data class DictPart(val entries: List<Pair<String, String>>, val name: String)

fun loadFromFile(file: Fi) = loadFromJsonObj(JsonReader().parse(file))
fun loadFromJsonObj(obj: JsonValue) {
    val partitions = obj.get("partitions")
    var funcCount = 0
    for (part in partitions) {
        val out = mutableListOf<Pair<String, String>>()
        part.get("funcs").forEach {
            funcCount++
            val randomSeed = UUID.randomUUID().toString()
            out.add(it.getString(0).replace("_", "\\_") to randomSeed)
            Vars.id2desc.put(randomSeed, if(it.size > 2) it.getString(2) else it.getString(1))
        }
        Vars.dicts.add(DictPart(out,part.getString("name")))
    }
    Vars.log.info("Fetched {} functions and {} modules", funcCount, Vars.dicts.size)
}
fun loadFromString(string: String) = loadFromJsonObj(JsonReader().parse(string))