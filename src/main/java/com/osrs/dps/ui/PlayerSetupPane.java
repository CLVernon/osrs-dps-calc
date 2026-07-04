package com.osrs.dps.ui;

import com.osrs.dps.data.DataRepository;
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
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import java.util.Arrays;
import java.util.EnumMap;
import java.util.Map;

/** Editor form for a single gear setup. Mutates the setup and notifies on change. */
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
    private final SearchableDropdown<SpellData> spellDropdown = new SearchableDropdown<>(
            java.util.List.of(), SpellData::displayName, s -> s.image, StatTooltips::forSpell);
    private final Button clearSpell = new Button("✕");
    private final CheckBox slayerTask = buff("On Slayer task", "Slayer icon.png",
            "Enables black mask / slayer helmet bonuses against the target");
    private final CheckBox wilderness = buff("In Wilderness", "Skull (status) icon.png",
            "Target is in the Wilderness: enables revenant weapon bonuses\n"
                    + "(Craw's/Webweaver bow, Viggora's/Ursine chainmace, Thammaron's/Accursed sceptre)");
    private final CheckBox forinthry = buff("Forinthry Surge", "Skull (status) icon.png",
            "Boosts the Amulet of avarice bonus vs revenants (35% instead of 20%)");
    private final CheckBox markOfDarkness = buff("Mark of Darkness", "Mark of Darkness.png",
            "Doubles demonbane spell accuracy bonus and adds +25% damage vs demons\n"
                    + "(+50% with the Purging staff)");
    private final CheckBox charge = buff("Charge", "Charge.png",
            "+10 max hit to god spells when wearing the matching god cape");
    private final CheckBox kandarin = buff("Kandarin diary", "Achievement Diaries icon.png",
            "Kandarin hard diary: +10% enchanted bolt proc chance");
    private final CheckBox sunfire = buff("Sunfire runes", "Sunfire rune.png",
            "Fire spells get a minimum hit of 10% of the max hit");
    private final Spinner<Integer> chinDistance = new Spinner<>(1, 10, 5);
    private final Map<EquipmentSlot, Button> slotButtons = new EnumMap<>(EquipmentSlot.class);
    private final Map<EquipmentSlot, ImageView> slotIcons = new EnumMap<>(EquipmentSlot.class);
    private final Label bonusSummary = new Label();

    private static final Map<EquipmentSlot, String> SLOT_PLACEHOLDERS = new EnumMap<>(Map.of(
            EquipmentSlot.HEAD, "Head slot.png",
            EquipmentSlot.CAPE, "Cape slot.png",
            EquipmentSlot.NECK, "Neck slot.png",
            EquipmentSlot.AMMO, "Ammo slot.png",
            EquipmentSlot.WEAPON, "Weapon slot.png",
            EquipmentSlot.BODY, "Body slot.png",
            EquipmentSlot.SHIELD, "Shield slot.png",
            EquipmentSlot.LEGS, "Legs slot.png",
            EquipmentSlot.HANDS, "Hands slot.png",
            EquipmentSlot.FEET, "Feet slot.png"));

    static {
        SLOT_PLACEHOLDERS.put(EquipmentSlot.RING, "Ring slot.png");
    }

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
        IconCombo.decorate(attackType, AttackType::imageName);
        attackType.valueProperty().addListener((o, old, type) -> {
            if (!updating && setup != null && type != null) {
                setup.setAttackType(type);
                refreshStyleDependentControls();
                changed();
            }
        });
        IconCombo.decorate(prayer, Prayer::imageName);
        IconCombo.decorate(potion, Potion::imageName);
        wireCombo(stance, v -> setup.setStance(v));
        wireCombo(prayer, v -> setup.setPrayer(v));
        wireCombo(potion, v -> setup.setPotion(v));

        spellDropdown.setItems(data.allSpells());
        spellDropdown.setFieldPromptText("(no spell)");
        spellDropdown.setOnSelect(spell -> {
            if (setup != null) {
                setup.setSpell(spell);
                changed();
            }
        });
        clearSpell.setOnAction(e -> {
            if (setup != null) {
                setup.setSpell(null);
                refreshSpellDropdown();
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
        HBox spellRow = new HBox(4, spellDropdown, clearSpell);
        HBox.setHgrow(spellDropdown, Priority.ALWAYS);
        combat.addRow(2, new Label("Spell"), spellRow, new Label("Chin distance"), chinDistance);

        FlowPane buffs = new FlowPane(14, 8, slayerTask, wilderness, forinthry,
                markOfDarkness, charge, kandarin, sunfire);

        bonusSummary.setWrapText(true);
        bonusSummary.getStyleClass().add("text-subtle");

        getChildren().addAll(
                labeledRow("Name", nameField),
                section("Combat"), combat,
                section("Buffs"), buffs,
                section("Equipment"), buildEquipmentGrid(),
                bonusSummary);
    }

    // ------------------------------------------------- OSRS-style slot layout

    private GridPane buildEquipmentGrid() {
        GridPane grid = new GridPane();
        grid.setHgap(6);
        grid.setVgap(6);
        grid.setAlignment(Pos.TOP_LEFT);

        placeSlot(grid, EquipmentSlot.HEAD, 1, 0);
        placeSlot(grid, EquipmentSlot.CAPE, 0, 1);
        placeSlot(grid, EquipmentSlot.NECK, 1, 1);
        placeSlot(grid, EquipmentSlot.AMMO, 2, 1);
        placeSlot(grid, EquipmentSlot.WEAPON, 0, 2);
        placeSlot(grid, EquipmentSlot.BODY, 1, 2);
        placeSlot(grid, EquipmentSlot.SHIELD, 2, 2);
        placeSlot(grid, EquipmentSlot.LEGS, 1, 3);
        placeSlot(grid, EquipmentSlot.HANDS, 0, 4);
        placeSlot(grid, EquipmentSlot.FEET, 1, 4);
        placeSlot(grid, EquipmentSlot.RING, 2, 4);
        return grid;
    }

    private void placeSlot(GridPane grid, EquipmentSlot slot, int col, int row) {
        ImageView icon = new ImageView();
        icon.setFitWidth(32);
        icon.setFitHeight(32);
        icon.setPreserveRatio(true);
        slotIcons.put(slot, icon);

        Button button = new Button();
        button.setGraphic(icon);
        button.setPrefSize(52, 52);
        button.setMinSize(52, 52);
        button.setOnMouseClicked(e -> {
            if (e.getButton() == MouseButton.SECONDARY) {
                if (setup != null) {
                    setup.setEquipped(slot, null);
                    changed();
                }
            } else if (e.getButton() == MouseButton.PRIMARY) {
                openSlotPicker(slot, button);
            }
        });
        slotButtons.put(slot, button);
        grid.add(button, col, row);
        refreshSlot(slot);
    }

    private void openSlotPicker(EquipmentSlot slot, Button anchor) {
        if (setup == null) {
            return;
        }
        PopupPicker.show(anchor, data.equipmentForSlot(slot),
                EquipmentItem::displayName, item -> item.image, StatTooltips::forEquipment,
                item -> {
                    setup.setEquipped(slot, item);
                    changed();
                });
    }

    private void refreshSlot(EquipmentSlot slot) {
        Button button = slotButtons.get(slot);
        ImageView icon = slotIcons.get(slot);
        EquipmentItem item = setup == null ? null : setup.getEquipped(slot);
        if (item == null) {
            Icons.set(icon, SLOT_PLACEHOLDERS.get(slot));
            icon.setOpacity(0.35);
            Tooltip tip = new Tooltip(slot.displayName()
                    + " slot\nClick to choose an item; right-click to clear");
            tip.setShowDelay(Duration.millis(250));
            button.setTooltip(tip);
        } else {
            Icons.set(icon, item.image);
            icon.setOpacity(1.0);
            Tooltip tip = new Tooltip();
            javafx.scene.layout.VBox content = new javafx.scene.layout.VBox(4,
                    StatTooltips.forEquipment(item),
                    subtleLabel("Click to change; right-click to clear"));
            tip.setGraphic(content);
            tip.setShowDelay(Duration.millis(250));
            tip.setShowDuration(Duration.INDEFINITE);
            button.setTooltip(tip);
        }
    }

    private static Label subtleLabel(String text) {
        Label label = new Label(text);
        label.setStyle("-fx-text-fill: #9da7b3; -fx-font-size: 11px;");
        return label;
    }

    // ---------------------------------------------------------------- helpers

    private static CheckBox buff(String text, String imageName, String tooltip) {
        CheckBox box = new CheckBox(text);
        box.setGraphic(Icons.view(imageName, 16));
        Tooltip tip = new Tooltip(tooltip);
        tip.setShowDelay(Duration.millis(200));
        tip.setShowDuration(Duration.INDEFINITE);
        tip.setWrapText(true);
        tip.setMaxWidth(360);
        box.setTooltip(tip);
        return box;
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
            refreshSpellDropdown();
            refreshAllSlots();
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
        spellDropdown.setDisable(!isMagic);
        clearSpell.setDisable(!isMagic);
        chinDistance.setDisable(type != AttackType.RANGED);
    }

    private void refreshSpellDropdown() {
        spellDropdown.setValue(setup == null ? null : setup.getSpell());
    }

    private void refreshAllSlots() {
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            refreshSlot(slot);
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
        refreshAllSlots();
        refreshSpellDropdown();
        if (onChanged != null) {
            onChanged.run();
        }
    }
}
