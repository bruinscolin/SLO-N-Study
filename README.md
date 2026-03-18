# SLO-N-Study
App that provides ideal study locations for students in San Luis Obispo, CA. Our app has search functionality at the top, that lets you look for certain spots you would want to go to around town. By adding a spot to the "Favorites" tab, you can easily find locations that you like to frequest, and easily access them again. When you decide on a spot you want to go to, click on the "Get Directions" button to automatically be on the way to your destination.

---

## Design Mockups
Example:
- `design-sketches/home.jpg`
- `design-sketches/map.jpg`
- `design-sketches/details.jpg`

![Home Sketch](design-sketches/home.jpg)
![Map Sketch](design-sketches/map.jpg)
![Details Sketch](design-sketches/details.jpg)

--- 

## Features Used

This app uses several Android and Jetpack Compose features:

### Android Features
- **Location services** to help show study spots based on the user’s area
- **Internet/network access** to load map data and nearby locations
- **Device GPS/location permissions**
- **Intent-based directions** so users can open navigation to a selected study spot

### Jetpack Compose Features
- **Composable UI** for building the app screens
- **State management** to update the UI when locations load or change
- **Navigation** between screens
- **Material Design components** for buttons, cards, and layout
- **Lazy lists / dynamic UI elements** to show place information cleanly

### Map / Data Features
- **OSMdroid** for displaying the interactive map
- **Overpass API** to retrieve nearby cafes, libraries, and other study-related locations
- **Custom data model** for storing and displaying study spot information

### Third-Party Libraries / Tools
- **OSMdroid** for OpenStreetMap support
- **Overpass API** for location data
- Any other libraries you used, such as:
  - Coil
  - Retrofit
  - Coroutines
  - Lifecycle/ViewModel
  - Accompanist

--- 
## Above and Beyond / Additional Notes

Some parts of this project that went beyond the basic requirements include:

- Integrating a real map instead of using a static layout
- Pulling study spot data from external sources
- Making the app useful for real students in San Luis Obispo
- Combining location-based features with a clean Jetpack Compose interface
- Supporting directions/navigation to a selected study location

---
## Authors

- [Julianne Legados]
- [Colin Bruins]
