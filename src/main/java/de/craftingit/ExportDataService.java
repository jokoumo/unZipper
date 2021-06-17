package de.craftingit;

import javafx.collections.ObservableList;
import javafx.concurrent.Service;
import javafx.concurrent.Task;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class ExportDataService extends Service<Boolean> {
    private final ObservableList<Archive> ARCHIVES;
    private final File FILE;

    ExportDataService(ObservableList<Archive> archives, File file) {
        this.ARCHIVES = archives;
        this.FILE = file;
    }

    @Override
    protected Task<Boolean> createTask() {
        return new Task<Boolean>() {
            @Override
            protected Boolean call() throws Exception {
                BufferedWriter writer = new BufferedWriter(new FileWriter(FILE));
                String data;

                try {
                    for(Archive archive : ARCHIVES) {
                        data = archive.getID() + ";" + archive.getDIR() + ";" + archive.getStatus() + ";" + archive.isExtracted() + "\n";
                        writer.write(data);
                    }
                    return true;
                } catch (IOException e) {
                    e.printStackTrace();
                    return false;
                } finally {
                    writer.flush();
                    writer.close();
                }
            }
        };
    }
}
