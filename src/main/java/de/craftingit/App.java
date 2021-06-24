package de.craftingit;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;

import java.io.IOException;

/**
 * JavaFX App
 */
public class App extends Application {
    private static Scene scene;

    @Override
    public void start(Stage stage) throws IOException {
        scene = new Scene(loadFXML("Main"));
        stage.setScene(scene);
        stage.setTitle("unZipper");
        stage.show();

        try {
            Image icon = new Image(getClass().getResourceAsStream("images/icon.png"));
            stage.getIcons().add(icon);
        } catch(Exception e) {
            System.err.println("Icon nicht gefunden: " + e.getMessage());
        }

        stage.setMinWidth(965);
        stage.setMinHeight(700);
    }

    static void setRoot(String fxml) throws IOException {
        scene.setRoot(loadFXML(fxml));
    }

    private static Parent loadFXML(String fxml) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(App.class.getResource(fxml + ".fxml"));
        return fxmlLoader.load();
    }

    public static void main(String[] args) {
        launch();
    }
}