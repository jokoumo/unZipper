package de.craftingit;

import java.io.File;
import java.io.IOException;
import javafx.fxml.FXML;
import org.apache.commons.compress.archivers.sevenz.SevenZArchiveEntry;
import org.apache.commons.compress.archivers.sevenz.SevenZFile;

public class PrimaryController {

    @FXML
    private void initialize() throws IOException {

    }

    @FXML
    private void test() throws IOException {
        SevenZFile file = new SevenZFile(new File("E:\\daten_0-1.7z"));
        SevenZArchiveEntry entry = file.getNextEntry();
        System.out.println(entry);
    }

    @FXML
    private void switchToSecondary() throws IOException {
        App.setRoot("secondary");
    }
}
