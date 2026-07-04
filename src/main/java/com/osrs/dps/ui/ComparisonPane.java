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
import javafx.scene.control.TablePosition;
import javafx.scene.control.TableView;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

/**
 * Matrix comparing every gear setup (rows) against every target monster (columns).
 * Cells show DPS; the best setup per monster is highlighted. A total time-to-kill
 * column sums the expected kill times across all targets.
 */
public class ComparisonPane extends VBox {

    /** One comparison row: a setup and its result against each monster. */
    public record Row(PlayerSetup setup, Map<Monster, DpsResult> results, double totalTtk) {
    }

    private final TableView<Row> table = new TableView<>();
    private final ObservableList<Row> rows = FXCollections.observableArrayList();
    private final Label detailLabel = new Label();
    /** Best DPS per monster, for per-column highlighting. */
    private final Map<Monster, Double> bestDps = new IdentityHashMap<>();
    private double bestTotalTtk = Double.POSITIVE_INFINITY;
    /** Monster shown in each table column, aligned with the column list. */
    private final Map<TableColumn<Row, ?>, Monster> columnMonsters = new IdentityHashMap<>();

    public ComparisonPane() {
        setSpacing(6);
        setPadding(new Insets(8));

        table.setItems(rows);
        table.setPrefHeight(240);
        table.getSelectionModel().setCellSelectionEnabled(true);
        table.getSelectionModel().getSelectedCells().addListener(
                (javafx.collections.ListChangeListener<TablePosition>) c -> updateDetail());

        detailLabel.setWrapText(true);
        detailLabel.getStyleClass().add("text-subtle");

        getChildren().addAll(table, detailLabel);
    }

    /** Recomputes the matrix for the given setups and monsters. */
    public void refresh(List<PlayerSetup> setups, List<Monster> monsters) {
        rows.clear();
        bestDps.clear();
        columnMonsters.clear();
        bestTotalTtk = Double.POSITIVE_INFINITY;
        detailLabel.setText("");

        List<TableColumn<Row, ?>> columns = new ArrayList<>();
        TableColumn<Row, String> nameCol = new TableColumn<>("Setup");
        nameCol.setCellValueFactory(d -> new ReadOnlyStringWrapper(d.getValue().setup().getName()));
        nameCol.setPrefWidth(170);
        columns.add(nameCol);

        if (monsters.isEmpty() || setups.isEmpty()) {
            table.getColumns().setAll(columns);
            return;
        }

        // Compute all results and the best-per-column values first
        List<Row> computed = new ArrayList<>();
        for (PlayerSetup setup : setups) {
            Map<Monster, DpsResult> results = new IdentityHashMap<>();
            double totalTtk = 0;
            for (Monster monster : monsters) {
                DpsResult result = DpsCalculator.calculate(setup, monster);
                results.put(monster, result);
                totalTtk += result.ttkSeconds();
                bestDps.merge(monster, result.dps(), Math::max);
            }
            bestTotalTtk = Math.min(bestTotalTtk, totalTtk);
            computed.add(new Row(setup, results, totalTtk));
        }

        for (Monster monster : monsters) {
            TableColumn<Row, DpsResult> col = new TableColumn<>(columnTitle(monster));
            col.setCellValueFactory(d -> new ReadOnlyObjectWrapper<>(d.getValue().results().get(monster)));
            col.setPrefWidth(Math.max(90, columnTitle(monster).length() * 7.5));
            col.setCellFactory(c -> new TableCell<>() {
                @Override
                protected void updateItem(DpsResult result, boolean empty) {
                    super.updateItem(result, empty);
                    if (empty || result == null) {
                        setText(null);
                        setTooltip(null);
                        setStyle("");
                        return;
                    }
                    setText(result.dps() <= 0 ? "-" : String.format("%.2f", result.dps()));
                    setTooltip(new Tooltip(String.format(
                            "Max hit %d | Accuracy %.1f%% | Avg %.2f/attack | TTK %s",
                            result.maxHit(), result.accuracy() * 100,
                            result.avgDamagePerAttack(), formatSeconds(result.ttkSeconds()))));
                    Double best = bestDps.get(monster);
                    boolean isBest = best != null && best > 0 && result.dps() >= best - 1e-9;
                    setStyle(isBest ? "-fx-background-color: rgba(46,160,67,0.30);"
                                    : "");
                }
            });
            columnMonsters.put(col, monster);
            columns.add(col);
        }

        if (monsters.size() > 1) {
            TableColumn<Row, Number> totalCol = new TableColumn<>("Total TTK");
            totalCol.setCellValueFactory(d -> new ReadOnlyObjectWrapper<>(d.getValue().totalTtk()));
            totalCol.setPrefWidth(90);
            totalCol.setCellFactory(c -> new TableCell<>() {
                @Override
                protected void updateItem(Number value, boolean empty) {
                    super.updateItem(value, empty);
                    if (empty || value == null) {
                        setText(null);
                        setStyle("");
                        return;
                    }
                    double ttk = value.doubleValue();
                    setText(formatSeconds(ttk));
                    boolean isBest = Double.isFinite(bestTotalTtk) && ttk <= bestTotalTtk + 1e-9;
                    setStyle(isBest ? "-fx-background-color: rgba(46,160,67,0.30);" : "");
                }
            });
            columns.add(totalCol);
        }

        table.getColumns().setAll(columns);
        rows.setAll(computed);
    }

    private static String columnTitle(Monster monster) {
        String name = monster.displayName();
        return name.length() > 24 ? name.substring(0, 22) + "..." : name;
    }

    private static String formatSeconds(double seconds) {
        if (!Double.isFinite(seconds)) {
            return "∞";
        }
        if (seconds >= 120) {
            return String.format("%dm %02ds", (int) (seconds / 60), (int) (seconds % 60));
        }
        return String.format("%.1fs", seconds);
    }

    /** Shows the applied special effects for the selected setup/monster cell. */
    private void updateDetail() {
        detailLabel.setText("");
        var cells = table.getSelectionModel().getSelectedCells();
        if (cells.isEmpty()) {
            return;
        }
        TablePosition<?, ?> pos = cells.get(0);
        if (pos.getRow() < 0 || pos.getRow() >= rows.size()) {
            return;
        }
        Row row = rows.get(pos.getRow());
        Monster monster = columnMonsters.get(pos.getTableColumn());
        if (monster == null) {
            return;
        }
        DpsResult result = row.results().get(monster);
        if (result == null) {
            return;
        }
        StringBuilder text = new StringBuilder();
        text.append(row.setup().getName()).append(" vs ").append(monster.displayName())
                .append(": max hit ").append(result.maxHit())
                .append(", accuracy ").append(String.format("%.1f%%", result.accuracy() * 100))
                .append(", TTK ").append(formatSeconds(result.ttkSeconds()));
        if (!result.notes().isEmpty()) {
            text.append("  |  ").append(String.join("; ", result.notes()));
        }
        detailLabel.setText(text.toString());
    }
}
