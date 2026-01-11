package com.nexus.service;

import com.nexus.model.Game;
import com.nexus.model.IgnoredGame;
import com.nexus.repository.GameRepository;
import com.nexus.repository.IgnoredGameRepository;

import java.util.*;

/**
 * Service layer for game data operations.
 * Provides a clean API for controllers to interact with game data.
 */
public class GameService {

    private static GameService instance;
    private final GameRepository gameRepository;
    private final IgnoredGameRepository ignoredGameRepository;
    private final MetadataService metadataService;

    private GameService() {
        this.gameRepository = new GameRepository();
        this.ignoredGameRepository = new IgnoredGameRepository();
        // Use dynamic API-based metadata service
        this.metadataService = new CombinedMetadataService();
    }

    public static synchronized GameService getInstance() {
        if (instance == null) {
            instance = new GameService();
        }
        return instance;
    }

    /**
     * Gets all games from the database, deduplicated and with metadata.
     */
    public List<Game> getAllGames() {
        List<Game> games = gameRepository.findAll();

        // Deduplicate by uniqueId (keep first occurrence)
        Map<String, Game> uniqueGames = new LinkedHashMap<>();
        List<Long> duplicateIds = new ArrayList<>();

        for (Game game : games) {
            String key = game.getUniqueId();
            if (key == null || key.isEmpty()) {
                // Use title + platform as fallback key
                key = (game.getTitle() + "_" + game.getPlatform()).toLowerCase();
            }

            if (!uniqueGames.containsKey(key)) {
                uniqueGames.put(key, game);
                ensureMetadata(game);
            } else {
                // This is a duplicate - mark for deletion
                duplicateIds.add(game.getId());
            }
        }

        // Delete duplicates from database
        for (Long id : duplicateIds) {
            try {
                gameRepository.delete(id);
                System.out.println("[GameService] Removed duplicate game with id: " + id);
            } catch (Exception e) {
                // Ignore deletion errors
            }
        }

        return new ArrayList<>(uniqueGames.values());
    }

    /**
     * Gets favorite games.
     */
    public List<Game> getFavoriteGames() {
        List<Game> games = gameRepository.findByFavorite(true);
        for (Game game : games) {
            ensureMetadata(game);
        }
        return games;
    }

    /**
     * Searches games by title.
     */
    public List<Game> searchGames(String query) {
        if (query == null || query.trim().isEmpty()) {
            return getAllGames();
        }
        List<Game> games = gameRepository.searchByTitle(query.trim());
        for (Game game : games) {
            ensureMetadata(game);
        }
        return games;
    }

    /**
     * Gets a game by ID.
     */
    public Optional<Game> getGameById(Long id) {
        Optional<Game> game = gameRepository.findById(id);
        game.ifPresent(this::ensureMetadata);
        return game;
    }

    /**
     * Gets a game by unique ID.
     */
    public Optional<Game> getGameByUniqueId(String uniqueId) {
        Optional<Game> game = gameRepository.findByUniqueId(uniqueId);
        game.ifPresent(this::ensureMetadata);
        return game;
    }

    /**
     * Saves or updates a game.
     */
    public Game saveGame(Game game) {
        return gameRepository.save(game);
    }

    /**
     * Toggles the favorite status of a game.
     */
    public void toggleFavorite(Game game) {
        game.setFavorite(!game.isFavorite());
        gameRepository.save(game);
    }

    /**
     * Deletes a game.
     */
    public void deleteGame(Game game) {
        if (game != null && game.getId() != null) {
            gameRepository.delete(game.getId());
        }
    }

    /**
     * Gets the total count of games.
     */
    public long getGameCount() {
        return gameRepository.count();
    }

    /**
     * Gets games by platform.
     */
    public List<Game> getGamesByPlatform(Game.Platform platform) {
        List<Game> games = gameRepository.findByPlatform(platform);
        for (Game game : games) {
            ensureMetadata(game);
        }
        return games;
    }

    /**
     * Ensures a game has metadata (cover, description, etc.).
     * Only applies if current values are empty/null.
     */
    private void ensureMetadata(Game game) {
        if (game == null) return;

        boolean needsUpdate = false;

        // Check if cover URL is missing or invalid
        String coverUrl = game.getCoverImageUrl();
        if (coverUrl == null || coverUrl.isEmpty() || coverUrl.startsWith("/assets/")) {
            metadataService.applyMetadata(game);
            needsUpdate = true;
        }

        // Check if description is missing
        if (game.getDescription() == null || game.getDescription().isEmpty() ||
            game.getDescription().startsWith("No description")) {
            metadataService.applyMetadata(game);
            needsUpdate = true;
        }

        // Save updated metadata
        if (needsUpdate && game.getId() != null) {
            try {
                gameRepository.save(game);
            } catch (Exception e) {
                // Ignore save errors for metadata updates
            }
        }
    }

    /**
     * Clears all games from the database (useful for rescan).
     */
    public void clearAllGames() {
        List<Game> games = gameRepository.findAll();
        for (Game game : games) {
            if (game.getPlatform() != Game.Platform.MANUAL) {
                gameRepository.delete(game.getId());
            }
        }
    }

    /**
     * Ignores a game - adds it to the ignored list and removes from the library.
     * The game will not appear in future scans.
     *
     * @param game The game to ignore
     */
    public void ignoreGame(Game game) {
        if (game == null) return;

        // Create an IgnoredGame record
        IgnoredGame ignoredGame = new IgnoredGame(
            game.getTitle(),
            game.getInstallPath(),
            game.getUniqueId()
        );

        // Save to ignored games table
        try {
            ignoredGameRepository.save(ignoredGame);
            System.out.println("[GameService] Game ignored: " + game.getTitle());
        } catch (Exception e) {
            System.err.println("[GameService] Failed to save ignored game: " + e.getMessage());
            // If it already exists, that's fine - continue with deletion
        }

        // Delete the game from the main games table
        if (game.getId() != null) {
            gameRepository.delete(game.getId());
            System.out.println("[GameService] Game removed from library: " + game.getTitle());
        }
    }

    /**
     * Restores a previously ignored game.
     * The game will reappear on the next scan.
     *
     * @param ignoredGame The ignored game to restore
     */
    public void restoreIgnoredGame(IgnoredGame ignoredGame) {
        if (ignoredGame == null || ignoredGame.getId() == null) return;

        ignoredGameRepository.delete(ignoredGame.getId());
        System.out.println("[GameService] Game restored (will appear on next scan): " + ignoredGame.getTitle());
    }

    /**
     * Gets all ignored games.
     */
    public List<IgnoredGame> getAllIgnoredGames() {
        return ignoredGameRepository.findAll();
    }

    /**
     * Checks if a game is ignored by its unique ID.
     */
    public boolean isGameIgnored(String uniqueId) {
        return ignoredGameRepository.isIgnored(uniqueId);
    }

    /**
     * Gets all ignored game unique IDs.
     */
    public List<String> getIgnoredGameIds() {
        return ignoredGameRepository.findAllUniqueIds();
    }
}

