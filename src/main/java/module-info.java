module de.craftingit {
    requires javafx.controls;
    requires javafx.fxml;
    requires org.apache.commons.compress;

    opens de.craftingit to javafx.fxml;
    exports de.craftingit;
}