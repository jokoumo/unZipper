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
    private static int countId = 1;
    private final int id;
    private final Path dir;
    private String status;
    private boolean isExtracted = false;

    Archive(Path path) {
        path = Paths.get(path.toString().replace(".\\", ""));
        this.dir = path;
        this.status = "Verpackt";
        this.id = countId;
        countId++;
    }

    public Path getDir() {
        return dir;
    }

    public static int getCountId() {
        return countId;
    }

    public static void setCountId(int countId) {
        Archive.countId = countId;
    }

    public int getId() {
        return id;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String toString() {
        return this.dir.toString();
    }

    public boolean isExtracted() {
        return isExtracted;
    }

    public void print() {
        System.out.println("ID: " + this.id);
        System.out.println("Pfad: " + this.dir);
        System.out.println("Status: " + this.status);
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
                    this.isExtracted = true;
                } catch (Exception ex) {
                    this.status = "Passwort falsch oder Archiv beschädigt.";
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
            this.status = "Passwort falsch oder Archiv beschädigt.";
        }
    }
}
