package de.craftingit;

import java.nio.file.Path;
import java.nio.file.Paths;

public class Archive {
  private static int countId = 1;
  private final int ID;
  private final Path DIR;
  private String status;
  private boolean isExtracted = false;

  Archive(Path dir) {
    dir = Paths.get(dir.toString().replace(".\\", ""));
    dir = Paths.get(dir.toString().replace("./", ""));
    this.DIR = dir;
    this.status = "Verpackt";
    this.ID = countId;
    countId++;
  }

  Archive(int id, Path dir, String status, boolean isExtracted) {
    this.ID = id;
    this.DIR = dir;
    this.status = status;
    this.isExtracted = isExtracted;
  }

  public static int getCountId() {
    return countId;
  }

  public static void setCountId(int countId) {
    Archive.countId = countId;
  }

  public Path getDIR() {
    return DIR;
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
