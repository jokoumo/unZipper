module de.craftingit {
    requires javafx.controls;
    requires javafx.fxml;

    opens de.craftingit to javafx.fxml;
    exports de.craftingit;
}