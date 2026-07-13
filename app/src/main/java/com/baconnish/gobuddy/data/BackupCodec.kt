package com.baconnish.gobuddy.data

import com.baconnish.gobuddy.data.db.TrackedPokemon
import org.json.JSONArray
import org.json.JSONObject

object BackupCodec {

    data class Backup(val settings: TrainerSettings, val pokemon: List<TrackedPokemon>)

    fun encode(settings: TrainerSettings, pokemon: List<TrackedPokemon>): String {
        val root = JSONObject()
        root.put("version", 1)
        root.put(
            "settings",
            JSONObject()
                .put("trainerLevel", settings.trainerLevel)
                .put("heartsPerDay", settings.heartsPerDay)
                .put("kmPerDay", settings.kmPerDay),
        )
        val array = JSONArray()
        pokemon.forEach { p ->
            array.put(
                JSONObject().apply {
                    put("id", p.id)
                    put("nickname", p.nickname)
                    put("speciesName", p.speciesName)
                    p.speciesDex?.let { put("speciesDex", it) }
                    put("kmPerCandy", p.kmPerCandy)
                    put("currentLevel", p.currentLevel)
                    put("targetLevel", p.targetLevel)
                    put("form", p.form.name)
                    put("isLucky", p.isLucky)
                    put("isCurrentBuddy", p.isCurrentBuddy)
                    put("hearts", p.hearts)
                    put("wantBestBuddy", p.wantBestBuddy)
                    put("candyOwned", p.candyOwned)
                    put("candyXlOwned", p.candyXlOwned)
                    p.ivAtk?.let { put("ivAtk", it) }
                    p.ivDef?.let { put("ivDef", it) }
                    p.ivSta?.let { put("ivSta", it) }
                    put("createdAt", p.createdAt)
                    put("priority", p.priority)
                    p.hyperTrainingStat?.let { put("hyperTrainingStat", it) }
                },
            )
        }
        root.put("pokemon", array)
        return root.toString(2)
    }

    fun decode(json: String): Backup {
        val root = JSONObject(json)
        val s = root.getJSONObject("settings")
        val settings = TrainerSettings(
            trainerLevel = s.optInt("trainerLevel", 40),
            heartsPerDay = s.optInt("heartsPerDay", 12),
            kmPerDay = s.optDouble("kmPerDay", 5.0),
        )
        val array = root.getJSONArray("pokemon")
        val pokemon = buildList(array.length()) {
            for (i in 0 until array.length()) {
                val o = array.getJSONObject(i)
                add(
                    TrackedPokemon(
                        id = o.optLong("id", 0),
                        nickname = o.optString("nickname", ""),
                        speciesName = o.getString("speciesName"),
                        speciesDex = if (o.has("speciesDex")) o.getInt("speciesDex") else null,
                        kmPerCandy = o.optDouble("kmPerCandy", 3.0),
                        currentLevel = o.optDouble("currentLevel", 20.0),
                        targetLevel = o.optDouble("targetLevel", 50.0),
                        form = PokemonForm.entries.firstOrNull { it.name == o.optString("form") }
                            ?: PokemonForm.NORMAL,
                        isLucky = o.optBoolean("isLucky", false),
                        isCurrentBuddy = o.optBoolean("isCurrentBuddy", false),
                        hearts = o.optInt("hearts", 0),
                        wantBestBuddy = o.optBoolean("wantBestBuddy", true),
                        candyOwned = o.optInt("candyOwned", 0),
                        candyXlOwned = o.optInt("candyXlOwned", 0),
                        ivAtk = if (o.has("ivAtk")) o.getInt("ivAtk") else null,
                        ivDef = if (o.has("ivDef")) o.getInt("ivDef") else null,
                        ivSta = if (o.has("ivSta")) o.getInt("ivSta") else null,
                        createdAt = o.optLong("createdAt", 0),
                        priority = o.optLong("priority", 0),
                        hyperTrainingStat = if (o.has("hyperTrainingStat")) o.getString("hyperTrainingStat") else null,
                    ),
                )
            }
        }
        return Backup(settings, pokemon)
    }
}
