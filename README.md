# Marg – Mock route simulator (Android)

Package: `com.rahulbaberwal.marg`

## Run
1. Open this folder in Android Studio (Koala or newer, JDK 17). Let Gradle sync.
2. Run on a real device or emulator (Android 8.0+).
3. On the device: Settings > Developer options > **Select mock location app** > Marg.
4. In Marg: long-press map for Start, long-press again for End, pick Walk / Cycle / Drive, press Start.
5. Open Google Maps or the app under test and watch the moving location.

## Data sources
- Routes: OSRM demo servers hosted by OpenStreetMap Germany (foot, bike, car profiles). Fair-use only.
- Tiles: tile.openstreetmap.org (dev use only).
- For production, self-host OSRM/Valhalla with an India OSM extract and use your own tile source.

## Roadmap
Search (Nominatim), waypoints, GPX import/export, saved routes, speed variation at turns, offline India packs.
