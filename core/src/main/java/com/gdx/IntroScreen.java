package com.gdx;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.viewport.ScreenViewport;

public class IntroScreen implements Screen {
    private final Main game;
    private Stage stage;


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
        loadNextImage();
    }

    private void loadNextImage() {
        if (currentIndex >= introImages.length) {
            // Semua selesai → masuk game dengan animasi fade out keren
            fadeOutAndStartGame();
            return;
        }

        Texture tex = new Texture(introImages[currentIndex]);
        textures.add(tex);

        currentImage = new Image(tex);
        currentImage.setFillParent(true);
        currentImage.getColor().a = 0f;
        currentImage.addAction(Actions.fadeIn(1.2f));

        stage.clear();
        stage.addActor(currentImage);

        timer = 0f; // reset timer untuk gambar ini
    }

    @Override
    public void render(float delta) {
        Gdx.gl.glClearColor(0, 0, 0, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        stage.act(delta);
        stage.draw();

        if (isTransitioning) return;

        timer += delta;

        // Tiap gambar tampil 5.5 detik (bisa diubah)
        if (timer > 2.5f) {
            nextImage();
        }

        // Klik / sentuh = langsung ke gambar berikutnya
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
        currentImage.addAction(Actions.sequence(
            Actions.fadeOut(0.4f),
            Actions.run(() -> {
                game.setScreen(new GameScreen(game));
                dispose();
            })
        ));
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
        for (Texture t : textures) {
            if (t != null) t.dispose();
        }
    }
}
