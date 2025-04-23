# Whereabouts

Whereabouts is a real-time location-sharing Android application built with Kotlin and Jetpack Compose. It enables users to securely share their location with others and view shared locations on a Google Map. This project is designed using the MVVM architecture and integrates Firebase and Google Maps APIs.

## Features

- 📍 **Real-time Location Sharing**
- 🔐 **Firebase Authentication**
- ☁️ **Cloud Firestore Integration**
- 🗺️ **Google Maps Compose for Visualization**
- 📡 **FusedLocationProviderClient for Location Tracking**
- 🎨 **Jetpack Compose UI with Material3 Theming**
- 🧭 **Manage Sharing Preferences**
- 🔄 **State Management with ViewModel and StateFlow**
- ✅ **Runtime Permissions Handling using Accompanist API**

## Screens

- **AuthScreen**: User login and registration using Firebase Authentication
- **HomeScreen**: Displays real-time map with markers for all shared locations
- **ShareScreen**: Allows users to manage who can see their location

## Architecture

- **MVVM Architecture**
- **ViewModel + StateFlow for reactive state management**
- **Separation of concerns between UI, data, and business logic**

## Technologies Used

| Tech                          | Version     |
|------------------------------|-------------|
| Kotlin                       | Jetpack Compose |
| Android SDK                  | 35 (min 25) |
| Firebase Authentication      | Latest      |
| Firebase Firestore           | Latest      |
| Google Maps Compose          | 2.11.4      |
| Google Play Services Location| 21.0.1      |
| Accompanist Permissions      | Latest      |

## Data Models

- **LocationData**: Stores user name, latitude, longitude, and timestamp.
- **Sharing Preferences**: Stored in the "shares" Firestore collection.

## Firebase Configuration

1. Add your Firebase project’s `google-services.json` to the `app/` directory.
2. Enable Firebase Authentication and Firestore.
3. Add your Google Maps API key in `AndroidManifest.xml`.

## Location Tracking

Location updates are managed by `LocationUploader` utility class, which uses `FusedLocationProviderClient` and provides configurable update intervals (5-15 seconds). Proper lifecycle management ensures efficient resource usage.

## Permissions Handling

Uses Accompanist’s permissions API to request and handle location permissions at runtime.

## Getting Started

1. Clone the repository.
2. Add `google-services.json` and your Maps API key.
3. Open in Android Studio and build the project.
4. Run the app on an Android device or emulator.

```bash
https://github.com/Mahith0534/Whereabouts.git
```

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

---
