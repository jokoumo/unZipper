package de.craftingit;

import javafx.concurrent.Service;
import javafx.concurrent.Task;
import org.apache.commons.compress.archivers.sevenz.SevenZArchiveEntry;
import org.apache.commons.compress.archivers.sevenz.SevenZFile;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

public class ExtractService extends Service<Integer> {
    private final static boolean IS_WINDOWS = System.getProperty("os.name").toLowerCase().startsWith("windows");
    private final boolean EXEC_EXTERN;
    private final String DIR_APP;
    private final Archive ARCHIVE;
    private final String PASSWORD;

    public ExtractService(boolean execExtern, String dirApp, Archive archive, String password) {
        this.EXEC_EXTERN = execExtern;
        this.DIR_APP = dirApp;
        this.ARCHIVE = archive;
        this.PASSWORD = password;
    }

    @Override
    protected Task<Integer> createTask() {
        return new Task<Integer>() {
            @Override
            protected Integer call() throws Exception {
                if (!ARCHIVE.isExtracted()) {
                    if(EXEC_EXTERN) {
                        extract7zExtern();  //extern mit 7z.exe entpacken
                    } else {
                        extract7zIntern();  //intern Entpacken
                    }
                }
                return 0;
            }
        };
    }

    private void extract7zIntern() {
        File destFile;
        ARCHIVE.setStatus("Wird entpackt...");

        try(SevenZFile file = new SevenZFile(new File(ARCHIVE.getDIR().toString()), PASSWORD.getBytes(StandardCharsets.UTF_16LE))) {
            SevenZArchiveEntry entry = file.getNextEntry();

            while (entry != null) {
                destFile = new File(ARCHIVE.getDIR().getParent().toString(), entry.getName());

                try (FileOutputStream out = new FileOutputStream(destFile)) {
                    byte[] content = new byte[(int) entry.getSize()];
                    file.read(content, 0, content.length);
                    out.write(content);
                    ARCHIVE.setStatus("Entpackt");
                    ARCHIVE.setExtracted(true);
                } catch (Exception ex) {
                    ARCHIVE.setStatus("Passwort falsch oder Archiv beschädigt.");
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
            ARCHIVE.setStatus("Passwort falsch oder Archiv beschädigt.");
        }
    }

    private void extract7zExtern() {
        ARCHIVE.setStatus("Wird entpackt...");
        ProcessBuilder builder;
        try {
            if(IS_WINDOWS) {
                builder = new ProcessBuilder(
                "cmd.exe", "/c",
                        "\"" + DIR_APP + "\" e \"" +                    //Pfad der 7z.exe + (e)xtract-Anweisung
                        ARCHIVE.getDIR() + "\" -p" +                    //Pfad des Archivs + (-p)assword Switch
                        PASSWORD + " -o\"" +                            //Passwort + (-o)Zielverzeichnis
                        ARCHIVE.getDIR().getParent() + "\\\" -aos");    //Pfad Zielverzeichnis + (-aos)Nichts überschreiben, sondern überspringen
            } else {
                builder = new ProcessBuilder(
                "7z", "e",
                        ARCHIVE.getDIR().toString(),
                        "-p" + PASSWORD,
                        "-o" + ARCHIVE.getDIR().getParent().toString(),
                        "-aos");
            }

            builder.redirectErrorStream(true);
            Process process = builder.start();
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;

            do {
                line = reader.readLine();
                System.out.println(line);
            } while (line != null);

            try {
                process.waitFor();
                System.out.println("ExitValue: " + process.exitValue());
                if(process.exitValue() == 0) {
                    ARCHIVE.setStatus("Entpackt");
                    ARCHIVE.setExtracted(true);

                } else
                    ARCHIVE.setStatus("Passwort falsch oder Archiv beschädigt.");
            } catch (InterruptedException e) {
                ARCHIVE.setStatus(e.getMessage());
            }
        } catch (IOException e) {
            System.out.println("Fehler: " + e.getMessage());
            ARCHIVE.setStatus("Passwort falsch oder Archiv beschädigt.");
        }
    }
}
