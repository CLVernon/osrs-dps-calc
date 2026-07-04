package com.osrs.dps.ui;

import com.osrs.dps.model.Monster;
import javafx.geometry.Insets;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.Spinner;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;

import java.util.ArrayList;
import java.util.List;

/** Dialog for editing a monster's combat stats (custom monsters or tweaked copies). */
public final class MonsterEditorDialog {

    private MonsterEditorDialog() {
    }

    /** Shows an editor pre-filled from the given monster; returns an edited copy, or null. */
    public static Monster edit(Monster source) {
        Dialog<Monster> dialog = new Dialog<>();
        dialog.setTitle("Edit monster");
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(6);
        grid.setPadding(new Insets(10));

        TextField name = new TextField(source.name == null ? "Custom monster" : source.name);
        Spinner<Integer> hp = spinner(source.skills.hp, 1, 50000);
        Spinner<Integer> def = spinner(source.skills.def, 0, 5000);
        Spinner<Integer> magicLvl = spinner(source.skills.magic, 0, 5000);
        Spinner<Integer> size = spinner(Math.max(source.size, 1), 1, 10);

        Spinner<Integer> defStab = spinner(source.defensive.stab, -200, 1000);
        Spinner<Integer> defSlash = spinner(source.defensive.slash, -200, 1000);
        Spinner<Integer> defCrush = spinner(source.defensive.crush, -200, 1000);
        Spinner<Integer> defMagic = spinner(source.defensive.magic, -200, 1000);
        Spinner<Integer> defHeavy = spinner(source.defensive.heavy, -200, 1000);
        Spinner<Integer> defStandard = spinner(source.defensive.standard, -200, 1000);
        Spinner<Integer> defLight = spinner(source.defensive.light, -200, 1000);
        Spinner<Integer> flatArmour = spinner(source.defensive.flatArmour, 0, 100);

        CheckBox undead = attribute(source, "undead");
        CheckBox demon = attribute(source, "demon");
        CheckBox dragon = attribute(source, "dragon");
        CheckBox kalphite = attribute(source, "kalphite");
        CheckBox leafy = attribute(source, "leafy");
        CheckBox golem = attribute(source, "golem");
        CheckBox rat = attribute(source, "rat");
        CheckBox shade = attribute(source, "shade");
        CheckBox fiery = attribute(source, "fiery");
        CheckBox flying = attribute(source, "flying");
        CheckBox xerician = attribute(source, "xerician");
        CheckBox vampyre1 = exactAttribute(source, "vampyre1");
        CheckBox vampyre2 = exactAttribute(source, "vampyre2");
        CheckBox vampyre3 = exactAttribute(source, "vampyre3");
        Spinner<Integer> toaInvocation = spinner(source.toaInvocationLevel, 0, 600);

        ComboBox<String> weakElement = new ComboBox<>();
        weakElement.getItems().addAll("none", "air", "water", "earth", "fire");
        weakElement.setValue(source.weakness == null || source.weakness.element == null
                ? "none" : source.weakness.element);
        Spinner<Integer> weakSeverity = spinner(
                source.weakness == null ? 0 : source.weakness.severity, 0, 300);

        int row = 0;
        grid.addRow(row++, new Label("Name"), name);
        grid.addRow(row++, new Label("Hitpoints"), hp);
        grid.addRow(row++, new Label("Defence level"), def);
        grid.addRow(row++, new Label("Magic level"), magicLvl);
        grid.addRow(row++, new Label("Size (tiles)"), size);
        grid.addRow(row++, new Label("Def: Stab"), defStab);
        grid.addRow(row++, new Label("Def: Slash"), defSlash);
        grid.addRow(row++, new Label("Def: Crush"), defCrush);
        grid.addRow(row++, new Label("Def: Magic"), defMagic);
        grid.addRow(row++, new Label("Def: Ranged (heavy)"), defHeavy);
        grid.addRow(row++, new Label("Def: Ranged (standard)"), defStandard);
        grid.addRow(row++, new Label("Def: Ranged (light)"), defLight);
        grid.addRow(row++, new Label("Flat armour"), flatArmour);
        grid.addRow(row++, new Label("Attributes"), new javafx.scene.layout.HBox(8, undead, demon, dragon));
        grid.addRow(row++, new Label(""), new javafx.scene.layout.HBox(8, kalphite, leafy, golem));
        grid.addRow(row++, new Label(""), new javafx.scene.layout.HBox(8, rat, shade, fiery));
        grid.addRow(row++, new Label(""), new javafx.scene.layout.HBox(8, flying, xerician));
        grid.addRow(row++, new Label(""), new javafx.scene.layout.HBox(8, vampyre1, vampyre2, vampyre3));
        grid.addRow(row++, new Label("Elemental weakness"), weakElement);
        grid.addRow(row++, new Label("Weakness severity %"), weakSeverity);
        grid.addRow(row++, new Label("ToA invocation"), toaInvocation);

        dialog.getDialogPane().setContent(new javafx.scene.control.ScrollPane(grid));

        dialog.setResultConverter(button -> {
            if (button != ButtonType.OK) {
                return null;
            }
            Monster m = new Monster();
            m.id = source.id;
            m.name = name.getText().isBlank() ? "Custom monster" : name.getText().trim();
            m.version = "";
            m.speed = source.speed;
            m.level = source.level;
            m.skills.hp = hp.getValue();
            m.skills.def = def.getValue();
            m.skills.magic = magicLvl.getValue();
            m.skills.atk = source.skills.atk;
            m.skills.str = source.skills.str;
            m.skills.ranged = source.skills.ranged;
            m.size = size.getValue();
            m.defensive.stab = defStab.getValue();
            m.defensive.slash = defSlash.getValue();
            m.defensive.crush = defCrush.getValue();
            m.defensive.magic = defMagic.getValue();
            m.defensive.heavy = defHeavy.getValue();
            m.defensive.standard = defStandard.getValue();
            m.defensive.light = defLight.getValue();
            m.defensive.flatArmour = flatArmour.getValue();
            List<String> attrs = new ArrayList<>();
            addIf(attrs, undead, "undead");
            addIf(attrs, demon, "demon");
            addIf(attrs, dragon, "dragon");
            addIf(attrs, kalphite, "kalphite");
            addIf(attrs, leafy, "leafy");
            addIf(attrs, golem, "golem");
            addIf(attrs, rat, "rat");
            addIf(attrs, shade, "shade");
            addIf(attrs, fiery, "fiery");
            addIf(attrs, flying, "flying");
            addIf(attrs, xerician, "xerician");
            addIf(attrs, vampyre1, "vampyre1");
            addIf(attrs, vampyre2, "vampyre2");
            addIf(attrs, vampyre3, "vampyre3");
            m.attributes = attrs;
            m.toaInvocationLevel = toaInvocation.getValue();
            if (!"none".equals(weakElement.getValue()) && weakSeverity.getValue() > 0) {
                m.weakness = new Monster.Weakness();
                m.weakness.element = weakElement.getValue();
                m.weakness.severity = weakSeverity.getValue();
            }
            return m;
        });

        return dialog.showAndWait().orElse(null);
    }

    private static Spinner<Integer> spinner(int value, int min, int max) {
        Spinner<Integer> s = new Spinner<>(min, max, clamp(value, min, max));
        s.setEditable(true);
        s.setPrefWidth(110);
        return s;
    }

    private static int clamp(int v, int min, int max) {
        return Math.max(min, Math.min(max, v));
    }

    private static CheckBox attribute(Monster source, String attr) {
        CheckBox cb = new CheckBox(attr);
        cb.setSelected(source.hasAttribute(attr));
        return cb;
    }

    private static CheckBox exactAttribute(Monster source, String attr) {
        CheckBox cb = new CheckBox(attr);
        cb.setSelected(source.attributes != null && source.attributes.contains(attr));
        return cb;
    }

    private static void addIf(List<String> attrs, CheckBox box, String attr) {
        if (box.isSelected()) {
            attrs.add(attr);
        }
    }
}
