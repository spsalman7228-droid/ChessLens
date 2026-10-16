# ChessLens ♞

**ChessLens** is an Android visual chess learning and analysis application. It detects chessboards directly on your screen (video streams, recordings, browser tabs, screenshots, diagrams, and photos of physical boards) and displays multi-level engine arrows, move quality ratings, and plain-language pedagogical explanations.

---

## 🌟 Key Features

1. **"On The Screen" Live Overlay & Snapshot**
   - **Live Screen Overlay:** Draws colored engine arrows directly over any video or chess app on screen.
   - **Snapshot Mode:** One tap on the floating bubble or Quick Settings tile analyzes the paused screen with minimal battery consumption.
   - **Photo & Image Import:** Analyze screenshots or camera photos with 4-corner perspective correction.

2. **Multi-Level Engine Analysis & Colored Arrows**
   - **Level 1 (Blue #38bdf8, Depth 5):** Instant tactical moves.
   - **Level 2 (Teal #2dd4bf, Depth 10):** Solid club player depth.
   - **Level 3 (Green #4ade80, Depth 15):** Deep positional calculation.
   - **Database (Lichess Blue #5aa4ff):** Master opening explorer.
   - **Moves to Avoid (Red #f87171):** Flags tempting but blunderous candidate moves with explanations.
   - **Arrow Styling:** Solid, dashed, curved knight arrows, and multi-level agreement badges (e.g. `x3`).

3. **Plain-Language Explanations**
   - Generates natural English explanations for every recommended move (e.g., *"Wins a knight with favorable trade"*, *"Delivers back-rank checkmate"*, *"Defends the hanging bishop on e7"*).
   - Adjustable explanation detail: **Beginner**, **Intermediate**, and **Advanced**.

4. **Think-First Learning Mode**
   - Promotes active calculation: Hides engine arrows until you tap **Reveal** or guess your move on the board. Compares your guess against the top engine lines with an evaluation score.

5. **Video Analysis & Scrubber Timeline**
   - Scrubber with markers at every detected move.
   - Interactive centipawn evaluation graph over time.
   - Key moments list: Blunders, Inaccuracies, Missed Wins, and Brilliant moves.
   - One-tap export to annotated PGN with timestamps.

6. **Interactive Analysis Studio & Puzzles**
   - Full analysis board with "Try a Move" sandbox and "Looks wrong? Fix it" piece editor.
   - Curated tactical puzzles with hints and step-by-step solutions.

7. **Privacy & Fair Play**
   - **Zero Cloud Storage:** All screen pixels and video frames are processed exclusively in device RAM and never saved or transmitted.
   - **Opt-In App Allowlist:** Overlay activates only in apps you select (e.g. YouTube, Browser).
   - **Strict Fair Play Notice:** Explicit onboarding acknowledgment ensuring the tool is used exclusively for study, review, and learning.

---

## 🛠 Tech Stack

- **Platform:** Android (minSdk 26, targetSdk 35)
- **Language:** Kotlin 2.2.10
- **UI:** Jetpack Compose with Material Design 3 (M3)
- **Architecture:** MVVM + Clean Architecture + Repository Pattern
- **Persistence:** Jetpack DataStore (Preferences) + Room (Database)
- **Computer Vision:** Board detection with contrast profiling and perspective homography
- **Engine:** Pure-Kotlin / Stockfish JNI minimax search with alpha-beta pruning and quiescence search
- **Theming:** Dark theme by default, with AMOLED Pure Black and Material You Dynamic Color

---

## 🚀 Build and Run Instructions

### Prerequisites
- Android Studio Ladybug / Meerkat or later
- Android SDK 35+
- JDK 17 or 21

### Building from Command Line
```bash
# Compile debug APK
gradle :app:assembleDebug

# Run unit tests
gradle :app:testDebugUnitTest
```

---

## 📋 Google Play Accessibility API Declaration

For review submission on Google Play Console:

**Accessibility API Usage Statement:**
> "ChessLens uses the Android AccessibilityService API strictly to detect chessboards displayed on the user's screen upon user request (or within user-allowlisted video and media apps) and to render educational analysis arrows over the board. Screen pixels are processed ephemerally in device memory (RAM) and are NEVER saved, stored, or transmitted over any network. Users can enable or disable this permission at any time in Android Settings."

---

## 🎬 Google Play Demo Video Script (30 seconds)

1. **[0:00 - 0:08]** User opens a chess video on YouTube. Taps the ChessLens floating bubble.
2. **[0:09 - 0:17]** ChessLens detects the board on screen and draws blue, teal, and green arrows showing candidate moves, plus an evaluation bar (+1.4).
3. **[0:18 - 0:24]** User expands the Result Card: sees "Defends the hanging bishop on d6" and "Avoid: loses queen to fork on f2".
4. **[0:25 - 0:30]** User opens the in-app Analysis Studio to test moves and export the position to PGN.

---

## ⚖️ Open Source & Third-Party Licenses

- **Stockfish:** Licensed under the GNU General Public License v3 (GPLv3). Source code is available at [https://github.com/official-stockfish/Stockfish](https://github.com/official-stockfish/Stockfish).
- **OpenCV Android:** Licensed under the Apache License 2.0.
- **TensorFlow Lite:** Licensed under the Apache License 2.0.
