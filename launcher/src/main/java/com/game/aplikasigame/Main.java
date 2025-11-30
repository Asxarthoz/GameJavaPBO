package com.game.aplikasigame;

import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaView;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.List;

public class Main extends Application {

    private static final String GAME_LAUNCHER_CLASS = "com.gdx.lwjgl3.Lwjgl3Launcher";

    List<String> videoList = List.of(
        "/video/video1.mp4",
        "/video/video2.mp4",
        "/video/video3.mp4"
    );

    @Override
    public void start(Stage stage) throws IOException {
        // ====== Bagian Header / Judul ======
        ImageView imageView = new ImageView(
            new Image(getClass().getResource("/images/judul.png").toExternalForm())
        );
        imageView.setFitWidth(600);
        imageView.setPreserveRatio(true);

        Label captionZejarah = new Label("Belajar sejarah dengan cara interaktif dan seru!");
        captionZejarah.getStyleClass().add("captionZejarah");
        captionZejarah.setTranslateX(-30);

        Button buttonExplore = new Button("Jelajah!");
        buttonExplore.getStyleClass().add("buttonExplore");
        buttonExplore.setTranslateX(140);
        buttonExplore.setTranslateY(80);

        VBox judulZejarahBox = new VBox(imageView, captionZejarah, buttonExplore);
        judulZejarahBox.setTranslateY(500);
        judulZejarahBox.setTranslateX(1200);

        Pane mainBox = new Pane(judulZejarahBox);
        mainBox.getStyleClass().add("mainBox");

        Image bg = new Image(getClass().getResource("/images/bg.png").toExternalForm());
        BackgroundImage backgroundImage = new BackgroundImage(
            bg,
            BackgroundRepeat.NO_REPEAT,
            BackgroundRepeat.NO_REPEAT,
            BackgroundPosition.CENTER,
            new BackgroundSize(BackgroundSize.AUTO, BackgroundSize.AUTO, false, false, true, true)
        );
        mainBox.setBackground(new Background(backgroundImage));
        mainBox.setMinHeight(1200);

        // ====== Halaman 2: Video ======
        ImageView thumbnailView = new ImageView();
        thumbnailView.setFitWidth(700);
        thumbnailView.setFitHeight(400);
        thumbnailView.setPreserveRatio(false);
        thumbnailView.setImage(new Image(getClass().getResource("/thumbnail/video1.png").toExternalForm()));

        final int[] currentIndex = {0};
        final MediaPlayer[] currentPlayer = {null};
        MediaView mediaView = new MediaView();
        mediaView.setFitWidth(700);
        mediaView.setFitHeight(400);

        Button btnPlay = new Button("Play");
        btnPlay.setStyle("-fx-padding: 10px;");
        btnPlay.getStyleClass().add("btnPlay");

        Runnable loadVideo = () -> {
            try {
                String thumbPath = "/thumbnail/video" + (currentIndex[0] + 1) + ".png";
                thumbnailView.setImage(new Image(getClass().getResource(thumbPath).toExternalForm()));

                if (currentPlayer[0] != null) {
                    currentPlayer[0].stop();
                }

                Media media = new Media(getClass().getResource(videoList.get(currentIndex[0])).toExternalForm());
                MediaPlayer newPlayer = new MediaPlayer(media);
                newPlayer.setAutoPlay(false);

                mediaView.setMediaPlayer(newPlayer);
                currentPlayer[0] = newPlayer;

                newPlayer.setOnReady(() -> {
                    thumbnailView.setVisible(false);
                    mediaView.setVisible(true);
                });

                thumbnailView.setVisible(true);
                mediaView.setVisible(false);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        };

        loadVideo.run();

        Button btnPrevVid = new Button("<");
        btnPrevVid.setStyle("-fx-font-size: 30px; -fx-padding: 20px;");
        btnPrevVid.getStyleClass().add("btnPrevVid");

        Button buttonNextVid = new Button(">");
        buttonNextVid.setStyle("-fx-font-size: 30px; -fx-padding: 20px;");
        buttonNextVid.getStyleClass().add("buttonNextVid");

        btnPrevVid.setOnAction(e -> {
            currentIndex[0] = (currentIndex[0] - 1 + videoList.size()) % videoList.size();
            loadVideo.run();
        });

        buttonNextVid.setOnAction(e -> {
            currentIndex[0] = (currentIndex[0] + 1) % videoList.size();
            loadVideo.run();
        });

        btnPlay.setOnAction(e -> {
            if (currentPlayer[0] != null) {
                currentPlayer[0].play();
            }
        });

        StackPane videoContainer = new StackPane(thumbnailView, mediaView);
        videoContainer.setPrefSize(700, 400);
        videoContainer.setMaxSize(700, 400);

        HBox videoControls = new HBox(30, btnPrevVid, videoContainer, buttonNextVid);
        videoControls.setAlignment(Pos.CENTER);

        VBox mainBox2 = new VBox(40, videoControls, btnPlay);
        mainBox2.setAlignment(Pos.CENTER);
        mainBox2.setMinHeight(1200);

        Image bg2 = new Image(getClass().getResource("/images/bg2.png").toExternalForm());
        BackgroundImage backgroundImage2 = new BackgroundImage(
            bg2,
            BackgroundRepeat.NO_REPEAT,
            BackgroundRepeat.NO_REPEAT,
            BackgroundPosition.CENTER,
            new BackgroundSize(BackgroundSize.AUTO, BackgroundSize.AUTO, false, false, true, true)
        );
        mainBox2.setBackground(new Background(backgroundImage2));

        // ====== Halaman 3: Cards & Mainkan button ======
        VBox mainBox3 = new VBox();
        mainBox3.setAlignment(Pos.CENTER);
        mainBox3.setMinHeight(1200);

        Image bg3 = new Image(getClass().getResource("/images/bg3.png").toExternalForm());
        BackgroundImage backgroundImage3 = new BackgroundImage(
            bg3,
            BackgroundRepeat.NO_REPEAT,
            BackgroundRepeat.NO_REPEAT,
            BackgroundPosition.CENTER,
            new BackgroundSize(BackgroundSize.AUTO, BackgroundSize.AUTO, false, false, true, true)
        );
        mainBox3.setBackground(new Background(backgroundImage3));

        VBox card1 = new VBox(20);
        card1.setAlignment(Pos.CENTER);
        card1.setStyle("-fx-background-color: rgba(255,255,255,0.7); -fx-padding: 20px; -fx-background-radius: 20px;");
        card1.setPrefSize(400, 500);

        ImageView img1 = new ImageView(new Image(getClass().getResource("/images/mataramislam.jpg").toExternalForm()));
        img1.setFitWidth(300);
        img1.setPreserveRatio(true);
        Label text1 = new Label("Kerajaan Mataram Islam didirikan pada akhir abad ke-16\n oleh Sutawijaya (Panembahan Senopati) \ndi Kotagede, Yogyakarta. Kerajaan ini mencapai kejayaan di bawah Sultan Agung\n pada abad ke-17 dan menjadi kekuatan besar di Jawa, \nlalu mengalami kemunduran karena konflik internal dan campur tangan VOC. \nAkhirnya, kerajaan ini terpecah menjadi dua pada \ntahun 1755 melalui Perjanjian Giyanti, \nyaitu Kesultanan Yogyakarta dan Kasunanan Surakarta.");
        text1.getStyleClass().add("text1");
        card1.getChildren().addAll(img1, text1);

        VBox card2 = new VBox(20);
        card2.setAlignment(Pos.CENTER);
        card2.setStyle("-fx-background-color: rgba(255,255,255,0.7); -fx-padding: 20px; -fx-background-radius: 20px;");
        card2.setPrefSize(400, 500);

        ImageView img2 = new ImageView(new Image(getClass().getResource("/images/kembar.png").toExternalForm()));
        img2.setFitWidth(300);
        img2.setPreserveRatio(true);
        Label text2 = new Label("Chapter 1 : Si Kembar dari Mataram");
        text2.getStyleClass().add("text2");

        VBox card3 = new VBox(20);
        card3.setAlignment(Pos.CENTER);
        card3.setStyle("-fx-background-color: rgba(255,255,255,0.7); -fx-padding: 20px; -fx-background-radius: 20px;");
        card3.setPrefSize(400, 500);

        ImageView img3 = new ImageView(new Image(getClass().getResource("/images/coming.jpg").toExternalForm()));
        img3.setFitWidth(300);
        img3.setPreserveRatio(true);
        Label text3 = new Label("Chapter 2 : Bagaimana kelanjutan kisah dari \nperjuangan rakyat Indonesia \ndemi meraih kemerdekaannya?\nTunggu Kelanjutannya!");
        text3.getStyleClass().add("text3");
        card3.getChildren().addAll(img3, text3);


        Button detailBtn = new Button("Mainkan!");
        detailBtn.getStyleClass().add("cardButton");
        card2.getChildren().addAll(img2, text2, detailBtn);

        List<VBox> cards = List.of(card1, card2, card3);
        int[] currentCard = {0};
        StackPane cardContainer = new StackPane();
        cardContainer.setPrefSize(500, 550);
        cardContainer.getChildren().add(cards.get(0));

        Button btnPrevCard = new Button("<");
        btnPrevCard.setStyle("-fx-font-size: 30px; -fx-padding: 20px;");
        Button btnNextCard = new Button(">");
        btnNextCard.setStyle("-fx-font-size: 30px; -fx-padding: 20px;");

        Runnable loadCard = () -> {
            cardContainer.getChildren().clear();
            VBox newCard = cards.get(currentCard[0]);
            newCard.setOpacity(0);
            cardContainer.getChildren().add(newCard);
            Timeline fade = new Timeline(
                new KeyFrame(Duration.seconds(0), new KeyValue(newCard.opacityProperty(), 0)),
                new KeyFrame(Duration.seconds(0.3), new KeyValue(newCard.opacityProperty(), 1))
            );
            fade.play();
        };

        btnPrevCard.setOnAction(e -> {
            currentCard[0] = (currentCard[0] - 1 + cards.size()) % cards.size();
            loadCard.run();
        });

        btnNextCard.setOnAction(e -> {
            currentCard[0] = (currentCard[0] + 1) % cards.size();
            loadCard.run();
        });

        HBox cardControls = new HBox(30, btnPrevCard, cardContainer, btnNextCard);
        cardControls.setAlignment(Pos.CENTER);
        mainBox3.getChildren().add(cardControls);

        // ====== Container & Scene ======
        VBox container = new VBox(mainBox, mainBox2, mainBox3);
        ScrollPane scrollPane = new ScrollPane(container);
        scrollPane.setFitToWidth(true);

        Scene scene = new Scene(scrollPane, 1080, 1080);
        scene.getStylesheets().add(getClass().getResource("/css/style.css").toExternalForm());

        // tombol explore (scroll)
        buttonExplore.setOnAction(e -> {
            Timeline scrollAnim = new Timeline(
                new KeyFrame(Duration.millis(0),
                    new KeyValue(scrollPane.vvalueProperty(), scrollPane.getVvalue())
                ),
                new KeyFrame(Duration.millis(500),
                    new KeyValue(scrollPane.vvalueProperty(), 0.5)
                )
            );
            scrollAnim.play();
        });

        // ====== ACTION: Mainkan! -> jalankan game LWJGL3 ======
        detailBtn.setOnAction(e -> {
            // 1) tutup / sembunyikan launcher
            Stage win = (Stage) detailBtn.getScene().getWindow();
            try {
                // prefer close supaya windowing resources dilepas
                win.close();
            } catch (Exception ignore) {
                win.hide();
            }

            // 2) jalankan game di thread terpisah (penting)
            new Thread(() -> {
                try {
                    // Pemanggilan via refleksi agar mudah ganti FQCN tanpa recompile
                    Class<?> cls = Class.forName(GAME_LAUNCHER_CLASS);
                    Method main = cls.getMethod("main", String[].class);
                    // panggil static main(new String[]{})
                    main.invoke(null, (Object) new String[]{});
                } catch (ClassNotFoundException cnf) {
                    System.err.println("Game launcher class not found: " + GAME_LAUNCHER_CLASS);
                    cnf.printStackTrace();
                } catch (Throwable t) {
                    t.printStackTrace();
                }
            }, "Game-Thread").start();
        });

        // show stage
        stage.setTitle("Game Launcher");
        stage.setScene(scene);
        stage.setFullScreen(true);
        stage.show();
    }

    public static void main(String[] args) {
        launch();
    }
}
