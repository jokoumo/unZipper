package de.craftingit;

import javafx.application.Platform;
import javafx.collections.ObservableArray;
import javafx.collections.ObservableList;
import javafx.concurrent.Service;
import javafx.concurrent.Task;

public class ExtractService extends Service<Boolean> {

    @Override
    protected Task<Boolean> createTask() {
        return new Task<Boolean>() {
            @Override
            protected Boolean call() throws Exception {
//                    if (!archives.get(index).isExtracted()) {
//                        countTasks++;
//                        archives.get(index).extract(pwField.getText());
//
//                        if(index == (archives.size() -1))   // letztes Archiv
//                            return true;
//                    }
                return false;
            }
        };
    }
}
