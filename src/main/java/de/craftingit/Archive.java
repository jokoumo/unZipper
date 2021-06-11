package de.craftingit;

import org.apache.commons.compress.archivers.sevenz.SevenZArchiveEntry;
import org.apache.commons.compress.archivers.sevenz.SevenZFile;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class Archive {
    private static int countId = 1;
    private final int ID;
    private final Path DIR;
    private String status;
    private boolean isExtracted = false;

    Archive(Path path) {
        path = Paths.get(path.toString().replace(".\\", ""));
        this.DIR = path;
        this.status = "Verpackt";
        this.ID = countId;
        countId++;
    }

    public Path getDIR() {
        return DIR;
    }

    public static int getCountId() {
        return countId;
    }

    public static void setCountId(int countId) {
        Archive.countId = countId;
    }

    public int getID() {
        return ID;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String toString() {
        return (this.ID + " : " + this.DIR.toString());
    }

    public boolean isExtracted() {
        return isExtracted;
    }

    public void print() {
        System.out.println("ID: " + this.ID);
        System.out.println("Pfad: " + this.DIR);
        System.out.println("Status: " + this.status);
    }

    public void extract7zIntern(String password) {
        File destFile;
        this.status = "Wird entpackt...";

        try(SevenZFile file = new SevenZFile(new File(DIR.toString()), password.getBytes(StandardCharsets.UTF_16LE))) {
            SevenZArchiveEntry entry = file.getNextEntry();

            while (entry != null) {
                destFile = new File(DIR.getParent().toString(), entry.getName());

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

    public void extract7zExtern(String password, String dirApp) {
        this.status = "Wird entpackt...";
        try {
            ProcessBuilder builder = new ProcessBuilder(
                    "cmd.exe", "/c",
                    "\"" + dirApp + "\" e \"" +
                            this.getDIR() + "\" -p" +
                            password + " -o\"" +
                            this.getDIR().getParent() + "\\\" -aos");
            System.out.println("\"" + dirApp + "\" e \"" +
                    this.getDIR() + "\" -p" +
                    password + " -o\"" +
                    this.getDIR().getParent() + "\\\" -aos");
            Process process = builder.start();
            BufferedReader r = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            while (true) {
                line = r.readLine();
                if (line == null) {
                    break;
                }
                System.out.println(line);
            }
            System.out.println("+++++ ERGEBNIS: " + process.exitValue());
            if(process.exitValue() == 0) {
                this.status = "Entpackt";
                this.isExtracted = true;
            }
            else
                this.status = "Passwort falsch oder Archiv beschädigt.";
        } catch (IOException e) {
            System.out.println("Fehler: " + e.getMessage());
            this.status = "Passwort falsch oder Archiv beschädigt.";
        }
    }
}
