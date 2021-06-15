package de.craftingit;

import javafx.collections.FXCollections;
import javafx.collections.ObservableArray;
import javafx.collections.ObservableList;
import javafx.concurrent.Service;
import javafx.concurrent.Task;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.Provider;

public class SearchService extends Service<ObservableList<Archive>> {
    private final ObservableList<Archive> archives = FXCollections.observableArrayList();
    private final Path dir;
    private final String format;
    private final String filter;
    private final String exclude;

    SearchService(Path dir, String format, String filter, String exclude) {
        this.dir = dir;
        this.format = format;
        this.filter = filter;
        this.exclude = exclude;
    }

    @Override
    protected Task<ObservableList<Archive>> createTask() {
        return new Task<ObservableList<Archive>>() {
            @Override
            protected ObservableList<Archive> call() {
                try {
                    Files.walkFileTree(dir, new SimpleFileVisitor<>() {

//                        @Override
//                        public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
//                            return FileVisitResult.SKIP_SUBTREE;
//                        }

                        @Override
                        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                            if(file.toString().endsWith(format) && !(file.toString().contains("$RECYCLE") || file.toString().contains("Trash/files"))) {
                                if(file.getFileName().toString().contains(filter)) {
                                    if(exclude.isEmpty() || !file.getFileName().toString().contains(exclude)) {
                                        archives.add(new Archive(file.toAbsolutePath()));
                                        return FileVisitResult.CONTINUE;
                                    }
                                }
                            }
                            return FileVisitResult.SKIP_SUBTREE;
                        }

                        @Override
                        public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
                            if(exc instanceof AccessDeniedException)
                                return FileVisitResult.SKIP_SUBTREE;
                            if(exc instanceof FileSystemException)
                                return FileVisitResult.SKIP_SUBTREE;
                            if(exc instanceof FileNotFoundException)
                                return FileVisitResult.SKIP_SUBTREE;
                            return super.visitFileFailed(file, exc);
                        }
                    });
                } catch (IOException e) {
                    System.out.println("Fehler bei der Suche: " + e.getMessage());
                }

                return archives;
            }
        };
    }
}
