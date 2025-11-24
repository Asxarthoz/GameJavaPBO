package com.gdx;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Rectangle;

public class NPC {

    public enum State {
        ENTERING,   // datang dari kiri
        WAITING,    // berhenti untuk dialog
        LEAVING,    // balik ke kiri
        IDLE        // diam
    }

    public float x, y;
    public float speed = 100;

    public State state = State.IDLE;

    Animation<TextureRegion> walkAnim;
    TextureRegion idleFrame;     // cuma satu frame idle
    float stateTime = 0f;

    TextureRegion[] walkFrames;
    public Rectangle hitbox;

    float scale = 0.25f;
    float stopX = 100;   // posisi berhenti dialog

    public NPC(float x, float y, Texture npcSheet) {
        this.x = x;
        this.y = y;

        // Sprite 2 baris 7 kolom
        int cols = 7;
        int rows = 2;

        int frameW = npcSheet.getWidth() / cols;
        int frameH = npcSheet.getHeight() / rows;

        TextureRegion[][] tmp = TextureRegion.split(npcSheet, frameW, frameH);

        // Baris 1 (index 1) = walk
        walkFrames = new TextureRegion[cols];
        for (int i = 0; i < cols; i++) {
            walkFrames[i] = tmp[1][i]; // row 1 = walk
        }

        // Baris 0 frame 0 = idle
        idleFrame = tmp[0][0];

        walkAnim = new Animation<TextureRegion>(0.12f, walkFrames);

        hitbox = new Rectangle(
            x, y,
            frameW * scale,
            frameH * scale
        );
    }

    public void startEnter() {
        state = State.ENTERING;
    }

    public void startLeave() {
        state = State.LEAVING;
    }

    public void update(float delta) {
        stateTime += delta;

        switch (state) {

            case ENTERING:
                x += speed * delta;
                if (x >= stopX) {
                    x = stopX;
                    state = State.WAITING;
                }
                break;

            case WAITING:
                // NPC diam di tempat untuk dialog
                break;

            case LEAVING:

                x -= speed * delta;
                if (x <= -250) {
                    x = -250;
                    state = State.IDLE;
                }
                break;

            case IDLE:
                // NPC diem
                break;
        }

        hitbox.setPosition(x, y);
    }

    public TextureRegion getFrame() {

        TextureRegion frame;

        if (state == State.ENTERING || state == State.LEAVING) {
            frame = walkAnim.getKeyFrame(stateTime, true);
        } else {
            frame = idleFrame;
        }

        // --- Buat salinan supaya flip tidak merusak frame asli ---
        TextureRegion result = new TextureRegion(frame);

        // ENTERING = jalan ke kanan → pastikan tidak flip
        if (state == State.ENTERING) {
            if (result.isFlipX()) result.flip(true, false);
        }

        // LEAVING = jalan ke kiri → pastikan flip X
        if (state == State.LEAVING) {
            if (!result.isFlipX()) result.flip(true, false);
        }

        return result;
    }

}
