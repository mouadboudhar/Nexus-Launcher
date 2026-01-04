# Nexus Launcher

A modern game library desktop application built with JavaFX.

## Project Structure

```
NexusLauncher/
├── client/                    # JavaFX client application
│   └── src/main/
│       ├── java/com/nexus/client/
│       │   ├── NexusLauncherApp.java    # Main entry point
│       │   ├── component/               # Custom UI components
│       │   │   └── GameCard.java
│       │   ├── controller/              # View controllers
│       │   │   ├── MainController.java
│       │   │   ├── LibraryController.java
│       │   │   ├── GameDetailsController.java
│       │   │   ├── ScanController.java
│       │   │   ├── FavoritesController.java
│       │   │   ├── SettingsController.java
│       │   │   ├── AddGameDialogController.java
│       │   │   └── EditGameDialogController.java
│       │   └── service/
│       │       ├── CombinedMetadataService.java  # Dynamic API metadata
│       │       ├── GameLauncher.java             # Game execution
│       │       ├── GameService.java              # Business logic
│       │       ├── MetadataService.java          # Interface
│       │       ├── PlaceholderMetadataService.java # Fallback metadata
│       │       ├── ScannerService.java           # Game detection
│       │       └── ScanTask.java                 # Background scanning
│       └── resources/com/nexus/client/
│           ├── views/                   # FXML layout files
│           └── styles/
│               └── application.css      # Dark theme stylesheet
├── shared/                    # Shared models & repositories
│   └── src/main/java/com/nexus/shared/
│       ├── model/
│       │   ├── Game.java
│       │   └── AppSettings.java
│       ├── repository/
│       │   ├── GameRepository.java
│       │   └── SettingsRepository.java
│       └── util/
│           └── HibernateUtil.java
└── pom.xml                    # Parent Maven POM
```

## Requirements

- Java 21 or later
- Maven 3.8+
- JavaFX 21

## Building the Project

```bash
# From the project root directory
mvn clean install
```

## Running the Application

### Using Maven
```bash
cd client
mvn javafx:run
```

### Using IDE (IntelliJ IDEA)
1. Open the project root folder in IntelliJ IDEA
2. Wait for Maven to import all dependencies
3. If modules aren't detected, go to File → Reload All from Disk, then reimport Maven
4. Run `NexusLauncherApp.java` from the client module

## Features

### Implemented Views
- **Library View**: Grid display of game cards with search and filter
- **Game Details View**: Full game information with hero banner
- **Scan View**: UI to scan Steam/Epic libraries (mock)
- **Favorites View**: Shows favorited games
- **Settings View**: Toggle switches for app preferences
- **Add Game Dialog**: Modal to manually add games

### Navigation
- Sidebar navigation with Library, Scan, Favorites, and Settings
- Click any game card to view details
- Back button to return to library

### Styling
- Dark theme matching the HTML mockup
- Colors: Gray-900 (#111827) background, Indigo-600 (#4f46e5) accents
- Custom scrollbars, buttons, and form elements
- Hover effects and animations on game cards

## Game Metadata

The application automatically fetches game metadata (covers, descriptions, developers) from:

### Steam API (No key required)
- Works for all Steam games using their App ID
- Also searches Steam for non-Steam games that might have a Steam page
- Provides cover images, hero images, descriptions, developers, and release dates

### IGDB API (Optional - requires free Twitch API key)
- For games not found on Steam
- To enable:
  1. Go to https://dev.twitch.tv/console/apps
  2. Create an application (free)
  3. Copy `nexus.properties.example` to `nexus.properties`
  4. Add your Client ID and Client Secret

### Fallback
- Hardcoded metadata for 100+ popular games
- Ensures common games always have covers

## Next Steps (Future Features)

- [ ] System tray integration
- [ ] Game time tracking
- [ ] Cloud sync for favorites/settings
- [ ] More platform support (GOG, Xbox Game Pass)

## License

MIT License

