package com.osrs.dps;

import atlantafx.base.theme.PrimerDark;
import com.osrs.dps.data.DataRepository;
import com.osrs.dps.data.DataUpdater;
import com.osrs.dps.data.ImageCache;
import com.osrs.dps.model.Monster;
import com.osrs.dps.model.PlayerCharacter;
import com.osrs.dps.model.PlayerSetup;
import com.osrs.dps.preset.PresetManager;
import com.osrs.dps.ui.CharacterPane;
import com.osrs.dps.ui.ComparisonPane;
import com.osrs.dps.ui.MonsterEditorDialog;
import com.osrs.dps.ui.PlayerSetupPane;
import com.osrs.dps.ui.SearchPickerDialog;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Separator;
import javafx.scene.control.SplitPane;
import javafx.scene.control.ToolBar;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/** OSRS DPS calculator desktop application. */
public class App extends Application {

    private DataRepository data;
    private PresetManager presets;

    private final ObservableList<PlayerSetup> setups = FXCollections.observableArrayList();
    private final ListView<PlayerSetup> setupList = new ListView<>(setups);
    private PlayerSetupPane editorPane;
    private ComparisonPane comparisonPane;
    private CharacterPane characterPane;
    private PlayerCharacter character = new PlayerCharacter();

    private final ObservableList<Monster> targets = FXCollections.observableArrayList();
    private final ListView<Monster> targetList = new ListView<>(targets);
    private final Label statusLabel = new Label();

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage stage) {
        Application.setUserAgentStylesheet(new PrimerDark().getUserAgentStylesheet());

        // Loading screen while data is checked/downloaded on first start
        ProgressIndicator progress = new ProgressIndicator();
        Label loading = new Label("Checking for updated OSRS data...");
        VBox loadingBox = new VBox(14, progress, loading);
        loadingBox.setAlignment(javafx.geometry.Pos.CENTER);
        stage.setTitle("OSRS DPS Calculator");
        stage.setScene(new Scene(new StackPane(loadingBox), 1280, 860));
        stage.show();

        Thread init = new Thread(() -> {
            boolean updated = false;
            try {
                updated = DataUpdater.updateIfDue();
            } catch (Exception e) {
                System.err.println("Data update check failed: " + e.getMessage());
            }
            DataRepository repository = DataRepository.get();
            boolean dataUpdated = updated;
            Platform.runLater(() -> {
                this.data = repository;
                this.presets = new PresetManager();
                buildMainUi(stage);
                statusLabel.setText(dataUpdated
                        ? "Wiki data updated today"
                        : "Using cached/bundled wiki data");
            });
        }, "data-init");
        init.setDaemon(true);
        init.start();
    }

    private void buildMainUi(Stage stage) {
        comparisonPane = new ComparisonPane();
        editorPane = new PlayerSetupPane(this::refreshComparison);
        character = presets.loadCharacter();
        characterPane = new CharacterPane(this::characterChanged);
        characterPane.setCharacter(character);

        Monster defaultTarget = data.findMonster("Zulrah (Serpentine)");
        if (defaultTarget == null && !data.allMonsters().isEmpty()) {
            defaultTarget = data.allMonsters().get(0);
        }
        if (defaultTarget != null) {
            targets.add(defaultTarget);
        }
        PlayerSetup first = new PlayerSetup("Setup 1");
        first.setCharacter(character);
        setups.add(first);

        BorderPane root = new BorderPane();
        root.setTop(buildStatusBar());
        SplitPane split = new SplitPane(buildSetupListPanel(), buildEditorPanel(),
                buildTargetsPanel());
        split.setDividerPositions(0.22, 0.78);
        root.setCenter(split);
        root.setBottom(comparisonPane);

        setupList.getSelectionModel().selectedItemProperty().addListener(
                (o, old, sel) -> editorPane.setSetup(sel));
        setupList.getSelectionModel().select(first);

        refreshComparison();

        stage.getScene().setRoot(root);
    }

    // ------------------------------------------------------------- status bar

    private ToolBar buildStatusBar() {
        statusLabel.getStyleClass().add("text-subtle");
        Label hint = new Label(
                "Compare gear setups (rows) against target monsters (columns) in the table below.");
        hint.getStyleClass().add("text-subtle");
        HBox spacer = new HBox();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        return new ToolBar(hint, spacer, statusLabel);
    }

    // ---------------------------------------------------------- targets panel

    private VBox buildTargetsPanel() {
        targetList.setPrefWidth(230);
        targetList.setCellFactory(lv -> new javafx.scene.control.ListCell<>() {
            private final ImageView icon = new ImageView();

            {
                icon.setFitWidth(22);
                icon.setFitHeight(22);
                icon.setPreserveRatio(true);
            }

            @Override
            protected void updateItem(Monster monster, boolean empty) {
                super.updateItem(monster, empty);
                if (empty || monster == null) {
                    setText(null);
                    setGraphic(null);
                    return;
                }
                setText(monster.displayName() + monster.scaleSuffix());
                icon.setImage(monster.image == null ? null
                        : com.osrs.dps.data.ImageCache.cached(monster.image));
                setGraphic(icon);
                if (icon.getImage() == null && monster.image != null && !monster.image.isBlank()) {
                    Monster current = monster;
                    com.osrs.dps.data.ImageCache.load(monster.image, img -> {
                        if (getItem() == current) {
                            icon.setImage(img);
                        }
                    });
                }
            }
        });

        List<Monster> pickable = new ArrayList<>(presets.loadMonsterPresets());
        pickable.addAll(data.allMonsters());
        com.osrs.dps.ui.SearchableDropdown<Monster> addDropdown =
                new com.osrs.dps.ui.SearchableDropdown<>(pickable, Monster::displayName,
                        mo -> mo.image, com.osrs.dps.ui.StatTooltips::forMonster);
        addDropdown.setClearOnSelect(true);
        addDropdown.setFieldPromptText("Type to add a target...");
        addDropdown.setOnSelect(monster -> {
            if (monster != null && !targets.contains(monster)) {
                targets.add(monster);
                refreshComparison();
            }
        });

        Button addMany = com.osrs.dps.ui.IconButtons.of(
                org.kordamp.ikonli.feather.Feather.LIST,
                "Add several targets at once (Ctrl/Shift multi-select)");
        addMany.setOnAction(e -> addTargets());
        Button remove = com.osrs.dps.ui.IconButtons.of(
                org.kordamp.ikonli.feather.Feather.TRASH_2, "Remove the selected target");
        remove.setOnAction(e -> {
            Monster sel = targetList.getSelectionModel().getSelectedItem();
            if (sel != null) {
                targets.remove(sel);
                refreshComparison();
            }
        });
        Button edit = com.osrs.dps.ui.IconButtons.of(
                org.kordamp.ikonli.feather.Feather.EDIT_2,
                "Edit / customise the selected target's stats");
        edit.setOnAction(e -> editTarget());
        Button savePreset = com.osrs.dps.ui.IconButtons.of(
                org.kordamp.ikonli.feather.Feather.SAVE,
                "Save the selected target as a monster preset");
        savePreset.setOnAction(e -> saveMonsterPreset());
        Button raidSettings = com.osrs.dps.ui.IconButtons.of(
                org.kordamp.ikonli.feather.Feather.SLIDERS,
                "Raid scaling: apply party size / CM settings to all CoX targets");
        raidSettings.setOnAction(e -> editRaidSettings());

        HBox buttons = new HBox(6, addMany, remove, edit, savePreset, raidSettings);
        Label header = new Label("Targets");
        header.getStyleClass().add("title-4");
        VBox box = new VBox(8, header, addDropdown, targetList, buttons);
        box.setPadding(new Insets(10));
        VBox.setVgrow(targetList, Priority.ALWAYS);
        return box;
    }

    private void addTargets() {
        List<Monster> all = new ArrayList<>(presets.loadMonsterPresets());
        all.addAll(data.allMonsters());
        SearchPickerDialog<Monster> picker = new SearchPickerDialog<>(
                "Add targets", all, Monster::displayName, mo -> mo.image);
        List<Monster> chosen = picker.showAndPickAll();
        if (chosen != null && !chosen.isEmpty()) {
            for (Monster monster : chosen) {
                if (!targets.contains(monster)) {
                    targets.add(monster);
                }
            }
            refreshComparison();
        }
    }

    private void editTarget() {
        Monster sel = targetList.getSelectionModel().getSelectedItem();
        if (sel == null) {
            return;
        }
        Monster edited = MonsterEditorDialog.edit(sel);
        if (edited != null) {
            targets.set(targets.indexOf(sel), edited);
            refreshComparison();
        }
    }

    private void saveMonsterPreset() {
        Monster sel = targetList.getSelectionModel().getSelectedItem();
        if (sel == null) {
            return;
        }
        try {
            presets.saveMonsterPreset(sel);
            info("Monster preset saved", "Saved \"" + sel.displayName() + "\".");
        } catch (IOException ex) {
            error("Could not save monster preset", ex.getMessage());
        }
    }

    /** Applies party size / CM raid scaling to every CoX target in the list. */
    private void editRaidSettings() {
        List<Monster> coxTargets = targets.stream()
                .filter(mo -> mo.hasAttribute("xerician"))
                .toList();
        if (coxTargets.isEmpty()) {
            info("No CoX targets",
                    "Add Chambers of Xeric monsters to the target list first - "
                            + "raid scaling applies to Xerician monsters.");
            return;
        }
        Monster first = coxTargets.get(0);

        javafx.scene.control.Dialog<Boolean> dialog = new javafx.scene.control.Dialog<>();
        dialog.setTitle("CoX raid scaling");
        dialog.setHeaderText("Applies to all " + coxTargets.size() + " CoX target(s)");
        dialog.getDialogPane().getButtonTypes().addAll(
                javafx.scene.control.ButtonType.OK, javafx.scene.control.ButtonType.CANCEL);

        javafx.scene.control.Spinner<Integer> partySize =
                new javafx.scene.control.Spinner<>(1, 100, Math.max(1, first.partySize));
        javafx.scene.control.Spinner<Integer> maxCombat =
                new javafx.scene.control.Spinner<>(3, 126, first.partyMaxCombatLevel);
        javafx.scene.control.Spinner<Integer> maxHp =
                new javafx.scene.control.Spinner<>(10, 99, first.partyMaxHpLevel);
        javafx.scene.control.Spinner<Integer> avgMining =
                new javafx.scene.control.Spinner<>(1, 99, first.partyAvgMiningLevel);
        javafx.scene.control.CheckBox challengeMode =
                new javafx.scene.control.CheckBox("Challenge Mode");
        challengeMode.setSelected(first.coxChallengeMode);
        for (var spinner : List.of(partySize, maxCombat, maxHp, avgMining)) {
            spinner.setEditable(true);
        }

        javafx.scene.layout.GridPane grid = new javafx.scene.layout.GridPane();
        grid.setHgap(10);
        grid.setVgap(8);
        grid.setPadding(new Insets(10));
        grid.addRow(0, new Label("Party size"), partySize);
        grid.addRow(1, new Label("Highest combat level"), maxCombat);
        grid.addRow(2, new Label("Highest Hitpoints"), maxHp);
        grid.addRow(3, new Label("Avg Mining (Guardians)"), avgMining);
        grid.addRow(4, new Label(""), challengeMode);
        dialog.getDialogPane().setContent(grid);
        dialog.setResultConverter(b -> b == javafx.scene.control.ButtonType.OK);

        if (dialog.showAndWait().orElse(false)) {
            for (Monster mo : coxTargets) {
                mo.partySize = partySize.getValue();
                mo.partyMaxCombatLevel = maxCombat.getValue();
                mo.partyMaxHpLevel = maxHp.getValue();
                mo.partyAvgMiningLevel = avgMining.getValue();
                mo.coxChallengeMode = challengeMode.isSelected();
            }
            targetList.refresh();
            refreshComparison();
        }
    }

    // ------------------------------------------------------------ setup panel

    private VBox buildSetupListPanel() {
        setupList.setPrefWidth(240);
        setupList.setCellFactory(lv -> new javafx.scene.control.ListCell<>() {
            private final ImageView icon = new ImageView();

            {
                icon.setFitWidth(22);
                icon.setFitHeight(22);
                icon.setPreserveRatio(true);
            }

            @Override
            protected void updateItem(PlayerSetup s, boolean empty) {
                super.updateItem(s, empty);
                if (empty || s == null) {
                    setText(null);
                    setGraphic(null);
                    return;
                }
                setText(s.getName());
                var weapon = s.getWeapon();
                com.osrs.dps.ui.Icons.set(icon, weapon == null ? null : weapon.image);
                setGraphic(icon);
            }
        });

        Button add = com.osrs.dps.ui.IconButtons.of(
                org.kordamp.ikonli.feather.Feather.PLUS, "Add a new gear setup");
        add.setOnAction(e -> {
            PlayerSetup s = new PlayerSetup("Setup " + (setups.size() + 1));
            s.setCharacter(character);
            setups.add(s);
            setupList.getSelectionModel().select(s);
            refreshComparison();
        });
        Button duplicate = com.osrs.dps.ui.IconButtons.of(
                org.kordamp.ikonli.feather.Feather.COPY, "Duplicate the selected setup");
        duplicate.setOnAction(e -> {
            PlayerSetup sel = setupList.getSelectionModel().getSelectedItem();
            if (sel != null) {
                PlayerSetup copy = sel.copy();
                setups.add(copy);
                setupList.getSelectionModel().select(copy);
                refreshComparison();
            }
        });
        Button remove = com.osrs.dps.ui.IconButtons.of(
                org.kordamp.ikonli.feather.Feather.TRASH_2, "Remove the selected setup");
        remove.setOnAction(e -> {
            PlayerSetup sel = setupList.getSelectionModel().getSelectedItem();
            if (sel != null) {
                setups.remove(sel);
                refreshComparison();
            }
        });
        Button save = com.osrs.dps.ui.IconButtons.of(
                org.kordamp.ikonli.feather.Feather.SAVE, "Save the selected setup as a preset");
        save.setOnAction(e -> savePlayerPreset());
        Button load = com.osrs.dps.ui.IconButtons.of(
                org.kordamp.ikonli.feather.Feather.FOLDER, "Load a saved gear preset");
        load.setOnAction(e -> loadPlayerPreset());

        HBox buttons = new HBox(6, add, duplicate, remove, save, load);
        Label header = new Label("Gear setups");
        header.getStyleClass().add("title-4");
        VBox box = new VBox(8, characterPane, new Separator(), header, setupList, buttons);
        box.setPadding(new Insets(10));
        VBox.setVgrow(setupList, Priority.ALWAYS);
        return box;
    }

    private ScrollPane buildEditorPanel() {
        ScrollPane scroll = new ScrollPane(editorPane);
        scroll.setFitToWidth(true);
        return scroll;
    }

    private void savePlayerPreset() {
        PlayerSetup sel = setupList.getSelectionModel().getSelectedItem();
        if (sel == null) {
            return;
        }
        try {
            presets.savePlayerPreset(sel);
            info("Preset saved", "Saved \"" + sel.getName() + "\".");
        } catch (IOException ex) {
            error("Could not save preset", ex.getMessage());
        }
    }

    private void loadPlayerPreset() {
        List<PlayerSetup> available = presets.loadPlayerPresets(data);
        if (available.isEmpty()) {
            info("No presets", "No saved player presets were found.");
            return;
        }
        SearchPickerDialog<PlayerSetup> picker = new SearchPickerDialog<>(
                "Load gear preset", available, PlayerSetup::getName);
        PlayerSetup chosen = picker.showAndPick();
        if (chosen != null) {
            chosen.setCharacter(character);
            setups.add(chosen);
            setupList.getSelectionModel().select(chosen);
            refreshComparison();
        }
    }

    // ---------------------------------------------------------------- helpers

    /** Character stats changed: persist and recalculate everything. */
    private void characterChanged() {
        presets.saveCharacter(character);
        refreshComparison();
    }

    private void refreshComparison() {
        setupList.refresh();
        comparisonPane.refresh(List.copyOf(setups), List.copyOf(targets));
    }

    private void info(String header, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION, message);
        alert.setHeaderText(header);
        alert.showAndWait();
    }

    private void error(String header, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR, message);
        alert.setHeaderText(header);
        alert.showAndWait();
    }
}
