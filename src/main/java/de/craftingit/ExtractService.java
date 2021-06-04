package de.craftingit;

import javafx.collections.ObservableArray;
import javafx.collections.ObservableList;
import javafx.concurrent.Service;
import javafx.concurrent.Task;

public class ExtractService extends Service<ObservableList<Archive>> {


    @Override
    protected Task<ObservableList<Archive>> createTask() {
        return new Task<ObservableList<Archive>>() {
            @Override
            protected ObservableList<Archive> call() throws Exception {
                return null;
            }
        };
    }
}
