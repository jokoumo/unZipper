package de.craftingit;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Service;
import javafx.concurrent.Task;

import java.io.File;
import java.nio.file.Path;
import java.util.Scanner;

public class ImportDataService extends Service<ObservableList<Archive>> {
    private final File FILE;
    private ObservableList<Archive> archives = FXCollections.observableArrayList();

    ImportDataService(File file) {
        this.FILE = file;
    }

    @Override
    protected Task<ObservableList<Archive>> createTask() {
        return new Task<ObservableList<Archive>>() {
            @Override
            protected ObservableList<Archive> call() throws Exception {
                Scanner scanner = new Scanner(FILE);
                String[] str;

                try {
                    while (scanner.hasNextLine()) {
                        str = scanner.nextLine().split(";");
                        archives.add(new Archive(Integer.parseInt(str[0]), Path.of(str[1]), str[2], Boolean.parseBoolean(str[3])));
                    }
                } catch (Exception e) {
                    System.err.println(e.getMessage());
                    this.cancel();
                }
                return archives;
            }
        };
    }
}
