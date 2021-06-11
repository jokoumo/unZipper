package de.craftingit;

import javafx.application.Platform;
import javafx.collections.ObservableArray;
import javafx.collections.ObservableList;
import javafx.concurrent.Service;
import javafx.concurrent.Task;

import java.io.BufferedReader;
import java.io.InputStreamReader;

public class ExtractService extends Service<Integer> {
    private final String dirApp;
    private final Archive archive;
    private final String password;

    public ExtractService(Archive archive, String password) {
        this.dirApp = "";
        this.archive = archive;
        this.password = password;
    }

    public ExtractService(String dirApp, Archive archive, String password) {
        this.dirApp = dirApp;
        this.archive = archive;
        this.password = password;
    }

    @Override
    protected Task<Integer> createTask() {
        return new Task<Integer>() {
            @Override
            protected Integer call() throws Exception {
                if (!archive.isExtracted()) {
                    if(dirApp.isEmpty()) {
                        archive.extract7zIntern(password);  //intern Entpacken
                    } else {
                        archive.extract7zExtern(password, dirApp);   //extern mit 7z.exe entpacken
                    }
                }
                return 0;
            }
        };
    }
}
