package de.craftingit;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.util.Arrays;
import java.util.Scanner;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.ScheduledService;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.AnchorPane;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.Duration;

public class C_Main {
    private final double MIN_WINDOW_HEIGHT = 700;
    private final double MIN_WINDOW_WIDTH = 965;
    private final int MAX_TASKS = 12;
    private int countTasks;
    private int countArchives;
    private int countSucceededServices;
    private double windowWidth;
    private boolean isExtracting;
    private Stage stage = null;
    private ObservableList<Archive> archives = FXCollections.observableArrayList();
    private File[] roots;
    private Path dir;
    private SearchService searchService;
    private ExtractService extractService;

    @FXML
    private AnchorPane anchorPane_main;
    @FXML
    private Button button_search;
    @FXML
    private Button button_cancelSearch;
    @FXML
    private Button button_extractAll;
    @FXML
    private Button button_extractSingle;
    @FXML
    private Button button_cancelExtract;
    @FXML
    private Button button_updateRoots;
    @FXML
    private Button button_rootAddition;
    @FXML
    private Button button_findArchivist;
    @FXML
    private TextField textField_rootAddition;
    @FXML
    private Label label_status;
    @FXML
    private ComboBox<String> comboBox_roots;
    @FXML
    private ComboBox<String> comboBox_formats;
    @FXML
    private ComboBox<Integer> comboBox_tasks;
    @FXML
    private TableView<Archive> tableView_archives;
    @FXML
    private TableColumn<Archive, String> tColumn_dir;
    @FXML
    private TableColumn<Archive, String> tColumn_status;
    @FXML
    private TableColumn<Archive, Long> tColumn_id;
    @FXML
    private TextField textField_includeFilter;
    @FXML
    private TextField textField_excludeFilter;
    @FXML
    private TextField textField_appDir;
    @FXML
    private PasswordField pwField;
    @FXML
    private CheckBox checkBox_hideExtracted;
    @FXML
    private CheckBox checkBox_deleteExtracted;
    @FXML
    private CheckBox checkBox_useExternalApp;
    @FXML
    private ProgressBar progressBar;
    @FXML
    private MenuItem menuItem_exportTable;
    @FXML
    private MenuItem menuItem_importTable;

    @FXML
    private void initialize() {
        updateRoots();

        textField_appDir.setText("C:\\Program Files\\7-Zip\\7z.exe");

        comboBox_tasks.setTooltip(new Tooltip("Viele Prozesse erhöhen die Prozessorlast."));
        for(int i = 1; i <= MAX_TASKS; i++)
            comboBox_tasks.getItems().add(i);
        comboBox_tasks.setValue(1);

        textField_rootAddition.setTooltip(new Tooltip("In MS Windows z.B. \"Benutzer\\Default\". Andere z.B. \"home/usr\""));

        comboBox_formats.getItems().add(".7z");
        //comboBox_formats.getItems().add(".iso");
        //comboBox_formats.getItems().add(".rar");
        //comboBox_formats.getItems().add(".tar");
        //comboBox_formats.getItems().add(".zip");
        comboBox_formats.setValue(".7z");

        tColumn_dir.setCellValueFactory(new PropertyValueFactory<>("DIR"));
        tColumn_status.setCellValueFactory(new PropertyValueFactory<>("status"));
        tColumn_id.setCellValueFactory(new PropertyValueFactory<>("ID"));

        button_search.setDisable(false);
        button_cancelSearch.setVisible(false);
        button_extractAll.setDisable(true);
        button_extractSingle.setDisable(true);
        button_cancelExtract.setVisible(false);
        menuItem_exportTable.setDisable(true);

        ScheduledService<Boolean> backgroundService = new ScheduledService<Boolean>() {
            @Override
            protected Task<Boolean> createTask() {
                return new Task<Boolean>() {
                    @Override
                    protected Boolean call() {
                        try {
                            if (stage == null) {
                                stage = (Stage) anchorPane_main.getScene().getWindow();
                                stage.setOnCloseRequest(event -> {
                                    event.consume();
                                    closeApp();
                                });
                            }
                            if (windowWidth != stage.getWidth()) {
                                windowWidth = stage.getWidth();
                                progressBar.setLayoutX(windowWidth / 2 - progressBar.getWidth() / 2);
                                label_status.setLayoutX(windowWidth / 2 - label_status.getWidth() / 2);
                                tColumn_dir.setPrefWidth(tColumn_dir.getMinWidth() + windowWidth - MIN_WINDOW_WIDTH);
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

        // Wenn kein MS Windows verwendet wird
        if(!System.getProperty("os.name").toLowerCase().startsWith("windows")) {
            comboBox_roots.setDisable(true);
            textField_appDir.setDisable(true);
            button_updateRoots.setDisable(true);
            button_findArchivist.setDisable(true);
            textField_appDir.setText("Installation von \"p7zip-full\" benötigt");
        }
    }

    @FXML
    private void updateGui(boolean disable) {
        button_rootAddition.setDisable(disable);
        button_findArchivist.setDisable(disable);
        button_search.setDisable(disable);
        menuItem_importTable.setDisable(disable);
        pwField.setDisable(disable);
        textField_rootAddition.setDisable(disable);
        textField_appDir.setDisable(disable);
        textField_includeFilter.setDisable(disable);
        textField_excludeFilter.setDisable(disable);
        checkBox_useExternalApp.setDisable(disable);
        checkBox_deleteExtracted.setDisable(disable);
        if(disable) {
            button_extractAll.setDisable(true);
            button_extractSingle.setDisable(true);
            menuItem_exportTable.setDisable(true);
        } else {
            button_extractAll.setDisable(tableView_archives.getItems().isEmpty());
            button_extractSingle.setDisable(tableView_archives.getSelectionModel().getSelectedItem() == null);
            menuItem_exportTable.setDisable(tableView_archives.getItems().isEmpty());
        }
        button_cancelExtract.setDisable(false);
        tableView_archives.refresh();
    }

    @FXML
    private void updateRoots() {
        comboBox_roots.getItems().clear();
        roots = File.listRoots();
        for (File root : roots) {
            comboBox_roots.getItems().add(root.toString());
        }
        comboBox_roots.setValue(roots[0].toString());
    }

    @FXML
    private void changeDir() {
        if(System.getProperty("os.name").toLowerCase().startsWith("windows"))
            dir = Paths.get(comboBox_roots.getValue() + (textField_rootAddition.getText().isEmpty() ? "." : textField_rootAddition.getText()));
        else
            dir = Paths.get(comboBox_roots.getValue() + textField_rootAddition.getText());
    }

    @FXML
    private void findRootAddition() {
        try {
            String dir = new DirectoryChooser().showDialog(stage).toString();
            for(File root : roots) {
                if(dir.contains(root.toString()))
                    comboBox_roots.setValue(root.toString());
                dir = dir.replace(root.toString(), "");
            }
            textField_rootAddition.setText(dir);
        } catch (Exception e) {
            System.err.println(e.getMessage());
        }
    }

    @FXML
    private void findArchivist() {
        String dir;
        FileChooser chooser = new FileChooser();
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("EXE Dateien","*.exe"));

        try {
            chooser.setInitialDirectory(new File(Path.of(textField_appDir.getText()).getParent().toString()));
        } catch(NullPointerException e) {
            chooser.setInitialDirectory(new File(System.getProperty("user.home")));
        }
        dir = new File(String.valueOf(chooser.showOpenDialog(stage))).getAbsolutePath();
        if(!dir.endsWith("null"))
            textField_appDir.setText(dir);

        if(comboBox_formats.getValue().equals(".7z") && !textField_appDir.getText().endsWith("7z.exe")) {
            textField_appDir.setStyle("-fx-text-fill: red;");
        } else
            textField_appDir.setStyle("-fx-text-fill: black;");
    }

    @FXML
    private void deleteExtractedWarning() {
        if(checkBox_deleteExtracted.isSelected()) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Achtung!");
            alert.setHeaderText("Damit werden alle entpackten Archive unwiderruflich gelöscht!");
            alert.show();
        }
    }

    @FXML
    private void searchArchives() {
        changeDir();
        Archive.setCountId(1);
        archives.clear();
        tableView_archives.getItems().clear();
        progressBar.setProgress(-1);
        button_search.setVisible(false);
        button_cancelSearch.setVisible(true);
        label_status.setText("Suche läuft...");
        updateGui(true);

        searchService = new SearchService(dir, comboBox_formats.getValue(),
                textField_includeFilter.getText(), textField_excludeFilter.getText());

        searchService.setOnSucceeded(workerStateEvent -> {
            archives.addAll(searchService.getValue());
            tableView_archives.setItems(archives);
            progressBar.setProgress(1);
            button_search.setVisible(true);
            button_cancelSearch.setVisible(false);
            label_status.setText("Suche abgeschlossen.");
            updateGui(false);
        });
        searchService.start();
    }

    @FXML
    private void cancelSearch() {
        searchService.cancel();
        progressBar.setProgress(0);
        button_search.setVisible(true);
        button_cancelSearch.setVisible(false);
        label_status.setText("Suche abgebrochen.");
        updateGui(false);
    }

    @FXML
    private void exportTable() {
        File file;
        FileChooser chooser = new FileChooser();
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV Datei","*.csv"));
        chooser.setInitialDirectory(new File(System.getProperty("user.home")));

        try {
            file = chooser.showSaveDialog(stage);
            label_status.setText("Exportiere...");
            progressBar.setProgress(-1);
            updateGui(true);
        } catch (NullPointerException e) {
            e.printStackTrace();
            return;
        }

        ExportDataService exportDataService = new ExportDataService(archives, file);
        
        exportDataService.setOnSucceeded(workerStateEvent -> {
            if(exportDataService.getValue()) {
                label_status.setText("Export abgeschlossen.");
                progressBar.setProgress(1);
            }
            else {
                label_status.setText("Export fehlgeschlagen.");
                progressBar.setProgress(0);
            }
            updateGui(false);
        });
        exportDataService.start();
    }

    @FXML
    private  void importTable() {
        FileChooser chooser = new FileChooser();
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV Dateien","*.csv"));
        chooser.setInitialDirectory(new File(System.getProperty("user.home")));
        File file;

        try {
            file = new File(chooser.showOpenDialog(stage).toString());
            updateGui(true);
            progressBar.setProgress(-1);
            label_status.setText("Importiere...");
            archives.clear();
            tableView_archives.getItems().clear();
        } catch (NullPointerException e1) {
            System.err.println(e1.getMessage());
            updateGui(false);
            return;
        }

        ImportDataService importDataService = new ImportDataService(file);

        importDataService.setOnCancelled(workerStateEvent -> {
            progressBar.setProgress(0);
            label_status.setText("Import fehlgeschlagen. Bitte Datei prüfen.");
            updateGui(false);
        });

        importDataService.setOnSucceeded(workerStateEvent -> {
            tableView_archives.setItems(importDataService.getValue());
            progressBar.setProgress(1);
            label_status.setText("Import abgeschlossen.");
            updateGui(false);
        });
        importDataService.start();
    }

    @FXML
    private void detectTableSelection() {
        button_extractSingle.setDisable(tableView_archives.getSelectionModel().getSelectedItem() == null);
    }

    @FXML
    private void extractArchive(ActionEvent event) {
        isExtracting = true;
        countSucceededServices = 0;
        countArchives = 0;
        countTasks = 0;
        button_cancelExtract.setVisible(true);
        button_extractSingle.setVisible(false);
        button_extractAll.setVisible(false);
        label_status.setText("Entpacke...");
        updateGui(true);

        if(event.getSource() == button_extractSingle) {
            countArchives = archives.size() -1;
            progressBar.setProgress(-1);
        }

        if(event.getSource() == button_extractAll)
            progressBar.setProgress(0);

        ScheduledService<Boolean> extractTaskService = new ScheduledService<Boolean>() {
            @Override
            protected Task<Boolean> createTask() {
                return new Task<Boolean>() {
                    @Override
                    protected Boolean call() {
                        if (countTasks == 0 && (countArchives < 0 || countArchives >= archives.size())) {
                            isExtracting = false;
                            this.cancel();
                            Platform.runLater(() -> {
                                updateGui(false);
                                progressBar.setProgress(1);
                                button_cancelExtract.setVisible(false);
                                button_extractSingle.setVisible(true);
                                button_extractAll.setVisible(true);
                                label_status.setText(countArchives < 0 ? "Vorgang abgebrochen." : "Fertig!");
                            });
                        } else if (countTasks < comboBox_tasks.getValue() && !(countArchives < 0 || countArchives >= archives.size())) {
                            if(progressBar.getProgress() >= 0)  // Alles entpacken
                                extractStart(countArchives);
                            else {                              // Auswahl entpacken
                                Archive archive = tableView_archives.getSelectionModel().getSelectedItem();
                                extractStart(archive.getID() -1);
                            }
                            countTasks++;
                            countArchives++;
                        }
                        return null;
                    }
                };
            }
        };
        extractTaskService.setPeriod(Duration.millis(50));
        extractTaskService.start();
    }

    @FXML
    private void extractStart(int index) {
        try {
            extractService = new ExtractService(checkBox_useExternalApp.isSelected(), textField_appDir.getText(),
                                                archives.get(index), pwField.getText());
        } catch (IndexOutOfBoundsException e) {
            System.out.println(e.getMessage());
        }

        extractService.setOnRunning(workerStateEvent -> {
            tableView_archives.refresh();
        });

        extractService.setOnSucceeded(workerStateEvent -> {
            countTasks--;
            countSucceededServices++;
            tableView_archives.refresh();

            if(archives.get(index).isExtracted() && checkBox_deleteExtracted.isSelected()) {
                try {
                    Files.deleteIfExists(archives.get(index).getDIR());
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            if (checkBox_hideExtracted.isSelected())
                hideExtracted();

            if (countArchives >= 0 && progressBar.getProgress() >= 0)
                progressBar.setProgress((double) countSucceededServices / (double) archives.size());
        });

        extractService.setOnFailed(workerStateEvent -> {
            System.err.println("ExtractService failed. Nummer: " + (index +1));
            countTasks--;
            tableView_archives.refresh();
        });

        extractService.start();
    }

    @FXML
    private void cancelExtract() {
        countArchives = -1;
        button_cancelExtract.setDisable(true);
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
        ButtonType buttonYes = new ButtonType("Ja", ButtonBar.ButtonData.YES);
        ButtonType buttonNo = new ButtonType("Nein", ButtonBar.ButtonData.NO);
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION, "", buttonYes, buttonNo);
        alert.setTitle("Beenden");
        alert.setHeaderText("Prozesse abbrechen und beenden?");

        if(stage == null)
            stage = (Stage) anchorPane_main.getScene().getWindow();

        if(searchService != null && searchService.isRunning()) {
            alert.setContentText("Alle laufenden Prozesse abbrechen und das Programm beenden?");
        } else if(isExtracting) {
            alert.setContentText("Alle laufenden Prozesse abbrechen und das Programm beenden?\n\nDabei kann zu einem Datenverlust kommen.");
        } else {
            stage.close();
            return;
        }

        if(alert.showAndWait().get() == buttonYes)
            stage.close();
    }

//    @FXML
//    private void switchToSecondary() throws IOException {
//        App.setRoot("secondary");
//    }
}
