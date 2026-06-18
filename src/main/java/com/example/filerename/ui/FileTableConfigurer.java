package com.example.filerename.ui;

import com.example.filerename.model.FileRowViewModel;
import com.example.filerename.model.RenameStatus;
import com.example.filerename.util.AnimationUtils;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.VBox;

import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

public final class FileTableConfigurer {

    private FileTableConfigurer() {
    }

    public static void configure(
            TableView<FileRowViewModel> table,
            TableColumn<FileRowViewModel, String> originalColumn,
            TableColumn<FileRowViewModel, String> newColumn,
            TableColumn<FileRowViewModel, String> typeColumn,
            TableColumn<FileRowViewModel, String> sizeColumn,
            TableColumn<FileRowViewModel, RenameStatus> statusColumn,
            TableColumn<FileRowViewModel, Void> actionColumn,
            Consumer<FileRowViewModel> removeHandler
    ) {
        Objects.requireNonNull(table, "table");
        Objects.requireNonNull(removeHandler, "removeHandler");

        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        table.setPlaceholder(createEmptyState());

        originalColumn.setCellValueFactory(data -> data.getValue().originalNameProperty());
        newColumn.setCellValueFactory(data -> data.getValue().proposedNameProperty());
        typeColumn.setCellValueFactory(data -> data.getValue().typeProperty());
        sizeColumn.setCellValueFactory(data -> data.getValue().sizeProperty());
        statusColumn.setCellValueFactory(data -> data.getValue().statusProperty());

        newColumn.setCellFactory(column -> new AccentTextCell());
        statusColumn.setCellFactory(column -> new StatusCell());
        actionColumn.setCellFactory(column -> new RemoveCell(removeHandler));

        List.of(originalColumn, newColumn, typeColumn, sizeColumn, statusColumn, actionColumn)
                .forEach(column -> column.setSortable(false));

        table.setRowFactory(ignored -> createAnimatedRow());
    }

    private static TableRow<FileRowViewModel> createAnimatedRow() {
        TableRow<FileRowViewModel> row = new TableRow<>();
        row.setOnMouseEntered(event -> {
            if (!row.isEmpty()) {
                AnimationUtils.scaleTo(row, 1.003, 100);
            }
        });
        row.setOnMouseExited(event -> AnimationUtils.scaleTo(row, 1.0, 120));
        return row;
    }

    private static Node createEmptyState() {
        VBox empty = new VBox(8);
        empty.setAlignment(Pos.CENTER);
        empty.getStyleClass().add("empty-state");

        Label icon = new Label("＋");
        icon.getStyleClass().add("empty-state-icon");

        Label title = new Label("No files selected yet");
        title.getStyleClass().add("empty-state-title");

        Label description = new Label("Browse or drop files above to preview their new names.");
        description.getStyleClass().add("empty-state-description");

        empty.getChildren().addAll(icon, title, description);
        return empty;
    }

    private static final class RemoveCell extends TableCell<FileRowViewModel, Void> {
        private final Button removeButton = new Button("×");
        private final Consumer<FileRowViewModel> removeHandler;

        private RemoveCell(Consumer<FileRowViewModel> removeHandler) {
            this.removeHandler = removeHandler;
            removeButton.getStyleClass().add("remove-button");
            removeButton.setTooltip(new Tooltip("Remove from selection"));
            AnimationUtils.installButtonScale(removeButton);
            removeButton.setOnAction(event -> {
                int index = getIndex();
                if (index >= 0 && index < getTableView().getItems().size()) {
                    removeHandler.accept(getTableView().getItems().get(index));
                }
            });
        }

        @Override
        protected void updateItem(Void item, boolean empty) {
            super.updateItem(item, empty);
            setGraphic(empty ? null : removeButton);
        }
    }

    private static final class StatusCell extends TableCell<FileRowViewModel, RenameStatus> {
        private final Label pill = new Label();

        private StatusCell() {
            pill.getStyleClass().add("status-pill");
        }

        @Override
        protected void updateItem(RenameStatus status, boolean empty) {
            super.updateItem(status, empty);

            if (empty || status == null) {
                setGraphic(null);
                return;
            }

            pill.setText(status.label());
            pill.getStyleClass().removeAll(
                    "status-ready",
                    "status-invalid",
                    "status-renaming",
                    "status-completed",
                    "status-failed"
            );
            pill.getStyleClass().add(status.cssClass());

            FileRowViewModel row = getTableRow() == null ? null : getTableRow().getItem();
            pill.setTooltip(row == null || row.getStatusMessage().isBlank()
                    ? null
                    : new Tooltip(row.getStatusMessage()));

            setGraphic(pill);
        }
    }

    private static final class AccentTextCell extends TableCell<FileRowViewModel, String> {
        @Override
        protected void updateItem(String value, boolean empty) {
            super.updateItem(value, empty);
            setText(empty ? null : value);
            getStyleClass().remove("accent-table-cell");
            if (!empty) {
                getStyleClass().add("accent-table-cell");
            }
        }
    }
}
