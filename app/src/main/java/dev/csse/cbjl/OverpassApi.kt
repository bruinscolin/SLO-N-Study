package dev.csse.cbjl.slo_n_study

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

suspend fun fetchStudySpots(
    south: Double,
    west: Double,
    north: Double,
    east: Double
): List<StudySpot> = withContext(Dispatchers.IO) {

    val query = """
        [out:json];
        (
          node["amenity"="cafe"]($south,$west,$north,$east);
          node["amenity"="library"]($south,$west,$north,$east);
        );
        out;
    """.trimIndent()

    try {
        val url = URL("https://overpass-api.de/api/interpreter")
        val connection = url.openConnection() as HttpURLConnection

        connection.requestMethod = "POST"
        connection.doOutput = true

        connection.outputStream.use {
            it.write(query.toByteArray())
        }

        val response = connection.inputStream.bufferedReader().readText()

        val json = JSONObject(response)
        val elements = json.getJSONArray("elements")

        val spots = mutableListOf<StudySpot>()

        for (i in 0 until elements.length()) {

            val element = elements.getJSONObject(i)

            val lat = element.getDouble("lat")
            val lon = element.getDouble("lon")

            val tags = element.optJSONObject("tags")

            val name = tags?.optString("name") ?: "Study Spot"
            val amenity = tags?.optString("amenity")

            val wifi = tags?.optString("internet_access") == "wlan"
            val power = tags?.optString("socket") != null
            val outdoor = tags?.optString("outdoor_seating") == "yes"

            spots.add(
                StudySpot(
                    name = name,
                    lat = lat,
                    lon = lon,
                    amenity = amenity,
                    address = null,
                    hasWifi = wifi,
                    hasPower = power,
                    hasOutdoorSeating = outdoor
                )
            )
        }

        connection.disconnect()

        return@withContext spots

    } catch (e: Exception) {
        e.printStackTrace()
        return@withContext emptyList()
    }
}