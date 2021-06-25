package de.craftingit;

import javafx.concurrent.Service;
import javafx.concurrent.Task;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.AccessDeniedException;
import java.nio.file.Files;
import java.nio.file.Path;

public class ExtractService extends Service<Integer> {
  private final static boolean IS_WINDOWS = System.getProperty("os.name").toLowerCase().startsWith("windows");
  private final String DIR_APP;
  private String targetDir;
  private final Archive ARCHIVE;
  private final String PASSWORD;

  public ExtractService(String dirApp, String targetDir, Archive archive, String password) {
    this.DIR_APP = dirApp;
    this.targetDir = targetDir;
    this.ARCHIVE = archive;
    this.PASSWORD = password;
  }

  @Override
  protected Task<Integer> createTask() {
    return new Task<Integer>() {
      @Override
      protected Integer call() throws Exception {
        File target = new File(targetDir);

        if(!targetDir.isEmpty() && !target.exists()) {
          try {
            Files.createDirectories(Path.of(targetDir));
          } catch (IOException e) {
            System.err.println("Zielverzeichnis konnte nicht angelegt werden. Fehlende Berechtigung?:\n" + target);
            return -1;
          }
        } else if(targetDir.isEmpty()) {
          targetDir = ARCHIVE.getDIR().getParent().toString();
        }

        if (!(new File(ARCHIVE.getDIR().toString()).exists())) {
          ARCHIVE.setStatus("Archiv nicht gefunden.");
          return 0;
        } else if (!ARCHIVE.isExtracted()) {
          extractExtern();    //extern mit 7-zip entpacken (für alle Formate)
        }
        return 0;
      }
    };
  }

  private void extractExtern() {
    ARCHIVE.setStatus("Wird entpackt...");

    try {
      ProcessBuilder builder;
      if (IS_WINDOWS) {
        builder = new ProcessBuilder(
            "cmd.exe", "/c",
            "\"" + DIR_APP +                             //Pfad der 7z.exe
            "\" e \"" + ARCHIVE.getDIR() +               //(e)xtract-Anweisung + Pfad des Archivs
            "\" -p" + PASSWORD +                         //(-p)assword Switch
            " -o\"" + targetDir +                        //(-o)Zielverzeichnis
            "\\\" -aos");                                //(-aos)Nichts überschreiben
      } else {
        builder = new ProcessBuilder(
            "7z", "e",
            ARCHIVE.getDIR().toString(),
            "-p" + PASSWORD,
            "-o" + targetDir,
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
//        System.out.println("+++ Ergebnis: " + process.exitValue());
        if (process.exitValue() == 0) {
          ARCHIVE.setStatus("Entpackt");
          ARCHIVE.setExtracted(true);
        } else
          ARCHIVE.setStatus("Passwort falsch/Archiv beschädigt/Kein Schreibrecht");
      } catch (InterruptedException e) {
        ARCHIVE.setStatus(e.getMessage());
      }
    } catch (IOException e) {
      System.err.println("Fehler: " + e.getMessage());
      ARCHIVE.setStatus("Passwort falsch/Archiv beschädigt");
    }
  }
}
