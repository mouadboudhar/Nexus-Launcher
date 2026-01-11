package com.nexus.controller;

import com.nexus.service.GameService;
import com.nexus.model.AppSettings;
import com.nexus.model.IgnoredGame;
import com.nexus.repository.SettingsRepository;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import org.kordamp.ikonli.javafx.FontIcon;

import java.io.File;
import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

/**
 * Controller for the Settings view.
 */
public class SettingsController implements Initializable {

    @FXML private HBox launchOnStartupToggle;
    @FXML private HBox closeToTrayToggle;
    @FXML private HBox darkModeToggle;
    @FXML private Button clearLibraryButton;
    @FXML private Label dbPathLabel;

    // Hidden Games UI
    @FXML private VBox hiddenGamesContainer;
    @FXML private VBox hiddenGamesEmptyState;
    @FXML private ListView<IgnoredGame> hiddenGamesList;

    private final SettingsRepository settingsRepository = new SettingsRepository();
    private final GameService gameService = GameService.getInstance();
    private AppSettings settings;

    private boolean launchOnStartup = false;
    private boolean closeToTray = false;
    private boolean darkMode = true;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        loadSettings();

        // Show database path
        if (dbPathLabel != null) {
            File dbFile = new File("nexus.db");
            dbPathLabel.setText(dbFile.getAbsolutePath());
        }

        // Setup hidden games list
        setupHiddenGamesList();
        loadHiddenGames();
    }

    private void loadSettings() {
        Task<AppSettings> loadTask = new Task<>() {
            @Override
            protected AppSettings call() {
                return settingsRepository.getSettings();
            }
        };

        loadTask.setOnSucceeded(e -> {
            settings = loadTask.getValue();
            Platform.runLater(() -> {
                launchOnStartup = settings.isLaunchOnStartup();
                closeToTray = settings.isCloseToTray();
                darkMode = settings.isDarkMode();
                setupToggles();
            });
        });

        loadTask.setOnFailed(e -> {
            Platform.runLater(this::setupToggles);
        });

        Thread thread = new Thread(loadTask);
        thread.setDaemon(true);
        thread.start();
    }

    private void setupToggles() {
        // Set initial states
        updateToggleVisual(launchOnStartupToggle, launchOnStartup);
        updateToggleVisual(closeToTrayToggle, closeToTray);
        updateToggleVisual(darkModeToggle, darkMode);

        // Add click handlers
        launchOnStartupToggle.setOnMouseClicked(e -> {
            launchOnStartup = !launchOnStartup;
            updateToggleVisual(launchOnStartupToggle, launchOnStartup);
            saveSetting("launchOnStartup", launchOnStartup);
        });

        closeToTrayToggle.setOnMouseClicked(e -> {
            closeToTray = !closeToTray;
            updateToggleVisual(closeToTrayToggle, closeToTray);
            saveSetting("closeToTray", closeToTray);
        });

        darkModeToggle.setOnMouseClicked(e -> {
            darkMode = !darkMode;
            updateToggleVisual(darkModeToggle, darkMode);
            saveSetting("darkMode", darkMode);
        });
    }

    private void saveSetting(String name, Object value) {
        Task<Void> saveTask = new Task<>() {
            @Override
            protected Void call() {
                settingsRepository.updateSetting(name, value);
                return null;
            }
        };

        saveTask.setOnSucceeded(e -> {
            System.out.println("[SETTINGS] Saved " + name + ": " + value);
        });

        Thread thread = new Thread(saveTask);
        thread.setDaemon(true);
        thread.start();
    }

    private void updateToggleVisual(HBox toggle, boolean isOn) {
        toggle.getStyleClass().removeAll("toggle-on", "toggle-off");
        toggle.getStyleClass().add(isOn ? "toggle-on" : "toggle-off");
    }

    @FXML
    private void onClearLibrary() {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Clear Library");
        alert.setHeaderText("Clear all scanned games?");
        alert.setContentText("This will remove all automatically detected games from your library. Manually added games will be preserved. A rescan will start automatically.");

        alert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                if (clearLibraryButton != null) {
                    clearLibraryButton.setDisable(true);
                    clearLibraryButton.setText("Clearing...");
                }

                Task<Void> clearTask = new Task<>() {
                    @Override
                    protected Void call() {
                        gameService.clearAllGames();
                        return null;
                    }
                };

                clearTask.setOnSucceeded(e -> {
                    Platform.runLater(() -> {
                        if (clearLibraryButton != null) {
                            clearLibraryButton.setDisable(false);
                            clearLibraryButton.setText("Clear & Rescan");
                        }

                        // Show success message
                        Alert successAlert = new Alert(Alert.AlertType.INFORMATION);
                        successAlert.setTitle("Library Cleared");
                        successAlert.setHeaderText("Library has been cleared");
                        successAlert.setContentText("Please go to the Library view or use Scan Games to rescan your games.");
                        successAlert.showAndWait();
                    });
                });

                clearTask.setOnFailed(e -> {
                    Platform.runLater(() -> {
                        if (clearLibraryButton != null) {
                            clearLibraryButton.setDisable(false);
                            clearLibraryButton.setText("Clear & Rescan");
                        }

                        Alert errorAlert = new Alert(Alert.AlertType.ERROR);
                        errorAlert.setTitle("Error");
                        errorAlert.setHeaderText("Failed to clear library");
                        errorAlert.setContentText("An error occurred while clearing the library.");
                        errorAlert.showAndWait();
                    });
                });

                Thread thread = new Thread(clearTask);
                thread.setDaemon(true);
                thread.start();
            }
        });
    }

    /**
     * Sets up the hidden games ListView with custom cell factory.
     */
    private void setupHiddenGamesList() {
        if (hiddenGamesList == null) return;

        hiddenGamesList.setCellFactory(listView -> new ListCell<>() {
            private final HBox container = new HBox(12);
            private final VBox textContainer = new VBox(2);
            private final Label titleLabel = new Label();
            private final Label pathLabel = new Label();
            private final Button restoreButton = new Button("Restore");
            private final FontIcon gameIcon = new FontIcon("fas-gamepad");

            {
                // Setup styles
                container.setAlignment(Pos.CENTER_LEFT);
                container.setPadding(new Insets(8, 12, 8, 12));
                container.getStyleClass().add("hidden-game-item");

                gameIcon.setIconSize(18);
                gameIcon.getStyleClass().add("hidden-game-icon");

                titleLabel.getStyleClass().add("hidden-game-title");
                pathLabel.getStyleClass().add("hidden-game-path");

                textContainer.getChildren().addAll(titleLabel, pathLabel);
                HBox.setHgrow(textContainer, Priority.ALWAYS);

                restoreButton.getStyleClass().add("secondary-button");
                restoreButton.setOnAction(e -> {
                    IgnoredGame game = getItem();
                    if (game != null) {
                        restoreHiddenGame(game);
                    }
                });

                container.getChildren().addAll(gameIcon, textContainer, restoreButton);
            }

            @Override
            protected void updateItem(IgnoredGame item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                } else {
                    titleLabel.setText(item.getTitle());
                    pathLabel.setText(item.getInstallPath() != null ? item.getInstallPath() : "Unknown path");
                    setGraphic(container);
                }
            }
        });
    }

    /**
     * Loads hidden games from the database.
     */
    private void loadHiddenGames() {
        Task<List<IgnoredGame>> loadTask = new Task<>() {
            @Override
            protected List<IgnoredGame> call() {
                return gameService.getAllIgnoredGames();
            }
        };

        loadTask.setOnSucceeded(e -> {
            List<IgnoredGame> ignoredGames = loadTask.getValue();
            Platform.runLater(() -> updateHiddenGamesUI(ignoredGames));
        });

        loadTask.setOnFailed(e -> {
            System.err.println("[SettingsController] Failed to load hidden games: " + loadTask.getException());
            Platform.runLater(() -> updateHiddenGamesUI(List.of()));
        });

        Thread thread = new Thread(loadTask);
        thread.setDaemon(true);
        thread.start();
    }

    /**
     * Updates the hidden games UI based on the list of ignored games.
     */
    private void updateHiddenGamesUI(List<IgnoredGame> ignoredGames) {
        if (hiddenGamesEmptyState == null || hiddenGamesList == null) return;

        if (ignoredGames == null || ignoredGames.isEmpty()) {
            // Show empty state
            hiddenGamesEmptyState.setVisible(true);
            hiddenGamesEmptyState.setManaged(true);
            hiddenGamesList.setVisible(false);
            hiddenGamesList.setManaged(false);
        } else {
            // Show list
            hiddenGamesEmptyState.setVisible(false);
            hiddenGamesEmptyState.setManaged(false);
            hiddenGamesList.setVisible(true);
            hiddenGamesList.setManaged(true);
            hiddenGamesList.getItems().setAll(ignoredGames);
        }
    }

    /**
     * Restores a hidden game (removes from ignored list).
     */
    private void restoreHiddenGame(IgnoredGame ignoredGame) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Restore Game");
        alert.setHeaderText("Restore " + ignoredGame.getTitle() + "?");
        alert.setContentText("This game will appear in your library on the next scan.");

        // Style the dialog
        DialogPane dialogPane = alert.getDialogPane();
        dialogPane.getStylesheets().add(getClass().getResource("/com/nexus/styles/application.css").toExternalForm());
        dialogPane.getStyleClass().add("custom-dialog");

        alert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                Task<Void> restoreTask = new Task<>() {
                    @Override
                    protected Void call() {
                        gameService.restoreIgnoredGame(ignoredGame);
                        return null;
                    }
                };

                restoreTask.setOnSucceeded(e -> Platform.runLater(() -> {
                    System.out.println("[SettingsController] Restored game: " + ignoredGame.getTitle());
                    loadHiddenGames(); // Refresh the list
                }));

                restoreTask.setOnFailed(e -> {
                    System.err.println("[SettingsController] Failed to restore game: " + restoreTask.getException());
                    Platform.runLater(() -> {
                        Alert errorAlert = new Alert(Alert.AlertType.ERROR);
                        errorAlert.setTitle("Error");
                        errorAlert.setHeaderText("Failed to restore game");
                        errorAlert.setContentText("An error occurred while restoring the game.");
                        errorAlert.showAndWait();
                    });
                });

                Thread thread = new Thread(restoreTask);
                thread.setDaemon(true);
                thread.start();
            }
        });
    }
}

