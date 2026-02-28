package dev.csse.cbjl.slo_n_study

import android.os.Bundle
import android.view.MotionEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBox
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import dev.csse.cbjl.slo_n_study.ui.theme.CoffeeCream
import dev.csse.cbjl.slo_n_study.ui.theme.CoffeeMocha
import dev.csse.cbjl.slo_n_study.ui.theme.WarmGray
import kotlinx.coroutines.launch
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        Configuration.getInstance().userAgentValue = packageName

        setContent {
            Slo_n_studyApp()
        }
    }
}

@Composable
fun Slo_n_studyApp() {

    var currentDestination by rememberSaveable { mutableStateOf(AppDestinations.HOME) }
    var searchText by rememberSaveable { mutableStateOf("") }
    var selectedSpot by rememberSaveable { mutableStateOf<StudySpot?>(null) }
    var studySpots by remember { mutableStateOf<List<StudySpot>>(emptyList()) }

    val suggestions =
        if (searchText.isBlank()) emptyList()
        else studySpots.filter {
            it.name.contains(searchText, ignoreCase = true)
        }.take(5)

    NavigationSuiteScaffold(
        navigationSuiteItems = {
            AppDestinations.entries.forEach {
                item(
                    icon = { Icon(it.icon, contentDescription = it.label) },
                    label = { Text(it.label) },
                    selected = it == currentDestination,
                    onClick = { currentDestination = it }
                )
            }
        }
    ) {

        Scaffold(
            modifier = Modifier
                .fillMaxSize()
                .background(CoffeeCream),
            topBar = {

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 20.dp)
                ) {

                    Text(
                        text = "SLO n Study",
                        style = MaterialTheme.typography.headlineLarge,
                        color = CoffeeMocha,
                        letterSpacing = 1.sp
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    TextField(
                        value = searchText,
                        onValueChange = { searchText = it },
                        placeholder = { Text("Find your perfect study spot") },
                        leadingIcon = {
                            Icon(Icons.Default.Search, contentDescription = null)
                        },
                        modifier = Modifier
                            .fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(40.dp),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color.White,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent
                        )
                    )

                    if (suggestions.isNotEmpty()) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 4.dp),
                            shape = RoundedCornerShape(20.dp)
                        ) {
                            Column {
                                suggestions.forEach { spot ->
                                    DropdownMenuItem(
                                        text = {
                                            Column {
                                                Text(
                                                    spot.name,
                                                    color = CoffeeMocha
                                                )
                                                Text(
                                                    spot.amenity ?: "",
                                                    color = WarmGray,
                                                    fontSize = 12.sp
                                                )
                                            }
                                        },
                                        onClick = {
                                            searchText = spot.name
                                            selectedSpot = spot
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        ) { innerPadding ->

            Box(modifier = Modifier.fillMaxSize()) {

                MapScreen(
                    modifier = Modifier.padding(innerPadding),
                    searchText = searchText,
                    studySpots = studySpots,
                    onSpotsLoaded = { studySpots = it },
                    onSpotSelected = { selectedSpot = it }
                )

                selectedSpot?.let { spot ->
                    StudySpotPreviewCard(
                        spot = spot,
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(start = 16.dp, end = 16.dp, bottom = 88.dp)
                    )
                }
            }
        }
    }
}

enum class AppDestinations(
    val label: String,
    val icon: ImageVector,
) {
    HOME("Home", Icons.Default.Home),
    FAVORITES("Favorites", Icons.Default.Favorite),
    PROFILE("Profile", Icons.Default.AccountBox),
}

@Composable
fun MapScreen(
    modifier: Modifier,
    searchText: String,
    studySpots: List<StudySpot>,
    onSpotsLoaded: (List<StudySpot>) -> Unit,
    onSpotSelected: (StudySpot?) -> Unit
) {

    val scope = rememberCoroutineScope()

    AndroidView(
        modifier = modifier.fillMaxSize(),
        factory = { context ->

            MapView(context).apply {

                setTileSource(TileSourceFactory.MAPNIK)
                setMultiTouchControls(true)

                zoomController.setVisibility(
                    CustomZoomButtonsController.Visibility.SHOW_AND_FADEOUT
                )

                controller.setZoom(14.0)
                controller.setCenter(GeoPoint(35.2828, -120.6596))

                setOnTouchListener { view, event ->
                    if (event.action == MotionEvent.ACTION_DOWN) {
                        onSpotSelected(null)
                        view.performClick()
                    }
                    false
                }

                post {
                    val box = boundingBox
                    scope.launch {
                        val spots = fetchStudySpots(
                            south = box.latSouth,
                            west = box.lonWest,
                            north = box.latNorth,
                            east = box.lonEast
                        )
                        onSpotsLoaded(spots)
                    }
                }
            }
        },

        update = { mapView ->

            val filteredSpots =
                if (searchText.isBlank()) studySpots
                else studySpots.filter {
                    it.name.contains(searchText, ignoreCase = true)
                }

            mapView.overlays.removeAll { it is Marker }

            filteredSpots.forEach { spot ->

                val marker = Marker(mapView).apply {
                    position = GeoPoint(spot.lat, spot.lon)
                    title = spot.name

                    setOnMarkerClickListener { clickedMarker, clickedMap ->
                        clickedMap.controller.animateTo(clickedMarker.position)
                        if (clickedMap.zoomLevelDouble < 16.5) {
                            clickedMap.controller.setZoom(17.0)
                        }
                        onSpotSelected(spot)
                        true
                    }
                }

                mapView.overlays.add(marker)
            }

            mapView.invalidate()
        }
    )
}

@Composable
fun StudySpotPreviewCard(
    spot: StudySpot,
    modifier: Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(Color.White, RoundedCornerShape(32.dp))
            .padding(20.dp)
    ) {

        Text(
            text = spot.name,
            style = MaterialTheme.typography.titleLarge,
            color = CoffeeMocha
        )

        if (spot.address != null) {
            Text(
                text = spot.address,
                color = WarmGray
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (spot.hasWifi) Text("Free Wi-Fi")
        if (spot.hasPower) Text("Power outlets")
        if (spot.hasOutdoorSeating) Text("Outdoor seating")
    }
}
