package com.osrs.dps.ui;

import com.osrs.dps.data.ImageCache;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.geometry.Pos;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.stage.Popup;
import javafx.util.Duration;

import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * An inline searchable dropdown: a text field that filters a popup list as you
 * type. Entries show an icon and a hover tooltip with details. Selecting an
 * entry fires the callback; in add-mode the field clears after selection so
 * several entries can be added in a row.
 */
public class SearchableDropdown<T> extends HBox {

    private final TextField field = new TextField();
    private final ImageView icon = new ImageView();
    private final Popup popup = new Popup();
    private final ListView<T> listView = new ListView<>();
    private final ObservableList<T> allItems = FXCollections.observableArrayList();
    private final FilteredList<T> filtered = new FilteredList<>(allItems);

    private final Function<T, String> labeller;
    private final Function<T, String> imageNamer;
    private final Function<T, String> tooltipper;
    private Consumer<T> onSelect;
    /** When true the field clears after selecting (for "add to list" usage). */
    private boolean clearOnSelect;

    private T value;
    private boolean updating;

    public SearchableDropdown(List<T> items, Function<T, String> labeller,
                              Function<T, String> imageNamer, Function<T, String> tooltipper) {
        this.labeller = labeller;
        this.imageNamer = imageNamer;
        this.tooltipper = tooltipper;
        allItems.setAll(items);

        setSpacing(4);
        setAlignment(Pos.CENTER_LEFT);
        icon.setFitWidth(22);
        icon.setFitHeight(22);
        icon.setPreserveRatio(true);
        field.setPromptText("Type to search...");
        HBox.setHgrow(field, Priority.ALWAYS);
        field.setMaxWidth(Double.MAX_VALUE);
        getChildren().addAll(icon, field);

        listView.setItems(filtered);
        listView.setPrefHeight(300);
        listView.setCellFactory(lv -> new DropdownCell());

        popup.getContent().add(listView);
        popup.setAutoHide(true);

        field.textProperty().addListener((o, old, text) -> {
            if (updating) {
                return;
            }
            String needle = text == null ? "" : text.toLowerCase(Locale.ROOT).trim();
            filtered.setPredicate(item -> needle.isEmpty()
                    || this.labeller.apply(item).toLowerCase(Locale.ROOT).contains(needle));
            if (!popup.isShowing() && field.isFocused()) {
                showPopup();
            }
            if (!filtered.isEmpty()) {
                listView.getSelectionModel().select(0);
                listView.scrollTo(0);
            }
        });

        field.setOnMouseClicked(e -> {
            if (!popup.isShowing()) {
                resetFilterAndShow();
            }
        });
        field.focusedProperty().addListener((o, old, focused) -> {
            if (!focused && !popup.isShowing()) {
                restoreDisplayText();
            }
        });
        field.setOnKeyPressed(e -> {
            switch (e.getCode()) {
                case DOWN -> {
                    if (!popup.isShowing()) {
                        resetFilterAndShow();
                    } else {
                        moveSelection(1);
                    }
                    e.consume();
                }
                case UP -> {
                    moveSelection(-1);
                    e.consume();
                }
                case ENTER -> {
                    T sel = listView.getSelectionModel().getSelectedItem();
                    if (popup.isShowing() && sel != null) {
                        choose(sel);
                    }
                    e.consume();
                }
                case ESCAPE -> {
                    popup.hide();
                    restoreDisplayText();
                    e.consume();
                }
                default -> {
                }
            }
        });
        listView.setOnMouseClicked(e -> {
            T sel = listView.getSelectionModel().getSelectedItem();
            if (sel != null) {
                choose(sel);
            }
        });
        listView.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ENTER) {
                T sel = listView.getSelectionModel().getSelectedItem();
                if (sel != null) {
                    choose(sel);
                }
            } else if (e.getCode() == KeyCode.ESCAPE) {
                popup.hide();
                restoreDisplayText();
            }
        });
        popup.setOnHidden(e -> restoreDisplayText());
    }

    private class DropdownCell extends ListCell<T> {
        private final ImageView cellIcon = new ImageView();

        DropdownCell() {
            cellIcon.setFitWidth(22);
            cellIcon.setFitHeight(22);
            cellIcon.setPreserveRatio(true);
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
                Image cached = ImageCache.cached(imageName);
                cellIcon.setImage(cached);
                setGraphic(cellIcon);
                if (cached == null && imageName != null && !imageName.isBlank()) {
                    T current = item;
                    ImageCache.load(imageName, img -> {
                        if (getItem() == current) {
                            cellIcon.setImage(img);
                        }
                    });
                }
            }
            if (tooltipper != null) {
                String text = tooltipper.apply(item);
                if (text != null && !text.isBlank()) {
                    Tooltip tip = new Tooltip(text);
                    tip.setShowDelay(Duration.millis(300));
                    tip.setStyle("-fx-font-family: monospace;");
                    setTooltip(tip);
                } else {
                    setTooltip(null);
                }
            }
        }
    }

    private void moveSelection(int delta) {
        int size = filtered.size();
        if (size == 0) {
            return;
        }
        int index = Math.max(0, Math.min(size - 1,
                listView.getSelectionModel().getSelectedIndex() + delta));
        listView.getSelectionModel().select(index);
        listView.scrollTo(index);
    }

    private void resetFilterAndShow() {
        updating = true;
        try {
            field.selectAll();
        } finally {
            updating = false;
        }
        filtered.setPredicate(item -> true);
        showPopup();
        if (value != null) {
            listView.getSelectionModel().select(value);
            listView.scrollTo(Math.max(0, listView.getSelectionModel().getSelectedIndex() - 3));
        } else if (!filtered.isEmpty()) {
            listView.getSelectionModel().select(0);
        }
    }

    private void showPopup() {
        listView.setPrefWidth(Math.max(getWidth(), 380));
        var screen = field.localToScreen(0, field.getHeight());
        if (screen != null) {
            popup.show(field, screen.getX() - icon.getFitWidth() - getSpacing(),
                    screen.getY() + 2);
        }
    }

    private void choose(T item) {
        popup.hide();
        if (clearOnSelect) {
            updating = true;
            try {
                field.setText("");
                icon.setImage(null);
            } finally {
                updating = false;
            }
        } else {
            setValue(item);
        }
        if (onSelect != null) {
            onSelect.accept(item);
        }
    }

    private void restoreDisplayText() {
        updating = true;
        try {
            if (clearOnSelect) {
                field.setText("");
            } else {
                field.setText(value == null ? "" : labeller.apply(value));
            }
        } finally {
            updating = false;
        }
    }

    /** Sets the displayed value without firing the selection callback. */
    public void setValue(T item) {
        this.value = item;
        updating = true;
        try {
            field.setText(item == null ? "" : labeller.apply(item));
        } finally {
            updating = false;
        }
        icon.setImage(null);
        if (item != null && imageNamer != null) {
            String imageName = imageNamer.apply(item);
            if (imageName != null && !imageName.isBlank()) {
                Image cached = ImageCache.cached(imageName);
                if (cached != null) {
                    icon.setImage(cached);
                } else {
                    T current = item;
                    ImageCache.load(imageName, img -> {
                        if (value == current) {
                            icon.setImage(img);
                        }
                    });
                }
            }
        }
    }

    public T getValue() {
        return value;
    }

    public void setOnSelect(Consumer<T> onSelect) {
        this.onSelect = onSelect;
    }

    /** In add-mode the field clears after each selection. */
    public void setClearOnSelect(boolean clearOnSelect) {
        this.clearOnSelect = clearOnSelect;
        if (clearOnSelect) {
            field.setPromptText("Type to add...");
        }
    }

    public void setItems(List<T> items) {
        allItems.setAll(items);
    }

    public void setFieldPromptText(String text) {
        field.setPromptText(text);
    }
}
