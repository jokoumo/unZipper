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
        path = Paths.get(path.toString().replace("./", ""));
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

    public void setExtracted(boolean extracted) {
        isExtracted = extracted;
    }

    public void print() {
        System.out.println("ID: " + this.ID);
        System.out.println("Pfad: " + this.DIR);
        System.out.println("Status: " + this.status);
    }
}
