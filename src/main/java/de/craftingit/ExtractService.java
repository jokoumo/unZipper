package de.craftingit;

import javafx.concurrent.Service;
import javafx.concurrent.Task;
import org.apache.commons.compress.archivers.sevenz.SevenZArchiveEntry;
import org.apache.commons.compress.archivers.sevenz.SevenZFile;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

public class ExtractService extends Service<Integer> {
    private final static boolean isWindows = System.getProperty("os.name").toLowerCase().startsWith("windows");
    private final String dirApp;
    private final Archive archive;
    private final String password;

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
                    if(dirApp.isEmpty() && isWindows) {
                        extract7zIntern();  //intern Entpacken
                    } else {
                        extract7zExtern();  //extern mit 7z.exe entpacken
                    }
                }
                return 0;
            }
        };
    }

    public void extract7zIntern() {
        File destFile;
        archive.setStatus("Wird entpackt...");

        try(SevenZFile file = new SevenZFile(new File(archive.getDIR().toString()), password.getBytes(StandardCharsets.UTF_16LE))) {
            SevenZArchiveEntry entry = file.getNextEntry();

            while (entry != null) {
                destFile = new File(archive.getDIR().getParent().toString(), entry.getName());

                try (FileOutputStream out = new FileOutputStream(destFile)) {
                    byte[] content = new byte[(int) entry.getSize()];
                    file.read(content, 0, content.length);
                    out.write(content);
                    archive.setStatus("Entpackt");
                    archive.setExtracted(true);
                } catch (Exception ex) {
                    archive.setStatus("Passwort falsch oder Archiv beschädigt.");
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
            archive.setStatus("Passwort falsch oder Archiv beschädigt.");
        }
    }

    public void extract7zExtern() {
        archive.setStatus("Wird entpackt...");
        ProcessBuilder builder;
        try {
            if(isWindows) {
                builder = new ProcessBuilder(
                        "cmd.exe", "/c",
                        "\"" + dirApp + "\" e \"" +                             //Pfad der 7z.exe + (e)xtract-Anweisung
                                archive.getDIR() + "\" -p" +                    //Pfad des Archivs + (-p)assword Switch
                                password + " -o\"" +                            //Passwort + (-o)Zielverzeichnis
                                archive.getDIR().getParent() + "\\\" -aos");    //Pfad Zielverzeichnis + (-aos)Nichts überschreiben, sondern überspringen
            } else {
                builder = new ProcessBuilder(
                        "7z e \"" +                                   //Anwendung aufrufen + (e)xtract-Anweisung
                                archive.getDIR() + "\" -p" +                    //Pfad des Archivs + (-p)assword Switch
                                password + " -o\"" +                            //Passwort + (-o)Zielverzeichnis
                                archive.getDIR().getParent() + "\\\" -aos");    //Pfad Zielverzeichnis + (-aos)Nichts überschreiben, sondern überspringen
            }

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
                archive.setStatus("Entpackt");
                archive.setExtracted(true);
            }
            else
                archive.setStatus("Passwort falsch oder Archiv beschädigt.");
        } catch (IOException e) {
            System.out.println("Fehler: " + e.getMessage());
            archive.setStatus("Passwort falsch oder Archiv beschädigt.");
        }
    }
}
