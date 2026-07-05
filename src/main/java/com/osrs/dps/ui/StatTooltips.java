package com.osrs.dps.ui;

import com.osrs.dps.model.EquipmentItem;
import com.osrs.dps.model.Monster;
import com.osrs.dps.model.SpellData;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;

import java.util.List;

/** Builds formatted hover-tooltip stat cards for dropdown entries. */
public final class StatTooltips {

    private static final String HEADER_STYLE = "-fx-font-weight: bold; -fx-font-size: 13px;";
    private static final String SECTION_STYLE = "-fx-text-fill: #9da7b3; -fx-font-size: 11px;";
    private static final String POSITIVE = "-fx-text-fill: #7ee787;";
    private static final String NEGATIVE = "-fx-text-fill: #ff7b72;";
    private static final String ZERO = "-fx-text-fill: #767d86;";

    private StatTooltips() {
    }

    public static Node forEquipment(EquipmentItem item) {
        VBox box = card(item.displayName());

        GridPane grid = grid();
        String[] styles = {"Stab", "Slash", "Crush", "Magic", "Ranged"};
        for (int i = 0; i < styles.length; i++) {
            Label header = new Label(styles[i]);
            header.setStyle(SECTION_STYLE);
            GridPane.setHalignment(header, HPos.RIGHT);
            grid.add(header, i + 1, 0);
        }
        grid.add(sectionLabel("Attack"), 0, 1);
        grid.add(sectionLabel("Defence"), 0, 2);
        int[] attack = {item.offensive.stab, item.offensive.slash, item.offensive.crush,
                item.offensive.magic, item.offensive.ranged};
        int[] defence = {item.defensive.stab, item.defensive.slash, item.defensive.crush,
                item.defensive.magic, item.defensive.ranged};
        for (int i = 0; i < 5; i++) {
            grid.add(valueLabel(attack[i]), i + 1, 1);
            grid.add(valueLabel(defence[i]), i + 1, 2);
        }
        box.getChildren().add(grid);

        GridPane other = grid();
        other.add(sectionLabel("Melee str"), 0, 0);
        other.add(valueLabel(item.bonuses.str), 1, 0);
        other.add(sectionLabel("Ranged str"), 2, 0);
        other.add(valueLabel(item.bonuses.rangedStr), 3, 0);
        other.add(sectionLabel("Magic dmg"), 0, 1);
        other.add(percentLabel(item.bonuses.magicStr / 10.0), 1, 1);
        other.add(sectionLabel("Prayer"), 2, 1);
        other.add(valueLabel(item.bonuses.prayer), 3, 1);
        box.getChildren().add(other);

        if ("weapon".equals(item.slot)) {
            StringBuilder meta = new StringBuilder("Speed " + item.speed + " ticks");
            if (item.category != null && !item.category.isBlank()) {
                meta.append("  •  ").append(item.category);
            }
            if (item.twoHanded) {
                meta.append("  •  two-handed");
            }
            Label metaLabel = new Label(meta.toString());
            metaLabel.setStyle(SECTION_STYLE);
            box.getChildren().add(metaLabel);
        }
        return box;
    }

    public static Node forMonster(Monster m) {
        VBox box = card(m.displayName());

        GridPane top = grid();
        addPair(top, 0, 0, "HP", m.skills.hp);
        addPair(top, 2, 0, "Defence", m.skills.def);
        addPair(top, 0, 1, "Magic", m.skills.magic);
        addPair(top, 2, 1, "Size", m.size);
        box.getChildren().add(top);

        Label defHeader = new Label("Defence bonuses");
        defHeader.setStyle(SECTION_STYLE);
        box.getChildren().add(defHeader);

        GridPane def = grid();
        String[] headers = {"Stab", "Slash", "Crush", "Magic"};
        int[] values = {m.defensive.stab, m.defensive.slash, m.defensive.crush, m.defensive.magic};
        for (int i = 0; i < headers.length; i++) {
            Label header = new Label(headers[i]);
            header.setStyle(SECTION_STYLE);
            GridPane.setHalignment(header, HPos.RIGHT);
            def.add(header, i, 0);
            def.add(valueLabel(values[i]), i, 1);
        }
        box.getChildren().add(def);

        GridPane rangedDef = grid();
        String[] rangedHeaders = {"Light", "Standard", "Heavy"};
        int[] rangedValues = {m.defensive.light, m.defensive.standard, m.defensive.heavy};
        for (int i = 0; i < rangedHeaders.length; i++) {
            Label header = new Label(rangedHeaders[i]);
            header.setStyle(SECTION_STYLE);
            GridPane.setHalignment(header, HPos.RIGHT);
            rangedDef.add(header, i + 1, 0);
            rangedDef.add(valueLabel(rangedValues[i]), i + 1, 1);
        }
        rangedDef.add(sectionLabel("Ranged"), 0, 1);
        box.getChildren().add(rangedDef);

        if (m.defensive.flatArmour > 0) {
            box.getChildren().add(plainLabel("Flat armour " + m.defensive.flatArmour));
        }
        List<String> attrs = m.attributes;
        if (attrs != null && !attrs.isEmpty()) {
            Label attrLabel = plainLabel("Attributes: " + String.join(", ", attrs));
            attrLabel.setWrapText(true);
            attrLabel.setMaxWidth(320);
            box.getChildren().add(attrLabel);
        }
        if (m.weakness != null && m.weakness.element != null
                && !"none".equalsIgnoreCase(m.weakness.element)) {
            Label weak = new Label("Weak to " + m.weakness.element
                    + " (+" + m.weakness.severity + "% severity)");
            weak.setStyle(POSITIVE);
            box.getChildren().add(weak);
        }
        return box;
    }

    public static Node forSpell(SpellData spell) {
        VBox box = card(spell.name);
        GridPane grid = grid();
        addPair(grid, 0, 0, "Max hit", spell.maxHit);
        box.getChildren().add(grid);
        StringBuilder meta = new StringBuilder();
        if (spell.spellbook != null) {
            meta.append(capitalize(spell.spellbook)).append(" spellbook");
        }
        if (spell.effectiveElement() != null) {
            if (meta.length() > 0) {
                meta.append("  •  ");
            }
            meta.append(capitalize(spell.effectiveElement())).append(" element");
        }
        if (meta.length() > 0) {
            Label metaLabel = new Label(meta.toString());
            metaLabel.setStyle(SECTION_STYLE);
            box.getChildren().add(metaLabel);
        }
        return box;
    }

    // ---------------------------------------------------------------- helpers

    private static VBox card(String title) {
        Label header = new Label(title);
        header.setStyle(HEADER_STYLE);
        VBox box = new VBox(6, header);
        box.setPadding(new Insets(4));
        return box;
    }

    private static GridPane grid() {
        GridPane grid = new GridPane();
        grid.setHgap(14);
        grid.setVgap(2);
        return grid;
    }

    static Label sectionLabel(String text) {
        Label label = new Label(text);
        label.setStyle(SECTION_STYLE);
        return label;
    }

    private static Label plainLabel(String text) {
        return new Label(text);
    }

    static Label valueLabel(int value) {
        Label label = new Label((value > 0 ? "+" : "") + value);
        label.setStyle(value > 0 ? POSITIVE : value < 0 ? NEGATIVE : ZERO);
        GridPane.setHalignment(label, HPos.RIGHT);
        return label;
    }

    static Label percentLabel(double percent) {
        String text = (percent > 0 ? "+" : "") + (percent == Math.rint(percent)
                ? String.valueOf((int) percent) : String.valueOf(percent)) + "%";
        Label label = new Label(text);
        label.setStyle(percent > 0 ? POSITIVE : percent < 0 ? NEGATIVE : ZERO);
        GridPane.setHalignment(label, HPos.RIGHT);
        return label;
    }

    private static void addPair(GridPane grid, int col, int row, String name, int value) {
        grid.add(sectionLabel(name), col, row);
        Label label = new Label(String.valueOf(value));
        grid.add(label, col + 1, row);
    }

    private static String capitalize(String s) {
        if (s == null || s.isEmpty()) {
            return "";
        }
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }
}
