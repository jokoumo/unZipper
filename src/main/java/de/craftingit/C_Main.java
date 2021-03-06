package de.craftingit;

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

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class C_Main {
  private final boolean IS_WINDOWS = System.getProperty("os.name").toLowerCase().startsWith("windows");
  private final int MAX_TASKS = 8;
  private int countTasks;
  private int countArchives;
  private int countSucceededServices;
  private double windowWidth;
  private boolean isBusy;
  private Stage stage = null;
  private ObservableList<Archive> archives = FXCollections.observableArrayList();
  private File[] roots;
  private Path dirRoot = Path.of("");
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
  private Button button_updateRoots;
  @FXML
  private Button button_rootAddition;
  @FXML
  private Button button_findArchivist;
  @FXML
  private Button button_findTargetDir;
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
  private TextField textField_targetDir;
  @FXML
  private PasswordField pwField;
  @FXML
  private CheckBox checkBox_hideExtracted;
  @FXML
  private CheckBox checkBox_deleteExtracted;
  @FXML
  private ProgressBar progressBar;
  @FXML
  private MenuItem menuItem_exportTable;
  @FXML
  private MenuItem menuItem_importTable;
  @FXML
  private Label label_pw;

  @FXML
  private void initialize() {
    updateRoots();

    for (int i = 1; i <= MAX_TASKS; i++) {
      comboBox_tasks.getItems().add(i);
    }
    comboBox_tasks.setValue(1);

    comboBox_tasks.setTooltip(new Tooltip("Viele Prozesse erh??hen die Auslastung von Prozessor und Datentr??ger."));
    textField_rootAddition.setTooltip(new Tooltip("In MS Windows z.B. 'Benutzer\\Default'. Andere z.B. 'home/usr'"));
    textField_targetDir.setTooltip(new Tooltip("Entpackt alle Dateien in das angegebene Verzeichnis. Leer lassen, um im Stammverzeichnis zu entpacken."));
    checkBox_hideExtracted.setTooltip(new Tooltip("Bereits entpackte Archive werden ausgeblendet"));
    checkBox_deleteExtracted.setTooltip(new Tooltip("Archive werden nach dem erfolgreichen Entpacken gel??scht."));
    button_extractSingle.setTooltip(new Tooltip("Entpackt das ausgew??hlte Archiv."));
    button_extractAll.setTooltip(new Tooltip("Entpackt alle aufgelisteten Archive."));
    pwField.setTooltip(new Tooltip("*Archive ohne Passwortschutz werden immer entpackt."));
    label_pw.setTooltip(new Tooltip("*Archive ohne Passwortschutz werden immer entpackt."));

    comboBox_formats.getItems().add(".7z");
    comboBox_formats.getItems().add(".img");
    comboBox_formats.getItems().add(".iso");
    comboBox_formats.getItems().add(".rar");
    comboBox_formats.getItems().add(".tar");
    comboBox_formats.getItems().add(".zip");
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

    if (!IS_WINDOWS) {
      comboBox_roots.setDisable(true);
      textField_appDir.setDisable(true);
      button_updateRoots.setDisable(true);
      button_findArchivist.setDisable(true);
      textField_appDir.setText("Installation von \"p7zip-full\" ben??tigt");
    } else {
      textField_appDir.setText("C:\\Program Files\\7-Zip\\7z.exe");
      setArchivistDirTextColor();
    }

    Platform.runLater(() -> {
      if (stage == null) {
        stage = (Stage) anchorPane_main.getScene().getWindow();
        stage.setOnCloseRequest(event -> {
          event.consume();
          closeApp();
        });
      }
      alignGui();
    });
  }

  @FXML
  private void alignGui() {
    ScheduledService<Boolean> backgroundService = new ScheduledService<Boolean>() {
      @Override
      protected Task<Boolean> createTask() {
        return new Task<Boolean>() {
          @Override
          protected Boolean call() {
            if (windowWidth != stage.getWidth()) {
              windowWidth = stage.getWidth();
              progressBar.setLayoutX(windowWidth / 2 - progressBar.getWidth() / 2);
              label_status.setLayoutX(windowWidth / 2 - label_status.getWidth() / 2);
              tColumn_dir.setPrefWidth(tableView_archives.getWidth() - tColumn_id.getWidth() - tColumn_status.getWidth());
            }
            return null;
          }
        };
      }
    };
    backgroundService.setPeriod(Duration.millis(50));
    backgroundService.start();
  }

  private void updateGui(boolean disable) {
    isBusy = disable;
    button_rootAddition.setDisable(disable);
    button_search.setDisable(disable);
    button_updateRoots.setDisable(disable);
    button_findTargetDir.setDisable(disable);
    menuItem_importTable.setDisable(disable);
    pwField.setDisable(disable);
    textField_rootAddition.setDisable(disable);
    textField_includeFilter.setDisable(disable);
    textField_excludeFilter.setDisable(disable);
    textField_targetDir.setDisable(disable);
    checkBox_deleteExtracted.setDisable(disable);
    comboBox_formats.setDisable(disable);
    comboBox_roots.setDisable(disable);

    if(IS_WINDOWS) {
      textField_appDir.setDisable(disable);
      button_findArchivist.setDisable(disable);
    }

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

  private void changeDir() {
    if (IS_WINDOWS) {
      dirRoot = Paths.get(comboBox_roots.getValue() + (textField_rootAddition.getText().isEmpty() ? "." : textField_rootAddition.getText()));
    } else {
      dirRoot = Paths.get(comboBox_roots.getValue() + textField_rootAddition.getText());
    }
  }

  @FXML
  private void findRootAddition() {
    try {
      String localDir = new DirectoryChooser().showDialog(stage).toString();
      for (File root : roots) {
        if (localDir.contains(root.toString())) {
          comboBox_roots.setValue(root.toString());
        }
        if (IS_WINDOWS) {
          localDir = localDir.replace(root.toString(), "");
        }
      }
      textField_rootAddition.setText(localDir);
    } catch (NullPointerException e) {
      System.err.println("Auswahl abgebrochen: " + e.getMessage());
    }
  }

  @FXML
  private void clearRootAddition() {
    textField_rootAddition.clear();
  }

  @FXML
  private void findArchivistDir() {
    String localDir = "";
    FileChooser chooser = new FileChooser();
    chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("EXE Dateien", "*.exe"));

    try {
      chooser.setInitialDirectory(new File(Path.of(textField_appDir.getText()).getParent().toString()));
    } catch (NullPointerException e) {
      chooser.setInitialDirectory(new File(System.getProperty("user.home")));
    }
    localDir = new File(String.valueOf(chooser.showOpenDialog(stage))).getAbsolutePath();
    if (!localDir.endsWith("null")) {
      textField_appDir.setText(localDir);
    }

    setArchivistDirTextColor();
  }

  @FXML
  private void setArchivistDirTextColor() {
    if (!textField_appDir.getText().endsWith("7z.exe") || !(new File(textField_appDir.getText()).exists())) {
      textField_appDir.setStyle("-fx-text-fill: red;");
    } else {
      textField_appDir.setStyle("-fx-text-fill: black;");
    }
  }

  @FXML
  private void deleteExtractedWarning() {
    if (checkBox_deleteExtracted.isSelected()) {
      Alert alert = new Alert(Alert.AlertType.WARNING);
      alert.setTitle("Achtung!");
      alert.setHeaderText("Damit werden alle entpackten Archive unwiderruflich gel??scht!");
      alert.show();
    }
  }

  @FXML
  private void setTargetDir() {
    try {
      String localDir = new DirectoryChooser().showDialog(stage).toString();
      textField_targetDir.setText(localDir);
    } catch (NullPointerException e) {
      System.err.println("Auswahl abgebrochen: " + e.getMessage());
    }
    setTargetDirTextColor();
  }

  @FXML
  private void setTargetDirTextColor() {
    if(isBusy) {
      textField_targetDir.setStyle("-fx-text-fill: red;");
    } else {
      textField_targetDir.setStyle("-fx-text-fill: black;");
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
    button_cancelSearch.setCancelButton(true);
    label_status.setText("Suche l??uft...");
    updateGui(true);

    searchService = new SearchService(dirRoot, comboBox_formats.getValue(),
        textField_includeFilter.getText(), textField_excludeFilter.getText());

    searchService.setOnSucceeded(workerStateEvent -> {
      archives.addAll(searchService.getValue());
      tableView_archives.setItems(archives);
      isBusy = false;
      progressBar.setProgress(1);
      button_search.setVisible(true);
      button_cancelSearch.setVisible(false);
      button_cancelSearch.setCancelButton(false);
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
    button_cancelSearch.setCancelButton(false);
    label_status.setText("Suche abgebrochen.");
    updateGui(false);
  }

  @FXML
  private void exportTable() {
    isBusy = true;
    File file;
    FileChooser chooser = new FileChooser();
    chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV Datei", "*.csv"));
    chooser.setInitialDirectory(new File(System.getProperty("user.home")));
    chooser.setInitialFileName("archivelist.csv");

    try {
      file = chooser.showSaveDialog(stage);
      label_status.setText("Exportiere...");
      progressBar.setProgress(-1);
      updateGui(true);
    } catch (NullPointerException e) {
      e.printStackTrace();
      updateGui(false);
      return;
    }

    if (file != null) {
      ExportDataService exportDataService = new ExportDataService(archives, file);

      exportDataService.setOnSucceeded(workerStateEvent -> {
        if (exportDataService.getValue()) {
          label_status.setText("Export abgeschlossen.");
          progressBar.setProgress(1);
        } else {
          label_status.setText("Export fehlgeschlagen.");
          progressBar.setProgress(0);
        }
        updateGui(false);
      });
      exportDataService.start();
    } else {
      label_status.setText("Export abgebrochen.");
      updateGui(false);
      progressBar.setProgress(0);
    }
    isBusy = false;
  }

  @FXML
  private void importTable() {
    isBusy = true;
    FileChooser chooser = new FileChooser();
    chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV Dateien", "*.csv"));
    chooser.setInitialDirectory(new File(System.getProperty("user.home")));
    File file;

    try {
      file = new File(chooser.showOpenDialog(stage).toString());
      updateGui(true);
      label_status.setText("Importiere...");
      progressBar.setProgress(-1);
      archives.clear();
      tableView_archives.getItems().clear();
    } catch (NullPointerException e) {
      System.err.println(e.getMessage());
      label_status.setText("Import abgebrochen.");
      progressBar.setProgress(0);
      updateGui(false);
      return;
    }

    ImportDataService importDataService = new ImportDataService(file);

    importDataService.setOnCancelled(workerStateEvent -> {
      label_status.setText("Import fehlgeschlagen. Bitte Datei pr??fen.");
      progressBar.setProgress(0);
      updateGui(false);
    });

    importDataService.setOnSucceeded(workerStateEvent -> {
      archives = importDataService.getValue();
      tableView_archives.setItems(archives);
      label_status.setText("Import abgeschlossen.");
      progressBar.setProgress(1);
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
    countSucceededServices = 0;
    countArchives = 0;
    countTasks = 0;
    button_cancelExtract.setVisible(true);
    button_extractSingle.setVisible(false);
    button_extractAll.setVisible(false);
    button_cancelExtract.setCancelButton(true);
    label_status.setText("Entpacke...");
    updateGui(true);

    if (event.getSource() == button_extractSingle) {
      countArchives = archives.size() - 1;
      progressBar.setProgress(-1);
    } else if (event.getSource() == button_extractAll) {
      progressBar.setProgress(0);
    }

    extractTaskService = new ScheduledService<Boolean>() {
      @Override
      protected Task<Boolean> createTask() {
        return new Task<Boolean>() {
          @Override
          protected Boolean call() {
            if (countTasks == 0 && (countArchives < 0 || countArchives >= archives.size())) {
              this.cancel();
              Platform.runLater(() -> {
                updateGui(false);
                button_cancelExtract.setVisible(false);
                button_extractSingle.setVisible(true);
                button_extractAll.setVisible(true);
                button_cancelExtract.setCancelButton(false);
                label_status.setText(countArchives < 0 ? "Vorgang abgebrochen." : "Fertig!");
                progressBar.setProgress(countArchives < 0 ? 0 : 1);
              });
            } else if (countTasks < comboBox_tasks.getValue() && !(countArchives < 0 || countArchives >= archives.size())) {
              if (progressBar.getProgress() >= 0) {    // Alles entpacken
                extractStart(countArchives);
              } else {                                  // Auswahl entpacken
                Archive archive = tableView_archives.getSelectionModel().getSelectedItem();
                archive.setExtracted(false);
                extractStart(archive.getID() - 1);
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

  private void extractStart(int index) {
    try {
      extractService = new ExtractService(textField_appDir.getText(), textField_targetDir.getText(), archives.get(index), pwField.getText());
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

      if(extractService.getValue() == -1) {
        countArchives = -1;
        setTargetDirTextColor();
      }

      if (archives.get(index).isExtracted() && checkBox_deleteExtracted.isSelected()) {
        try {
          Files.deleteIfExists(archives.get(index).getDIR());
        } catch (IOException e) {
          e.printStackTrace();
        }
      }

      if (checkBox_hideExtracted.isSelected()) {
        hideExtracted();
      }

      if (countArchives >= 0 && progressBar.getProgress() >= 0) {
        progressBar.setProgress((double) countSucceededServices / (double) archives.size());
      }
    });

    extractService.setOnFailed(workerStateEvent -> {
      System.err.println("ExtractService failed. Nummer: " + (index + 1));
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
        if (!archive.isExtracted()) {
          hiddenArchives.add(archive);
        }
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
    alert.setHeaderText("unZipper by Andr?? Krippendorf");
    alert.setContentText(
        "CraftingIT GmbH\n" +
            "Erzbergerstra??e 1-2\n" +
            "39104 Magdeburg\n" +
            "Deutschland\n" +
            "Telefon: +49 391 28921 500\n" +
            "Fax: +49 391 28921 555\n" +
            "Mail: info@crafting-it.de");
    alert.show();
  }

  private void cancelServices() {
    if (extractTaskService != null) {
      extractTaskService.cancel();
    }
    if (extractService != null) {
      extractService.cancel();
    }
    if (searchService != null) {
      searchService.cancel();
    }
  }

  @FXML
  private void closeApp() {
    ButtonType buttonYes = new ButtonType("Ja", ButtonBar.ButtonData.YES);
    ButtonType buttonNo = new ButtonType("Nein", ButtonBar.ButtonData.NO);
    Alert alert = new Alert(Alert.AlertType.CONFIRMATION, "", buttonYes, buttonNo);
    alert.setTitle("Beenden");
    alert.setHeaderText("Prozesse abbrechen und beenden?");
    alert.setContentText("Alle laufenden Prozesse abbrechen und das Programm beenden?");

    if (isBusy) {
      alert.setContentText("Alle laufenden Prozesse abbrechen und das Programm beenden?");
    } else {
      stage.close();
      return;
    }

    if (alert.showAndWait().get() == buttonYes) {
      cancelServices();
      stage.close();
    }
  }

  @FXML
  private void resetApp() throws IOException {
    ButtonType buttonYes = new ButtonType("Ja", ButtonBar.ButtonData.YES);
    ButtonType buttonNo = new ButtonType("Nein", ButtonBar.ButtonData.NO);
    Alert alert = new Alert(Alert.AlertType.CONFIRMATION, "", buttonYes, buttonNo);
    alert.setTitle("GUI zur??cksetzen");
    alert.setHeaderText("Prozesse abbrechen und Oberfl??che zur??cksetzen?");
    alert.setContentText("Alle laufenden Prozesse abbrechen und das Programm zur??cksetzen?");

    if (isBusy) {
      if (alert.showAndWait().get() == buttonYes) {
        cancelServices();
        App.setRoot("Main");
      }
    } else {
      App.setRoot("Main");
    }
  }
}
