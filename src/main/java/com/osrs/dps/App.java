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

    private Monster selectedMonster;
    private final Button monsterButton = new Button("Choose monster...");
    private final ImageView monsterIcon = new ImageView();
    private final Label monsterInfo = new Label();
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

        selectedMonster = data.findMonster("Zulrah (Serpentine)");
        if (selectedMonster == null && !data.allMonsters().isEmpty()) {
            selectedMonster = data.allMonsters().get(0);
        }
        PlayerSetup first = new PlayerSetup("Setup 1");
        first.setCharacter(character);
        setups.add(first);

        BorderPane root = new BorderPane();
        root.setTop(buildMonsterBar());
        SplitPane split = new SplitPane(buildSetupListPanel(), buildEditorPanel());
        split.setDividerPositions(0.24);
        root.setCenter(split);
        root.setBottom(comparisonPane);

        setupList.getSelectionModel().selectedItemProperty().addListener(
                (o, old, sel) -> editorPane.setSetup(sel));
        setupList.getSelectionModel().select(first);

        updateMonsterLabels();
        refreshComparison();

        stage.getScene().setRoot(root);
    }

    // ------------------------------------------------------------ monster bar

    private ToolBar buildMonsterBar() {
        monsterIcon.setFitWidth(28);
        monsterIcon.setFitHeight(28);
        monsterIcon.setPreserveRatio(true);
        monsterButton.setGraphic(monsterIcon);
        monsterButton.setOnAction(e -> chooseMonster());
        Button edit = new Button("Edit / customise...");
        edit.setOnAction(e -> editMonster());
        Button savePreset = new Button("Save monster preset");
        savePreset.setOnAction(e -> saveMonsterPreset());
        monsterInfo.getStyleClass().add("text-subtle");
        statusLabel.getStyleClass().add("text-subtle");
        HBox spacer = new HBox();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        return new ToolBar(new Label("Target:"), monsterButton, edit, savePreset,
                new Separator(), monsterInfo, spacer, statusLabel);
    }

    private void chooseMonster() {
        List<Monster> all = new ArrayList<>(presets.loadMonsterPresets());
        all.addAll(data.allMonsters());
        SearchPickerDialog<Monster> picker = new SearchPickerDialog<>(
                "Choose monster", all, Monster::displayName, mo -> mo.image);
        Monster chosen = picker.showAndPick();
        if (chosen != null) {
            selectedMonster = chosen;
            updateMonsterLabels();
            refreshComparison();
        }
    }

    private void editMonster() {
        Monster base = selectedMonster != null ? selectedMonster : new Monster();
        Monster edited = MonsterEditorDialog.edit(base);
        if (edited != null) {
            selectedMonster = edited;
            updateMonsterLabels();
            refreshComparison();
        }
    }

    private void saveMonsterPreset() {
        if (selectedMonster == null) {
            return;
        }
        try {
            presets.saveMonsterPreset(selectedMonster);
            info("Monster preset saved", "Saved \"" + selectedMonster.displayName() + "\".");
        } catch (IOException ex) {
            error("Could not save monster preset", ex.getMessage());
        }
    }

    private void updateMonsterLabels() {
        if (selectedMonster == null) {
            monsterButton.setText("Choose monster...");
            monsterInfo.setText("");
            monsterIcon.setImage(null);
            return;
        }
        monsterButton.setText(selectedMonster.displayName());
        monsterIcon.setImage(null);
        if (selectedMonster.image != null && !selectedMonster.image.isBlank()) {
            Monster current = selectedMonster;
            com.osrs.dps.data.ImageCache.load(selectedMonster.image, img -> {
                if (selectedMonster == current) {
                    monsterIcon.setImage(img);
                }
            });
        }
        monsterInfo.setText(String.format(
                "HP %d | Def %d | Magic %d | Def stab/slash/crush %d/%d/%d, magic %d, ranged %d/%d/%d%s",
                selectedMonster.skills.hp, selectedMonster.skills.def, selectedMonster.skills.magic,
                selectedMonster.defensive.stab, selectedMonster.defensive.slash,
                selectedMonster.defensive.crush, selectedMonster.defensive.magic,
                selectedMonster.defensive.heavy, selectedMonster.defensive.standard,
                selectedMonster.defensive.light,
                selectedMonster.attributes.isEmpty()
                        ? "" : " | " + String.join(", ", selectedMonster.attributes)));
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
        comparisonPane.refresh(List.copyOf(setups), selectedMonster);
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
