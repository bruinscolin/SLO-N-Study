package dev.csse.cbjl.slo_n_study

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

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

    val url =
        "https://overpass-api.de/api/interpreter?data=${URLEncoder.encode(query, "UTF-8")}"

    val connection = URL(url).openConnection() as HttpURLConnection
    connection.requestMethod = "GET"

    val response = connection.inputStream.bufferedReader().readText()
    val json = JSONObject(response)
    val elements = json.getJSONArray("elements")

    val spots = mutableListOf<StudySpot>()

    for (i in 0 until elements.length()) {
        val el = elements.getJSONObject(i)
        val tags = el.optJSONObject("tags") ?: continue

        val hasWifi =
            tags.optString("internet_access") == "wlan" ||
                    tags.optString("internet_access") == "yes"

        val hasOutdoorSeating =
            tags.optString("outdoor_seating") == "yes"

        val hasPower =
            tags.optString("power_supply") == "yes" ||
                    tags.optString("socket") == "yes"

        val amenity = tags.optString("amenity", null)

        val name = tags.optString("name", null) ?: continue
        val lat = el.getDouble("lat")
        val lon = el.getDouble("lon")
        val address = tags.optString("addr:street", null)

        spots.add(
            StudySpot(
                name = name,
                lat = lat,
                lon = lon,
                address = address,
                hasWifi = hasWifi,
                hasOutdoorSeating = hasOutdoorSeating,
                hasPower = hasPower,
                amenity = amenity
            )
        )

    }

    spots
}
