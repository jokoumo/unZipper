package de.craftingit;

import javafx.collections.ObservableList;
import javafx.concurrent.Service;
import javafx.concurrent.Task;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class ExportData extends Service<String> {
    private final ObservableList<Archive> ARCHIVES;
    private final Stage STAGE;

    ExportData(ObservableList<Archive> archives, Stage stage) {
        this.ARCHIVES = archives;
        this.STAGE = stage;
    }

    @Override
    protected Task<String> createTask() {
        return new Task<String>() {
            @Override
            protected String call() throws Exception {
                String dir;
                File file;
                BufferedWriter writer = null;
                FileChooser chooser = new FileChooser();
                chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV Datei","*.csv"));
                chooser.setInitialDirectory(new File(System.getProperty("user.home")));

                try {
                    dir = chooser.showSaveDialog(STAGE).toString();

                    try {
                        String data;
                        file = Files.createFile(Path.of(dir)).toFile();
                        writer = new BufferedWriter(new FileWriter(file));
                        for(Archive archive : ARCHIVES) {
                            data = (archive.getID() + 1) + ";" + archive.getDIR() + ";" + archive.getStatus() + "\n";
                            writer.write(data);
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    } finally {
                        assert writer != null;
                        writer.flush();
                        writer.close();
                    }
                } catch (NullPointerException | IOException e) {
                    e.printStackTrace();
                }
                return "Export abgeschlossen.";
            }
        };
    }
}
