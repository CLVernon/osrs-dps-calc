package com.osrs.dps.ui;

import com.osrs.dps.data.ImageCache;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.VBox;

import java.util.List;
import java.util.Locale;
import java.util.function.Function;

/** A type-to-filter picker dialog with a search box, icons, and a result list. */
public final class SearchPickerDialog<T> {

    private final String title;
    private final List<T> items;
    private final Function<T, String> labeller;
    private final Function<T, String> imageNamer;

    public SearchPickerDialog(String title, List<T> items, Function<T, String> labeller) {
        this(title, items, labeller, null);
    }

    public SearchPickerDialog(String title, List<T> items, Function<T, String> labeller,
                              Function<T, String> imageNamer) {
        this.title = title;
        this.items = items;
        this.labeller = labeller;
        this.imageNamer = imageNamer;
    }

    /** Shows the dialog; returns the chosen item or null if cancelled. */
    public T showAndPick() {
        Dialog<T> dialog = new Dialog<>();
        dialog.setTitle(title);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        TextField search = new TextField();
        search.setPromptText("Type to search...");

        ObservableList<T> all = FXCollections.observableArrayList(items);
        FilteredList<T> filtered = new FilteredList<>(all);
        ListView<T> listView = new ListView<>(filtered);
        listView.setPrefSize(440, 400);
        listView.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(T item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                    return;
                }
                setText(labeller.apply(item));
                if (imageNamer != null) {
                    String imageName = imageNamer.apply(item);
                    Image cached = ImageCache.cached(imageName);
                    ImageView view = new ImageView(cached);
                    view.setFitWidth(24);
                    view.setFitHeight(24);
                    view.setPreserveRatio(true);
                    setGraphic(view);
                    if (cached == null) {
                        T current = item;
                        ImageCache.load(imageName, img -> {
                            if (getItem() == current) {
                                view.setImage(img);
                            }
                        });
                    }
                } else {
                    setGraphic(null);
                }
            }
        });

        search.textProperty().addListener((obs, old, text) -> {
            String needle = text == null ? "" : text.toLowerCase(Locale.ROOT).trim();
            filtered.setPredicate(item ->
                    needle.isEmpty() || labeller.apply(item).toLowerCase(Locale.ROOT).contains(needle));
            if (!filtered.isEmpty()) {
                listView.getSelectionModel().select(0);
            }
        });
        search.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.DOWN) {
                listView.requestFocus();
                if (listView.getSelectionModel().isEmpty() && !filtered.isEmpty()) {
                    listView.getSelectionModel().select(0);
                }
            } else if (e.getCode() == KeyCode.ENTER && !listView.getSelectionModel().isEmpty()) {
                dialog.setResult(listView.getSelectionModel().getSelectedItem());
                dialog.close();
            }
        });
        listView.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2 && !listView.getSelectionModel().isEmpty()) {
                dialog.setResult(listView.getSelectionModel().getSelectedItem());
                dialog.close();
            }
        });

        VBox content = new VBox(8, search, listView);
        dialog.getDialogPane().setContent(content);
        dialog.setResultConverter(button ->
                button == ButtonType.OK ? listView.getSelectionModel().getSelectedItem() : null);
        javafx.application.Platform.runLater(search::requestFocus);
        return dialog.showAndWait().orElse(null);
    }
}
