package com.example.ui.legal

object LegalContent {
    const val PRIVACY_POLICY = """CHESSLENS PRIVACY POLICY
Effective Date: [EFFECTIVE DATE]
Developer: [DEVELOPER NAME]
Contact: [CONTACT EMAIL]

1. WHO WE ARE AND WHAT CHESSLENS DOES
ChessLens is a visual chess learning and analysis application designed to assist users in understanding chess positions from video streams, recordings, diagrams, screenshots, puzzle apps, and physical boards.

2. INTENDED USE AND FAIR PLAY POLICY
ChessLens is strictly intended for study, education, post-game review, puzzle training, and video analysis. It is explicitly NOT designed or intended to be used during live, rated, competitive, tournament, or rule-restricted games against other human players. Using ChessLens for real-time assistance during rated online or offline games violates our Terms of Use and the fair-play policies of chess platforms (including Chess.com and Lichess). We do not provide, and will never implement, features that disguise the app's execution, bypass anti-cheat mechanisms, or automate move entry.

3. ACCESSIBILITY SERVICE DISCLOSURE
ChessLens utilizes the Android Accessibility API (AccessibilityService) under user authorization for:
• Locating and detecting chessboard boundaries on screen.
• Reading on-screen piece arrangements to construct a chess position (FEN) on demand.
• Rendering instructional colored arrows directly over the detected board.
The service only activates in apps explicitly allowlisted by the user, runs entirely on-device, and can be disabled at any time via Android Settings.

4. DATA PROCESSED ON-DEVICE
• Screen pixels and video frames are read directly into device memory (RAM) and processed in real time. They are NEVER stored to permanent disk, saved to media galleries, or transmitted over any network.
• Chess positions (FEN strings) and evaluation history are saved only in local on-device SQLite storage if the user enables history.
• Settings and user preferences are stored strictly in local DataStore.

5. DATA TRANSMITTED EXTERNALLY
By default, ChessLens operates 100% offline using an on-device chess engine. If the user explicitly enables optional web analysis sources (e.g. Lichess Cloud Evaluation or Stockfish Online), only an anonymous FEN position string is transmitted to retrieve opening statistics or deep evaluations. No IP addresses, device identifiers, or user information are associated with these queries.

6. DATA WE DO NOT COLLECT
ChessLens does NOT collect:
• User account credentials or email addresses
• Location or GPS telemetry
• Contacts, microphone audio, or biometric records
• Advertising IDs or persistent device fingerprints
• Analytics, tracking pixels, or crash telemetries without user consent

7. DATA RETENTION AND DELETION
All application data resides strictly on the local device. Users can permanently purge all stored evaluations, profiles, and cached lines at any time via Settings > Data & Privacy > Delete All Local Data. Uninstalling the application automatically deletes all associated local data.

8. CHILDREN'S PRIVACY
ChessLens does not knowingly collect or solicit personal information from children under 13.

9. THIRD-PARTY COMPONENTS AND LICENSES
• Stockfish Chess Engine: Licensed under GNU General Public License v3 (GPLv3).
• OpenCV Android SDK & TensorFlow Lite: Licensed under Apache 2.0.

10. USER RESPONSIBILITY
The user is solely responsible for complying with the fair play and service terms of any chess server, website, or tournament in which they participate.

11. CONTACT
For privacy inquiries or technical questions, please contact [CONTACT EMAIL]."""

    const val TERMS_OF_USE = """CHESSLENS TERMS OF USE
Effective Date: [EFFECTIVE DATE]
Developer: [DEVELOPER NAME]
Contact: [CONTACT EMAIL]

1. ACCEPTANCE OF TERMS
By downloading, installing, or using ChessLens ("the Application"), you agree to be bound by these Terms of Use. If you do not agree, do not use the Application.

2. PERMITTED USE
ChessLens is licensed for personal educational, learning, coaching, and analytical purposes. You may use it to study positions from recorded videos, puzzles, finished games, and educational streams.

3. PROHIBITED USES AND CHEATING RESTRICTIONS
You expressly agree NOT to:
• Use the Application to obtain computer engine assistance during any live, rated, competitive, or tournament chess match against human players where external assistance is prohibited.
• Attempt to modify, disguise, or repackage the Application to circumvent anti-cheat detection on chess platforms.
• Use the Application to harvest copyrighted chess video streams without authorization.

4. NO WARRANTY
ChessLens is provided "as is" and "as available". While we strive for high recognition and evaluation accuracy, computer vision and engine outputs may occasionally produce misidentifications or errors. The developer provides no warranty regarding absolute accuracy.

5. INTELLECTUAL PROPERTY & THIRD-PARTY LICENSES
The Stockfish chess engine utilized within or alongside ChessLens is free open-source software licensed under GNU General Public License v3 (GPLv3). Corresponding source code is accessible at https://github.com/official-stockfish/Stockfish.

6. TERMINATION AND CHANGES
We reserve the right to update these terms or modify features of the Application to preserve fair play, technical security, and operating system compatibility."""

    const val FAIR_PLAY_NOTICE = """FAIR PLAY AND INTEGRITY NOTICE

ChessLens is designed as a learning companion, puzzle solver, and video review tool. 

Fair Play Pledge:
1. Never use ChessLens during rated or competitive matches against human opponents.
2. Respect the terms of service of all chess platforms (Chess.com, Lichess, FIDE, USCF).
3. Use ChessLens to understand *why* moves work, to review finished games, and to learn from instructional streams."""
}
