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
import javafx.scene.Scene;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.nio.file.Files;
import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

public class Main extends Application {
    private final DatabaseManager db = DatabaseManager.getInstance();
    private final HealthAnalyzer analyzer = new HealthAnalyzer();
    private final WeatherService weatherService = new WeatherService();
    private final ExecutorService executor = Executors.newFixedThreadPool(3);
    private Stage stage;
    private int currentUserId = 1;
    private Label scoreLabel, weatherLabel, summaryLabel, streakLabel, riskLabel, coachLabel, challengeLabel, achievementsLabel, weeklyLabel;
    private ProgressBar scoreBar;
    private TableView<HealthRecord> table;
    private final ObservableList<HealthRecord> tableData = FXCollections.observableArrayList();
    private List<HealthRecord> loadedRecords = List.of();

    @Override
    public void start(Stage primaryStage) {
        this.stage = primaryStage;

        db.initialize();

        stage.setTitle("HealthLens");
        stage.setMinWidth(1000);
        stage.setMinHeight(650);

        stage.show();
        keepMaximized();

        showLogin();
    }

    private void keepMaximized() {
        Platform.runLater(() -> {
            javafx.geometry.Rectangle2D bounds =
                    javafx.stage.Screen.getPrimary().getVisualBounds();

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
        box.setMaxWidth(420);

        Label logo = new Label("HealthLens"); logo.setFont(Font.font(36)); logo.setTextFill(Color.web("#2f80ed"));
        Label sub = new Label("Interactive Health & Wellness Dashboard"); sub.getStyleClass().add("subtitle");
        TextField email = new TextField("anirban@gmail.com"); email.setPromptText("Email"); email.setMaxWidth(330); email.getStyleClass().add("input");
        PasswordField password = new PasswordField(); password.setText("2307023"); password.setPromptText("Password"); password.setMaxWidth(330); password.getStyleClass().add("input");
        Button login = new Button("Sign In"); login.getStyleClass().add("primary"); login.setPrefWidth(330);
        Label hint = new Label("Login: anirban@gmail.com / 2307023"); hint.getStyleClass().add("muted");

        login.setOnAction(e -> {
            int id = db.login(email.getText(), password.getText());
            if (id != -1) { currentUserId = id; showDashboard(); }
            else new Alert(Alert.AlertType.ERROR, "Invalid email or password.").showAndWait();
        });
        password.setOnAction(e -> login.fire());
        box.getChildren().addAll(logo, sub, email, password, login, hint);
        StackPane root = new StackPane(box);
        root.setStyle("-fx-background-color: linear-gradient(to bottom right, #eaf4ff, #ffffff);");
        stage.setScene(scene(root));
        stage.setTitle("HealthLens - Sign In");
        keepMaximized();
    }

    private void showDashboard() {
        BorderPane root = baseLayout("🏠 Dashboard");
        root.setCenter(createDashboardContent());
        stage.setScene(scene(root));
        stage.setTitle("HealthLens - Dashboard");
        keepMaximized();
    }

    private Button navButton(String text) {
        Button b = new Button(text); b.getStyleClass().add("nav-button"); b.setMaxWidth(Double.MAX_VALUE); return b;
    }

    private BorderPane baseLayout(String titleText) {
        BorderPane root = new BorderPane();
        VBox nav = new VBox(8); nav.setPadding(new Insets(20, 10, 20, 10)); nav.prefWidthProperty().bind(stage.widthProperty().multiply(0.18)); nav.getStyleClass().add("nav");
        Label brand = new Label("HEALTHLENS"); brand.setTextFill(Color.WHITE); brand.setFont(Font.font(20)); brand.setPadding(new Insets(0,0,20,10));
        Button dashboard = navButton("🏠  Dashboard"); Button records = navButton("📝  Records"); Button analysis = navButton("📊  Analysis");Button logout = navButton("↩ Logout");
        logout.setOnAction(e -> showLogin());
        dashboard.setOnAction(e -> showDashboard()); records.setOnAction(e -> showRecords()); analysis.setOnAction(e -> showAnalysis());
        nav.getChildren().addAll(brand, dashboard, records, analysis, new Region(), logout);
        root.setLeft(nav);
        Label title = new Label(titleText); title.getStyleClass().add("title"); HBox top = new HBox(title); top.setPadding(new Insets(25)); root.setTop(top);
        return root;
    }

    private VBox createDashboardContent() {
        VBox content = new VBox(18); content.setPadding(new Insets(24));
        Label welcome = new Label("Good day! 👋  Your health at a glance"); welcome.getStyleClass().add("title");
        Label sub = new Label("HealthLens combines your records, trends, goals and live environment data."); sub.getStyleClass().add("subtitle");

        HBox cards = new HBox(14); cards.setFillHeight(true);
        VBox scoreCard = card("Overall Health", "Calculating..."); scoreLabel=(Label)scoreCard.getChildren().get(1); scoreBar=(ProgressBar)scoreCard.getChildren().get(2);
        VBox streakCard = card("🔥 Streak", "0 days"); streakLabel=(Label)streakCard.getChildren().get(1);
        VBox recordsCard = card("Records", "0");
        Label recordsLabel = (Label) recordsCard.getChildren().get(1);
        VBox riskCard = card("Health Status", "Checking..."); riskLabel=(Label)riskCard.getChildren().get(1);
        cards.getChildren().addAll(scoreCard,streakCard,recordsCard,riskCard);
        ((ProgressBar) streakCard.getChildren().get(2)).setVisible(false);
        ((ProgressBar) recordsCard.getChildren().get(2)).setVisible(false);
        ((ProgressBar) riskCard.getChildren().get(2)).setVisible(false);
        for (javafx.scene.Node n: cards.getChildren()) HBox.setHgrow(n,Priority.ALWAYS);

        HBox lower = new HBox(14); lower.setFillHeight(true);
        VBox goals = panel("🎯 Daily Goals");
        Label goalText = new Label("Sleep 7h+  •  Water 2.0 L  •  Exercise 30 min"); goalText.setWrapText(true);
        ProgressBar goalBar = new ProgressBar(); goalBar.setMaxWidth(Double.MAX_VALUE);
        goals.getChildren().addAll(goalText,goalBar);

        VBox coach = panel("💡 Health Coach"); coachLabel=new Label("Loading personalized advice..."); coachLabel.setWrapText(true); coach.getChildren().add(coachLabel);
        VBox achievement = panel("🏆 Achievements"); achievementsLabel=new Label("Loading..."); achievementsLabel.setWrapText(true); achievement.getChildren().add(achievementsLabel);
        lower.getChildren().addAll(goals,coach,achievement); for (javafx.scene.Node n: lower.getChildren()) HBox.setHgrow(n,Priority.ALWAYS);

        HBox lower2 = new HBox(14); lower2.setFillHeight(true);
        VBox environment = panel("🌍 Weather + Wellness"); weatherLabel=new Label("Loading weather..."); weatherLabel.setWrapText(true); environment.getChildren().add(weatherLabel);
        VBox weekly = panel("📈 Today vs 7-Day Average"); weeklyLabel=new Label("Loading comparison..."); weeklyLabel.setWrapText(true); weekly.getChildren().add(weeklyLabel);
        VBox challenge = panel("⚡ Personalized Challenge"); challengeLabel=new Label("Loading challenge..."); challengeLabel.setWrapText(true); challenge.getChildren().add(challengeLabel);
        lower2.getChildren().addAll(environment,weekly,challenge); for (javafx.scene.Node n: lower2.getChildren()) HBox.setHgrow(n,Priority.ALWAYS);

        VBox summary = panel("🧠 Smart Summary"); summaryLabel=new Label("Loading analysis..."); summaryLabel.setWrapText(true); summary.getChildren().add(summaryLabel);
        content.getChildren().addAll(welcome,sub,cards,lower,lower2,summary); VBox.setVgrow(lower,Priority.ALWAYS); VBox.setVgrow(lower2,Priority.ALWAYS);

        executor.submit(() -> {
            List<HealthRecord> records=db.getRecords(currentUserId); loadedRecords=records;
            double avg=analyzer.averageScore(records);
            HealthRecord latest=records.stream().findFirst().orElse(null); double todayScore=latest==null?0:latest.calculateScore();
            int goalDone=latest==null?0:((latest.getSleepHours()>=7?1:0)+(latest.getWaterGlasses()>=2?1:0)+(latest.getExerciseMinutes()>=30?1:0));
            String healthStatus = analyzer.riskLevel(records);
            Platform.runLater(() -> {
                scoreLabel.setText(String.format("%.0f%%",avg)); scoreBar.setProgress(avg/100);
                streakLabel.setText(analyzer.streak(records)+" days");
                riskLabel.setText(healthStatus);
                recordsLabel.setText(String.valueOf(records.size()));
                goalBar.setProgress(goalDone/3.0); coachLabel.setText(analyzer.coachAdvice(records)); achievementsLabel.setText(analyzer.achievements(records)); summaryLabel.setText(analyzer.summary(records));
                weeklyLabel.setText(latest==null?"Add a record to compare today with the 7-day average.":String.format("Latest: %.0f%%\n7-day average: %.0f%%\nDifference: %+.0f points",todayScore,analyzer.sevenDayAverage(records),todayScore-analyzer.sevenDayAverage(records)));
                challengeLabel.setText(analyzer.challenge(records));
            });
        });
        executor.submit(() -> { try { String weather=weatherService.fetchWeather(); Platform.runLater(() -> weatherLabel.setText(weather+"\nTip: Stay hydrated and adjust outdoor activity to conditions.")); } catch(Exception ex){ Platform.runLater(() -> weatherLabel.setText("Weather service unavailable.\nTip: Keep your normal hydration and activity routine.")); } });
        return content;
    }

    private VBox card(String title,String value){ VBox b=panel(title); Label v=new Label(value); v.getStyleClass().add("metric"); ProgressBar p=new ProgressBar(0); p.setMaxWidth(Double.MAX_VALUE); b.getChildren().addAll(v,p); return b; }
    private VBox panel(String title){ VBox b=new VBox(8); b.getStyleClass().add("card"); Label t=new Label(title); t.setFont(Font.font(18)); b.getChildren().add(t); return b; }

    private void showRecords() {
        BorderPane root=baseLayout("📝 Health Records"); VBox center=new VBox(12); center.setPadding(new Insets(20));
        HBox toolbar=new HBox(10); toolbar.setAlignment(Pos.CENTER_LEFT);
        Button add=new Button("+ Add Health Record"); add.getStyleClass().add("primary"); add.setOnAction(e->showRecordForm(null));
        TextField search=new TextField(); search.setPromptText("Search date, mood or stress..."); search.setPrefWidth(300); search.getStyleClass().add("input");
        Button report=new Button("Export Health Report"); report.setOnAction(e->exportReport()); toolbar.getChildren().addAll(add,search,report);
        table=new TableView<>(); table.setItems(tableData); table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        TableColumn<HealthRecord,String> date=col("Date",r->r.getDate().toString());
        TableColumn<HealthRecord,String> sleep=col("Sleep",r->r.getSleepHours()+" h");
        TableColumn<HealthRecord,String> water=col("Water",r->String.format("%.1f L",r.getWaterGlasses()));
        TableColumn<HealthRecord,String> exercise=col("Exercise",r->r.getExerciseMinutes()+" min");
        TableColumn<HealthRecord,String> mood=col("Mood",HealthRecord::getMood); TableColumn<HealthRecord,String> stress=col("Stress",HealthRecord::getStress);
        TableColumn<HealthRecord,String> score=col("Score",r->String.format("%.0f%%",r.calculateScore())); table.getColumns().addAll(date,sleep,water,exercise,mood,stress,score);
        search.textProperty().addListener((obs,oldVal,newVal)->filterTable(newVal));
        HBox actions=new HBox(10); Button edit=new Button("Edit Selected"); Button delete=new Button("Delete Selected"); delete.getStyleClass().add("danger");
        edit.setOnAction(e->{HealthRecord s=table.getSelectionModel().getSelectedItem(); if(s!=null)showRecordForm(s);});
        delete.setOnAction(e->{HealthRecord s=table.getSelectionModel().getSelectedItem(); if(s!=null){db.deleteRecord(s.getId(),currentUserId);loadRecords();}});
        actions.getChildren().addAll(edit,delete); center.getChildren().addAll(toolbar,table,actions); VBox.setVgrow(table,Priority.ALWAYS); root.setCenter(center);
        stage.setScene(scene(root)); stage.setTitle("HealthLens - Records"); keepMaximized(); loadRecords();
    }

    private TableColumn<HealthRecord,String> col(String name, java.util.function.Function<HealthRecord,String> fn){ TableColumn<HealthRecord,String> c=new TableColumn<>(name); c.setCellValueFactory(x->new javafx.beans.property.SimpleStringProperty(fn.apply(x.getValue()))); return c; }
    private void filterTable(String q){ String s=q==null?"":q.toLowerCase(); tableData.setAll(loadedRecords.stream().filter(r->r.getDate().toString().contains(s)||r.getMood().toLowerCase().contains(s)||r.getStress().toLowerCase().contains(s)).collect(Collectors.toList())); }
    private void loadRecords(){ executor.submit(()->{loadedRecords=db.getRecords(currentUserId); Platform.runLater(()->tableData.setAll(loadedRecords));}); }

    private void showRecordForm(HealthRecord existing){
        VBox form=new VBox(14); form.setPadding(new Insets(30)); form.setMaxWidth(620); Label title=new Label(existing==null?"Add Health Record":"Edit Health Record"); title.getStyleClass().add("title");
        DatePicker date=new DatePicker(existing==null?LocalDate.now():existing.getDate()); TextField sleep=new TextField(existing==null?"":String.valueOf(existing.getSleepHours())); TextField water=new TextField(existing==null?"":String.valueOf(existing.getWaterGlasses())); TextField exercise=new TextField(existing==null?"":String.valueOf(existing.getExerciseMinutes()));
        ComboBox<String> mood=new ComboBox<>(FXCollections.observableArrayList("Excellent","Good","Okay","Bad")); mood.setValue(existing==null?"Good":existing.getMood()); ComboBox<String> stress=new ComboBox<>(FXCollections.observableArrayList("Low","Medium","High")); stress.setValue(existing==null?"Low":existing.getStress());
        sleep.setPromptText("e.g. 7.5"); water.setPromptText("Litres, e.g. 2.5"); exercise.setPromptText("Minutes, e.g. 30");
        Button save=new Button(existing==null?"Save Record":"Update Record"); save.getStyleClass().add("primary"); Button back=new Button("Cancel");
        save.setOnAction(e->{try{
            if(date.getValue()==null)throw new IllegalArgumentException(); double sl=Double.parseDouble(sleep.getText()); double wa=Double.parseDouble(water.getText()); int ex=Integer.parseInt(exercise.getText()); if(sl<0||wa<0||ex<0)throw new IllegalArgumentException();
            HealthRecord r=new HealthRecord(existing==null?0:existing.getId(),currentUserId,date.getValue(),sl,wa,ex,mood.getValue(),stress.getValue());
            executor.submit(()->{if(existing==null)db.insertRecord(r);else db.updateRecord(r);Platform.runLater(this::showRecords);});
        }catch(Exception ex){new Alert(Alert.AlertType.ERROR,"Please enter valid positive values.").showAndWait();}});
        back.setOnAction(e->showRecords()); form.getChildren().addAll(title,new Label("Date"),date,new Label("Sleep (hours)"),sleep,new Label("Water (litres)"),water,new Label("Exercise (minutes)"),exercise,new Label("Mood"),mood,new Label("Stress"),stress,new HBox(10,save,back));
        StackPane root=new StackPane(form); root.setPadding(new Insets(20)); stage.setScene(scene(root)); stage.setTitle("HealthLens - Health Record"); keepMaximized();
    }

    private void showAnalysis(){
        BorderPane root=baseLayout("📊 Health Analysis"); VBox content=new VBox(14); content.setPadding(new Insets(15,25,25,25));
        HBox stats=new HBox(12); Label weekly=new Label("Weekly summary: loading..."); weekly.getStyleClass().add("subtitle"); Label trend=new Label("Trend: loading..."); trend.getStyleClass().add("subtitle"); stats.getChildren().addAll(weekly,trend);
        CategoryAxis x=new CategoryAxis(); NumberAxis y=new NumberAxis(0,100,20); x.setLabel("Date"); y.setLabel("Health Score"); BarChart<String,Number> chart=new BarChart<>(x,y); chart.setTitle("Health Score Trend"); chart.setAnimated(false);
        Label insight=new Label("Loading insights..."); insight.setWrapText(true); content.getChildren().addAll(stats,chart,insight); VBox.setVgrow(chart,Priority.ALWAYS); root.setCenter(content); stage.setScene(scene(root)); stage.setTitle("HealthLens - Analysis"); keepMaximized();
        executor.submit(()->{List<HealthRecord> records=db.getRecords(currentUserId); double avg=analyzer.averageScore(records); double seven=analyzer.sevenDayAverage(records); Platform.runLater(()->{
            XYChart.Series<String,Number> series=new XYChart.Series<>(); series.setName("Score"); records.stream().limit(14).sorted(java.util.Comparator.comparing(HealthRecord::getDate)).forEach(r->series.getData().add(new XYChart.Data<>(r.getDate().toString(),r.calculateScore()))); chart.getData().add(series);
            weekly.setText(String.format("Average: %.0f%%   |   7-day: %.0f%%",avg,seven)); trend.setText(records.size()<2?"Trend: add more records":"Trend: compare the last records on the chart"); insight.setText(analyzer.summary(records)+"\nRisk status: "+analyzer.riskLevel(records)+"\nCoach: "+analyzer.coachAdvice(records));
        });});
    }

    private void exportReport(){
        List<HealthRecord> records=db.getRecords(currentUserId); if(records.isEmpty()){new Alert(Alert.AlertType.INFORMATION,"Add at least one record before exporting.").showAndWait();return;}
        FileChooser chooser=new FileChooser(); chooser.setTitle("Save HealthLens Report"); chooser.setInitialFileName("HealthLens-Report.txt"); chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Text files","*.txt")); File file=chooser.showSaveDialog(stage); if(file==null)return;
        StringBuilder report=new StringBuilder("HEALTHLENS HEALTH REPORT\n========================\n"); report.append("Average score: ").append(String.format("%.0f%%",analyzer.averageScore(records))).append("\n"); report.append("7-day average: ").append(String.format("%.0f%%",analyzer.sevenDayAverage(records))).append("\n"); report.append("Streak: ").append(analyzer.streak(records)).append(" days\n"); report.append("Status: ").append(analyzer.riskLevel(records)).append("\n\n"); report.append("Coach: ").append(analyzer.coachAdvice(records)).append("\n"); report.append("Challenge: ").append(analyzer.challenge(records)).append("\n\nRECORDS\n"); records.forEach(r->report.append(r.getDate()).append(" | score ").append(String.format("%.0f%%",r.calculateScore())).append(" | sleep ").append(r.getSleepHours()).append("h | water ").append(r.getWaterGlasses()).append("L | exercise ").append(r.getExerciseMinutes()).append("m\n"));
        try{Files.writeString(file.toPath(),report.toString());new Alert(Alert.AlertType.INFORMATION,"Report saved successfully.").showAndWait();}catch(Exception ex){new Alert(Alert.AlertType.ERROR,"Could not save the report.").showAndWait();}
    }

    @Override public void stop(){executor.shutdownNow();}
    public static void main(String[] args){launch(args);}
}
