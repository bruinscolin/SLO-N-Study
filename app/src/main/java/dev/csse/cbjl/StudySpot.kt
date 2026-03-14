package dev.csse.cbjl.slo_n_study

import kotlinx.serialization.Serializable

@Serializable
data class StudySpot(
    val name: String,
    val lat: Double,
    val lon: Double,
    val amenity: String? = null,
    val address: String? = null,
    val hasWifi: Boolean = false,
    val hasPower: Boolean = false,
    val hasOutdoorSeating: Boolean = false
)

