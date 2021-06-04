package de.craftingit;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Service;
import javafx.concurrent.WorkerStateEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Stage;

public class C_Main {
    private ObservableList<Archive> archives = FXCollections.observableArrayList();
    private File[] roots;
    private Path dir;
    private double quantityNotExtracted;

    @FXML
    private AnchorPane anchorPane_main;
    @FXML
    private Button button_search;
    @FXML
    private Button button_extract;
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
    private TableColumn<Archive, String> tColumn_error;
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
        tColumn_error.setCellValueFactory(new PropertyValueFactory<>("error"));
        tColumn_id.setCellValueFactory(new PropertyValueFactory<>("id"));
    }

    @FXML
    private void updateRoots() {
        comboBox_roots.getItems().clear();
        roots = File.listRoots();
        for(File root : roots) {
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
        button_search.setDisable(true);

        Service<ObservableList<Archive>> searchService = new SearchService(dir, comboBox_formats.getValue(),
                                                                  textField_filter.getText(), textField_exclude.getText());

        searchService.setOnSucceeded(new EventHandler<WorkerStateEvent>() {
            @Override
            public void handle(WorkerStateEvent workerStateEvent) {
                archives.addAll(searchService.getValue());
                progressBar.setProgress(0);
                quantityNotExtracted = archives.size();
                tableView_archives.setItems(archives);
                button_extract.setDisable(tableView_archives.getItems().isEmpty());
                button_search.setDisable(false);
            }
        });
        searchService.start();

//        try {
//            Files.walkFileTree(dir, new SimpleFileVisitor<>() {
//                @Override
//                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
//                    if(file.toString().endsWith(comboBox_formats.getValue())) {
//                        if(file.getFileName().toString().contains(textField_filter.getText())) {
//                            if(textField_exclude.getText().isEmpty() || !file.getFileName().toString().contains(textField_exclude.getText())) {
//                                archives.add(new Archive(file.toAbsolutePath()));
//                                quantityNotExtracted++;
//                                return FileVisitResult.CONTINUE;
//                            }
//                        }
//                    }
//                    return FileVisitResult.SKIP_SUBTREE;
//                }
//
//                @Override
//                public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
//                    if(exc instanceof AccessDeniedException)
//                        return FileVisitResult.SKIP_SUBTREE;
//                    return super.visitFileFailed(file, exc);
//                }
//            });
//        } catch (IOException e) {
//            System.out.println("Fehler bei der Suche: " + e.getMessage());
//        }
    }

    @FXML
    private void extractArchives() {
        progressBar.setProgress(0);

        Archive.setCountErrors(0);
        double count = 0;

        for(Archive archive : archives) {
            if(archive.getStatus().equals("Verpackt")) {
                archive.extract(pwField.getText());
                count++;
                progressBar.setProgress(count / quantityNotExtracted);
            }
        }

        tableView_archives.refresh();
        quantityNotExtracted = Archive.getCountErrors();

        if(checkBox_hideExtracted.isSelected())
            hideExtracted();
    }

    @FXML
    private void hideExtracted() {
        if(checkBox_hideExtracted.isSelected()) {
            ObservableList<Archive> hiddenArchives = FXCollections.observableArrayList();
            for(Archive archive : archives) {
                if(archive.getStatus().equals("Verpackt"))
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
        alert.setHeaderText("Entwickelt von André Krippendorf");
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
        Stage stage = (Stage) anchorPane_main.getScene().getWindow();
        stage.close();
    }

//    @FXML
//    private void switchToSecondary() throws IOException {
//        App.setRoot("secondary");
//    }
}
