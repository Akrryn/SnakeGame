package com.example.demo;

import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.*;
import javafx.scene.paint.*;
import javafx.scene.shape.StrokeLineCap;
import javafx.scene.shape.StrokeLineJoin;
import javafx.util.Duration;
import javafx.stage.Stage;
import java.util.Objects;

public class SnakeGame extends Application {
    final int size = 600, dotSize = 30, up = 1, right = 2, down = 3, left = 4;
    int delay = 90, length = 3, direction = 0, foodX, foodY, score = 0, applesEaten = 0, bananasEaten = 0;
    Canvas canvas;
    GraphicsContext gc;
    int[] x = new int[size * size / dotSize / dotSize];
    int[] y = new int[size * size / dotSize / dotSize];
    Timeline timeline;
    boolean lost = false, speedBoostActive = false;
    Button restartButton;
    ColorPicker snakeColorPicker;
    Stage primaryStage;
    Label scoreLabel;
    Image foodImage, headImage, bananaImage;
    Image headIcon;
    Banana banana; // Новая переменная для банана


    public static class JelloTransition {

        private final Timeline timeline;

        public JelloTransition(Node node) {

            timeline = new Timeline(
                    new KeyFrame(Duration.seconds(0), new KeyValue(node.scaleXProperty(), 1)),
                    new KeyFrame(Duration.seconds(0.1), new KeyValue(node.scaleXProperty(), 1.4)),
                    new KeyFrame(Duration.seconds(0.2), new KeyValue(node.scaleXProperty(), 0.9)),
                    new KeyFrame(Duration.seconds(0.3), new KeyValue(node.scaleXProperty(), 1.1)),
                    new KeyFrame(Duration.seconds(0.4), new KeyValue(node.scaleXProperty(), 1)),
                    new KeyFrame(Duration.seconds(0), new KeyValue(node.scaleYProperty(), 1)),
                    new KeyFrame(Duration.seconds(0.1), new KeyValue(node.scaleYProperty(), 1.4)),
                    new KeyFrame(Duration.seconds(0.2), new KeyValue(node.scaleYProperty(), 0.9)),
                    new KeyFrame(Duration.seconds(0.3), new KeyValue(node.scaleYProperty(), 1.1)),
                    new KeyFrame(Duration.seconds(0.4), new KeyValue(node.scaleYProperty(), 1))
            );
        }

        public void play() {
            timeline.play();
        }
    }


    public SnakeGame() {
        // Default constructor
    }

    @Override
    public void start(Stage primaryStage) {
        this.primaryStage = primaryStage;
        BorderPane root = new BorderPane();
        canvas = new Canvas(size , size); // увеличиваем ширину Canvas
        gc = canvas.getGraphicsContext2D();

        BorderPane borderPane = new BorderPane(canvas);
        borderPane.setStyle("-fx-border-color: #8B4513; -fx-border-width: 10; -fx-background-color: #32CD32;");
        root.setCenter(borderPane);

        HBox scoreBox = new HBox();
        scoreLabel = new Label("Score: 0");
        scoreLabel.setStyle("-fx-font-size: 18; -fx-font-weight: bold; -fx-text-fill: #8B4513;");
        scoreBox.getChildren().add(scoreLabel);
        scoreBox.setAlignment(Pos.CENTER);
        root.setTop(scoreBox);

        foodImage = new Image(Objects.requireNonNull(getClass().getResourceAsStream("/apple.png")));
        bananaImage = new Image(Objects.requireNonNull(getClass().getResourceAsStream("/BANANA.png")));

        snakeColorPicker = new ColorPicker(Color.ORANGE);

        snakeColorPicker.setOnAction(event -> changeSnakeColor(snakeColorPicker.getValue()));

        headImage = new Image(Objects.requireNonNull(getClass().getResourceAsStream("/HEAD1.png")));



        restartButton = new Button("Restart");
        restartButton.setStyle(
                "-fx-font-size: 16; " +
                        "-fx-font-weight: bold; " +
                        "-fx-background-color: #FFD700; " +  // желтый цвет фона
                        "-fx-border-color: #8B4513; " +     // коричневая рамка
                        "-fx-border-width: 3; " +            // толщина рамки
                        "-fx-text-fill: #8B4513; "           // коричневый цвет текста
        );
        restartButton.setOnAction(event -> restartGame());
        restartButton.setVisible(false);

        startGame();

        canvas.setOnKeyPressed(e -> handleKeyPress(e.getCode()));
        canvas.setFocusTraversable(true);

        Scene scene = new Scene(root, size + 150, size + 50); // увеличиваем ширину окна

        scene.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ESCAPE) {
                pauseGame();
            }
        });

        primaryStage.setTitle("Snake");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    private void restartGame() {
        lost = false;
        restartButton.setVisible(false);
        initializeGame();
        hidePauseMenu();

        BorderPane borderPane = new BorderPane(canvas);
        borderPane.setStyle("-fx-border-color: #8B4513; -fx-border-width: 10; -fx-background-color: #32CD32;"); // коричневая рамка, зеленый фон
        ((BorderPane) primaryStage.getScene().getRoot()).setCenter(borderPane);

        timeline.play();
    }

    private void startGame() {
        initializeGame();
        loadHeadImage("/HEAD1.png");
        changeSnakeColor(snakeColorPicker.getValue()); // Добавляем эту строку
        draw(gc);

        canvas.setOnKeyPressed(e -> {
            handleKeyPress(e.getCode());
            canvas.requestFocus();
        });

        timeline = new Timeline(new KeyFrame(Duration.millis(delay), event -> gameLoop()));
        timeline.setCycleCount(Timeline.INDEFINITE);

        timeline.play();
    }

    private void gameLoop() {
        if (!lost) {
            checkFood();
            checkCollision();
            move();
        } else {
            restartButton.setVisible(true);
            timeline.stop();
        }
        draw(gc);
    }

    private void initializeGame() {
        length = 3;
        score = 0;
        direction = 0;
        applesEaten = 0;
        bananasEaten = 0;
        speedBoostActive = false;
        banana = null;

        for (int i = 0; i < length; i++) {
            x[i] = (size / 2 - i * dotSize) / dotSize * dotSize;
            y[i] = (size / 2) / dotSize * dotSize;
        }

        locateFood();
    }

    private void handleKeyPress(KeyCode key) {
        if (key == KeyCode.UP && direction != down) direction = up;
        if (key == KeyCode.DOWN && direction != up) direction = down;
        if (key == KeyCode.LEFT && direction != right) direction = left;
        if (key == KeyCode.RIGHT && direction != left) direction = right;
    }

    private void draw(GraphicsContext gc) {
        gc.setFill(Color.LIGHTGREEN);
        gc.fillRect(0, 0, size, size);

        if (!lost) {
            // Рисование яблока
            gc.drawImage(foodImage, foodX, foodY, dotSize, dotSize);

            // Рисование банана (если он есть)
            if (banana != null) {
                gc.drawImage(banana.getImage(), banana.getX(), banana.getY(), dotSize, dotSize);
            }

            gc.setLineCap(StrokeLineCap.ROUND);
            gc.setLineJoin(StrokeLineJoin.ROUND);
            gc.setLineWidth(dotSize);



            for (int i = 1; i < length; i++) {
                double startX = x[i - 1] + dotSize / 2;
                double startY = y[i - 1] + dotSize / 2;
                double endX = x[i] + dotSize / 2;
                double endY = y[i] + dotSize / 2;

                double controlX = (startX + endX) / 2;
                double controlY = (startY + endY) / 2;

                gc.beginPath();
                gc.moveTo(startX, startY);
                gc.quadraticCurveTo(startX, endY, controlX, controlY);
                gc.stroke();
                gc.closePath();
            }

            gc.setFill(snakeColorPicker.getValue());
            for (int i = 1; i < length; i++) {
                gc.setLineWidth(dotSize);
                gc.strokeLine(x[i - 1] + dotSize / 2, y[i - 1] + dotSize / 2, x[i] + dotSize / 2, y[i] + dotSize / 2);
            }

            if (speedBoostActive) {
                // Рисуем тело змеи с градиентом при активном ускорении
                for (int i = 1; i < length; i++) {
                    double hue = (applesEaten * 20) % 360;
                    double saturation = 1.0;
                    double brightness = 1.0;

                    Paint gradient = new RadialGradient(
                            0, 0, 0.5, 0.5, 0.7, true, CycleMethod.NO_CYCLE,
                            new Stop(0, Color.hsb(hue, saturation, brightness)),
                            new Stop(1, Color.hsb(hue + 20, saturation, brightness))
                    );

                    gc.setStroke(gradient);
                    gc.setLineWidth(dotSize);
                    gc.strokeLine(x[i - 1] + dotSize / 2, y[i - 1] + dotSize / 2, x[i] + dotSize / 2, y[i] + dotSize / 2);
                }
            } else {
                // Рисуем тело змеи в обычном цвете
                gc.setFill(snakeColorPicker.getValue());
                for (int i = 1; i < length; i++) {
                    gc.setLineWidth(dotSize);
                    gc.strokeLine(x[i - 1] + dotSize / 2, y[i - 1] + dotSize / 2, x[i] + dotSize / 2, y[i] + dotSize / 2);
                }
            }

            // Обновляем цвет головы змеи после рисования тела
            gc.setFill(snakeColorPicker.getValue());
            for (int i = 1; i < length; i++) {
                gc.setLineWidth(dotSize);
                gc.strokeLine(x[i - 1] + dotSize / 2, y[i - 1] + dotSize / 2, x[i] + dotSize / 2, y[i] + dotSize / 2);
            }
            gc.drawImage(headIcon, x[0], y[0], dotSize, dotSize);

            // Рисуем тело змеи в обычном цвете
            gc.setFill(snakeColorPicker.getValue());
            for (int i = 1; i < length; i++) {
                gc.setLineWidth(dotSize);
                gc.strokeLine(x[i - 1] + dotSize / 2, y[i - 1] + dotSize / 2, x[i] + dotSize / 2, y[i] + dotSize / 2);
            }

            // Отображение текущего счета
            scoreLabel.setText("Score: " + score);

            // Рисование головы змеи
            gc.drawImage(headIcon, x[0], y[0], dotSize, dotSize);
        } else {

            if (lost) {


                // Отображение большой надписи с эффектом желе
                Label gameOverLabel = new Label("Game Over");
                gameOverLabel.setStyle("-fx-font-size: 36; -fx-font-weight: bold;");
                JelloTransition jelloTransition = new JelloTransition(gameOverLabel);
                jelloTransition.play();

                // Создание кнопки Restart
                Button restartButton = createIconButton("/Restart.png");
                restartButton.setOnAction(event -> restartGame());

                // Создание вертикального контейнера для размещения надписи и кнопки
                VBox gameOverBox = new VBox(20);
                gameOverBox.getChildren().addAll(gameOverLabel, restartButton);
                gameOverBox.setAlignment(Pos.CENTER);

                // Размещение контейнера в центре сцены
                BorderPane borderPane = new BorderPane(gameOverBox);
                borderPane.setStyle("-fx-border-color: #8B4513; -fx-border-width: 10; -fx-background-color: #32CD32;");;
                ((BorderPane) primaryStage.getScene().getRoot()).setCenter(borderPane);
                gameOverBox.setStyle("-fx-background-color: lightgreen;");
            }
        }
    }

    private void locateFood() {
        if (banana == null || bananasEaten % 3 != 0) {
            if (banana == null || !banana.isActive()) {
                foodX = ((int) (Math.random() * ((size / dotSize)))) * dotSize;
                foodY = ((int) (Math.random() * ((size / dotSize)))) * dotSize;
            }
        } else {
            do {
                foodX = ((int) (Math.random() * ((size / dotSize)))) * dotSize;
                foodY = ((int) (Math.random() * ((size / dotSize)))) * dotSize;
            } while (foodX == banana.getX() && foodY == banana.getY());

            banana.setInactive();
        }
    }

    private void checkFood() {
        if (x[0] == foodX && y[0] == foodY) {
            applesEaten++;
            score++;
            length++;

            if (applesEaten % 3 == 0) {
                banana = new Banana(dotSize);
            }

            locateFood();
        }

        if (banana != null && x[0] == banana.getX() && y[0] == banana.getY()) {
            bananasEaten++;
            activateSpeedBoost();
            banana = null;

            for (int i = 0; i < 3; i++) {
                locateFood();
            }
        }
    }

    private void activateSpeedBoost() {
        speedBoostActive = true;

        // Добавляем таймер для ускорения и изменения цвета
        Timeline speedBoostTimeline = new Timeline(
                new KeyFrame(Duration.ZERO, e -> {
                    // Применяем все цвета градиента к телу змеи
                    for (int i = 1; i < length; i++) {
                        double hue = (applesEaten * 20 + i * 20) % 360;
                        double saturation = 1.0;
                        double brightness = 1.0;

                        Paint gradientColor = Color.hsb(hue, saturation, brightness);
                        gc.setFill(gradientColor);
                        gc.fillRect(x[i], y[i], dotSize, dotSize);
                    }
                }),
                new KeyFrame(Duration.seconds(5), e -> {
                    // Восстанавливаем обычный цвет змеи
                    changeSnakeColor(snakeColorPicker.getValue());
                    draw(gc); // Обновляем отрисовку после восстановления цвета
                    speedBoostActive = false;
                })
        );
        speedBoostTimeline.play();


        // Добавляем эффект ускорения
        Timeline timer = new Timeline(
                new KeyFrame(Duration.seconds(5), event -> deactivateSpeedBoost())
        );
        timer.setCycleCount(1);
        timer.play();
    }

    private void deactivateSpeedBoost() {
        speedBoostActive = false;
        changeSnakeColor(snakeColorPicker.getValue());
    }

    private void checkCollision() {
        if (x[0] >= size || y[0] >= size || x[0] < 0 || y[0] < 0) {
            lost = true;
        }

        for (int i = 3; i < length; i++) {
            if (x[0] == x[i] && y[0] == y[i]) {
                lost = true;
            }
        }
    }

    private void move() {
        if (direction != 0) {
            for (int i = length - 1; i > 0; i--) {
                x[i] = x[i - 1];
                y[i] = y[i - 1];
            }

            if (direction == up) y[0] -= dotSize;
            if (direction == down) y[0] += dotSize;
            if (direction == right) x[0] += dotSize;
            if (direction == left) x[0] -= dotSize;

            if (speedBoostActive) {
                if (direction == up) y[0] -= dotSize;
                if (direction == down) y[0] += dotSize;
                if (direction == right) x[0] += dotSize;
                if (direction == left) x[0] -= dotSize;
            }
        }
    }

    private void pauseGame() {
        if (timeline != null) {
            timeline.pause();
            showPauseMenu();
        }
    }

    private void showPauseMenu() {
        String buttonColor = "#90EE90"; // Светло-зеленый цвет

        Button continueButton = createIconButton("/Resume.png");
        Button restartButton = createIconButton("/Restart.png");
        Button exitButton = createIconButton("/Exit.png");
        Button optionsButton = createIconButton("/headchange.png");

        continueButton.setOnAction(event -> resumeGame());
        restartButton.setOnAction(event -> restartGame());
        exitButton.setOnAction(event -> System.exit(0));
        optionsButton.setOnAction(event -> showOptionsMenu());

        VBox menuBox = new VBox(10);
        menuBox.getChildren().addAll(continueButton, restartButton, optionsButton, snakeColorPicker, exitButton);
        menuBox.setAlignment(Pos.CENTER);

        StackPane pauseMenu = new StackPane(menuBox);
        pauseMenu.setStyle("-fx-background-color: #90EE90;"); // Светло-зеленый цвет фона

        BorderPane borderPane = new BorderPane(pauseMenu);
        borderPane.setStyle("-fx-border-color: #8B4513; -fx-border-width: 10;");
        borderPane.requestFocus();

        borderPane.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ESCAPE) {
                hidePauseMenu();
                resumeGame();
            }
        });

        ((BorderPane) primaryStage.getScene().getRoot()).setCenter(borderPane);
        JelloTransition jelloTransition = new JelloTransition(menuBox);
        jelloTransition.play();
    }




    private void showOptionsMenu() {
        Button head1Button = createIconButton("/HEAD1.png");
        Button head2Button = createIconButton("/HEAD2.png");
        Button head3Button = createIconButton("/HEAD3.png");

        VBox optionsBox = new VBox(10);
        optionsBox.getChildren().addAll(head1Button, head2Button, head3Button);
        optionsBox.setAlignment(Pos.CENTER);

        StackPane optionsMenu = new StackPane(optionsBox);
        optionsMenu.setStyle("-fx-background-color: lightblue;");

        BorderPane borderPane = new BorderPane(optionsMenu);
        borderPane.setStyle("-fx-border-color: #8B4513; -fx-border-width: 10; -fx-background-color: #32CD32;"); // коричневая рамка, зеленый фон

        head1Button.setOnAction(event -> changeHeadIcon("/HEAD1.png"));
        head2Button.setOnAction(event -> changeHeadIcon("/HEAD2.png"));
        head3Button.setOnAction(event -> changeHeadIcon("/HEAD3.png"));

        borderPane.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ESCAPE) {
                hideOptionsMenu();
            }
        });

        ((BorderPane) primaryStage.getScene().getRoot()).setCenter(borderPane);
        JelloTransition jelloTransition = new JelloTransition(optionsBox);
        jelloTransition.play();
    }

    private void changeHeadIcon(String imagePath) {
        headIcon = new Image(Objects.requireNonNull(getClass().getResourceAsStream(imagePath)));
        draw(gc);
        hideOptionsMenu();
        restartGame();
    }

    private void hideOptionsMenu() {
        ((BorderPane) primaryStage.getScene().getRoot()).setCenter(canvas);
    }

    private Button createIconButton(String imagePath) {
        Image icon = new Image(Objects.requireNonNull(getClass().getResourceAsStream(imagePath)));
        ImageView iconView = new ImageView(icon);
        iconView.setFitWidth(50);
        iconView.setFitHeight(50);

        Button button = new Button();
        button.setGraphic(iconView);
        button.setStyle("-fx-background-color: transparent;");

        return button;
    }

    private void hidePauseMenu() {
        ((BorderPane) primaryStage.getScene().getRoot()).setCenter(canvas);
    }

    private void resumeGame() {
        if (timeline != null) {
            timeline.play();
            hidePauseMenu();
        }

        BorderPane borderPane = new BorderPane(canvas);
        borderPane.setStyle("-fx-border-color: #8B4513; -fx-border-width: 10; -fx-background-color: #32CD32;"); // коричневая рамка, зеленый фон
        ((BorderPane) primaryStage.getScene().getRoot()).setCenter(borderPane);
    }

    private void loadHeadImage(String fileName) {
        headIcon = new Image(Objects.requireNonNull(getClass().getResourceAsStream(fileName)));
    }

    private void changeSnakeColor(Paint color) {
        // Обновляем цвет головы змеи
        gc.setFill(color);
        gc.drawImage(headIcon, x[0], y[0], dotSize, dotSize);

        // Обновляем цвет тела змеи
        for (int i = 1; i < length; i++) {
            gc.setStroke(color);
            gc.setLineWidth(dotSize);
            gc.strokeLine(x[i - 1] + dotSize / 2, y[i - 1] + dotSize / 2, x[i] + dotSize / 2, y[i] + dotSize / 2);
        }
    }

    public static void main(String[] args) {
        launch(args);
    }

    // Класс для представления банана
    private class Banana {
        private int x, y;
        private final int dotSize;
        private final Image bananaImage;
        private boolean active;

        public Banana(int dotSize) {
            this.dotSize = dotSize;
            x = ((int) (Math.random() * ((size / dotSize) - 1))) * dotSize;
            y = ((int) (Math.random() * ((size / dotSize) - 1))) * dotSize;
            bananaImage = new Image(Objects.requireNonNull(getClass().getResourceAsStream("/BANANA.png")));
            active = true;
        }

        public int getX() {
            return x;
        }

        public int getY() {
            return y;
        }

        public Image getImage() {
            return bananaImage;
        }

        public boolean isActive() {
            return active;
        }

        public void setInactive() {
            active = false;
        }
    }
}




