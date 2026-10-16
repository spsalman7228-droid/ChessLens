# ChessLens Architecture Documentation

## 1. System Overview

ChessLens is designed as a modular, privacy-preserving chess study and analysis application. It decouples screen reading, computer vision, chess engine evaluation, and user interface rendering into clean layers.

```
[Screen / Photo / Video Frame]
              │
              ▼
    [Vision: BoardDetector]
    - Checkerboard contrast grid locator
    - Perspective rectification & homography
    - Square crop classifier (13 classes)
              │
              ▼ (FEN + Confidence)
    [Core: ChessMoveGenerator]
    - Full legal move generation (castling, en-passant, promotions)
    - Check / checkmate / stalemate detection
    - Move validation & disambiguated SAN formatting
              │
              ▼
    [Engine & Tactical Evaluation]
    - Multi-level minimax alpha-beta with quiescence search
    - Piece-square tables, pawn structure, king safety
    - MultiPV candidate lines + WDL calculations
    - TacticalDetector: natural language pedagogical explanations
    - Moves to avoid: identifies temptation blunders
    - Optional Web Sources: Lichess Cloud Eval & Stockfish.online fallback
              │
              ▼
   [UI & Presentation (Jetpack Compose)]
   - Live Screen Overlay & Floating Draggable Bubble
   - Interactive Analysis Board with dynamic Canvas arrows
   - Video Timeline Scrubber with centipawn eval graph
   - Tactical Puzzles with Think-First guess flow
   - Settings & Per-App Opt-In Allowlist
              │
              ▼
   [Data Persistence]
   - DataStore: Preferences (theme, detail level, allowlist, arrow styles)
   - Room: AnalysisHistoryEntity, HistoryDao, AppDatabase (local-only)
```

## 2. Key Modules

### `com.example.chess`
- `ChessMoveGenerator`: Performs fast, zero-allocation legal move generation and board state updates.
- `EvaluationEngine`: Implements progressive depth analysis (Level 1 Blue, Level 2 Teal, Level 3 Green, Level 4 Purple), centipawn evaluations, and WDL percentages.
- `TacticalDetector`: Analyzes move dynamics (forks, pins, hanging piece captures, king safety, center outposts) and formats beginner-to-advanced explanations.
- `WebAnalysisSource`: Provides network queries to Lichess Cloud APIs with timeout and graceful fallback to on-device engine.

### `com.example.vision`
- `BoardDetector`: Scans incoming bitmaps, identifies checkerboard contrast boundaries, divides the grid into 64 cells, assesses confidence per square, and constructs validated `ChessPosition` instances.

### `com.example.service`
- `ChessLensAccessibilityService`: Subscribes to window events, monitors active foreground applications for allowlist validation, and executes hardware screenshot capture (`takeScreenshot` on API 30+).
- `ChessLensOverlayService`: Foreground service with a persistent notification and floating window manager.
- `ChessLensTileService`: Quick Settings tile for one-tap screen analysis.

### `com.example.data`
- `PreferencesManager`: Jetpack DataStore preferences for theme mode (Dark/Light/AMOLED), arrow styles, Think-First mode, and allowlisted apps.
- `AppDatabase`: Room database for storing analyzed positions and annotated game moments.

## 3. Privacy & Compliance
- **Zero Cloud Storage of Pixels:** All screen captures and video frames are read into volatile device memory and discarded immediately following analysis.
- **Fair Play Enforcement:** Prominent onboarding disclosure and in-app terms restrict use to study, coaching, and review.
- **Stockfish GPLv3:** Licensed under GNU General Public License v3. Source code link provided in-app and in documentation.
