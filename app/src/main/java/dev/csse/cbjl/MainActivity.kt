package dev.csse.cbjl.slo_n_study

import android.content.Intent
import android.net.Uri
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
import androidx.compose.material.icons.filled.FavoriteBorder
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
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

        val repository = FavoritesRepository(applicationContext)

        setContent {
            Slo_n_studyApp(repository)
        }
    }
}

@Composable
fun Slo_n_studyApp(repository: FavoritesRepository) {

    val favoritesViewModel: FavoritesViewModel = viewModel(
        factory = FavoritesViewModel.factory(repository)
    )
    var currentDestination by rememberSaveable { mutableStateOf(AppDestinations.HOME) }
    var searchText by rememberSaveable { mutableStateOf("") }
    var selectedSpot by remember { mutableStateOf<StudySpot?>(null) }
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
        when (currentDestination) {
            AppDestinations.FAVORITES -> {
                FavoritesScreen(favoritesViewModel = favoritesViewModel)
            }

            AppDestinations.PROFILE -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Profile coming soon", color = WarmGray)
                }
            }

            AppDestinations.HOME -> {
                Scaffold(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(CoffeeCream),
                    topBar = {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(CoffeeCream)
                                .windowInsetsPadding(WindowInsets.statusBars)
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
                                modifier = Modifier.fillMaxWidth(),
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
                                                        Text(text = spot.name, color = CoffeeMocha)
                                                        Text(
                                                            text = spot.amenity ?: "",
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
                    }

                    if (selectedSpot != null) {
                        StudySpotBottomSheet(
                            spot = selectedSpot!!,
                            favoritesViewModel = favoritesViewModel,
                            onDismiss = { selectedSpot = null }
                        )
                    }
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
                    val box = projection.boundingBox
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
            if (studySpots.isEmpty()) return@AndroidView

            mapView.overlays.removeIf { it is Marker }

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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudySpotBottomSheet(
    spot: StudySpot,
    favoritesViewModel: FavoritesViewModel,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState()
    val context = LocalContext.current
    val favorites by favoritesViewModel.favorites.collectAsState()
    val isFav = favorites.any { it.name == spot.name && it.lat == spot.lat && it.lon == spot.lon }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color.White
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = spot.name,
                    style = MaterialTheme.typography.headlineSmall,
                    color = CoffeeMocha,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = { favoritesViewModel.toggleFavorite(spot) }) {
                    Icon(
                        imageVector = if (isFav) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = if (isFav) "Remove from favorites" else "Add to favorites",
                        tint = CoffeeMocha
                    )
                }
            }

            if (spot.address != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = spot.address, color = WarmGray)
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (spot.hasWifi) {
                Text("Free Wi-Fi")
                Spacer(modifier = Modifier.height(6.dp))
            }
            if (spot.hasPower) {
                Text("Power outlets")
                Spacer(modifier = Modifier.height(6.dp))
            }
            if (spot.hasOutdoorSeating) {
                Text("Outdoor seating")
                Spacer(modifier = Modifier.height(6.dp))
            }
            if (!spot.hasWifi && !spot.hasPower && !spot.hasOutdoorSeating) {
                Text("📖 Study-friendly space")
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    onDismiss()
                    val gmmIntentUri = Uri.parse("google.navigation:q=${spot.lat},${spot.lon}")
                    val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri)
                    try {
                        context.startActivity(mapIntent)
                    } catch (_: Exception) {
                        val fallbackUri = Uri.parse("geo:${spot.lat},${spot.lon}?q=${spot.lat},${spot.lon}(${spot.name})")
                        context.startActivity(Intent(Intent.ACTION_VIEW, fallbackUri))
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp)
            ) {
                Text("Get Directions")
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}