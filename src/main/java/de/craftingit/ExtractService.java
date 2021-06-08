package de.craftingit;

import javafx.application.Platform;
import javafx.collections.ObservableArray;
import javafx.collections.ObservableList;
import javafx.concurrent.Service;
import javafx.concurrent.Task;

public class ExtractService extends Service<Boolean> {
    private final Archive archive;
    private final String password;

    public ExtractService(Archive archive, String password) {
        this.archive = archive;
        this.password = password;
    }

    @Override
    protected Task<Boolean> createTask() {
        return new Task<Boolean>() {
            @Override
            protected Boolean call() throws Exception {
                if (!archive.isExtracted()) {
                    archive.extract(password);
                }
                return null;
            }
        };
    }
}
