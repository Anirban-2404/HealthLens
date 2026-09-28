package com.healthlens;

import com.healthlens.database.DatabaseManager;
import com.healthlens.model.HealthRecord;
import com.healthlens.service.HealthAnalyzer;
import com.healthlens.service.WeatherService;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.stage.Stage;
import javafx.stage.Screen;

import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Main extends Application {

    private final DatabaseManager db = DatabaseManager.getInstance();
    private final HealthAnalyzer analyzer = new HealthAnalyzer();
    private final WeatherService weatherService = new WeatherService();
    private final ExecutorService executor = Executors.newFixedThreadPool(3);

    private Stage stage;
    private int currentUserId = 1;
    private Label scoreLabel;
    private Label weatherLabel;
    private Label summaryLabel;
    private ProgressBar scoreBar;
    private TableView<HealthRecord> table;
    private final ObservableList<HealthRecord> tableData = FXCollections.observableArrayList();

    private final String css = """
        .root { -fx-font-family: "Segoe UI"; -fx-background-color: #f4f7fb; }
        .title { -fx-font-size: 28px; -fx-font-weight: bold; -fx-text-fill: #17324d; }
        .subtitle { -fx-font-size: 14px; -fx-text-fill: #6b7c8f; }
        .card { -fx-background-color: white; -fx-background-radius: 16; -fx-padding: 18; 
                -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.10), 10, 0, 0, 3); }
        .nav { -fx-background-color: #17324d; }
        .nav-button { -fx-background-color: transparent; -fx-text-fill: white; -fx-font-size: 14px; 
                      -fx-alignment: CENTER_LEFT; -fx-padding: 12 18; }
        .nav-button:hover { -fx-background-color: #28516f; }
        .primary { -fx-background-color: #2f80ed; -fx-text-fill: white; -fx-font-weight: bold; 
                   -fx-background-radius: 8; -fx-padding: 10 18; }
        .danger { -fx-background-color: #e74c3c; -fx-text-fill: white; -fx-background-radius: 8; }
        .input { -fx-background-radius: 8; -fx-border-radius: 8; -fx-padding: 8; }
        .metric { -fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: #17324d; }
        .muted { -fx-text-fill: #6b7c8f; }
        """;

    @Override
    public void start(Stage primaryStage) {
        this.stage = primaryStage;

        db.initialize();

        stage.setMinWidth(1000);
        stage.setMinHeight(650);

        showLogin();

        stage.show();

        maximizeStage();
    }

    private void maximizeStage() {
        Platform.runLater(() -> {
            Screen screen = Screen.getPrimary();
            Rectangle2D bounds = screen.getVisualBounds();

            stage.setX(bounds.getMinX());
            stage.setY(bounds.getMinY());
            stage.setWidth(bounds.getWidth());
            stage.setHeight(bounds.getHeight());
        });
    }

    private Scene scene(Region root) {
        Scene scene = new Scene(root);

        var cssUrl = getClass().getResource("/style.css");
        if (cssUrl != null) {
            scene.getStylesheets().add(cssUrl.toExternalForm());
        }

        return scene;
    }

    private void showLogin() {
        VBox box = new VBox(15);
        box.setAlignment(Pos.CENTER);
        box.setPadding(new Insets(40));

        Label logo = new Label("HealthLens");
        logo.setFont(Font.font(34));
        logo.setTextFill(Color.web("#2f80ed"));

        Label sub = new Label("Interactive Health & Wellness Dashboard");
        sub.getStyleClass().add("subtitle");

        TextField email = new TextField("demo@healthlens.com");
        email.setPromptText("Email");
        email.setMaxWidth(330);
        email.getStyleClass().add("input");

        PasswordField password = new PasswordField();
        password.setText("1234");
        password.setPromptText("Password");
        password.setMaxWidth(330);
        password.getStyleClass().add("input");

        Button login = new Button("Sign In");
        login.getStyleClass().add("primary");
        login.setPrefWidth(330);

        Label hint = new Label("Demo login: demo@healthlens.com / 1234");
        hint.getStyleClass().add("muted");

        login.setOnAction(e -> {
            int id = db.login(email.getText(), password.getText());
            if (id != -1) {
                currentUserId = id;
                showDashboard();
            } else {
                new Alert(Alert.AlertType.ERROR, "Invalid login. Use the demo credentials.").showAndWait();
            }
        });

        box.getChildren().addAll(logo, sub, email, password, login, hint);

        StackPane root = new StackPane(box);
        root.setStyle("-fx-background-color: linear-gradient(to bottom right, #eaf4ff, #ffffff);");
        stage.setTitle("HealthLens");
        stage.setScene(scene(root));
        stage.show();
        maximizeStage();
    }

    private void showDashboard() {
        BorderPane root = new BorderPane();

        VBox nav = new VBox(8);
        nav.setPadding(new Insets(20, 10, 20, 10));
        nav.setPrefWidth(190);
        nav.prefWidthProperty().bind(stage.widthProperty().multiply(0.18));
        nav.getStyleClass().add("nav");

        Label brand = new Label("HEALTHLENS");
        brand.setTextFill(Color.WHITE);
        brand.setFont(Font.font(20));
        brand.setPadding(new Insets(0, 0, 20, 10));

        Button dashboard = navButton("🏠  Dashboard");
        Button records = navButton("📝  Records");
        Button analysis = navButton("📊  Analysis");
        Button logout = navButton("↩  Logout");

        dashboard.setOnAction(e -> showDashboard());
        records.setOnAction(e -> showRecords());
        analysis.setOnAction(e -> showAnalysis());
        logout.setOnAction(e -> showLogin());

        nav.getChildren().addAll(brand, dashboard, records, analysis, new Region(), logout);
        VBox.setVgrow(nav.getChildren().get(4), Priority.ALWAYS);

        root.setLeft(nav);
        root.setCenter(createDashboardContent());

        Scene sc = scene(root);
        stage.setScene(sc);
        stage.setTitle("HealthLens - Dashboard");
        maximizeStage();
    }

    private Button navButton(String text) {
        Button b = new Button(text);
        b.getStyleClass().add("nav-button");
        b.setMaxWidth(Double.MAX_VALUE);
        return b;
    }

    private VBox createDashboardContent() {
        VBox content = new VBox(20);
        content.setPadding(new Insets(28));

        HBox header = new HBox();
        VBox heading = new VBox(4);
        Label title = new Label("Good day! 👋");
        title.getStyleClass().add("title");
        Label sub = new Label("Your personal health overview");
        sub.getStyleClass().add("subtitle");
        heading.getChildren().addAll(title, sub);
        header.getChildren().add(heading);

        HBox cards = new HBox(18);
        cards.setFillHeight(true);

        VBox scoreCard = card("Overall Health", "Calculating...");
        scoreLabel = (Label) scoreCard.getChildren().get(1);
        scoreBar = (ProgressBar) scoreCard.getChildren().get(2);

        VBox sleepCard = card("Average Sleep", "0.0 h");
        VBox exerciseCard = card("Exercise", "0 min");
        VBox recordsCard = card("Records", "0");

        cards.getChildren().addAll(scoreCard, sleepCard, exerciseCard, recordsCard);
        for (javafx.scene.Node node : cards.getChildren()) HBox.setHgrow(node, Priority.ALWAYS);

        VBox weatherCard = new VBox(8);
        weatherCard.getStyleClass().add("card");
        Label wt = new Label("🌍 Environment");
        wt.setFont(Font.font(18));
        weatherLabel = new Label("Loading weather...");
        weatherLabel.getStyleClass().add("subtitle");
        weatherCard.getChildren().addAll(wt, weatherLabel);

        VBox summaryCard = new VBox(8);
        summaryCard.getStyleClass().add("card");
        Label st = new Label("💡 Health Summary");
        st.setFont(Font.font(18));
        summaryLabel = new Label("Loading analysis...");
        summaryLabel.setWrapText(true);
        summaryCard.getChildren().addAll(st, summaryLabel);

        content.getChildren().addAll(header, cards, weatherCard, summaryCard);

        loadDashboardAsync(sleepCard, exerciseCard, recordsCard);
        return content;
    }

    private VBox card(String title, String value) {
        VBox box = new VBox(8);
        box.getStyleClass().add("card");
        Label t = new Label(title);
        t.getStyleClass().add("muted");
        Label v = new Label(value);
        v.getStyleClass().add("metric");
        ProgressBar pb = new ProgressBar(0);
        pb.setMaxWidth(Double.MAX_VALUE);
        box.getChildren().addAll(t, v, pb);
        return box;
    }

    private void loadDashboardAsync(VBox sleepCard, VBox exerciseCard, VBox recordsCard) {
        executor.submit(() -> {
            List<HealthRecord> records = db.getRecords(currentUserId);
            double avgScore = analyzer.averageScore(records);
            double avgSleep = analyzer.averageSleep(records);
            int totalExercise = analyzer.totalExercise(records);
            String summary = analyzer.summary(records);

            Platform.runLater(() -> {
                scoreLabel.setText(String.format("%.0f%%", avgScore));
                scoreBar.setProgress(avgScore / 100.0);
                ((Label) sleepCard.getChildren().get(1)).setText(String.format("%.1f h", avgSleep));
                ((Label) exerciseCard.getChildren().get(1)).setText(totalExercise + " min");
                ((Label) recordsCard.getChildren().get(1)).setText(String.valueOf(records.size()));
                summaryLabel.setText(summary);
            });
        });

        executor.submit(() -> {
            try {
                String weather = weatherService.fetchWeather();
                Platform.runLater(() -> weatherLabel.setText(weather));
            } catch (Exception ex) {
                Platform.runLater(() -> weatherLabel.setText("Weather service unavailable."));
            }
        });
    }

    private void showRecords() {
        BorderPane root = baseLayout("📝 Health Records");

        VBox center = new VBox(15);
        center.setPadding(new Insets(25));

        Button add = new Button("+ Add Health Record");
        add.getStyleClass().add("primary");
        add.setOnAction(e -> showRecordForm(null));

        table = new TableView<>();
        table.setItems(tableData);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);

        TableColumn<HealthRecord, String> date = new TableColumn<>("Date");
        date.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(c.getValue().getDate().toString()));

        TableColumn<HealthRecord, String> sleep = new TableColumn<>("Sleep");
        sleep.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(c.getValue().getSleepHours() + " h"));

        TableColumn<HealthRecord, String> water = new TableColumn<>("Water");
        water.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(c.getValue().getWaterGlasses() + " L"));

        TableColumn<HealthRecord, String> exercise = new TableColumn<>("Exercise");
        exercise.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(c.getValue().getExerciseMinutes() + " min"));

        TableColumn<HealthRecord, String> mood = new TableColumn<>("Mood");
        mood.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(c.getValue().getMood()));

        TableColumn<HealthRecord, String> stress = new TableColumn<>("Stress");
        stress.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(c.getValue().getStress()));

        TableColumn<HealthRecord, String> score = new TableColumn<>("Score");
        score.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(
                String.format("%.0f%%", c.getValue().calculateScore())));

        table.getColumns().addAll(date, sleep, water, exercise, mood, stress, score);

        HBox actions = new HBox(10);
        Button edit = new Button("Edit Selected");
        Button delete = new Button("Delete Selected");
        delete.getStyleClass().add("danger");

        edit.setOnAction(e -> {
            HealthRecord selected = table.getSelectionModel().getSelectedItem();
            if (selected != null) showRecordForm(selected);
        });

        delete.setOnAction(e -> {
            HealthRecord selected = table.getSelectionModel().getSelectedItem();
            if (selected != null) {
                db.deleteRecord(selected.getId(), currentUserId);
                loadRecords();
            }
        });

        actions.getChildren().addAll(edit, delete);
        center.getChildren().addAll(add, table, actions);
        VBox.setVgrow(table, Priority.ALWAYS);

        root.setCenter(center);
        stage.setScene(scene(root));
        maximizeStage();
        loadRecords();
    }

    private BorderPane baseLayout(String titleText) {
        BorderPane root = new BorderPane();

        VBox nav = new VBox(8);
        nav.setPadding(new Insets(20, 10, 20, 10));
        nav.setPrefWidth(190);
        nav.prefWidthProperty().bind(stage.widthProperty().multiply(0.18));
        nav.getStyleClass().add("nav");

        Label brand = new Label("HEALTHLENS");
        brand.setTextFill(Color.WHITE);
        brand.setFont(Font.font(20));
        brand.setPadding(new Insets(0, 0, 20, 10));

        Button dashboard = navButton("🏠  Dashboard");
        Button records = navButton("📝  Records");
        Button analysis = navButton("📊  Analysis");
        Button logout = navButton("↩  Logout");

        dashboard.setOnAction(e -> showDashboard());
        records.setOnAction(e -> showRecords());
        analysis.setOnAction(e -> showAnalysis());
        logout.setOnAction(e -> showLogin());

        nav.getChildren().addAll(brand, dashboard, records, analysis, new Region(), logout);
        VBox.setVgrow(nav.getChildren().get(4), Priority.ALWAYS);

        root.setLeft(nav);

        Label title = new Label(titleText);
        title.getStyleClass().add("title");
        HBox top = new HBox(title);
        top.setPadding(new Insets(25));
        root.setTop(top);

        return root;
    }

    private void loadRecords() {
        executor.submit(() -> {
            List<HealthRecord> records = db.getRecords(currentUserId);
            Platform.runLater(() -> tableData.setAll(records));
        });
    }

    private void showRecordForm(HealthRecord existing) {
        VBox form = new VBox(14);
        form.setPadding(new Insets(35));
        form.setMaxWidth(600);

        Label title = new Label(existing == null ? "Add Health Record" : "Edit Health Record");
        title.getStyleClass().add("title");

        DatePicker date = new DatePicker(existing == null ? LocalDate.now() : existing.getDate());

        TextField sleep = new TextField(existing == null ? "" : String.valueOf(existing.getSleepHours()));
        TextField water = new TextField(existing == null ? "" : String.valueOf(existing.getWaterGlasses()));
        TextField exercise = new TextField(existing == null ? "" : String.valueOf(existing.getExerciseMinutes()));

        ComboBox<String> mood = new ComboBox<>(FXCollections.observableArrayList("Excellent", "Good", "Okay", "Bad"));
        mood.setValue(existing == null ? "Good" : existing.getMood());

        ComboBox<String> stress = new ComboBox<>(FXCollections.observableArrayList("Low", "Medium", "High"));
        stress.setValue(existing == null ? "Low" : existing.getStress());

        sleep.setPromptText("Sleep hours, e.g. 7.5");
        water.setPromptText("Glasses, e.g. 2.5");
        exercise.setPromptText("Minutes, e.g. 30");

        Button save = new Button(existing == null ? "Save Record" : "Update Record");
        save.getStyleClass().add("primary");
        Button back = new Button("Cancel");

        save.setOnAction(e -> {
            try {
                HealthRecord record = new HealthRecord(
                        existing == null ? 0 : existing.getId(),
                        currentUserId,
                        date.getValue(),
                        Double.parseDouble(sleep.getText()),
                        Double.parseDouble(water.getText()),
                        Integer.parseInt(exercise.getText()),
                        mood.getValue(),
                        stress.getValue()
                );

                executor.submit(() -> {
                    if (existing == null) db.insertRecord(record);
                    else db.updateRecord(record);
                    Platform.runLater(this::showRecords);
                });
            } catch (Exception ex) {
                new Alert(Alert.AlertType.ERROR, "Please enter valid values.").showAndWait();
            }
        });

        back.setOnAction(e -> showRecords());

        form.getChildren().addAll(
                title,
                new Label("Date"), date,
                new Label("Sleep (hours)"), sleep,
                new Label("Water (litres)"), water,
                new Label("Exercise (minutes)"), exercise,
                new Label("Mood"), mood,
                new Label("Stress"), stress,
                new HBox(10, save, back)
        );

        StackPane root = new StackPane(form);
        root.setPadding(new Insets(20));
        stage.setScene(scene(root));
    }

    private void showAnalysis() {
        BorderPane root = baseLayout("📊 Health Analysis");
        VBox content = new VBox(20);
        content.setPadding(new Insets(20, 30, 30, 30));

        CategoryAxis x = new CategoryAxis();
        NumberAxis y = new NumberAxis(0, 100, 20);
        x.setLabel("Date");
        y.setLabel("Health Score");

        BarChart<String, Number> chart = new BarChart<>(x, y);
        chart.setTitle("Health Score by Day");
        chart.setAnimated(false);

        Label summary = new Label("Loading analysis...");
        summary.getStyleClass().add("subtitle");

        content.getChildren().addAll(chart, summary);
        VBox.setVgrow(chart, Priority.ALWAYS);
        root.setCenter(content);

        executor.submit(() -> {
            List<HealthRecord> records = db.getRecords(currentUserId);
            Platform.runLater(() -> {
                XYChart.Series<String, Number> series = new XYChart.Series<>();
                series.setName("Health Score");
                records.stream().limit(10).forEach(r ->
                        series.getData().add(new XYChart.Data<>(r.getDate().toString(), r.calculateScore()))
                );
                chart.getData().add(series);
                summary.setText(String.format(
                        "Average score: %.0f%%   |   Average sleep: %.1f hours   |   Total exercise: %d minutes",
                        analyzer.averageScore(records),
                        analyzer.averageSleep(records),
                        analyzer.totalExercise(records)
                ));
            });
        });

        stage.setScene(scene(root));
        maximizeStage();
    }

    @Override
    public void stop() {
        executor.shutdownNow();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
