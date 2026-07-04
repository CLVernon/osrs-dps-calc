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
                setText(monster.displayName());
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

        Button add = new Button("Add...");
        add.setOnAction(e -> addTargets());
        Button remove = new Button("Remove");
        remove.setOnAction(e -> {
            Monster sel = targetList.getSelectionModel().getSelectedItem();
            if (sel != null) {
                targets.remove(sel);
                refreshComparison();
            }
        });
        Button edit = new Button("Edit...");
        edit.setOnAction(e -> editTarget());
        Button savePreset = new Button("Save preset");
        savePreset.setOnAction(e -> saveMonsterPreset());

        HBox row1 = new HBox(6, add, remove);
        HBox row2 = new HBox(6, edit, savePreset);
        Label header = new Label("Targets");
        header.getStyleClass().add("title-4");
        VBox box = new VBox(8, header, targetList, row1, row2);
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

    // ------------------------------------------------------------ setup panel

    private VBox buildSetupListPanel() {
        setupList.setPrefWidth(240);

        Button add = new Button("Add");
        add.setOnAction(e -> {
            PlayerSetup s = new PlayerSetup("Setup " + (setups.size() + 1));
            s.setCharacter(character);
            setups.add(s);
            setupList.getSelectionModel().select(s);
            refreshComparison();
        });
        Button duplicate = new Button("Duplicate");
        duplicate.setOnAction(e -> {
            PlayerSetup sel = setupList.getSelectionModel().getSelectedItem();
            if (sel != null) {
                PlayerSetup copy = sel.copy();
                setups.add(copy);
                setupList.getSelectionModel().select(copy);
                refreshComparison();
            }
        });
        Button remove = new Button("Remove");
        remove.setOnAction(e -> {
            PlayerSetup sel = setupList.getSelectionModel().getSelectedItem();
            if (sel != null) {
                setups.remove(sel);
                refreshComparison();
            }
        });
        Button save = new Button("Save preset");
        save.setOnAction(e -> savePlayerPreset());
        Button load = new Button("Load preset");
        load.setOnAction(e -> loadPlayerPreset());

        HBox row1 = new HBox(6, add, duplicate, remove);
        HBox row2 = new HBox(6, save, load);
        Label header = new Label("Gear setups");
        header.getStyleClass().add("title-4");
        VBox box = new VBox(8, characterPane, new Separator(), header, setupList, row1, row2);
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
