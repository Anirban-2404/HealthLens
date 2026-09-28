# HealthLens

HealthLens is a JavaFX-based health and wellness dashboard designed to turn daily health habits into meaningful insights through interactive tracking, visual analysis, SQLite data management, weather information, and personalized recommendations.

## Main Features
- JavaFX responsive dashboard with maximized/fullscreen navigation
- Daily goals for sleep, water and exercise
- Health score, streak and health-status indicator
- Personalized Health Coach, challenges and achievements
- Today vs 7-day average comparison
- Weather information with wellness advice using HTTP + JSON
- SQLite CRUD for health records with user relationship
- Search/filter records and exportable text health report
- Weekly/trend chart in the Analysis screen
- OOP with abstract class, interface and inheritance
- Multithreading with a fixed ExecutorService thread pool
- Git/GitHub version-control workflow

## Demo Login
Email: `anirban@gmail.com`
Password: `2307023`

## Run
Open the project in IntelliJ IDEA as a Maven project, then run:

```text
mvn clean javafx:run
```

Or use IntelliJ Maven -> Plugins -> javafx -> javafx:run.

The SQLite database file `healthlens.db` is created automatically on first run.

## Weather API
The project uses Open-Meteo and does not require an API key.
