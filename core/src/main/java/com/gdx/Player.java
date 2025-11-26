package com.gdx;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Rectangle;

/**
 * Player: movement, attack, and dash mechanics.
 * Comments focus on intent and tricky bits such as hitbox offsets and dash timing.
 */
public class Player {

    static final int MAX_HEALTH = 100;
    public float health = 100f;

    public float x, y;
    public float speed = 200f;
    public float gravity = -800f;
    public float velocityY = 0;
    public boolean isGround = true;

    public boolean facingRight = true;
    public boolean isAttack = false;
    public float attackTime = 0f;
    public boolean hitRegistered = false;

    public Rectangle hitbox;
    float scale = 0.2f;

    Animation<TextureRegion> walkAnim;
    Animation<TextureRegion> attackAnim;
    TextureRegion[] walkFrames;
    TextureRegion[] attackFrames;

    // DASH system (seconds)
    boolean isDashing = false;
    float dashTime = 0f;
    float dashDuration = 0.15f; // short burst
    float dashSpeed = 900f;
    float dashCooldown = 3f;    // 3 seconds cooldown as requested
    float dashCooldownTimer = 0f;

    float stateTime = 0f;

    public Player(float x, float y, Texture spriteSheet, Texture attackSheet) {
        this.x = x;
        this.y = y;

        // character sub-rectangle (sheet contains multiple characters)
        int charX = 750;
        int charY = 410;
        int charW = 585;
        int charH = 920;

        int frameCols = 7;
        int frameRows = 2;
        int frameWidth = spriteSheet.getWidth() / frameCols;
        int frameHeight = spriteSheet.getHeight() / frameRows;
        TextureRegion[][] tmp = TextureRegion.split(spriteSheet, frameWidth, frameHeight);

        walkFrames = new TextureRegion[7];
        attackFrames = new TextureRegion[7];
        for (int i = 0; i < 7; i++) {
            // extract consistent sub-rectangle for player visuals
            walkFrames[i] = new TextureRegion(tmp[1][i], charX, charY, charW, charH);
            attackFrames[i] = new TextureRegion(tmp[0][i], charX, charY, charW, charH);
        }

        walkAnim = new Animation<TextureRegion>(0.1f, walkFrames);
        attackAnim = new Animation<TextureRegion>(0.1f, attackFrames);

        // Hitbox uses a trimmed size relative to frame to match visuals
        hitbox = new Rectangle(x, y, walkFrames[0].getRegionWidth() * scale - 60, walkFrames[0].getRegionHeight() * scale - 30);
    }

    /**
     * Main update handles:
     * - movement inputs (A/D)
     * - jump (W)
     * - sprint (Shift)
     * - dash (Space) with cooldown timer in seconds (frame-rate independent)
     * - attack (mouse left button)
     */
    public void update(float delta) {
        stateTime += delta;
        boolean moving = false;

        // Horizontal movement input
        if (Gdx.input.isKeyPressed(Input.Keys.D)) { x += speed * delta; facingRight = true; moving = true;
        System.out.println(
            "Player X : " + x
        );}
        if (Gdx.input.isKeyPressed(Input.Keys.A)) { x -= speed * delta; facingRight = false; moving = true; }

        // Jump
        if (Gdx.input.isKeyPressed(Input.Keys.W) && isGround) { velocityY = 400; isGround = false; }

        // Sprint (temporary speed increase)
        if (Gdx.input.isKeyPressed(Input.Keys.SHIFT_LEFT)) speed = 550; else speed = 200;

        // Dash cooldown timer (counts down independently of frame rate)
        dashCooldownTimer -= delta;
        if (Gdx.input.isKeyJustPressed(Input.Keys.SPACE) && dashCooldownTimer <= 0 && !isDashing) {
            isDashing = true;
            dashTime = 0f;
            dashCooldownTimer = dashCooldown;
        }

        // Attack input
        if (Gdx.input.isButtonJustPressed(Input.Buttons.LEFT) && !isAttack) {
            isAttack = true;
            attackTime = 0f;
            hitRegistered = false; // allow next attack to register hits again
        }

        // Attack animation lifecycle
        if (isAttack) {
            attackTime += delta;
            if (attackAnim.isAnimationFinished(attackTime)) {
                isAttack = false;
                attackTime = 0f;
            }
        } else {
            hitRegistered = false;
        }

        // Dashing movement: short burst, blocks other movement during dash
        if (isDashing) {
            dashTime += delta;
            if (facingRight) x += dashSpeed * delta; else x -= dashSpeed * delta;
            if (dashTime >= dashDuration) isDashing = false;
            hitbox.setPosition(x + 20, y);
            return; // skip gravity & other movement while dashing
        }

        // Gravity and vertical movement
        velocityY += gravity * delta;
        y += velocityY * delta;
        if (y <= 0) { y = 0; velocityY = 0; isGround = true; }

        // Update hitbox position (offset so sprite anchor aligns)
        hitbox.setPosition(x + 20, y);
    }

    /**
     * Returns a TextureRegion ready to draw.
     * We copy the region to avoid mutating the shared frame array when flipping.
     */
    public TextureRegion getFrame() {
        TextureRegion rawFrame;
        TextureRegion idle = walkFrames[0];
        if (isAttack) rawFrame = attackAnim.getKeyFrame(attackTime, false);
        else if (Gdx.input.isKeyPressed(Input.Keys.A) || Gdx.input.isKeyPressed(Input.Keys.D)) rawFrame = walkAnim.getKeyFrame(stateTime, true);
        else rawFrame = idle;

        TextureRegion frame = new TextureRegion(rawFrame);
        if (facingRight && frame.isFlipX()) frame.flip(true, false);
        if (!facingRight && !frame.isFlipX()) frame.flip(true, false);
        return frame;
    }

    // Lightweight update used while dialog is active so animations still progress
    public void updateDialog(float delta) {
        stateTime += delta;
    }

    // Attack hitbox is a short rectangle in front of the player. Facing affects side.
    public Rectangle getAttackHitbox() {
        if (!isAttack) return null;
        float attackWidth = 60;
        float attackHeight = hitbox.height;
        if (facingRight) return new Rectangle(hitbox.x + hitbox.width, hitbox.y, attackWidth, attackHeight);
        else return new Rectangle(hitbox.x - attackWidth, hitbox.y, attackWidth, attackHeight);
    }

    // HUD helper: concise dash status for display
    public String getDashStatusString() {
        if (dashCooldownTimer <= 0f) return "DASH: READY";
        return String.format("DASH: cooldown %.1fs", dashCooldownTimer);
    }
}
