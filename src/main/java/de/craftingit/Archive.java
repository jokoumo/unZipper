package de.craftingit;

import org.apache.commons.compress.PasswordRequiredException;
import org.apache.commons.compress.archivers.sevenz.SevenZArchiveEntry;
import org.apache.commons.compress.archivers.sevenz.SevenZFile;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class Archive {
    private static double countErrors = 0;
    private static long countId = 1;
    private final long id;
    private final Path dir;
    private String status;
    private String error;

    Archive(Path path) {
        path = Paths.get(path.toString().replace(".\\", ""));
        this.dir = path;
        this.status = "Verpackt";
        this.error = "-";
        this.id = countId;
        countId++;
    }

    public Path getDir() {
        return dir;
    }

    public static double getCountErrors() {
        return countErrors;
    }

    public static void setCountErrors(double countErrors) {
        Archive.countErrors = countErrors;
    }

    public static long getCountId() {
        return countId;
    }

    public static void setCountId(long countId) {
        Archive.countId = countId;
    }

    public long getId() {
        return id;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }

    public String toString() {
        return this.dir.toString();
    }

    public void print() {
        System.out.println("ID: " + this.id);
        System.out.println("Pfad: " + this.dir);
        System.out.println("Status: " + this.status);
        System.out.println("Fehler: " + this.error);
    }

    public void extract(String password) {
        File destFile;

        try(SevenZFile file = new SevenZFile(new File(dir.toString()), password.getBytes(StandardCharsets.UTF_16LE))) {
            SevenZArchiveEntry entry = file.getNextEntry();

            while (entry != null) {
                destFile = new File(dir.getParent().toString(), entry.getName());

                try (FileOutputStream out = new FileOutputStream(destFile)) {
                    byte[] content = new byte[(int) entry.getSize()];
                    file.read(content, 0, content.length);
                    out.write(content);
                    this.status = "Entpackt";
                    this.error = "-";
                } catch (PasswordRequiredException ex) {
                    this.error = "Passwort erforderlich.";
                    countErrors++;
                    break;
                } catch (IOException ex) {
                    this.error = "Passwort falsch oder Archiv beschädigt.";
                    countErrors++;
                    break;
                } finally {
                    try {
                        if (Files.size(destFile.toPath()) == 0)
                            Files.delete(destFile.toPath());
                    } catch(Exception exDel) {
                        System.out.println("Keine Datei gelöscht: " + exDel.getMessage());
                    }
                    entry = file.getNextEntry();
                }
            }
        } catch(IOException e) {
            this.error = e.getMessage();
            countErrors++;
        }
    }
}
