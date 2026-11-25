# Marg – App Store Listing (Google Play & PhonePe / Indus Appstore)

**Title (29/30):** Marg: Fake GPS & Route Mocker

**Short description (75/80):**
Fake GPS: simulate drive, walk, cycle & train routes or fixed indoor jitter.

*Alternative Short Descriptions (under 80 chars):*
- `Fake GPS location changer: drive, walk, cycle, train & realistic indoor drift.` (77/80)
- `Fake GPS & route mocker: drive, train, walk & realistic indoor walking jitter.` (77/80)

---

## Full Description

Marg is a realistic fake GPS location changer, route simulator, and indoor presence mocker built for developers, QA engineers, testers, and anyone who needs a believable moving or stationary location on Android.

Simulate driving along highways, walking through footpaths, cycling city streets, or travelling railway tracks on a train. Or stay in one place with **Fixed Location mode**, featuring realistic indoor walking jitter that simulates a person moving naturally between rooms inside a home.

### 🚗 🚶 🚲 🚆 5 SIMULATION MODES
• **Drive (Default):** Realistic road routing for cars, cabs, and delivery vehicles up to 200 km/h.
• **Walk:** Follows pedestrian footpaths, sidewalks, and walking trails at realistic walking speeds.
• **Cycle:** Routes through bike-friendly paths, streets, and trails.
• **Train:** Routes along real OpenStreetMap railway tracks with automatic road fallback.
• **Fixed Location:** Mocks a single stationary location with realistic indoor walking drift—no destination route required.

### 🏠 REALISTIC INDOOR WALKING JITTER
Anti-cheat engines, work-tracking apps, and live location trackers easily detect dead-frozen GPS coordinates or erratic, high-frequency jumping noise. Marg's indoor jitter engine simulates organic human presence:
• **Room-to-room drift:** Alternates naturally between resting in a room (speed 0 m/s, subtle antenna noise) and walking to another room (0.8 to 1.2 m/s human indoor pace).
• **Smooth kinematics:** Continuous velocity with inertia and heading changes—zero teleportation.
• **Adjustable home radius:** Set your indoor wandering boundary from 0 m (strictly fixed point) to 30 m (full house/apartment) using the slider.
• **Authentic GPS metrics:** Feeds realistic indoor accuracy (6m–16m), barometric altitude drift, and Android 8.0+ accuracy metadata (`bearingAccuracy`, `speedAccuracy`, `verticalAccuracy`).

### 📍 MULTI-STOP ROUTE PLANNER & COLLAPSIBLE UI
• **Collapsible ~52dp Route Pill:** Keeps almost 100% of the map visible while planning or traveling (`Start → Destination (N stops)`).
• **Add Intermediate Stops:** Add unlimited stops between start, waypoints, and destination with inline `+ Add stop` buttons.
• **Draggable Markers:** Adjust start, waypoints, and destination pins by dragging them across the map with physical lift shadows and haptic feedback.
• **Instant Re-routing:** Re-routes smoothly when waypoints are added, removed, or dragged, with debounced road snapping.
• **Real-Time Compass:** Floating compass needle rotates in real time with map bearing—tap anytime to smoothly reset orientation to North.

### 🔔 LIVE INTERACTIVE ROUTE NOTIFICATION (Zomato / Delivery-Style)
• **Live Progress Bar:** Pinned at the top of the Android notification drawer with a live progress bar tracking journey completion percentage.
• **Real-Time ETA & Distance:** Shows remaining distance in km, speed in km/h, and live remaining ETA in minutes, updated every 2 seconds.
• **In-Notification Controls:** Pause simulation and hold location with organic jitter, resume movement, or stop anytime without needing to open the app.
• **Dynamic Mode Icon:** Displays mode emoji (`🚗`, `🚶`, `🚴`, `🚆`, `🏠`) and destination details directly in the shade.

### 📍 3 EASY WAYS TO PICK LOCATIONS
• **Place Search:** Search addresses, landmarks, and cities worldwide with instant autocomplete powered by OpenStreetMap Nominatim.
• **Coordinates (Lat / Long):** Type or paste exact decimal coordinates from your clipboard with automatic validation.
• **Map Selection:** Tap or long-press directly on the map.
• **1-Tap Swap:** Instantly reverse Start and Destination points.

### ⚡ COMPLETE SIMULATION CONTROL
• **Speed Control:** Fine-tune speeds from 1 to 200 km/h in real time while moving.
• **Pause at Current Location:** Pausing displays your exact coordinates, completion percentage, and reverse-geocoded place name.
• **Continuous Mock Signal:** While paused or in Fixed mode, Marg continues broadcasting location updates at 1-second intervals with speed = 0 m/s, so apps never drop your GPS lock.
• **Stop on Current Spot:** Tapping Stop while running holds your current position instead of wiping it out. Reset to start whenever you choose.
• **Control Anywhere:** Start, pause, resume, and stop from inside the app or directly from the persistent Android notification.

### 🛡️ PRIVACY FIRST — NO ADS, NO ACCOUNTS
• **No login or sign-up required:** Open the app and start immediately.
• **No ads, no tracking SDKs, no analytics:** We respect your device and battery.
• **No location hoarding:** Your actual physical location is never collected or transmitted.

### 🛠️ HOW TO SET IT UP IN 4 STEPS
1. Enable **Developer options** in your Android Settings (`Settings > About phone > tap "Build number" 7 times`).
2. Go to `Settings > System > Developer options`, tap **Select mock location app**, and choose **Marg**.
3. In Marg, pick **Drive**, **Walk**, **Cycle**, **Train**, or **Fixed**.
4. Set your point(s) using Search, Coordinates, or the Map, and tap **Start**!

### 📱 DOWNLOAD LINKS
• **Google Play Store:** https://play.google.com/store/apps/details?id=com.rahulbaberwal.marg
• **PhonePe Appstore (Indus Appstore):** https://indusapp.store/3y8hrlz0

---

## What's New in Version 2.1.0 (Build 5)
• **Open with Marg (Map Links & Shares):** Click any Google Maps, OpenStreetMap, Apple Maps link or geo: URI in browser/WhatsApp/messages and open directly in Marg with one tap.
• **1-Tap Current GPS Location:** Instantly set Start, Destination, or Fixed location using your device's real GPS before starting simulation.
• **Shareable Route Links:** Share planned routes via a custom link with start, destination, and stops. Opening the link loads the route directly in Marg (or redirects to download if not installed).
• **Pause & Edit Flow:** Pause simulation anytime to add intermediate stops, adjust coordinates, or edit pointers, then resume seamlessly.

---

## What's New in Version 2.0.0 (Build 4)
• **Theme Appearance Switcher:** Switch effortlessly between System (Auto), Light, and Dark modes directly in the app with high-contrast UI tokens.
• **Interactive In-App Feature Guide:** Step-by-step illustrated hints explaining pin hold-and-drop road snapping, multi-stop routing, compass controls, and travel modes.
• **Direct Mock Location Deep-Link:** Tapping Start immediately jumps and highlights "Select mock location app" in Android Developer Options.
• **Real-Time Live Route Tracking Notification:** Track live progress, remaining distance (km), and ETA right from Android's notification shade with instant Pause/Resume/Stop actions.
• **Unified Settings & About Hub:** One-stop settings page combining theme controls, quick guides, developer profile, privacy policy, and open-source licenses.
• **Collapsible Multi-Stop Planner:** Slim 52dp screen footprint with dynamic stop addition and one-tap Done collapse.
• **Android 15 & 16 Ready:** Upgraded to compileSdk 36, edge-to-edge support, and predictive back navigation.

---

**Category:** Tools  
**Tags / Keywords:** fake GPS, mock location, route simulator, GPS spoofer for testing, drive simulator, walk simulator, train route GPS, indoor jitter, GPS drift, mock provider, India, Indus Appstore, developer tools

**Data safety declaration:**
• No personal user account data collected.
• No analytics or third-party ads.
• Coordinates chosen for routing are queried against public OpenStreetMap routing APIs solely for route generation.
• Real physical location of the device is never stored or transmitted.
