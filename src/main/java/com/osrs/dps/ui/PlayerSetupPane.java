package com.osrs.dps.ui;

import com.osrs.dps.data.DataRepository;
import com.osrs.dps.data.ImageCache;
import com.osrs.dps.model.AttackType;
import com.osrs.dps.model.EquipmentItem;
import com.osrs.dps.model.EquipmentSlot;
import com.osrs.dps.model.PlayerSetup;
import com.osrs.dps.model.Potion;
import com.osrs.dps.model.Prayer;
import com.osrs.dps.model.SpellData;
import com.osrs.dps.model.Stance;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Spinner;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.FlowPane;
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
    private final ComboBox<AttackType> attackType = new ComboBox<>();
    private final ComboBox<Stance> stance = new ComboBox<>();
    private final ComboBox<Prayer> prayer = new ComboBox<>();
    private final ComboBox<Potion> potion = new ComboBox<>();
    private final Button spellButton = new Button("(no spell)");
    private final Button clearSpell = new Button("✕");
    private final CheckBox slayerTask = new CheckBox("On Slayer task");
    private final CheckBox wilderness = new CheckBox("In Wilderness");
    private final CheckBox forinthry = new CheckBox("Forinthry Surge");
    private final CheckBox markOfDarkness = new CheckBox("Mark of Darkness");
    private final CheckBox charge = new CheckBox("Charge");
    private final CheckBox kandarin = new CheckBox("Kandarin diary");
    private final CheckBox sunfire = new CheckBox("Sunfire runes");
    private final Spinner<Integer> chinDistance = new Spinner<>(1, 10, 5);
    private final Map<EquipmentSlot, Button> gearButtons = new EnumMap<>(EquipmentSlot.class);
    private final Map<EquipmentSlot, ImageView> gearIcons = new EnumMap<>(EquipmentSlot.class);
    private final Label bonusSummary = new Label();

    public PlayerSetupPane(Runnable onChanged) {
        this.onChanged = onChanged;
        setSpacing(12);
        setPadding(new Insets(14));

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
        wireCombo(stance, v -> setup.setStance(v));
        wireCombo(prayer, v -> setup.setPrayer(v));
        wireCombo(potion, v -> setup.setPotion(v));

        spellButton.setOnAction(e -> pickSpell());
        spellButton.setMaxWidth(Double.MAX_VALUE);
        clearSpell.setOnAction(e -> {
            if (setup != null) {
                setup.setSpell(null);
                refreshSpellButton();
                changed();
            }
        });

        wireCheck(slayerTask, v -> setup.setOnSlayerTask(v));
        wireCheck(wilderness, v -> setup.setInWilderness(v));
        wireCheck(forinthry, v -> setup.setForinthrySurge(v));
        wireCheck(markOfDarkness, v -> setup.setMarkOfDarkness(v));
        wireCheck(charge, v -> setup.setChargeSpell(v));
        wireCheck(kandarin, v -> setup.setKandarinDiary(v));
        wireCheck(sunfire, v -> setup.setSunfireRunes(v));

        slayerTask.setTooltip(new Tooltip("Enables black mask / slayer helmet bonuses"));
        wilderness.setTooltip(new Tooltip("Enables revenant (wilderness) weapon bonuses"));
        forinthry.setTooltip(new Tooltip("Boosts the Amulet of avarice bonus vs revenants"));
        markOfDarkness.setTooltip(new Tooltip("Boosts demonbane spells"));
        charge.setTooltip(new Tooltip("+10 max hit to god spells with the matching cape"));
        kandarin.setTooltip(new Tooltip("+10% enchanted bolt proc chance"));
        sunfire.setTooltip(new Tooltip("Fire spells: minimum hit of 10% of max"));

        chinDistance.setEditable(true);
        chinDistance.setPrefWidth(70);
        chinDistance.valueProperty().addListener((o, old, v) -> {
            if (!updating && setup != null && v != null) {
                setup.setChinchompaDistance(v);
                changed();
            }
        });

        GridPane combat = grid();
        combat.addRow(0, new Label("Attack type"), attackType, new Label("Stance"), stance);
        combat.addRow(1, new Label("Prayer"), prayer, new Label("Potion"), potion);
        HBox spellRow = new HBox(4, spellButton, clearSpell);
        HBox.setHgrow(spellButton, Priority.ALWAYS);
        combat.addRow(2, new Label("Spell"), spellRow, new Label("Chin distance"), chinDistance);

        FlowPane buffs = new FlowPane(14, 8, slayerTask, wilderness, forinthry,
                markOfDarkness, charge, kandarin, sunfire);

        GridPane gear = grid();
        int row = 0;
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            ImageView icon = new ImageView();
            icon.setFitWidth(24);
            icon.setFitHeight(24);
            icon.setPreserveRatio(true);
            gearIcons.put(slot, icon);

            Button pick = new Button("(empty)");
            pick.setMaxWidth(Double.MAX_VALUE);
            pick.setAlignment(Pos.CENTER_LEFT);
            pick.setGraphic(icon);
            pick.setOnAction(e -> pickItem(slot));
            Button clear = new Button("✕");
            clear.setOnAction(e -> {
                setup.setEquipped(slot, null);
                changed();
            });
            gearButtons.put(slot, pick);
            gear.addRow(row++, new Label(slot.displayName()), pick, clear);
        }
        ColumnConstraints c0 = new ColumnConstraints(72);
        ColumnConstraints c1 = new ColumnConstraints();
        c1.setHgrow(Priority.ALWAYS);
        c1.setFillWidth(true);
        gear.getColumnConstraints().addAll(c0, c1);

        bonusSummary.setWrapText(true);
        bonusSummary.getStyleClass().add("text-subtle");

        getChildren().addAll(
                labeledRow("Name", nameField),
                section("Combat"), combat,
                section("Buffs"), buffs,
                section("Equipment"), gear,
                bonusSummary);
    }

    private static GridPane grid() {
        GridPane g = new GridPane();
        g.setHgap(10);
        g.setVgap(6);
        return g;
    }

    private static Label section(String text) {
        Label l = new Label(text);
        l.getStyleClass().add("title-4");
        return l;
    }

    private static HBox labeledRow(String label, javafx.scene.Node node) {
        Label l = new Label(label);
        l.setMinWidth(50);
        HBox box = new HBox(10, l, node);
        box.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(node, Priority.ALWAYS);
        return box;
    }

    private <T> void wireCombo(ComboBox<T> combo, java.util.function.Consumer<T> apply) {
        combo.valueProperty().addListener((o, old, v) -> {
            if (!updating && setup != null && v != null) {
                apply.accept(v);
                changed();
            }
        });
    }

    private void wireCheck(CheckBox check, java.util.function.Consumer<Boolean> apply) {
        check.selectedProperty().addListener((o, old, v) -> {
            if (!updating && setup != null) {
                apply.accept(v);
                changed();
            }
        });
    }

    private void pickItem(EquipmentSlot slot) {
        SearchPickerDialog<EquipmentItem> picker = new SearchPickerDialog<>(
                "Choose " + slot.displayName().toLowerCase(),
                data.equipmentForSlot(slot),
                EquipmentItem::displayName,
                item -> item.image);
        EquipmentItem chosen = picker.showAndPick();
        if (chosen != null) {
            setup.setEquipped(slot, chosen);
            changed();
        }
    }

    private void pickSpell() {
        SearchPickerDialog<SpellData> picker = new SearchPickerDialog<>(
                "Choose spell", data.allSpells(), SpellData::displayName, s -> s.image);
        SpellData chosen = picker.showAndPick();
        if (chosen != null && setup != null) {
            setup.setSpell(chosen);
            refreshSpellButton();
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
            attackType.setValue(setup.getAttackType());
            refreshStyleDependentControls();
            stance.setValue(setup.getStance());
            prayer.setValue(setup.getPrayer());
            potion.setValue(setup.getPotion());
            slayerTask.setSelected(setup.isOnSlayerTask());
            wilderness.setSelected(setup.isInWilderness());
            forinthry.setSelected(setup.isForinthrySurge());
            markOfDarkness.setSelected(setup.isMarkOfDarkness());
            charge.setSelected(setup.isChargeSpell());
            kandarin.setSelected(setup.isKandarinDiary());
            sunfire.setSelected(setup.isSunfireRunes());
            chinDistance.getValueFactory().setValue(setup.getChinchompaDistance());
            refreshSpellButton();
            refreshGearButtons();
            refreshSummary();
        } finally {
            updating = false;
        }
    }

    /** Repopulates stance/prayer/potion options for the current attack type. */
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
        spellButton.setDisable(!isMagic);
        clearSpell.setDisable(!isMagic);
        chinDistance.setDisable(type != AttackType.RANGED);
    }

    private void refreshSpellButton() {
        SpellData spell = setup == null ? null : setup.getSpell();
        spellButton.setText(spell == null ? "(no spell)" : spell.displayName());
    }

    private void refreshGearButtons() {
        for (Map.Entry<EquipmentSlot, Button> e : gearButtons.entrySet()) {
            EquipmentItem item = setup.getEquipped(e.getKey());
            e.getValue().setText(item == null ? "(empty)" : item.displayName());
            ImageView icon = gearIcons.get(e.getKey());
            icon.setImage(null);
            if (item != null && item.image != null && !item.image.isBlank()) {
                EquipmentItem current = item;
                Image cached = ImageCache.cached(item.image);
                if (cached != null) {
                    icon.setImage(cached);
                } else {
                    ImageCache.load(item.image, img -> {
                        if (setup != null && setup.getEquipped(e.getKey()) == current) {
                            icon.setImage(img);
                        }
                    });
                }
            }
        }
    }

    private void refreshSummary() {
        if (setup == null) {
            bonusSummary.setText("");
            return;
        }
        bonusSummary.setText(String.format(
                "Bonuses - Stab %d | Slash %d | Crush %d | Ranged %d | Magic %d || "
                        + "Melee str %d | Ranged str %d | Magic dmg %.1f%% || Speed %d ticks",
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
        refreshSpellButton();
        if (onChanged != null) {
            onChanged.run();
        }
    }
}
