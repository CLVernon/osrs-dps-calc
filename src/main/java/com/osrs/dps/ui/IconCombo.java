package com.osrs.dps.ui;

import javafx.scene.control.ComboBox;
import javafx.scene.control.ListCell;

import java.util.function.Function;

/** Decorates a ComboBox so entries show a wiki icon next to their text. */
public final class IconCombo {

    private IconCombo() {
    }

    public static <T> void decorate(ComboBox<T> combo, Function<T, String> imageNamer) {
        combo.setCellFactory(lv -> cell(imageNamer));
        combo.setButtonCell(cell(imageNamer));
    }

    private static <T> ListCell<T> cell(Function<T, String> imageNamer) {
        return new ListCell<>() {
            private final javafx.scene.image.ImageView icon = new javafx.scene.image.ImageView();

            {
                icon.setFitWidth(18);
                icon.setFitHeight(18);
                icon.setPreserveRatio(true);
            }

            @Override
            protected void updateItem(T item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                    return;
                }
                setText(item.toString());
                String imageName = imageNamer.apply(item);
                if (imageName == null || imageName.isBlank()) {
                    setGraphic(null);
                } else {
                    Icons.set(icon, imageName);
                    setGraphic(icon);
                }
            }
        };
    }
}
