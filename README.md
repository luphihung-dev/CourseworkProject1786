# M·Hike

<p align="center">
  <img src="app/src/main/res/mipmap-xxxhdpi/ic_launcher_foreground.png" width="120" alt="M-Hike logo" />
</p>

<p align="center">
  <b>Plan, record and share your hikes.</b><br/>
  A hike-management app built twice: once as a native Android app in Java,
  and once as a cross-platform prototype in .NET MAUI.
</p>

---

## Overview

M-Hike lets hikers plan trips in advance, log observations while on the trail
(sightings, trail conditions, photos), and look everything up later with a fast
search. All data is stored locally in SQLite — the app works fully offline,
which is exactly what you want halfway up a mountain.

The repository contains two implementations that share one design language:

| | Native app | Cross-platform prototype |
|---|---|---|
| Location | [`app/`](app) | [`MHike.Maui/`](MHike.Maui) |
| Language | Java | C# |
| UI toolkit | Android Views + Material 3 | .NET MAUI (XAML) |
| Storage | SQLite (`SQLiteOpenHelper`) | SQLite (`sqlite-net-pcl`) |
| Scope | Full feature set | Hike entry + persistence |

## Features

### Native Android app

- **Hike planner** — name, location, date, parking, length, difficulty,
  estimated duration, terrain type and notes, with inline validation and a
  confirmation summary before anything is saved
- **Trail observations** — attach multiple timestamped observations to a hike,
  each with optional comments and a photo taken straight from the camera
- **Search** — live search-as-you-type by name, plus advanced filters for
  location, length range and date
- **Location autofill** — one tap fills the location field from GPS and
  reverse-geocodes it into a readable place name
- **Hike sharing** — send hike details to any app via the system share sheet
- **Living welcome screen** — an animated first-launch intro (drifting scenery
  in sunny mode, real-time rain in rainy mode) that doubles as the light/dark
  theme picker; the theme can be switched any time from the home screen
- **Full CRUD** — edit or delete individual hikes and observations, or reset
  the whole database

### MAUI prototype

- Hike entry form with the same fields, validation rules and confirmation step
- SQLite persistence with list, edit, delete and delete-all
- GPS location autofill and hike sharing via MAUI Essentials

## Tech notes

- **Native**: Material 3 components, adaptive launcher icon, edge-to-edge
  insets handling, `RecyclerView` lists, `FileProvider` for camera photos,
  foreign-key cascade between hikes and observations, and a custom
  `Canvas`-based rain animation view
- **MAUI**: single-project targeting `net9.0-android`, dependency-injected
  database layer, `Geolocation`/`Share` from MAUI Essentials

## Getting started

### Native Android app

1. Open the repository root in **Android Studio** (Ladybug or newer)
2. Let Gradle sync, then run the `app` configuration on a device or emulator
   (min SDK 24)

Or from the command line:

```bash
./gradlew :app:assembleDebug
adb install app/build/outputs/apk/debug/app-debug.apk
```

### MAUI prototype

Requires the .NET 9 SDK with the `maui-android` workload:

```bash
cd MHike.Maui
dotnet build -c Debug -p:EmbedAssembliesIntoApk=true
adb install bin/Debug/net9.0-android/com.luphihung.mhike.maui-Signed.apk
```

> The two apps use different application IDs, so they install side by side.

## Permissions

| Permission | Used for |
|---|---|
| `ACCESS_FINE_LOCATION` | Filling in the hike location from GPS (optional) |
| Camera (via system intent) | Observation photos — no camera permission needed |

## Project structure

```
app/                          Native Android app (Java)
 └─ src/main/java/com/luphihung/mhike/
     ├─ database/             SQLiteOpenHelper + DAOs
     ├─ model/                Hike, Observation
     ├─ adapter/              RecyclerView adapters
     ├─ widget/               Custom views (rain animation)
     └─ util/                 Formatting & insets helpers
MHike.Maui/                   Cross-platform prototype (C# / .NET MAUI)
 ├─ Models/  Data/            Entity + SQLite data layer
 └─ *.xaml                    Pages
```
