package com.baconnish.gobuddy.data

import android.content.Context
import org.json.JSONArray

data class Species(
    val dex: Int,
    val name: String,
    val kmPerCandy: Double,
    val baseAtk: Int,
    val baseDef: Int,
    val baseSta: Int,
    val family: String,
)

class SpeciesRepository(private val context: Context) {

    val all: List<Species> by lazy {
        val json = context.assets.open("species.json").bufferedReader().use { it.readText() }
        val array = JSONArray(json)
        buildList(array.length()) {
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                add(
                    Species(
                        dex = obj.getInt("dex"),
                        name = obj.getString("name"),
                        kmPerCandy = obj.getDouble("km"),
                        baseAtk = obj.getInt("atk"),
                        baseDef = obj.getInt("def"),
                        baseSta = obj.getInt("sta"),
                        family = obj.optString("family", obj.getString("name")),
                    ),
                )
            }
        }
    }

    fun byName(name: String): Species? =
        all.firstOrNull { it.name.equals(name.trim(), ignoreCase = true) }

    fun search(query: String, limit: Int = 12): List<Species> {
        if (query.isBlank()) return emptyList()
        val q = query.trim()
        return all.asSequence()
            .filter { it.name.contains(q, ignoreCase = true) }
            .sortedBy { !it.name.startsWith(q, ignoreCase = true) }
            .take(limit)
            .toList()
    }
}
