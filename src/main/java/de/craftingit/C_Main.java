package de.craftingit;

import java.io.File;
import java.nio.file.*;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.ScheduledService;
import javafx.concurrent.Service;
import javafx.concurrent.Task;
import javafx.concurrent.WorkerStateEvent;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.DragEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseDragEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Stage;
import javafx.util.Duration;

public class C_Main {
    private Stage stage = null;
    private ObservableList<Archive> archives = FXCollections.observableArrayList();
    private File[] roots;
    private Path dir;
    private int maxArchives;
    private int countArchives;
    private double windowWidth;
    private SearchService searchService;
    private ScheduledService<Boolean> extractService;

    @FXML
    private AnchorPane anchorPane_main;
    @FXML
    private Button button_search;
    @FXML
    private Button button_cancelSearch;
    @FXML
    private Button button_extract;
    @FXML
    private Button button_cancelExtract;
    @FXML
    private Label label_status;
    @FXML
    private ComboBox<String> comboBox_roots;
    @FXML
    private ComboBox<String> comboBox_formats;
    @FXML
    private TableView<Archive> tableView_archives;
    @FXML
    private TableColumn<Archive, String> tColumn_dir;
    @FXML
    private TableColumn<Archive, String> tColumn_status;
    @FXML
    private TableColumn<Archive, Long> tColumn_id;
    @FXML
    private TextField textField_filter;
    @FXML
    private TextField textField_exclude;
    @FXML
    private PasswordField pwField;
    @FXML
    private CheckBox checkBox_hideExtracted;
    @FXML
    private ProgressBar progressBar;

    @FXML
    private void initialize() {
        updateRoots();

        comboBox_formats.getItems().add(".7z");
        //comboBox_formats.getItems().add(".zip");
        //comboBox_formats.getItems().add(".rar");
        comboBox_formats.setValue(".7z");

        tColumn_dir.setCellValueFactory(new PropertyValueFactory<>("dir"));
        tColumn_status.setCellValueFactory(new PropertyValueFactory<>("status"));
        tColumn_id.setCellValueFactory(new PropertyValueFactory<>("id"));

        button_search.setDisable(false);
        button_cancelSearch.setVisible(false);
        button_extract.setDisable(true);
        button_cancelExtract.setVisible(false);
        label_status.setVisible(false);

        ScheduledService<Boolean> backgroundService = new ScheduledService<Boolean>() {
            @Override
            protected Task<Boolean> createTask() {
                return new Task<Boolean>() {
                    @Override
                    protected Boolean call() {
                        try {
                            if (stage == null) {
                                stage = (Stage) anchorPane_main.getScene().getWindow();
                            }
                            if (windowWidth != stage.getWidth()) {
                                windowWidth = stage.getWidth();
                                progressBar.setLayoutX(windowWidth / 2 - progressBar.getWidth() / 2);
                                label_status.setLayoutX(windowWidth / 2 - label_status.getWidth() / 2);
                                tColumn_dir.setPrefWidth(tColumn_dir.getMinWidth() + windowWidth - 965);
                            }
                        } catch (Exception e) {
                            System.out.println(e.getMessage());
                        }
                        return null;
                    }
                };
            }
        };
        backgroundService.setPeriod(Duration.millis(50));
        backgroundService.start();
    }

    @FXML
    private void updateRoots() {
        comboBox_roots.getItems().clear();
        roots = File.listRoots();
        for (File root : roots) {
            comboBox_roots.getItems().add(root.toString());
        }
        comboBox_roots.setValue(roots[0].toString());
        changeDir();
    }

    @FXML
    private void changeDir() {
        dir = Paths.get(comboBox_roots.getValue() + ".");
        //System.out.println(dir);
    }

    @FXML
    private void searchArchives() {
        progressBar.setProgress(-1);
        Archive.setCountId(1);
        archives.clear();
        button_search.setVisible(false);
        button_cancelSearch.setVisible(true);
        label_status.setVisible(true);
        label_status.setText("Suche läuft...");

        searchService = new SearchService(dir, comboBox_formats.getValue(),
                textField_filter.getText(), textField_exclude.getText());

        searchService.setOnSucceeded(workerStateEvent -> {
            archives.addAll(searchService.getValue());
            progressBar.setProgress(0);
            maxArchives = archives.size();
            tableView_archives.setItems(archives);
            button_extract.setDisable(tableView_archives.getItems().isEmpty());
            button_search.setVisible(true);
            button_cancelSearch.setVisible(false);
            label_status.setText("Suche abgeschlossen.");
        });
        searchService.start();

        maxArchives = archives.size();
    }

    @FXML
    private void cancelSearch() {
        searchService.cancel();
        progressBar.setProgress(0);
        button_search.setVisible(true);
        button_cancelSearch.setVisible(false);
        label_status.setText("Suche abgebrochen.");
    }

    @FXML
    private void extractArchives() {
        countArchives = 0;
        progressBar.setProgress(0);
        button_extract.setVisible(false);
        button_cancelExtract.setVisible(true);
        button_search.setDisable(true);
        label_status.setVisible(true);

        extractService = new ScheduledService<Boolean>() {
            @Override
            protected Task<Boolean> createTask() {
                return new Task<Boolean>() {
                    @Override
                    protected Boolean call() {
                        try {
                            if (!archives.get(countArchives).isExtracted())
                                archives.get(countArchives).extract(pwField.getText());
                        } catch (Exception e) {
                            this.cancel();

                            Platform.runLater(() -> {
                                button_extract.setVisible(true);
                                button_cancelExtract.setVisible(false);
                                button_search.setDisable(false);
                                button_cancelExtract.setDisable(false);
                                button_cancelExtract.setVisible(false);
                                if (countArchives < 0) {
                                    label_status.setText("Entpacken abgebrochen.");
                                    progressBar.setProgress(0);
                                } else
                                    label_status.setText("Fertig!");
                            });
                        }
                        return null;
                    }
                };
            }
        };

        extractService.setOnScheduled(workerStateEvent -> {
            if (countArchives >= 0 && countArchives < archives.size() && !archives.get(countArchives).isExtracted()) {
                label_status.setText("Entpacke " + (countArchives + 1) + "/" + archives.size());
                archives.get(countArchives).setStatus("Wird entpackt...");
                tableView_archives.refresh();
                if (checkBox_hideExtracted.isSelected())
                    hideExtracted();
            }
        });

        extractService.setOnSucceeded(workerStateEvent -> {
            countArchives++;
            tableView_archives.refresh();
            progressBar.setProgress((double) countArchives / (double) archives.size());
        });

        extractService.start();
    }

    @FXML
    private void cancelExtract() {
        countArchives = -10;
        button_cancelExtract.setDisable(true);
        label_status.setVisible(true);
        label_status.setText("Vorgang wird abgebrochen. Bitte warten.");
        progressBar.setProgress(-1);
    }

    @FXML
    private void hideExtracted() {
        if (checkBox_hideExtracted.isSelected()) {
            ObservableList<Archive> hiddenArchives = FXCollections.observableArrayList();
            for (Archive archive : archives) {
                if (!archive.isExtracted())
                    hiddenArchives.add(archive);
            }
            tableView_archives.setItems(hiddenArchives);
        } else {
            tableView_archives.setItems(archives);
        }
    }

    @FXML
    private void showAppInfo() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Information");
        alert.setHeaderText("unZipper by André Krippendorf");
        alert.setContentText(
                "CraftingIT GmbH\n" +
                        "Erzbergerstraße 1-2\n" +
                        "39104 Magdeburg\n" +
                        "Deutschland\n" +
                        "Telefon: +49 391 28921 500\n" +
                        "Fax: +49 391 28921 555\n" +
                        "Mail: info@crafting-it.de");
        alert.show();
    }

    @FXML
    private void closeApp() {
        //TODO Warnmeldung bei laufendem Service
        if (searchService.isRunning() || extractService.isRunning()) {

        } else {
            stage.close();
        }
    }

//    @FXML
//    private void switchToSecondary() throws IOException {
//        App.setRoot("secondary");
//    }
}
