package com.gdx;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.scenes.scene2d.Actor;

/**
 * NPC handles simple enter/wait/leave animations and exposes a frame for rendering.
 * Comments explain state intent and frame extraction logic.
 */
public class NPC extends Actor {

    public enum State {
        ENTERING,
        WAITING,
        LEAVING,
        IDLE
    }

    public State state = State.IDLE;
    Animation<TextureRegion> walkAnim;
    TextureRegion idleFrame;
    TextureRegion[] walkFrames;
    public Rectangle hitbox;
    public float speed = 100;
    float stateTime = 0f;
    float scale = 0.25f;
    float stopX = 100; // where NPC stops for dialog

    public NPC(float x, float y, Texture npcSheet) {
        setPosition(x, y);

        // sprite sheet layout: 2 rows x 7 cols
        int cols = 7;
        int rows = 2;
        int frameW = npcSheet.getWidth() / cols;
        int frameH = npcSheet.getHeight() / rows;
        TextureRegion[][] tmp = TextureRegion.split(npcSheet, frameW, frameH);

        // row 1 = walk frames, row 0 frame 0 = idle
        walkFrames = new TextureRegion[cols];
        for (int i = 0; i < cols; i++) walkFrames[i] = tmp[1][i];
        idleFrame = tmp[0][0];

        walkAnim = new Animation<TextureRegion>(0.12f, walkFrames);

        // hitbox sized from raw frame dims and scale
        hitbox = new Rectangle(getX(), getY(),  frameW * scale, frameH * scale);
    }

    public void startEnter() { state = State.ENTERING; stateTime = 0f; }
    public void startLeave() { state = State.LEAVING; stateTime = 0f; }

    @Override
    public void act(float delta) {
        super.act(delta);
        stateTime += delta;
        switch (state) {
            case ENTERING:
                moveBy(speed * delta, 0);
                if (getX() >= stopX) { setX(stopX); state = State.WAITING;}
                break;
            case WAITING:
                break;
            case LEAVING:
                moveBy(-speed * delta, 0);
                if (getX() <= -300) { setX(-300); state = State.IDLE;}
                break;
            case IDLE:
                break;
        }
        hitbox.setPosition(getX(), getY());
    }

    @Override
    public void draw(Batch batch, float parentAlpha) {
        TextureRegion frame;
        if (state == state.ENTERING || state == State.LEAVING) {
            frame = walkAnim.getKeyFrame(stateTime, true);
        } else {
            frame = idleFrame;
        }
        TextureRegion r = new TextureRegion(frame);
        if (state == state.ENTERING) {
            if (r.isFlipX()) r.flip(true, false);
        }
        if (state == State.LEAVING) {
            if (!r.isFlipX()) r.flip(true, false);
        }
        batch.draw(r, getX(), getY(), r.getRegionWidth() * scale, r.getRegionHeight() * scale);
    }

    public Rectangle getHitbox() { return hitbox; }
}
