package dev.csse.cbjl.slo_n_study

data class StudySpot(
    val name: String,
    val lat: Double,
    val lon: Double,
    val address: String?,
    val hasWifi: Boolean,
    val hasOutdoorSeating: Boolean,
    val hasPower: Boolean,
    val amenity: String?
)

