package com.gdx;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.viewport.ScreenViewport;

public class EndScreen implements Screen {
    private final Main game;
    private Stage stage;

    // --- Audio ---
    private Music winMusic;

    // --- Assets untuk End Screen Cerita ---
    private final String[] winImages = {
        "win1.png",
        "win2.png",
        "win3.png",
        "win4.png",
        "win5.png"
    };

    private Array<Texture> textures = new Array<>();
    private Image currentImage;
    private int currentIndex = 0;
    private float timer = 0f;
    private boolean isTransitioning = false;

    public EndScreen(Main game) {
        this.game = game;
        stage = new Stage(new ScreenViewport());

        // --- SETUP AUDIO ---
        try {
            winMusic = Gdx.audio.newMusic(Gdx.files.internal("sound/win.ogg"));
            winMusic.setLooping(false); // Musik menang biasanya sekali putar atau loop, terserah
            winMusic.setVolume(1.0f);
            winMusic.play();
        } catch (Exception e) {
            System.out.println("Error memuat musik win: " + e.getMessage());
        }

        // Langsung mulai menampilkan gambar pertama
        loadNextImage();
    }

    /**
     * Memuat gambar cerita kemenangan berikutnya dari array winImages.
     */
    private void loadNextImage() {
        if (currentIndex >= winImages.length) {
            // Jika gambar habis, kembali ke IntroScreen (Menu Awal)
            backToIntro();
            return;
        }

        Texture tex = new Texture(winImages[currentIndex]);
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
        // LOGIKA SLIDESHOW
        // ==========================================
        if (isTransitioning) return;

        timer += delta;

        // Ganti gambar otomatis setelah 4 detik (biar tidak terlalu lama)
        if (timer > 4.0f) {
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
            Actions.fadeOut(1.0f),
            Actions.run(() -> {
                currentIndex++;
                isTransitioning = false;
                loadNextImage();
            })
        ));
    }

    private void backToIntro() {
        isTransitioning = true;

        // Siapkan perintah pindah screen
        Runnable switchScreen = () -> {
            // STOP MUSIK SEBELUM PINDAH
            if (winMusic != null && winMusic.isPlaying()) {
                winMusic.stop();
            }

            // Kembali ke Menu Awal (IntroScreen)
            // Pastikan Anda sudah punya IntroScreen yang siap menerima parameter game
            game.setScreen(new IntroScreen(game));
            dispose(); // Bersihkan memori EndScreen
        };

        if (currentImage != null) {
            currentImage.addAction(Actions.sequence(
                Actions.fadeOut(0.5f),
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

        // Dispose musik
        if (winMusic != null) {
            winMusic.dispose();
        }

        // Dispose semua texture yang dimuat
        for (Texture t : textures) {
            if (t != null) t.dispose();
        }
    }
}
