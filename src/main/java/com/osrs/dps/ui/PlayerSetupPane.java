package com.osrs.dps.ui;

import com.osrs.dps.data.DataRepository;
import com.osrs.dps.model.AttackType;
import com.osrs.dps.model.EquipmentItem;
import com.osrs.dps.model.EquipmentSlot;
import com.osrs.dps.model.PlayerSetup;
import com.osrs.dps.model.Potion;
import com.osrs.dps.model.Prayer;
import com.osrs.dps.model.Spell;
import com.osrs.dps.model.Stance;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.control.Spinner;
import javafx.scene.control.TextField;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.util.Arrays;
import java.util.EnumMap;
import java.util.Map;

/** Editor form for a single player setup. Mutates the setup and notifies on change. */
public class PlayerSetupPane extends VBox {

    private final DataRepository data = DataRepository.get();
    private final Runnable onChanged;

    private PlayerSetup setup;
    private boolean updating;

    private final TextField nameField = new TextField();
    private final Spinner<Integer> attack = levelSpinner();
    private final Spinner<Integer> strength = levelSpinner();
    private final Spinner<Integer> defence = levelSpinner();
    private final Spinner<Integer> ranged = levelSpinner();
    private final Spinner<Integer> magic = levelSpinner();
    private final Spinner<Integer> hitpoints = levelSpinner();
    private final ComboBox<AttackType> attackType = new ComboBox<>();
    private final ComboBox<Stance> stance = new ComboBox<>();
    private final ComboBox<Prayer> prayer = new ComboBox<>();
    private final ComboBox<Potion> potion = new ComboBox<>();
    private final ComboBox<Spell> spell = new ComboBox<>();
    private final CheckBox slayerTask = new CheckBox("On Slayer task");
    private final Map<EquipmentSlot, Button> gearButtons = new EnumMap<>(EquipmentSlot.class);
    private final Label bonusSummary = new Label();

    public PlayerSetupPane(Runnable onChanged) {
        this.onChanged = onChanged;
        setSpacing(10);
        setPadding(new Insets(12));

        nameField.textProperty().addListener((o, old, text) -> {
            if (!updating && setup != null) {
                setup.setName(text);
                changed();
            }
        });

        attackType.getItems().addAll(AttackType.values());
        attackType.valueProperty().addListener((o, old, type) -> {
            if (!updating && setup != null && type != null) {
                setup.setAttackType(type);
                refreshStyleDependentControls();
                changed();
            }
        });
        stance.valueProperty().addListener((o, old, s) -> {
            if (!updating && setup != null && s != null) {
                setup.setStance(s);
                changed();
            }
        });
        prayer.valueProperty().addListener((o, old, pr) -> {
            if (!updating && setup != null && pr != null) {
                setup.setPrayer(pr);
                changed();
            }
        });
        potion.valueProperty().addListener((o, old, po) -> {
            if (!updating && setup != null && po != null) {
                setup.setPotion(po);
                changed();
            }
        });
        spell.valueProperty().addListener((o, old, sp) -> {
            if (!updating && setup != null) {
                setup.setSpell(sp);
                changed();
            }
        });
        slayerTask.selectedProperty().addListener((o, old, sel) -> {
            if (!updating && setup != null) {
                setup.setOnSlayerTask(sel);
                changed();
            }
        });

        wireLevel(attack, v -> setup.setAttackLevel(v));
        wireLevel(strength, v -> setup.setStrengthLevel(v));
        wireLevel(defence, v -> setup.setDefenceLevel(v));
        wireLevel(ranged, v -> setup.setRangedLevel(v));
        wireLevel(magic, v -> setup.setMagicLevel(v));
        wireLevel(hitpoints, v -> setup.setHitpointsLevel(v));

        GridPane levels = grid();
        levels.addRow(0, new Label("Attack"), attack, new Label("Strength"), strength);
        levels.addRow(1, new Label("Defence"), defence, new Label("Ranged"), ranged);
        levels.addRow(2, new Label("Magic"), magic, new Label("Hitpoints"), hitpoints);

        GridPane combat = grid();
        combat.addRow(0, new Label("Attack type"), attackType, new Label("Stance"), stance);
        combat.addRow(1, new Label("Prayer"), prayer, new Label("Potion"), potion);
        combat.addRow(2, new Label("Spell"), spell, new Label(""), slayerTask);

        GridPane gear = grid();
        int row = 0;
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            Button pick = new Button("(empty)");
            pick.setMaxWidth(Double.MAX_VALUE);
            pick.setOnAction(e -> pickItem(slot));
            Button clear = new Button("x");
            clear.setOnAction(e -> {
                setup.setEquipped(slot, null);
                refreshGearButtons();
                changed();
            });
            gearButtons.put(slot, pick);
            gear.addRow(row++, new Label(slot.displayName()), pick, clear);
        }
        ColumnConstraints c0 = new ColumnConstraints(70);
        ColumnConstraints c1 = new ColumnConstraints();
        c1.setHgrow(Priority.ALWAYS);
        c1.setFillWidth(true);
        gear.getColumnConstraints().addAll(c0, c1);

        bonusSummary.setStyle("-fx-text-fill: #555;");

        getChildren().addAll(
                labeledRow("Name", nameField),
                section("Levels"), levels,
                section("Combat"), combat,
                section("Equipment"), gear,
                new Separator(), bonusSummary);
    }

    private static GridPane grid() {
        GridPane g = new GridPane();
        g.setHgap(10);
        g.setVgap(6);
        return g;
    }

    private static Label section(String text) {
        Label l = new Label(text);
        l.setStyle("-fx-font-weight: bold; -fx-font-size: 13px;");
        return l;
    }

    private static HBox labeledRow(String label, javafx.scene.Node node) {
        Label l = new Label(label);
        l.setMinWidth(50);
        HBox box = new HBox(10, l, node);
        HBox.setHgrow(node, Priority.ALWAYS);
        return box;
    }

    private static Spinner<Integer> levelSpinner() {
        Spinner<Integer> s = new Spinner<>(1, 120, 99);
        s.setEditable(true);
        s.setPrefWidth(80);
        return s;
    }

    private void wireLevel(Spinner<Integer> spinner, java.util.function.IntConsumer apply) {
        spinner.valueProperty().addListener((o, old, v) -> {
            if (!updating && setup != null && v != null) {
                apply.accept(v);
                changed();
            }
        });
    }

    private void pickItem(EquipmentSlot slot) {
        SearchPickerDialog<EquipmentItem> picker = new SearchPickerDialog<>(
                "Choose " + slot.displayName().toLowerCase(),
                data.equipmentForSlot(slot),
                EquipmentItem::displayName);
        EquipmentItem chosen = picker.showAndPick();
        if (chosen != null) {
            setup.setEquipped(slot, chosen);
            refreshGearButtons();
            changed();
        }
    }

    /** Binds the pane to a setup (or null to disable). */
    public void setSetup(PlayerSetup setup) {
        this.setup = setup;
        setDisable(setup == null);
        if (setup == null) {
            return;
        }
        updating = true;
        try {
            nameField.setText(setup.getName());
            attack.getValueFactory().setValue(setup.getAttackLevel());
            strength.getValueFactory().setValue(setup.getStrengthLevel());
            defence.getValueFactory().setValue(setup.getDefenceLevel());
            ranged.getValueFactory().setValue(setup.getRangedLevel());
            magic.getValueFactory().setValue(setup.getMagicLevel());
            hitpoints.getValueFactory().setValue(setup.getHitpointsLevel());
            attackType.setValue(setup.getAttackType());
            refreshStyleDependentControls();
            stance.setValue(setup.getStance());
            prayer.setValue(setup.getPrayer());
            potion.setValue(setup.getPotion());
            spell.setValue(setup.getSpell());
            slayerTask.setSelected(setup.isOnSlayerTask());
            refreshGearButtons();
            refreshSummary();
        } finally {
            updating = false;
        }
    }

    /** Repopulates stance/prayer/potion/spell options for the current attack type. */
    private void refreshStyleDependentControls() {
        AttackType type = setup.getAttackType();
        Stance[] stances = switch (type) {
            case STAB, SLASH, CRUSH -> Stance.meleeStances();
            case RANGED -> Stance.rangedStances();
            case MAGIC -> Stance.magicStances();
        };
        stance.getItems().setAll(stances);
        if (!Arrays.asList(stances).contains(setup.getStance())) {
            setup.setStance(stances[0]);
        }
        stance.setValue(setup.getStance());

        prayer.getItems().setAll(Prayer.forAttackType(type));
        if (!prayer.getItems().contains(setup.getPrayer())) {
            setup.setPrayer(Prayer.NONE);
        }
        prayer.setValue(setup.getPrayer());

        potion.getItems().setAll(Potion.forAttackType(type));
        if (!potion.getItems().contains(setup.getPotion())) {
            setup.setPotion(Potion.NONE);
        }
        potion.setValue(setup.getPotion());

        boolean isMagic = type == AttackType.MAGIC;
        spell.setDisable(!isMagic);
        if (isMagic) {
            spell.getItems().setAll(Spell.values());
            spell.setValue(setup.getSpell());
        } else {
            spell.setValue(null);
        }
    }

    private void refreshGearButtons() {
        for (Map.Entry<EquipmentSlot, Button> e : gearButtons.entrySet()) {
            EquipmentItem item = setup.getEquipped(e.getKey());
            e.getValue().setText(item == null ? "(empty)" : item.displayName());
        }
    }

    private void refreshSummary() {
        if (setup == null) {
            bonusSummary.setText("");
            return;
        }
        bonusSummary.setText(String.format(
                "Bonuses - Stab: %d  Slash: %d  Crush: %d  Ranged: %d  Magic: %d   |   "
                        + "Melee str: %d  Ranged str: %d  Magic dmg: %.1f%%   |   Speed: %d ticks",
                setup.attackBonus(AttackType.STAB),
                setup.attackBonus(AttackType.SLASH),
                setup.attackBonus(AttackType.CRUSH),
                setup.attackBonus(AttackType.RANGED),
                setup.attackBonus(AttackType.MAGIC),
                setup.meleeStrengthBonus(),
                setup.rangedStrengthBonus(),
                setup.magicDamageBonusTenths() / 10.0,
                setup.attackSpeedTicks()));
    }

    private void changed() {
        refreshSummary();
        refreshGearButtons();
        if (onChanged != null) {
            onChanged.run();
        }
    }
}
