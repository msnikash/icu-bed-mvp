package com.icu;

import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import javafx.geometry.Pos;
import java.text.NumberFormat;
import java.util.Locale;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;

import java.sql.SQLException;
import java.util.List;
import java.net.URL;

public class Main extends Application {

    private DBManager db;

    private ObservableList<Patient> waitingListData = FXCollections.observableArrayList();
    private ObservableList<AllocatedEntry> allocatedListData = FXCollections.observableArrayList();
    private ObservableList<String> bedsData = FXCollections.observableArrayList();

    @Override
    public void start(Stage stage) throws Exception {
        try {
            db = new DBManager();
        } catch (SQLException e) {
            showError("DB Connection failed: " + e.getMessage());
            return;
        }

        TextField nameField = new TextField();
        nameField.setPromptText("Name");

        TextField ageField = new TextField();
        ageField.setPromptText("Age");

        ComboBox<String> prioCombo = new ComboBox<>(FXCollections.observableArrayList("High", "Medium", "Low"));
        prioCombo.setValue("Medium");
        
        DatePicker startDate = new DatePicker(LocalDate.now());
        DatePicker endDate = new DatePicker(LocalDate.now());

        Button addBtn = new Button("Admit Patient");
        Button allocBtn = new Button("Allocate Next");

        ListView<Patient> waitingView = new ListView<>(waitingListData);
        ListView<AllocatedEntry> allocatedView = new ListView<>(allocatedListData);
        ListView<String> bedsView = new ListView<>(bedsData);

        waitingView.setId("waiting-view");
        allocatedView.setId("allocated-view");

        allocatedView.setCellFactory(lv -> new ListCell<AllocatedEntry>() {
            @Override
            protected void updateItem(AllocatedEntry e, boolean empty) {
                super.updateItem(e, empty);
                if (empty || e == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    Label info = new Label(e.getName() + " (P=" + e.getPriority() + " D=" + (e.getDays()==null?"?":e.getDays()) + ") -> " + e.getBedNumber());
                    Label idLabel = new Label("bid=" + e.getBedId());
                    idLabel.setStyle("-fx-font-weight: bold;");
                    Region spacer = new Region();
                    HBox.setHgrow(spacer, javafx.scene.layout.Priority.ALWAYS);
                    HBox h = new HBox(info, spacer, idLabel);
                    h.setAlignment(Pos.CENTER_LEFT);
                    setGraphic(h);
                }
            }
        });
        bedsView.setId("beds-view");

        waitingView.setCellFactory(lv -> new ListCell<Patient>() {
            @Override
            protected void updateItem(Patient p, boolean empty) {
                super.updateItem(p, empty);
                if (empty || p == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    Label info = new Label(p.getName() + " (Age:" + p.getAge() + " P=" + p.getConditionPriority() + " D=" + (p.getAllocatedDays()==null?"?":p.getAllocatedDays()) + ")");
                    Label idLabel = new Label("id=" + p.getId());
                    idLabel.setStyle("-fx-font-weight: bold;");
                    Region spacer = new Region();
                    HBox.setHgrow(spacer, javafx.scene.layout.Priority.ALWAYS);
                    HBox h = new HBox(info, spacer, idLabel);
                    h.setAlignment(Pos.CENTER_LEFT);
                    setGraphic(h);
                }
            }
        });

        addBtn.setOnAction(ev -> {
            String name = nameField.getText().trim();
            String ageS = ageField.getText().trim();
            String priority = prioCombo.getValue();

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

            LocalDate s = startDate.getValue();
            LocalDate e = endDate.getValue();
            if (s == null || e == null) {
                showError("Please select start and end dates.");
                return;
            }
            if (e.isBefore(s)) {
                showError("End date must be on or after start date.");
                return;
            }
            long daysLong = ChronoUnit.DAYS.between(s, e) + 1;
            int days = (int) Math.max(1, Math.min(daysLong, Integer.MAX_VALUE));

            Patient p = new Patient(name, age, priority, days, s, e);
            try {
                db.addPatient(p);
                refreshAll();
                nameField.clear();
                ageField.clear();
                startDate.setValue(LocalDate.now());
                endDate.setValue(LocalDate.now());
            } catch (SQLException ex) {
                showError("Failed to admit patient: " + ex.getMessage());
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

        Button freeSelectedBtn = new Button("Discharge Patient");
        freeSelectedBtn.setOnAction(ev -> {
            AllocatedEntry sel = allocatedView.getSelectionModel().getSelectedItem();
            if (sel == null) {
                showError("Select an allocated entry to discharge the patient.");
                return;
            }

            Stage receipt = new Stage();
            receipt.setTitle("Discharge Receipt - " + sel.getName());

            VBox root = new VBox(12);
            root.setPadding(new Insets(16));
            root.setStyle("-fx-background-color: white;");

            VBox header = new VBox(2);
            Label hospital = new Label("City Bed & Care Hospital");
            hospital.setStyle("-fx-font-size:18px; -fx-font-weight:bold;");
            Label address = new Label("123 Health St, Wellness City - Phone: (555) 123-4567");
            address.setStyle("-fx-font-size:11px; -fx-text-fill: #444444;");
            header.getChildren().addAll(hospital, address);

            Separator sep = new Separator();

            HBox meta = new HBox(10);
            meta.setAlignment(Pos.CENTER_LEFT);
            Label receiptId = new Label("Receipt #: " + java.util.UUID.randomUUID().toString().substring(0,8).toUpperCase());
            receiptId.setStyle("-fx-font-weight:bold;");
            Label dateLabel = new Label("Date: " + java.time.LocalDate.now().toString());
            meta.getChildren().addAll(receiptId, new Region(), dateLabel);
            HBox.setHgrow(meta.getChildren().get(1), javafx.scene.layout.Priority.ALWAYS);

            GridPane grid = new GridPane();
            grid.setHgap(12);
            grid.setVgap(8);

            grid.add(new Label("Patient:"), 0, 0);
            grid.add(new Label(sel.getName()), 1, 0);

            grid.add(new Label("Age:"), 0, 1);
            grid.add(new Label(String.valueOf(sel.getAge() == null ? "-" : sel.getAge())), 1, 1);

            grid.add(new Label("Priority:"), 0, 2);
            grid.add(new Label(sel.getPriority() == null ? "Medium" : sel.getPriority()), 1, 2);

            grid.add(new Label("Bed:"), 0, 3);
            grid.add(new Label(sel.getBedNumber() + " (id=" + sel.getBedId() + ")"), 1, 3);

            grid.add(new Label("Allocated On:"), 0, 4);
            grid.add(new Label(sel.getAllocatedOn() == null ? "N/A" : sel.getAllocatedOn().toString()), 1, 4);

            grid.add(new Label("Leaving Date:"), 0, 5);
            grid.add(new Label(sel.getEndDate() == null ? "N/A" : sel.getEndDate().toString()), 1, 5);

            int billDays = 0;
            if (sel.getAllocatedOn() != null && sel.getEndDate() != null) {
                billDays = (int) (java.time.temporal.ChronoUnit.DAYS.between(sel.getAllocatedOn(), sel.getEndDate()) + 1);
            } else if (sel.getDays() != null) {
                billDays = sel.getDays();
            }

            grid.add(new Label("Days charged:"), 0, 6);
            grid.add(new Label(String.valueOf(billDays)), 1, 6);

            int ratePerDay;
            switch ((sel.getPriority() == null) ? "Medium" : sel.getPriority()) {
                case "High": ratePerDay = 1000; break;
                case "Low": ratePerDay = 300; break;
                default: ratePerDay = 600; break;
            }
            int total = ratePerDay * billDays;

            grid.add(new Label("Rate/day:"), 0, 7);
            NumberFormat nf = NumberFormat.getCurrencyInstance(new Locale("en", "IN"));
            nf.setMaximumFractionDigits(0);
            grid.add(new Label(nf.format(ratePerDay)), 1, 7);

            HBox totalBox = new HBox(8);
            totalBox.setAlignment(Pos.CENTER_RIGHT);
            Label totalLabel = new Label("TOTAL: ");
            totalLabel.setStyle("-fx-font-size:16px; -fx-font-weight:bold;");
            Label totalAmount = new Label(nf.format(total));
            totalAmount.setStyle("-fx-font-size:16px; -fx-font-weight:bold; -fx-text-fill:#1a73e8;");
            totalBox.getChildren().addAll(totalLabel, totalAmount);

            HBox sign = new HBox(40);
            sign.setPadding(new Insets(12,0,0,0));
            Label sigLabel = new Label("Authorized Signature");
            sigLabel.setStyle("-fx-text-fill:#666666;");
            Region sigSpacer = new Region();
            HBox.setHgrow(sigSpacer, javafx.scene.layout.Priority.ALWAYS);
            sign.getChildren().addAll(new Label("________________________"), sigSpacer, sigLabel);

            HBox actions = new HBox(10);
            actions.setAlignment(Pos.CENTER_RIGHT);
            Button printBtn = new Button("Print");
            Button confirm = new Button("Confirm Release");
            Button cancel = new Button("Cancel");
            actions.getChildren().addAll(printBtn, confirm, cancel);

            root.getChildren().addAll(header, sep, meta, grid, totalBox, sign, actions);

            Scene rs = new Scene(root, 520, 420);
            rs.getStylesheets().add(getClass().getResource("/styles.css") != null ? getClass().getResource("/styles.css").toExternalForm() : "");
            receipt.setScene(rs);
            receipt.initOwner(stage);
            receipt.show();

            cancel.setOnAction(a -> receipt.close());
            printBtn.setOnAction(a -> {
                Alert a2 = new Alert(Alert.AlertType.INFORMATION, "Printing not implemented in this demo.", ButtonType.OK);
                a2.setHeaderText("Print");
                a2.showAndWait();
            });
            confirm.setOnAction(a -> {
                try {
                    String msg = db.freeBedById(sel.getBedId());
                    receipt.close();
                    refreshAll();
                    showInfo("Released: " + msg + "\nBill: " + nf.format(total));
                } catch (SQLException ex) {
                    showError("Discharge failed: " + ex.getMessage());
                }
            });
        });

        Label addLabel = new Label("Admit Patient");
        addLabel.getStyleClass().add("section-title");
        Label prioLabel = new Label("Priority");
        prioLabel.getStyleClass().add("section-subtitle");

        VBox inputBox = new VBox(12,
            addLabel,
            nameField,
            ageField,
            new Label("Start Date"),
            startDate,
            new Label("End Date"),
            endDate,
            prioLabel,
            prioCombo,
            addBtn
        );
        inputBox.setPadding(new Insets(10));
        inputBox.setPrefWidth(220);

        Label waitingLabel = new Label("Waiting (by priority)");
        waitingLabel.getStyleClass().add("section-title");
        Label allocatedLabel = new Label("Allocated");
        allocatedLabel.getStyleClass().add("section-title");
        Label bedsLabel = new Label("All Free Beds");
        bedsLabel.getStyleClass().add("section-title");

        VBox waitingSection = new VBox(8, waitingView, allocBtn);
        waitingSection.setPadding(new Insets(8,0,0,0));
        VBox listsBox = new VBox(12,
            waitingLabel,
            waitingSection,
            allocatedLabel,
            allocatedView,
            freeSelectedBtn,
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
        URL cssUrl = getClass().getResource("/styles.css");
        if (cssUrl != null) {
            scene.getStylesheets().add(cssUrl.toExternalForm());
        }
        stage.setScene(scene);
        stage.setTitle("Bed Allocation - MVP");
        stage.show();

        refreshAll();
    }

    private void refreshAll() {
        try {
            List<Patient> waiting = db.getWaitingPatients();
            waitingListData.clear();
            for (Patient p : waiting) {
                waitingListData.add(p);
            }

            List<AllocatedEntry> allocated = db.getAllocatedInfo();
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
        a.getDialogPane().getStyleClass().add("error-dialog");
        a.showAndWait();
    }

    private void showInfo(String msg) {
        Alert a = new Alert(Alert.AlertType.INFORMATION, msg, ButtonType.OK);
        a.setHeaderText("Info");
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
