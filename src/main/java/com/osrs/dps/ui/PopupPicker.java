package com.osrs.dps.ui;

import javafx.collections.FXCollections;
import javafx.collections.transformation.FilteredList;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.VBox;
import javafx.stage.Popup;
import javafx.util.Duration;

import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * A search-filtered picker in a popup anchored to any node (used by the
 * OSRS-style equipment slot buttons). Same look and behaviour as
 * {@link SearchableDropdown}'s list, but opened on demand.
 */
public final class PopupPicker {

    private PopupPicker() {
    }

    public static <T> void show(Node anchor, List<T> items,
                                Function<T, String> labeller,
                                Function<T, String> imageNamer,
                                Function<T, Node> tooltipper,
                                Consumer<T> onPick) {
        Popup popup = new Popup();
        popup.setAutoHide(true);

        TextField search = new TextField();
        search.setPromptText("Type to search...");

        FilteredList<T> filtered = new FilteredList<>(FXCollections.observableArrayList(items));
        ListView<T> listView = new ListView<>(filtered);
        listView.setPrefSize(400, 320);
        listView.setCellFactory(lv -> new ListCell<>() {
            private final ImageView icon = new ImageView();

            {
                icon.setFitWidth(22);
                icon.setFitHeight(22);
                icon.setPreserveRatio(true);
            }

            @Override
            protected void updateItem(T item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                    setTooltip(null);
                    return;
                }
                setText(labeller.apply(item));
                if (imageNamer != null) {
                    String imageName = imageNamer.apply(item);
                    Image cached = com.osrs.dps.data.ImageCache.cached(imageName);
                    icon.setImage(cached);
                    setGraphic(icon);
                    if (cached == null && imageName != null && !imageName.isBlank()) {
                        T current = item;
                        com.osrs.dps.data.ImageCache.load(imageName, img -> {
                            if (getItem() == current) {
                                icon.setImage(img);
                            }
                        });
                    }
                }
                if (tooltipper != null) {
                    Node content = tooltipper.apply(item);
                    if (content != null) {
                        Tooltip tip = new Tooltip();
                        tip.setGraphic(content);
                        tip.setShowDelay(Duration.millis(250));
                        tip.setShowDuration(Duration.INDEFINITE);
                        setTooltip(tip);
                    }
                }
            }
        });

        search.textProperty().addListener((o, old, text) -> {
            String needle = text == null ? "" : text.toLowerCase(Locale.ROOT).trim();
            filtered.setPredicate(item -> needle.isEmpty()
                    || labeller.apply(item).toLowerCase(Locale.ROOT).contains(needle));
            if (!filtered.isEmpty()) {
                listView.getSelectionModel().select(0);
                listView.scrollTo(0);
            }
        });
        Runnable pick = () -> {
            T sel = listView.getSelectionModel().getSelectedItem();
            if (sel != null) {
                popup.hide();
                onPick.accept(sel);
            }
        };
        search.setOnKeyPressed(e -> {
            switch (e.getCode()) {
                case DOWN -> {
                    listView.requestFocus();
                    if (listView.getSelectionModel().isEmpty() && !filtered.isEmpty()) {
                        listView.getSelectionModel().select(0);
                    }
                    e.consume();
                }
                case ENTER -> {
                    pick.run();
                    e.consume();
                }
                case ESCAPE -> {
                    popup.hide();
                    e.consume();
                }
                default -> {
                }
            }
        });
        listView.setOnMouseClicked(e -> pick.run());
        listView.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ENTER) {
                pick.run();
            } else if (e.getCode() == KeyCode.ESCAPE) {
                popup.hide();
            }
        });

        VBox content = new VBox(6, search, listView);
        content.setPadding(new Insets(6));
        content.setStyle("-fx-background-color: -color-bg-default; -fx-background-radius: 6;"
                + " -fx-border-color: -color-border-default; -fx-border-radius: 6;");
        popup.getContent().add(content);

        var screen = anchor.localToScreen(0, anchor.getLayoutBounds().getHeight());
        if (screen != null) {
            popup.show(anchor, screen.getX(), screen.getY() + 2);
        }
        javafx.application.Platform.runLater(search::requestFocus);
    }
}
