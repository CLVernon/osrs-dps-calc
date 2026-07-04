package com.osrs.dps.ui;

import com.osrs.dps.data.HiscoresClient;
import com.osrs.dps.model.PlayerCharacter;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Spinner;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.util.function.IntConsumer;

/**
 * Panel for the shared character: name, hiscores import, and stats.
 * All gear setups use these stats.
 */
public class CharacterPane extends VBox {

    private final Runnable onChanged;
    private PlayerCharacter character;
    private boolean updating;

    private final TextField nameField = new TextField();
    private final ComboBox<HiscoresClient.GameMode> gameMode = new ComboBox<>();
    private final Button importButton = new Button("Import");
    private final Label statusLabel = new Label();
    private final Spinner<Integer> attack = levelSpinner();
    private final Spinner<Integer> strength = levelSpinner();
    private final Spinner<Integer> defence = levelSpinner();
    private final Spinner<Integer> ranged = levelSpinner();
    private final Spinner<Integer> magic = levelSpinner();
    private final Spinner<Integer> hitpoints = levelSpinner();
    private final Spinner<Integer> currentHp = levelSpinner();
    private final Spinner<Integer> mining = levelSpinner();

    public CharacterPane(Runnable onChanged) {
        this.onChanged = onChanged;
        setSpacing(8);
        setPadding(new Insets(10));

        Label header = new Label("Character");
        header.getStyleClass().add("title-4");

        nameField.setPromptText("Username");
        nameField.textProperty().addListener((o, old, text) -> {
            if (!updating && character != null) {
                character.name = text;
                changed();
            }
        });

        gameMode.getItems().addAll(HiscoresClient.GameMode.values());
        gameMode.setValue(HiscoresClient.GameMode.REGULAR);

        importButton.setTooltip(new Tooltip("Fetch levels from the official OSRS hiscores"));
        importButton.setOnAction(e -> importFromHiscores());

        HBox nameRow = new HBox(6, nameField, importButton);
        HBox.setHgrow(nameField, Priority.ALWAYS);

        statusLabel.getStyleClass().add("text-subtle");
        statusLabel.setWrapText(true);

        wireLevel(attack, v -> character.attack = v);
        wireLevel(strength, v -> character.strength = v);
        wireLevel(defence, v -> character.defence = v);
        wireLevel(ranged, v -> character.ranged = v);
        wireLevel(magic, v -> character.magic = v);
        wireLevel(hitpoints, v -> character.hitpoints = v);
        wireLevel(currentHp, v -> character.currentHitpoints = v);
        wireLevel(mining, v -> character.mining = v);

        GridPane levels = new GridPane();
        levels.setHgap(8);
        levels.setVgap(5);
        levels.addRow(0, new Label("Attack"), attack, new Label("Strength"), strength);
        levels.addRow(1, new Label("Defence"), defence, new Label("Ranged"), ranged);
        levels.addRow(2, new Label("Magic"), magic, new Label("Hitpoints"), hitpoints);
        levels.addRow(3, new Label("Cur. HP"), currentHp, new Label("Mining"), mining);

        currentHp.setTooltip(new Tooltip("Current hitpoints, for Dharok's set effect"));
        mining.setTooltip(new Tooltip("Mining level, for CoX Guardians"));

        getChildren().addAll(header, nameRow, gameMode, statusLabel, levels);
    }

    private static Spinner<Integer> levelSpinner() {
        Spinner<Integer> s = new Spinner<>(1, 120, 99);
        s.setEditable(true);
        s.setPrefWidth(72);
        return s;
    }

    private void wireLevel(Spinner<Integer> spinner, IntConsumer apply) {
        spinner.valueProperty().addListener((o, old, v) -> {
            if (!updating && character != null && v != null) {
                apply.accept(v);
                changed();
            }
        });
    }

    private void importFromHiscores() {
        String username = nameField.getText();
        if (username == null || username.isBlank()) {
            statusLabel.setText("Enter a username to import.");
            return;
        }
        HiscoresClient.GameMode mode = gameMode.getValue();
        importButton.setDisable(true);
        statusLabel.setText("Importing " + username.trim() + "...");
        Thread fetch = new Thread(() -> {
            try {
                PlayerCharacter imported = HiscoresClient.fetch(username, mode);
                Platform.runLater(() -> {
                    // Copy into the existing shared instance so setups keep their reference
                    character.name = imported.name;
                    character.attack = imported.attack;
                    character.strength = imported.strength;
                    character.defence = imported.defence;
                    character.ranged = imported.ranged;
                    character.magic = imported.magic;
                    character.hitpoints = imported.hitpoints;
                    character.mining = imported.mining;
                    character.currentHitpoints = 0;
                    setCharacter(character);
                    statusLabel.setText("Imported " + imported.name + " from the "
                            + mode + " hiscores.");
                    importButton.setDisable(false);
                    changed();
                });
            } catch (Exception ex) {
                Platform.runLater(() -> {
                    statusLabel.setText(ex.getMessage());
                    importButton.setDisable(false);
                });
            }
        }, "hiscores-import");
        fetch.setDaemon(true);
        fetch.start();
    }

    /** Binds the pane to the shared character. */
    public void setCharacter(PlayerCharacter character) {
        this.character = character;
        updating = true;
        try {
            nameField.setText(character.name == null ? "" : character.name);
            attack.getValueFactory().setValue(character.attack);
            strength.getValueFactory().setValue(character.strength);
            defence.getValueFactory().setValue(character.defence);
            ranged.getValueFactory().setValue(character.ranged);
            magic.getValueFactory().setValue(character.magic);
            hitpoints.getValueFactory().setValue(character.hitpoints);
            currentHp.getValueFactory().setValue(character.effectiveCurrentHitpoints());
            mining.getValueFactory().setValue(character.mining);
        } finally {
            updating = false;
        }
    }

    private void changed() {
        if (onChanged != null) {
            onChanged.run();
        }
    }
}
