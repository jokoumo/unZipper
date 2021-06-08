package de.craftingit;

import java.io.File;
import java.nio.file.*;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.ScheduledService;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Stage;
import javafx.util.Duration;

public class C_Main {
    private Stage stage = null;
    private ObservableList<Archive> archives = FXCollections.observableArrayList();
    private File[] roots;
    private Path dir;
    private final int maxTasks = 6;
    private int countTasks;
    private int countArchives;
    private int countSucceededServices;
    private double windowWidth;
    private SearchService searchService;
    private ExtractService extractService;
    private ScheduledService<Boolean> extractTaskService;

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
    private Label label_status;
    @FXML
    private ComboBox<String> comboBox_roots;
    @FXML
    private ComboBox<String> comboBox_formats;
    @FXML
    private ChoiceBox<Integer> choiceBox_tasks;
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

        choiceBox_tasks.setTooltip(new Tooltip("Viele Prozesse erhöhen die Prozessorlast."));
        for(int i = 1; i <= maxTasks; i++)
            choiceBox_tasks.getItems().add(i);
        choiceBox_tasks.setValue(1);

        comboBox_formats.getItems().add(".7z");
        //comboBox_formats.getItems().add(".zip");
        //comboBox_formats.getItems().add(".rar");
        comboBox_formats.setValue(".7z");

        tColumn_dir.setCellValueFactory(new PropertyValueFactory<>("dir"));
        tColumn_status.setCellValueFactory(new PropertyValueFactory<>("status"));
        tColumn_id.setCellValueFactory(new PropertyValueFactory<>("id"));

        button_search.setDisable(false);
        button_cancelSearch.setVisible(false);
        button_extractAll.setDisable(true);
        button_extractSingle.setDisable(true);
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
                                stage.setOnCloseRequest(event -> {
                                    event.consume();
                                    closeApp();
                                });
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
            tableView_archives.setItems(archives);
            button_extractAll.setDisable(tableView_archives.getItems().isEmpty());
            button_extractSingle.setDisable(tableView_archives.getItems().isEmpty());
            button_search.setVisible(true);
            button_cancelSearch.setVisible(false);
            label_status.setText("Suche abgeschlossen.");
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
    private void extractSingle() {
        Archive archive = tableView_archives.getSelectionModel().getSelectedItem();
        archive.extract(pwField.getText());
        tableView_archives.refresh();
    }

    @FXML
    private void extractAll() {
        countSucceededServices = 0;
        countArchives = 0;
        countTasks = 0;
        progressBar.setProgress(0);
        button_extractAll.setVisible(false);
        button_extractSingle.setVisible(false);
        button_cancelExtract.setVisible(true);
        button_search.setDisable(true);
        label_status.setVisible(true);
        label_status.setText("Entpacke...");

        extractTaskService = new ScheduledService<Boolean>() {
            @Override
            protected Task<Boolean> createTask() {
                return new Task<Boolean>() {
                    @Override
                    protected Boolean call() {
                        if(countArchives < 0 || countArchives >= archives.size()) {
                            this.cancel();
                        } else
                        if(countTasks < choiceBox_tasks.getValue()) {
                            extractAllStart(countArchives);
                            countTasks++;
                            countArchives++;
                            tableView_archives.refresh();
                        }
                        return null;
                    }
                };
            }
        };
        extractTaskService.setPeriod(Duration.millis(100));
        extractTaskService.start();
    }

    @FXML
    private void extractAllStart(int index) {
        try {
            extractService = new ExtractService(archives.get(index), pwField.getText());
        } catch (IndexOutOfBoundsException e) {
            System.out.println(e.getMessage());
        }

        extractService.setOnRunning(workerStateEvent -> {
            if (countArchives >= 0 && countArchives < archives.size() && !archives.get(countArchives).isExtracted()) {
                tableView_archives.refresh();
                if (checkBox_hideExtracted.isSelected())
                    hideExtracted();
            }
        });

        extractService.setOnSucceeded(workerStateEvent -> {
            countTasks--;
            countSucceededServices++;
            tableView_archives.refresh();

            if(index == (archives.size() -1)) {
                button_extractAll.setVisible(true);
                button_extractSingle.setVisible(true);
                button_cancelExtract.setVisible(false);
                button_search.setDisable(false);
                button_cancelExtract.setDisable(false);
                button_cancelExtract.setVisible(false);
                label_status.setText("Fertig!");
            }
            if (countArchives < 0) {
                label_status.setText("Entpacken abgebrochen.");
                progressBar.setProgress(0);
            } else
                progressBar.setProgress((double) countSucceededServices / (double) archives.size());
        });
        extractService.start();
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
        } else if(extractService != null && extractService.isRunning()) {
            alert.setContentText("Alle laufenden Prozesse abbrechen und das Programm beenden?\nEs kann zu einem Datenverlust kommen.");
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
