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
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
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
import kotlinx.coroutines.launch
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import dev.csse.cbjl.slo_n_study.ui.theme.Slo_n_studyTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        Configuration.getInstance().userAgentValue = packageName

        val repository = FavoritesRepository(applicationContext)

        setContent {

            val themeViewModel: ThemeViewModel = viewModel()
            val darkMode by themeViewModel.darkMode.collectAsState()

            Slo_n_studyTheme(
                darkTheme = darkMode
            ) {
                Slo_n_studyApp(repository, themeViewModel)
            }
        }
    }
}

@Composable
fun Slo_n_studyApp(
    repository: FavoritesRepository,
    themeViewModel: ThemeViewModel
) {

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

            AppDestinations.SETTINGS -> {
                SettingsScreen(themeViewModel)
            }

            AppDestinations.HOME -> {
                Scaffold(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background),
                    topBar = {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.background)                                .windowInsetsPadding(WindowInsets.statusBars)
                                .padding(horizontal = 20.dp, vertical = 20.dp)
                        ) {
                            Text(
                                text = "SLO n Study",
                                style = MaterialTheme.typography.headlineLarge,
                                color = MaterialTheme.colorScheme.primary,
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
                                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                                    focusedIndicatorColor = Color.Transparent,
                                    unfocusedIndicatorColor = Color.Transparent
                                )
                            )

                            if (suggestions.isNotEmpty()) {
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 4.dp),
                                    shape = RoundedCornerShape(20.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.surface
                                    )
                                ) {
                                    Column {
                                        suggestions.forEach { spot ->
                                            DropdownMenuItem(
                                                text = {
                                                    Column {
                                                        Text(
                                                            text = spot.name,
                                                            color = MaterialTheme.colorScheme.onSurface
                                                        )
                                                        Text(
                                                            text = spot.amenity ?: "",
                                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
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
                            onSpotSelected = { selectedSpot = it },
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
    SETTINGS("Settings", Icons.Default.Settings),
}
@Composable
fun MapScreen(
    modifier: Modifier,
    searchText: String,
    studySpots: List<StudySpot>,
    onSpotsLoaded: (List<StudySpot>) -> Unit,
    onSpotSelected: (StudySpot?) -> Unit,
) {

    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    AndroidView(
        modifier = modifier.fillMaxSize(),

        factory = { ctx ->

            MapView(ctx).apply {

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

                // Fetch study spots once map is ready
                post {

                    val box = projection.boundingBox

                    scope.launch {

                        val spots = fetchStudySpots(
                            south = box.latSouth,
                            west = box.lonWest,
                            north = box.latNorth,
                            east = box.lonEast
                        )

                        if (spots.isNotEmpty()) {
                            onSpotsLoaded(spots)
                        }
                    }
                }
            }
        },

        update = { map ->

            val filteredSpots =
                if (searchText.isBlank()) studySpots
                else studySpots.filter {
                    it.name.contains(searchText, ignoreCase = true)
                }

            if (filteredSpots.isEmpty()) return@AndroidView

            // Remove existing markers only
            map.overlays.removeAll { it is Marker }

            // Add markers
            filteredSpots.forEach { spot ->

                val marker = Marker(map).apply {

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

                map.overlays.add(marker)
            }

            map.invalidate()
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
        containerColor = MaterialTheme.colorScheme.surface
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
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = { favoritesViewModel.toggleFavorite(spot) }) {
                    Icon(
                        imageVector = if (isFav) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = if (isFav) "Remove from favorites" else "Add to favorites",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            if (spot.address != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = spot.address,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
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
                        val fallbackUri =
                            Uri.parse("geo:${spot.lat},${spot.lon}?q=${spot.lat},${spot.lon}(${spot.name})")
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
