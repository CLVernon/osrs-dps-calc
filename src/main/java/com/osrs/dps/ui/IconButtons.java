package com.osrs.dps.ui;

import javafx.scene.control.Button;
import javafx.scene.control.Tooltip;
import javafx.util.Duration;
import org.kordamp.ikonli.Ikon;
import org.kordamp.ikonli.javafx.FontIcon;

/** Compact icon-only buttons with tooltips. */
public final class IconButtons {

    private IconButtons() {
    }

    public static Button of(Ikon icon, String tooltip) {
        Button button = new Button();
        button.setGraphic(new FontIcon(icon));
        Tooltip tip = new Tooltip(tooltip);
        tip.setShowDelay(Duration.millis(200));
        button.setTooltip(tip);
        return button;
    }
}
