package com.gdx;

import com.badlogic.gdx.Game;

public class Main extends Game {
    @Override
    public void create() {
        // Langsung panggil IntroScreen
        // IntroScreen sekarang akan menangani Start Image dulu, baru Intro Cerita
        setScreen(new IntroScreen(this));
    }
}
