package com.gdx;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Rectangle;

/**
 * NPC handles simple enter/wait/leave animations and exposes a frame for rendering.
 * Comments explain state intent and frame extraction logic.
 */
public class NPC {

    public enum State {
        ENTERING,
        WAITING,
        LEAVING,
        IDLE
    }

    public float x, y;
    public float speed = 100;
    public State state = State.IDLE;

    Animation<TextureRegion> walkAnim;
    TextureRegion idleFrame;
    TextureRegion[] walkFrames;
    public Rectangle hitbox;

    float stateTime = 0f;
    float scale = 0.25f;
    float stopX = 100; // where NPC stops for dialog

    public NPC(float x, float y, Texture npcSheet) {
        this.x = x;
        this.y = y;

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
        hitbox = new Rectangle(x, y, frameW * scale, frameH * scale);
    }

    public void startEnter() { state = State.ENTERING; stateTime = 0f; }
    public void startLeave() { state = State.LEAVING; stateTime = 0f; }

    public void update(float delta) {
        stateTime += delta;
        switch (state) {
            case ENTERING:
                x += speed * delta;
                if (x >= stopX) { x = stopX; state = State.WAITING; }
                break;
            case WAITING:
                // remain for dialog
                break;
            case LEAVING:
                x -= speed * delta;
                if (x <= -250) { x = -250; state = State.IDLE; }
                break;
            case IDLE:
                break;
        }
        hitbox.setPosition(x, y);
    }

    /**
     * Returns a cleaned TextureRegion (copy) so flipping won't mutate original frames.
     * Ensures orientation matches movement state.
     */
    public TextureRegion getFrame() {
        TextureRegion frame;
        if (state == State.ENTERING || state == State.LEAVING) frame = walkAnim.getKeyFrame(stateTime, true);
        else frame = idleFrame;

        TextureRegion result = new TextureRegion(frame);
        if (state == State.ENTERING) { if (result.isFlipX()) result.flip(true, false); }
        if (state == State.LEAVING)  { if (!result.isFlipX()) result.flip(true, false); }
        return result;
    }
}
