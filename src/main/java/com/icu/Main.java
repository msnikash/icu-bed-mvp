package com.icu;

import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;

import java.sql.SQLException;
import java.util.List;
import java.net.URL;

public class Main extends Application {

    private DBManager db;

    private ObservableList<String> waitingListData = FXCollections.observableArrayList();
    private ObservableList<String> allocatedListData = FXCollections.observableArrayList();
    private ObservableList<String> bedsData = FXCollections.observableArrayList();

    @Override
    public void start(Stage stage) throws Exception {
        try {
            db = new DBManager();
        } catch (SQLException e) {
            showError("DB Connection failed: " + e.getMessage());
            return;
        }

        // UI controls
        TextField nameField = new TextField();
        nameField.setPromptText("Name");

        TextField ageField = new TextField();
        ageField.setPromptText("Age");

        Spinner<Integer> prioSpinner = new Spinner<>(1, 5, 3);
        prioSpinner.setEditable(true);

        Button addBtn = new Button("Add Patient");
        Button allocBtn = new Button("Allocate Next");

        ListView<String> waitingView = new ListView<>(waitingListData);
        ListView<String> allocatedView = new ListView<>(allocatedListData);
        ListView<String> bedsView = new ListView<>(bedsData);

        // ids for CSS styling
        waitingView.setId("waiting-view");
        allocatedView.setId("allocated-view");
        bedsView.setId("beds-view");

        addBtn.setOnAction(ev -> {
            String name = nameField.getText().trim();
            String ageS = ageField.getText().trim();
            int priority = prioSpinner.getValue();

            if (name.isEmpty()) {
                showError("Name required");
                return;
            }
            int age = 0;
            try {
                age = Integer.parseInt(ageS);
            } catch (NumberFormatException ex) {
                showError("Invalid age. Use a number.");
                return;
            }

            Patient p = new Patient(name, age, priority);
            try {
                db.addPatient(p);
                refreshAll();
                nameField.clear();
                ageField.clear();
            } catch (SQLException ex) {
                showError("Failed to add patient: " + ex.getMessage());
            }
        });

        allocBtn.setOnAction(ev -> {
            try {
                String msg = db.allocateNextPatient();
                refreshAll();
                showInfo(msg);
            } catch (SQLException ex) {
                showError("Allocation failed: " + ex.getMessage());
            }
        });

        // Layout
        Label addLabel = new Label("Add Patient");
        addLabel.getStyleClass().add("section-title");
        Label prioLabel = new Label("Priority (1 = highest)");
        prioLabel.getStyleClass().add("section-subtitle");

        VBox inputBox = new VBox(12,
            addLabel,
            nameField,
            ageField,
            prioLabel,
            prioSpinner,
            addBtn,
            allocBtn
        );
        inputBox.setPadding(new Insets(10));
        inputBox.setPrefWidth(220);

        Label waitingLabel = new Label("Waiting (by priority)");
        waitingLabel.getStyleClass().add("section-title");
        Label allocatedLabel = new Label("Allocated");
        allocatedLabel.getStyleClass().add("section-title");
        Label bedsLabel = new Label("All Free Beds");
        bedsLabel.getStyleClass().add("section-title");

        VBox listsBox = new VBox(12,
            waitingLabel,
            waitingView,
            allocatedLabel,
            allocatedView,
            bedsLabel,
            bedsView
        );
        listsBox.setPadding(new Insets(10));
        listsBox.setPrefWidth(460);

        HBox root = new HBox(20, inputBox, listsBox);
        root.setId("app-root");
        inputBox.setId("input-card");
        listsBox.setId("lists-card");
        root.setPadding(new Insets(10));

        Scene scene = new Scene(root, 720, 520);
        // load stylesheet from resources if present
        URL cssUrl = getClass().getResource("/styles.css");
        if (cssUrl != null) {
            scene.getStylesheets().add(cssUrl.toExternalForm());
        }
        stage.setScene(scene);
        stage.setTitle("ICU Bed Allocation - MVP");
        stage.show();

        refreshAll();
    }

    private void refreshAll() {
        try {
            List<Patient> waiting = db.getWaitingPatients();
            waitingListData.clear();
            for (Patient p : waiting) {
                waitingListData.add(p.getName() + " (Age:" + p.getAge() + " P=" + p.getConditionPriority() + ")");
            }

            List<String> allocated = db.getAllocatedInfo();
            allocatedListData.setAll(allocated);

            List<Bed> beds = db.getAvailableBeds();
            bedsData.clear();
            for (Bed b : beds) {
                bedsData.add(b.getBedNumber() + " (id=" + b.getId() + ")");
            }
        } catch (SQLException e) {
            showError("Refresh failed: " + e.getMessage());
        }
    }

    private void showError(String msg) {
        Alert a = new Alert(Alert.AlertType.ERROR, msg, ButtonType.OK);
        a.setHeaderText("Error");
        // allow CSS targeting of error dialogs
        a.getDialogPane().getStyleClass().add("error-dialog");
        a.showAndWait();
    }

    private void showInfo(String msg) {
        Alert a = new Alert(Alert.AlertType.INFORMATION, msg, ButtonType.OK);
        a.setHeaderText("Info");
        // allow CSS targeting of info dialogs
        a.getDialogPane().getStyleClass().add("info-dialog");
        a.showAndWait();
    }

    @Override
    public void stop() throws Exception {
        super.stop();
        if (db != null) db.close();
    }

    public static void main(String[] args) {
        launch();
    }
}
