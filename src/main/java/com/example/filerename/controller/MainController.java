package com.example.filerename.controller;

import com.example.filerename.model.FileRowViewModel;
import com.example.filerename.model.RenameExecutionResult;
import com.example.filerename.model.RenameOperation;
import com.example.filerename.model.RenamePlan;
import com.example.filerename.model.RenameSettings;
import com.example.filerename.model.RenameStatus;
import com.example.filerename.service.RenameExecutionException;
import com.example.filerename.service.RenameExecutor;
import com.example.filerename.service.RenamePlanner;
import com.example.filerename.ui.FileTableConfigurer;
import com.example.filerename.ui.ToastManager;
import com.example.filerename.util.AnimationUtils;
import com.example.filerename.validation.RenameValidator;
import com.example.filerename.validation.ValidationIssue;
import com.example.filerename.validation.ValidationResult;
import javafx.animation.FadeTransition;
import javafx.animation.ParallelTransition;
import javafx.animation.PauseTransition;
import javafx.animation.ScaleTransition;
import javafx.animation.TranslateTransition;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.css.PseudoClass;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.input.DragEvent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.util.Duration;
import javafx.util.StringConverter;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public final class MainController {

    private static final PseudoClass DRAG_OVER = PseudoClass.getPseudoClass("drag-over");
    private static final PseudoClass COMPACT = PseudoClass.getPseudoClass("compact");
    private static final PseudoClass NARROW = PseudoClass.getPseudoClass("narrow");

    private final RenamePlanner renamePlanner;
    private final RenameValidator renameValidator;
    private final RenameExecutor renameExecutor;
    private final ObservableList<FileRowViewModel> rows = FXCollections.observableArrayList();

    private RenamePlan currentPlan = new RenamePlan(List.of());
    private ValidationResult currentValidation = ValidationResult.valid();
    private ToastManager toastManager;
    private boolean busy;

    @FXML
    private StackPane rootStack;
    @FXML
    private VBox contentContainer;
    @FXML
    private VBox dropZone;
    @FXML
    private Button browseButton;
    @FXML
    private TextField prefixField;
    @FXML
    private Spinner<Integer> startNumberSpinner;
    @FXML
    private ComboBox<Integer> paddingCombo;
    @FXML
    private Label exampleLabel;
    @FXML
    private Label validationLabel;
    @FXML
    private TableView<FileRowViewModel> fileTable;
    @FXML
    private TableColumn<FileRowViewModel, String> originalColumn;
    @FXML
    private TableColumn<FileRowViewModel, String> newColumn;
    @FXML
    private TableColumn<FileRowViewModel, String> typeColumn;
    @FXML
    private TableColumn<FileRowViewModel, String> sizeColumn;
    @FXML
    private TableColumn<FileRowViewModel, RenameStatus> statusColumn;
    @FXML
    private TableColumn<FileRowViewModel, Void> actionColumn;
    @FXML
    private Label selectedCountLabel;
    @FXML
    private Button clearButton;
    @FXML
    private Button renameButton;
    @FXML
    private Label renameButtonText;
    @FXML
    private ProgressIndicator renameProgress;
    @FXML
    private HBox toastContainer;
    @FXML
    private Label toastTitle;
    @FXML
    private Label toastIcon;
    @FXML
    private Label toastLabel;

    public MainController(
            RenamePlanner renamePlanner,
            RenameValidator renameValidator,
            RenameExecutor renameExecutor
    ) {
        this.renamePlanner = Objects.requireNonNull(renamePlanner, "renamePlanner");
        this.renameValidator = Objects.requireNonNull(renameValidator, "renameValidator");
        this.renameExecutor = Objects.requireNonNull(renameExecutor, "renameExecutor");
    }

    @FXML
    private void initialize() {
        toastManager = new ToastManager(toastContainer, toastTitle, toastIcon, toastLabel);
        configureSettingsControls();
        configureTable();
        configureBindings();
        configureResponsiveLayout();
        installAnimations();

        Platform.runLater(() -> AnimationUtils.fadeAndSlideIn(
                contentContainer,
                16,
                Duration.millis(420)
        ));

        refreshPreview();
    }

    @FXML
    private void handleBrowse(ActionEvent ignored) {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Select files to rename");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("All files", "*.*"));

        File home = new File(System.getProperty("user.home"));
        if (home.isDirectory()) {
            chooser.setInitialDirectory(home);
        }

        List<File> selected = chooser.showOpenMultipleDialog(rootStack.getScene().getWindow());
        if (selected != null) {
            addFiles(selected.stream().map(File::toPath).toList());
        }
    }

    @FXML
    private void handleDragOver(DragEvent event) {
        Dragboard dragboard = event.getDragboard();
        if (dragboard.hasFiles()) {
            event.acceptTransferModes(TransferMode.COPY);
            if (!dropZone.getPseudoClassStates().contains(DRAG_OVER)) {
                dropZone.pseudoClassStateChanged(DRAG_OVER, true);
                AnimationUtils.scaleTo(dropZone, 1.018, 140);
            }
        }
        event.consume();
    }

    @FXML
    private void handleDragExited(DragEvent event) {
        resetDropZone();
        event.consume();
    }

    @FXML
    private void handleDrop(DragEvent event) {
        Dragboard dragboard = event.getDragboard();
        boolean accepted = dragboard.hasFiles();

        if (accepted) {
            addFiles(dragboard.getFiles().stream().map(File::toPath).toList());
        }

        event.setDropCompleted(accepted);
        resetDropZone();
        event.consume();
    }

    @FXML
    private void handleClear(ActionEvent ignored) {
        if (busy || rows.isEmpty()) {
            return;
        }

        FadeTransition fade = new FadeTransition(Duration.millis(150), fileTable);
        fade.setToValue(0.35);
        fade.setOnFinished(event -> {
            rows.clear();
            fileTable.setOpacity(1);
            refreshPreview();
            showToast("Selection cleared", ToastManager.Type.INFO);
        });
        fade.play();
    }

    @FXML
    private void handleRename(ActionEvent ignored) {
        refreshPreview();

        if (!currentValidation.isValid()) {
            showToast(currentValidation.messages().getFirst(), ToastManager.Type.ERROR);
            return;
        }

        RenamePlan planToExecute = currentPlan;
        setBusy(true);
        rows.forEach(row -> {
            row.setStatus(RenameStatus.RENAMING);
            row.setStatusMessage("Waiting to be processed");
        });

        Task<RenameExecutionResult> task = new Task<>() {
            @Override
            protected RenameExecutionResult call() throws Exception {
                updateProgress(0, planToExecute.size());
                return renameExecutor.execute(planToExecute, progress -> {
                    updateProgress(progress.completed(), progress.total());
                    Platform.runLater(() -> markOperationCompleted(planToExecute, progress.source()));
                });
            }
        };

        renameProgress.progressProperty().bind(task.progressProperty());

        task.setOnSucceeded(event -> {
            renameProgress.progressProperty().unbind();
            renameProgress.setProgress(ProgressIndicator.INDETERMINATE_PROGRESS);

            RenameExecutionResult result = task.getValue();
            applySuccessfulResult(planToExecute);
            setBusy(false);
            int renamedCount = result.renamedCount();
            String renamedLabel = renamedCount == 1 ? "file" : "files";
            showToast(renamedCount + " " + renamedLabel + " renamed successfully", ToastManager.Type.SUCCESS);
            playSuccessAnimation();
        });

        task.setOnFailed(event -> {
            renameProgress.progressProperty().unbind();
            renameProgress.setProgress(ProgressIndicator.INDETERMINATE_PROGRESS);

            Throwable failure = task.getException();
            rows.forEach(row -> {
                row.setStatus(RenameStatus.FAILED);
                row.setStatusMessage("Rename failed; rollback was attempted");
            });

            setBusy(false);
            refreshPreviewAfterFailure();

            String message = extractFailureMessage(failure);
            showToast(message, ToastManager.Type.ERROR);
        });

        Thread worker = new Thread(task, "file-renamer-worker");
        worker.setDaemon(true);
        worker.start();
    }

    private void configureSettingsControls() {
        startNumberSpinner.setValueFactory(
                new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 999_999_999, 1)
        );
        startNumberSpinner.setEditable(true);

        paddingCombo.setItems(FXCollections.observableArrayList(1, 2, 3, 4, 5, 6, 7, 8));
        paddingCombo.setConverter(new StringConverter<>() {
            @Override
            public String toString(Integer padding) {
                if (padding == null) {
                    return "";
                }
                return "0".repeat(Math.max(0, padding - 1)) + "1";
            }

            @Override
            public Integer fromString(String value) {
                return value == null || value.isBlank() ? 1 : value.length();
            }
        });
        paddingCombo.getSelectionModel().select(Integer.valueOf(2));

        prefixField.textProperty().addListener((observable, oldValue, newValue) -> refreshPreview());
        startNumberSpinner.valueProperty().addListener((observable, oldValue, newValue) -> refreshPreview());
        startNumberSpinner.getEditor().textProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null && newValue.matches("\\d{1,9}")) {
                try {
                    int parsed = Integer.parseInt(newValue);
                    startNumberSpinner.getValueFactory().setValue(parsed);
                } catch (NumberFormatException ignored) {
                    // The spinner keeps its last valid value until the input becomes valid again.
                }
            }
        });
        paddingCombo.valueProperty().addListener((observable, oldValue, newValue) -> refreshPreview());
    }

    private void configureTable() {
        fileTable.setItems(rows);
        fileTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        FileTableConfigurer.configure(
                fileTable,
                originalColumn,
                newColumn,
                typeColumn,
                sizeColumn,
                statusColumn,
                actionColumn,
                this::removeRow
        );
    }

    private void configureBindings() {
        selectedCountLabel.textProperty().bind(Bindings.createStringBinding(
                () -> rows.size() + (rows.size() == 1 ? " file selected" : " files selected"),
                rows
        ));

    }

    private void configureResponsiveLayout() {
        rootStack.widthProperty().addListener((observable, oldWidth, newWidth) ->
                updateResponsiveLayout(newWidth.doubleValue())
        );
        Platform.runLater(() -> updateResponsiveLayout(rootStack.getWidth()));
    }

    private void updateResponsiveLayout(double width) {
        boolean compact = width < 900;
        boolean narrow = width < 700;

        rootStack.pseudoClassStateChanged(COMPACT, compact);
        rootStack.pseudoClassStateChanged(NARROW, narrow);

        typeColumn.setVisible(!compact);
        sizeColumn.setVisible(!compact);
        statusColumn.setVisible(!narrow);
    }

    private void installAnimations() {
        AnimationUtils.installButtonScale(browseButton);
        AnimationUtils.installButtonScale(clearButton);
        AnimationUtils.installButtonScale(renameButton);
    }

    private void addFiles(List<Path> candidates) {
        int added = 0;
        int skipped = 0;

        for (Path candidate : candidates) {
            Path normalized = candidate.toAbsolutePath().normalize();
            boolean duplicate = rows.stream().anyMatch(row -> row.getPath().equals(normalized));

            if (duplicate || !Files.isRegularFile(normalized)) {
                skipped++;
                continue;
            }

            rows.add(new FileRowViewModel(normalized));
            added++;
        }

        refreshPreview();

        if (added > 0) {
            animateFilesAdded();
            String message = added + (added == 1 ? " file added" : " files added");
            if (skipped > 0) {
                message += " · " + skipped + " skipped";
            }
            showToast(message, ToastManager.Type.SUCCESS);
        } else if (skipped > 0) {
            showToast("No new files were added. Duplicates and folders are skipped.", ToastManager.Type.INFO);
        }
    }

    private void refreshPreview() {
        RenameSettings settings = readSettings();
        updateExample(settings);

        if (rows.isEmpty()) {
            currentPlan = new RenamePlan(List.of());
            currentValidation = renameValidator.validate(List.of(), settings, currentPlan);
            validationLabel.setVisible(false);
            validationLabel.setManaged(false);
            updateRenameButtonState();
            return;
        }

        List<Path> paths = rows.stream().map(FileRowViewModel::getPath).toList();

        try {
            currentPlan = renamePlanner.createPlan(paths, settings);
            currentValidation = renameValidator.validate(paths, settings, currentPlan);
            applyPlanToRows(currentPlan, currentValidation);
        } catch (RuntimeException exception) {
            currentPlan = new RenamePlan(List.of());
            currentValidation = new ValidationResult(List.of(
                    new ValidationIssue(null, "Unable to generate the preview: " + exception.getMessage())
            ));
            rows.forEach(row -> {
                row.setProposedName("—");
                row.setStatus(RenameStatus.INVALID);
                row.setStatusMessage("Preview unavailable");
            });
        }

        updateValidationMessage();
        updateRenameButtonState();
    }

    private void applyPlanToRows(RenamePlan plan, ValidationResult validation) {
        Map<Path, RenameOperation> operations = new HashMap<>();
        for (RenameOperation operation : plan.operations()) {
            operations.put(operation.source(), operation);
        }

        Optional<ValidationIssue> globalIssue = validation.issues().stream()
                .filter(issue -> issue.sourcePath().isEmpty())
                .findFirst();

        for (FileRowViewModel row : rows) {
            RenameOperation operation = operations.get(row.getPath());
            if (operation != null) {
                row.setProposedName(operation.destination().getFileName().toString());
            }

            Optional<ValidationIssue> rowIssue = validation.issues().stream()
                    .filter(issue -> issue.sourcePath().map(row.getPath()::equals).orElse(false))
                    .findFirst();

            if (rowIssue.isPresent()) {
                row.setStatus(RenameStatus.INVALID);
                row.setStatusMessage(rowIssue.get().message());
            } else if (globalIssue.isPresent()) {
                row.setStatus(RenameStatus.INVALID);
                row.setStatusMessage(globalIssue.get().message());
            } else {
                row.setStatus(RenameStatus.READY);
                row.setStatusMessage("Ready to rename");
            }
        }
    }

    private RenameSettings readSettings() {
        Integer start = startNumberSpinner.getValue();
        Integer padding = paddingCombo.getValue();

        return new RenameSettings(
                prefixField.getText(),
                start == null ? 0 : start,
                padding == null ? 2 : padding
        );
    }

    private void updateExample(RenameSettings settings) {
        Path samplePath = rows.isEmpty()
                ? Path.of("sample.png")
                : rows.getFirst().getPath();

        try {
            String preview = renamePlanner.previewName(samplePath, settings, 0);
            exampleLabel.setText("Preview: " + preview);
        } catch (RuntimeException exception) {
            exampleLabel.setText("Preview unavailable");
        }
    }

    private void updateValidationMessage() {
        if (currentValidation.isValid()) {
            validationLabel.setVisible(false);
            validationLabel.setManaged(false);
            validationLabel.setText("");
            return;
        }

        List<String> messages = currentValidation.messages();
        String text = messages.getFirst();
        if (messages.size() > 1) {
            text += "  +" + (messages.size() - 1) + " more";
        }

        validationLabel.setText(text);
        validationLabel.setVisible(true);
        validationLabel.setManaged(true);
    }

    private void updateRenameButtonState() {
        clearButton.setDisable(busy || rows.isEmpty());
        renameButton.setDisable(busy || rows.isEmpty() || !currentValidation.isValid());
    }

    private void setBusy(boolean value) {
        busy = value;
        prefixField.setDisable(value);
        startNumberSpinner.setDisable(value);
        paddingCombo.setDisable(value);
        browseButton.setDisable(value);
        fileTable.setDisable(value);

        renameProgress.setVisible(value);
        renameProgress.setManaged(value);
        renameButtonText.setText(value ? "Renaming…" : "Rename Files");
        updateRenameButtonState();
    }

    private void markOperationCompleted(RenamePlan plan, Path source) {
        plan.operations().stream()
                .filter(operation -> operation.source().equals(source))
                .findFirst()
                .ifPresent(operation -> rows.stream()
                        .filter(row -> row.getPath().equals(operation.source()))
                        .findFirst()
                        .ifPresent(row -> {
                            row.setStatus(RenameStatus.COMPLETED);
                            row.setStatusMessage("Renamed successfully");
                        }));
    }

    private void applySuccessfulResult(RenamePlan completedPlan) {
        Map<Path, RenameOperation> operations = new HashMap<>();
        for (RenameOperation operation : completedPlan.operations()) {
            operations.put(operation.source(), operation);
        }

        for (FileRowViewModel row : rows) {
            RenameOperation operation = operations.get(row.getPath());
            if (operation != null) {
                row.setPath(operation.destination());
                row.setProposedName(operation.destination().getFileName().toString());
                row.setStatus(RenameStatus.COMPLETED);
                row.setStatusMessage("Renamed successfully");
            }
        }

        currentPlan = new RenamePlan(List.of());
        currentValidation = ValidationResult.valid();
        validationLabel.setVisible(false);
        validationLabel.setManaged(false);
        updateExample(readSettings());
        updateRenameButtonState();
    }

    private void refreshPreviewAfterFailure() {
        PauseTransition pause = new PauseTransition(Duration.millis(650));
        pause.setOnFinished(event -> refreshPreview());
        pause.play();
    }

    private String extractFailureMessage(Throwable failure) {
        if (failure instanceof RenameExecutionException executionException) {
            if (executionException.rollbackErrors().isEmpty()) {
                return "Rename failed. Original filenames were restored.";
            }
            return "Rename failed, and some files could not be restored. Check the folder.";
        }

        String message = failure == null ? null : failure.getMessage();
        return message == null || message.isBlank() ? "Rename failed." : message;
    }

    private void animateFilesAdded() {
        fileTable.setOpacity(0.72);
        fileTable.setTranslateY(7);

        FadeTransition fade = new FadeTransition(Duration.millis(220), fileTable);
        fade.setToValue(1);

        TranslateTransition slide = new TranslateTransition(Duration.millis(220), fileTable);
        slide.setToY(0);

        new ParallelTransition(fade, slide).play();
    }

    private void playSuccessAnimation() {
        ScaleTransition scale = new ScaleTransition(Duration.millis(180), renameButton);
        scale.setFromX(0.97);
        scale.setFromY(0.97);
        scale.setToX(1.04);
        scale.setToY(1.04);
        scale.setAutoReverse(true);
        scale.setCycleCount(2);
        scale.play();
    }

    private void resetDropZone() {
        dropZone.pseudoClassStateChanged(DRAG_OVER, false);
        AnimationUtils.scaleTo(dropZone, 1.0, 150);
    }

    private void removeRow(FileRowViewModel row) {
        if (busy) {
            return;
        }

        int index = rows.indexOf(row);
        if (index < 0) {
            return;
        }

        FadeTransition fade = new FadeTransition(Duration.millis(120), fileTable);
        fade.setToValue(0.7);
        fade.setOnFinished(event -> {
            rows.remove(row);
            fileTable.setOpacity(1);
            refreshPreview();
            showToast("File removed from selection", ToastManager.Type.INFO);
        });
        fade.play();
    }

    private void showToast(String message, ToastManager.Type type) {
        toastManager.show(message, type);
    }
}
