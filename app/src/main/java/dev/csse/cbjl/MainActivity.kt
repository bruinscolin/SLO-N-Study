package dev.csse.cbjl.slo_n_study

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBox
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import dev.csse.cbjl.slo_n_study.ui.theme.Slo_n_studyTheme
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import org.osmdroid.views.overlay.Marker
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.ui.Alignment
import android.view.MotionEvent
import org.osmdroid.views.CustomZoomButtonsController


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Configure osmdroid
        Configuration.getInstance().userAgentValue = packageName

        setContent {
            Slo_n_studyTheme {
                Slo_n_studyApp()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Slo_n_studyApp() {
    var currentDestination by rememberSaveable { mutableStateOf(AppDestinations.HOME) }
    var searchText by rememberSaveable { mutableStateOf("") }
    var selectedSpot by rememberSaveable { mutableStateOf<StudySpot?>(null) }


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
                .fillMaxSize(),
            topBar = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 48.dp, start = 16.dp, end = 16.dp, bottom = 16.dp)
                ) {
                    Text(
                        text = "SLO n Study",
                        style = MaterialTheme.typography.headlineMedium,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    TextField(
                        value = searchText,
                        onValueChange = { searchText = it },
                        placeholder = { Text("Search...") },
                        leadingIcon = {
                            Icon(Icons.Default.Search, contentDescription = "Search")
                        },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = TextFieldDefaults.colors(
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            disabledIndicatorColor = Color.Transparent
                        )
                    )
                }
            }
        ) { innerPadding ->
            Box(modifier = Modifier.fillMaxSize()) {

                MapScreen(
                    modifier = Modifier.padding(innerPadding),
                    onSpotSelected = { selectedSpot = it }
                )

                selectedSpot?.let { spot ->
                    StudySpotPreviewCard(
                        spot = spot,
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(
                                start = 16.dp,
                                end = 16.dp,
                                bottom = 88.dp
                            )
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
    modifier: Modifier = Modifier,
    onSpotSelected: (StudySpot?) -> Unit
) {
    val scope = rememberCoroutineScope()

    AndroidView(
        modifier = modifier.fillMaxSize(),
        factory = { context ->
            MapView(context).apply {
                setTileSource(TileSourceFactory.MAPNIK)
                setMultiTouchControls(true)
                setBuiltInZoomControls(true)

                zoomController.setVisibility(
                    CustomZoomButtonsController.Visibility.SHOW_AND_FADEOUT
                )

                controller.setZoom(14.0)
                controller.setCenter(GeoPoint(35.2828, -120.6596))

                setOnTouchListener { _, event ->
                    if (event.action == MotionEvent.ACTION_DOWN) {
                        onSpotSelected(null) // dismiss card
                    }
                    false // IMPORTANT: let MapView keep handling gestures
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

                        spots.forEach { spot ->
                            val marker = Marker(this@apply).apply {
                                position = GeoPoint(spot.lat, spot.lon)
                                title = spot.name
                                setOnMarkerClickListener { marker, mapView ->
                                    // zoom + center smoothly on the marker
                                    mapView.controller.animateTo(marker.position)

                                    val currentZoom = mapView.zoomLevelDouble
                                    if (currentZoom < 16.5) {
                                        mapView.controller.setZoom(17.0)
                                    }

                                    onSpotSelected(spot)
                                    true
                                }
                            }
                            overlays.add(marker)
                        }

                        invalidate()
                    }
                }
            }
        }
    )
}


@Composable
fun StudySpotPreviewCard(
    spot: StudySpot,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp)
            .background(
                color = Color(0xFFEAD7C3), // tan vibe
                shape = RoundedCornerShape(20.dp)
            )
            .padding(16.dp)
    ) {
        Text(
            text = spot.name,
            style = MaterialTheme.typography.titleMedium
        )

        if (spot.address != null) {
            Text(
                text = spot.address,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.DarkGray
            )
        }

        Column(modifier = Modifier.padding(top = 8.dp)) {

            if (spot.hasWifi) {
                Text("• Free Wi-Fi")
            }

            if (spot.hasPower) {
                Text("• Power outlets")
            }

            if (spot.hasOutdoorSeating) {
                Text("• Outdoor seating")
            }

            if (!spot.hasWifi && !spot.hasPower && !spot.hasOutdoorSeating) {
                Text("• Study-friendly space")
            }
        }
    }
}


@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    Slo_n_studyTheme {
        Greeting("Android")
    }
}