package de.craftingit;

import javafx.concurrent.Service;
import javafx.concurrent.Task;

import java.io.*;

public class ExtractService extends Service<Integer> {
    private final static boolean IS_WINDOWS = System.getProperty("os.name").toLowerCase().startsWith("windows");
    private final String DIR_APP;
    private final Archive ARCHIVE;
    private final String PASSWORD;

    public ExtractService(String dirApp, Archive archive, String password) {
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
                    extractExtern();    //extern mit 7-zip entpacken (für alle Formate)
                }
                return 0;
            }
        };
    }

    private void extractExtern() {
        if(!(new File(ARCHIVE.getDIR().toString()).exists())) {
            ARCHIVE.setStatus("Archiv nicht gefunden.");
            return;
        } else {
            ARCHIVE.setStatus("Wird entpackt...");
        }

        try {
            ProcessBuilder builder;
            if(IS_WINDOWS) {
                builder = new ProcessBuilder(
                "cmd.exe", "/c",
                        "\"" + DIR_APP + "\" e \"" +                    //Pfad der 7z.exe + (e)xtract-Anweisung
                        ARCHIVE.getDIR() + "\" -p" +                    //Pfad des Archivs + (-p)assword Switch
                        PASSWORD + " -o\"" +                            //Passwort + (-o)Zielverzeichnis
                        ARCHIVE.getDIR().getParent() + "\\\" -aos");    //Pfad Zielverzeichnis + (-aos)Nichts überschreiben
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
                if(process.exitValue() == 0) {
                    ARCHIVE.setStatus("Entpackt");
                    ARCHIVE.setExtracted(true);

                } else
                    ARCHIVE.setStatus("Passwort falsch oder Archiv beschädigt.");
            } catch (InterruptedException e) {
                ARCHIVE.setStatus(e.getMessage());
            }
        } catch (IOException e) {
            System.err.println("Fehler: " + e.getMessage());
            ARCHIVE.setStatus("Passwort falsch oder Archiv beschädigt.");
        }
    }
}
