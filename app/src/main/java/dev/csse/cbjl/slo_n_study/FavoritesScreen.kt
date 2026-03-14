package dev.csse.cbjl.slo_n_study

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.csse.cbjl.slo_n_study.ui.theme.CoffeeCream
import dev.csse.cbjl.slo_n_study.ui.theme.CoffeeMocha
import dev.csse.cbjl.slo_n_study.ui.theme.WarmGray
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch


//class FavoritesViewModel : ViewModel() {
//
//    private val _favorites = MutableStateFlow<List<StudySpot>>(emptyList())
//    val favorites: StateFlow<List<StudySpot>> = _favorites.asStateFlow()
//
//    fun addFavorite(spot: StudySpot) {
//        _favorites.update { current ->
//            if (current.none { it.name == spot.name && it.lat == spot.lat && it.lon == spot.lon }) {
//                current + spot
//            } else {
//                current
//            }
//        }
//    }
//
//    fun removeFavorite(spot: StudySpot) {
//        _favorites.update { current ->
//            current.filter { it.name != spot.name || it.lat != spot.lat || it.lon != spot.lon }
//        }
//    }
//
//    fun toggleFavorite(spot: StudySpot) {
//        if (isFavorite(spot)) removeFavorite(spot) else addFavorite(spot)
//    }
//
//    fun isFavorite(spot: StudySpot): Boolean {
//        return _favorites.value.any { it.name == spot.name && it.lat == spot.lat && it.lon == spot.lon }
//    }
//}
class FavoritesViewModel(private val repository: FavoritesRepository) : ViewModel() {

    val favorites: StateFlow<List<StudySpot>> = repository.favoritesFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun addFavorite(spot: StudySpot) {
        viewModelScope.launch {
            val updated = favorites.value.toMutableList()
            if (updated.none { it.name == spot.name && it.lat == spot.lat && it.lon == spot.lon }) {
                updated.add(spot)
                repository.saveFavorites(updated)
            }
        }
    }

    fun removeFavorite(spot: StudySpot) {
        viewModelScope.launch {
            val updated = favorites.value.filter {
                it.name != spot.name || it.lat != spot.lat || it.lon != spot.lon
            }
            repository.saveFavorites(updated)
        }
    }

    fun toggleFavorite(spot: StudySpot) {
        if (isFavorite(spot)) removeFavorite(spot) else addFavorite(spot)
    }

    fun isFavorite(spot: StudySpot): Boolean =
        favorites.value.any { it.name == spot.name && it.lat == spot.lat && it.lon == spot.lon }

    // Factory so the ViewModel can receive the repository via constructor
    companion object {
        fun factory(repository: FavoritesRepository): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    FavoritesViewModel(repository) as T
            }
    }
}
@Composable
fun FavoritesScreen(
    favoritesViewModel: FavoritesViewModel = viewModel()
) {
    val favorites by favoritesViewModel.favorites.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(CoffeeCream)
            .padding(horizontal = 20.dp, vertical = 20.dp)
    ) {
        Text(
            text = "Favorites",
            style = MaterialTheme.typography.headlineLarge,
            color = CoffeeMocha,
            letterSpacing = 1.sp
        )

        Spacer(modifier = Modifier.height(16.dp))

        if (favorites.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.FavoriteBorder,
                        contentDescription = null,
                        tint = WarmGray,
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "No favorites yet",
                        color = WarmGray,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = "Tap the heart on a study spot to save it here.",
                        color = WarmGray,
                        fontSize = 13.sp
                    )
                }
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(favorites, key = { "${it.lat},${it.lon},${it.name}" }) { spot ->
                    FavoriteSpotCard(
                        spot = spot,
                        onRemove = { favoritesViewModel.removeFavorite(spot) }
                    )
                }
            }
        }
    }
}

@Composable
fun FavoriteSpotCard(
    spot: StudySpot,
    onRemove: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = spot.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = CoffeeMocha,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                if (spot.address != null) {
                    Text(
                        text = spot.address,
                        color = WarmGray,
                        fontSize = 13.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                if (spot.amenity != null) {
                    Text(
                        text = spot.amenity,
                        color = WarmGray,
                        fontSize = 12.sp
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (spot.hasWifi) AmenityChip("Wi-Fi")
                    if (spot.hasPower) AmenityChip("Power")
                    if (spot.hasOutdoorSeating) AmenityChip("Outdoor")
                }
            }

            IconButton(onClick = onRemove) {
                Icon(
                    imageVector = Icons.Default.Favorite,
                    contentDescription = "Remove from favorites",
                    tint = CoffeeMocha
                )
            }
        }
    }
}

@Composable
fun AmenityChip(label: String) {
    Surface(
        shape = RoundedCornerShape(50),
        color = CoffeeCream
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            fontSize = 11.sp,
            color = CoffeeMocha
        )
    }
}

// Convenience composable — shows a toggle heart button you can drop anywhere (e.g. on the map card)
@Composable
fun FavoriteToggleButton(
    spot: StudySpot,
    favoritesViewModel: FavoritesViewModel = viewModel()
) {
    val favorites by favoritesViewModel.favorites.collectAsState()
    val isFav = favorites.any { it.name == spot.name && it.lat == spot.lat && it.lon == spot.lon }

    IconButton(onClick = { favoritesViewModel.toggleFavorite(spot) }) {
        Icon(
            imageVector = if (isFav) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
            contentDescription = if (isFav) "Remove from favorites" else "Add to favorites",
            tint = CoffeeMocha
        )
    }
}