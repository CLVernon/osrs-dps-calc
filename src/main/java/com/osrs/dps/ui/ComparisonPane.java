package com.osrs.dps.ui;

import com.osrs.dps.calc.DpsCalculator;
import com.osrs.dps.calc.DpsResult;
import com.osrs.dps.model.Monster;
import com.osrs.dps.model.PlayerSetup;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.layout.VBox;

import java.util.List;

/** Table comparing every setup's DPS against the selected monster, best first. */
public class ComparisonPane extends VBox {

    /** One comparison row. */
    public record Row(PlayerSetup setup, DpsResult result, double ttkSeconds, boolean best) {
    }

    private final TableView<Row> table = new TableView<>();
    private final ObservableList<Row> rows = FXCollections.observableArrayList();
    private final Label notesLabel = new Label();

    public ComparisonPane() {
        setSpacing(6);
        setPadding(new Insets(8));

        table.setItems(rows);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        table.setPrefHeight(220);

        TableColumn<Row, String> nameCol = new TableColumn<>("Setup");
        nameCol.setCellValueFactory(d -> new ReadOnlyStringWrapper(d.getValue().setup().getName()));
        nameCol.setPrefWidth(180);

        TableColumn<Row, Number> dpsCol = numberColumn("DPS",
                r -> r.result().dps(), "%.3f");
        TableColumn<Row, Number> maxCol = numberColumn("Max hit",
                r -> (double) r.result().maxHit(), "%.0f");
        TableColumn<Row, Number> accCol = numberColumn("Accuracy",
                r -> r.result().accuracy() * 100, "%.1f%%");
        TableColumn<Row, Number> avgCol = numberColumn("Avg dmg/attack",
                r -> r.result().avgDamagePerAttack(), "%.2f");
        TableColumn<Row, Number> speedCol = numberColumn("Speed (s)",
                r -> r.result().attackIntervalSeconds(), "%.1f");
        TableColumn<Row, Number> ttkCol = numberColumn("Est. TTK (s)",
                Row::ttkSeconds, "%.1f");

        table.getColumns().setAll(List.of(nameCol, dpsCol, maxCol, accCol, avgCol, speedCol, ttkCol));

        // Highlight the best setup.
        table.setRowFactory(tv -> new TableRow<>() {
            @Override
            protected void updateItem(Row row, boolean empty) {
                super.updateItem(row, empty);
                if (!empty && row != null && row.best()) {
                    setStyle("-fx-background-color: #d8f5d8;");
                } else {
                    setStyle("");
                }
            }
        });

        table.getSelectionModel().selectedItemProperty().addListener((o, old, row) -> {
            if (row == null || row.result().notes().isEmpty()) {
                notesLabel.setText("");
            } else {
                notesLabel.setText("Applied effects: " + String.join("; ", row.result().notes()));
            }
        });
        notesLabel.setWrapText(true);
        notesLabel.setStyle("-fx-text-fill: #555;");

        getChildren().addAll(table, notesLabel);
    }

    private TableColumn<Row, Number> numberColumn(String title,
                                                  java.util.function.ToDoubleFunction<Row> extractor,
                                                  String format) {
        TableColumn<Row, Number> col = new TableColumn<>(title);
        col.setCellValueFactory(d ->
                new ReadOnlyObjectWrapper<>(extractor.applyAsDouble(d.getValue())));
        col.setCellFactory(c -> new TableCell<>() {
            @Override
            protected void updateItem(Number value, boolean empty) {
                super.updateItem(value, empty);
                setText(empty || value == null ? null : String.format(format, value.doubleValue()));
            }
        });
        return col;
    }

    /** Recomputes all rows for the given setups and monster. */
    public void refresh(List<PlayerSetup> setups, Monster monster) {
        rows.clear();
        notesLabel.setText("");
        if (monster == null || setups.isEmpty()) {
            return;
        }
        List<Row> computed = new java.util.ArrayList<>();
        double bestDps = 0;
        for (PlayerSetup setup : setups) {
            DpsResult result = DpsCalculator.calculate(setup, monster);
            bestDps = Math.max(bestDps, result.dps());
            computed.add(new Row(setup, result, result.expectedTimeToKill(monster.skills.hp), false));
        }
        final double best = bestDps;
        computed.sort((a, b) -> Double.compare(b.result().dps(), a.result().dps()));
        for (int i = 0; i < computed.size(); i++) {
            Row r = computed.get(i);
            computed.set(i, new Row(r.setup(), r.result(), r.ttkSeconds(),
                    best > 0 && r.result().dps() == best));
        }
        rows.setAll(computed);
    }
}
