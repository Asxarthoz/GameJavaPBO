package com.gdx;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.viewport.ScreenViewport;

public class IntroScreen implements Screen {
    private final Main game;
    private Stage stage;
    private SpriteBatch batch; // Batch untuk menggambar teks manual

    // --- Audio ---
    private Music introMusic;

    // --- Assets untuk Start Screen ---
    private Texture startBgTexture;
    private Image startImageActor;
    private BitmapFont font;
    private boolean isStartScreen = true; // Penanda apakah masih di halaman "Tap to Start"
    private float blinkTimer = 0f;        // Timer untuk efek kedip teks

    // --- Assets untuk Intro Cerita ---
    // Pastikan nama file ini sesuai dengan yang ada di folder assets
    private final String[] introImages = {
        "PerjuanganKemerdekaan.png",
        "LoadScreen1.png",
        "LoadScreen2.png",
        "LoadScreen3.png",
        "LoadScreen4.png",
        "Hero_Intro1.png"
    };

    private Array<Texture> textures = new Array<>();
    private Image currentImage;
    private int currentIndex = 0;
    private float timer = 0f;
    private boolean isTransitioning = false;

    public IntroScreen(Main game) {
        this.game = game;
        stage = new Stage(new ScreenViewport());
        batch = new SpriteBatch();
        font = new BitmapFont();
        font.getData().setScale(2f); // Memperbesar ukuran font

        // --- SETUP AUDIO ---
        // Memuat musik dari folder assets/sound/soundEpic.mp3
        try {
            introMusic = Gdx.audio.newMusic(Gdx.files.internal("sound/soundEpic.ogg"));
            introMusic.setLooping(true); // Musik mengulang terus
            introMusic.setVolume(0.5f);  // Volume 50%
            introMusic.play();           // Mainkan langsung
        } catch (Exception e) {
            System.out.println("Error memuat musik: " + e.getMessage());
        }

        // --- SETUP START SCREEN ---
        // Memuat gambar start.png (pastikan ada di root assets)
        startBgTexture = new Texture("start.png");
        startImageActor = new Image(startBgTexture);
        startImageActor.setFillParent(true);

        // Masukkan gambar start ke stage agar muncul paling awal
        stage.addActor(startImageActor);
    }

    /**
     * Method ini dipanggil ketika user menyentuh layar saat di Start Screen.
     * Mengubah status isStartScreen jadi false dan memulai slideshow cerita.
     */
    private void startIntroSequence() {
        isStartScreen = false;

        // Hilangkan gambar start dengan efek fade out
        startImageActor.addAction(Actions.sequence(
            Actions.fadeOut(0.5f),
            Actions.run(() -> {
                startImageActor.remove(); // Hapus dari stage
                loadNextImage();          // Mulai muat gambar cerita pertama
            })
        ));
    }

    /**
     * Memuat gambar cerita berikutnya dari array introImages.
     */
    private void loadNextImage() {
        if (currentIndex >= introImages.length) {
            // Jika gambar habis, masuk ke GameScreen
            fadeOutAndStartGame();
            return;
        }

        Texture tex = new Texture(introImages[currentIndex]);
        textures.add(tex); // Simpan referensi texture untuk didispose nanti

        currentImage = new Image(tex);
        currentImage.setFillParent(true);
        currentImage.getColor().a = 0f; // Mulai transparan
        currentImage.addAction(Actions.fadeIn(1.2f)); // Muncul perlahan

        stage.clear(); // Bersihkan aktor sebelumnya
        stage.addActor(currentImage);

        timer = 0f; // Reset timer durasi gambar
    }

    @Override
    public void render(float delta) {
        // Bersihkan layar dengan warna hitam
        Gdx.gl.glClearColor(0, 0, 0, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        stage.act(delta);
        stage.draw();

        // ==========================================
        // LOGIKA 1: START SCREEN (TAP TO START)
        // ==========================================
        if (isStartScreen) {
            // Logika efek teks kedip-kedip
            blinkTimer += delta;
            float alpha = (float) Math.abs(Math.sin(blinkTimer * 2));

            batch.begin();
            font.setColor(1, 1, 1, alpha);
            GlyphLayout layout = new GlyphLayout(font, "TAP ANYWHERE TO START");
            float fontX = (Gdx.graphics.getWidth() - layout.width) / 2;
            float fontY = (Gdx.graphics.getHeight() / 2) - 100; // Posisi di bawah tengah
            font.draw(batch, layout, fontX, fontY);
            batch.end();

            // Jika user klik layar, mulai intro
            if (Gdx.input.justTouched()) {
                startIntroSequence();
            }
            return; // Stop di sini, jangan jalankan logika intro di bawah
        }

        // ==========================================
        // LOGIKA 2: INTRO CERITA SLIDESHOW
        // ==========================================
        if (isTransitioning) return;

        timer += delta;

        // Ganti gambar otomatis setelah 5.5 detik
        if (timer > 5.5f) {
            nextImage();
        }

        // Ganti gambar manual jika user klik layar (skip)
        if (Gdx.input.justTouched() && !isTransitioning) {
            nextImage();
        }
    }

    private void nextImage() {
        if (isTransitioning) return;
        isTransitioning = true;

        currentImage.addAction(Actions.sequence(
            Actions.fadeOut(1.2f),
            Actions.run(() -> {
                currentIndex++;
                isTransitioning = false;
                loadNextImage();
            })
        ));
    }

    private void fadeOutAndStartGame() {
        isTransitioning = true;

        // Siapkan perintah pindah screen
        Runnable switchScreen = () -> {
            // STOP MUSIK SEBELUM PINDAH
            if (introMusic != null && introMusic.isPlaying()) {
                introMusic.stop();
            }

            game.setScreen(new GameScreen(game));
            dispose(); // Bersihkan memori IntroScreen
        };

        if (currentImage != null) {
            currentImage.addAction(Actions.sequence(
                Actions.fadeOut(0.4f),
                Actions.run(switchScreen)
            ));
        } else {
            switchScreen.run();
        }
    }

    @Override
    public void resize(int width, int height) {
        stage.getViewport().update(width, height, true);
    }

    @Override public void show() {}
    @Override public void pause() {}
    @Override public void resume() {}
    @Override public void hide() {}

    @Override
    public void dispose() {
        stage.dispose();
        batch.dispose();
        font.dispose();

        // Jangan lupa dispose musik agar memori tidak bocor
        if (introMusic != null) {
            introMusic.dispose();
        }

        if (startBgTexture != null) startBgTexture.dispose();
        for (Texture t : textures) {
            if (t != null) t.dispose();
        }
    }
}
