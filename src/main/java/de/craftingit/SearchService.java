package de.craftingit;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Service;
import javafx.concurrent.Task;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;

public class SearchService extends Service<ObservableList<Archive>> {
    private final ObservableList<Archive> archives = FXCollections.observableArrayList();
    private final Path DIR;
    private final String FORMAT;
    private final String INCLUDE;
    private final String EXCLUDE;

    SearchService(Path dir, String format, String include, String exclude) {
        this.DIR = dir;
        this.FORMAT = format;
        this.INCLUDE = include;
        this.EXCLUDE = exclude;
    }

    @Override
    protected Task<ObservableList<Archive>> createTask() {
        return new Task<ObservableList<Archive>>() {
            @Override
            protected ObservableList<Archive> call() {
                try {
                    Files.walkFileTree(DIR, new SimpleFileVisitor<>() {

                        @Override
                        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                            if(file.toString().endsWith(FORMAT) && !(file.toString().toLowerCase().contains("$recycle") || file.toString().toLowerCase().contains("trash/files"))) {
                                if(file.getFileName().toString().contains(INCLUDE)) {
                                    if(EXCLUDE.isEmpty() || !file.getFileName().toString().contains(EXCLUDE)) {
                                        archives.add(new Archive(file.toAbsolutePath()));
                                        return FileVisitResult.CONTINUE;
                                    }
                                }
                            }
                            return FileVisitResult.SKIP_SUBTREE;
                        }

                        @Override
                        public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
                            if(exc instanceof FileSystemException || exc instanceof FileNotFoundException) {
                                return FileVisitResult.SKIP_SUBTREE;
                            }
                            return super.visitFileFailed(file, exc);
                        }
                    });
                } catch (IOException e) {
                    System.err.println("Fehler bei der Suche: " + e.getMessage());
                }

                return archives;
            }
        };
    }
}
