# ChessLens Privacy Policy

**Effective Date:** [EFFECTIVE DATE]  
**Developer:** [DEVELOPER NAME]  
**Contact Email:** [CONTACT EMAIL]  

---

### 1. Who We Are and What ChessLens Does
ChessLens is a visual chess learning and analysis application designed to assist users in understanding chess positions from video streams, recorded lessons, diagram images, screenshots, puzzle apps, and physical boards.

### 2. Intended Use and Fair Play Policy
ChessLens is strictly intended for study, education, post-game review, puzzle training, and video analysis. It is **NOT** designed or intended to be used during live, rated, competitive, tournament, or rule-restricted games against other human players. Using ChessLens for real-time assistance during rated online or offline games violates our Terms of Use and the fair-play policies of chess platforms (including Chess.com and Lichess). We do not provide, and will never implement, features that disguise the app's execution, bypass anti-cheat mechanisms, or automate move entry.

### 3. Accessibility Service Disclosure
ChessLens utilizes the Android Accessibility API (`AccessibilityService`) under explicit user authorization for:
- Locating and detecting chessboard boundaries on screen.
- Reading on-screen piece arrangements to construct a chess position (FEN) on demand.
- Rendering instructional colored arrows directly over the detected board.

The service only activates in apps explicitly allowlisted by the user, runs entirely on-device, and can be disabled at any time via Android Settings.

### 4. Data We Process On-Device
- **Screen Pixels and Video Frames:** Read directly into device memory (RAM) and processed in real time. They are **never stored** to permanent disk, saved to media galleries, or transmitted over any network.
- **Chess Positions (FEN Strings):** Evaluated and saved only in local on-device SQLite storage if the user enables history.
- **Settings and Profiles:** Stored strictly in local DataStore.

### 5. Data Transmitted Externally
By default, ChessLens operates 100% offline using an on-device chess engine. If the user explicitly enables optional web analysis sources (e.g. Lichess Cloud Evaluation or Stockfish Online), only an anonymous FEN position string is transmitted to retrieve opening statistics or deep evaluations. No IP addresses, device identifiers, or user information are associated with these queries.

### 6. Data We Do Not Collect
ChessLens does **NOT** collect:
- User account credentials or email addresses
- Location or GPS telemetry
- Contacts, microphone audio, or biometric records
- Advertising IDs or persistent device fingerprints
- Analytics, tracking pixels, or crash telemetries without user consent

### 7. Data Retention and Deletion
All application data resides strictly on the local device. Users can permanently purge all stored evaluations, profiles, and cached lines at any time via **Settings > Data & Privacy > Delete All Local Data**. Uninstalling the application automatically deletes all associated local data.

### 8. Children's Privacy
ChessLens does not knowingly collect or solicit personal information from children under 13.

### 9. Third-Party Components and Licenses
- **Stockfish Chess Engine:** Licensed under GNU General Public License v3 (GPLv3).
- **OpenCV Android SDK & TensorFlow Lite:** Licensed under Apache 2.0.

### 10. User Responsibility
The user is solely responsible for complying with the fair play and service terms of any chess server, website, or tournament in which they participate.

### 11. Contact
For privacy inquiries or technical questions, please contact [CONTACT EMAIL].
