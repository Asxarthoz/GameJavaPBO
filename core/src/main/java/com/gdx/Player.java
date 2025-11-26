package com.gdx;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Rectangle;

/**
 * Player: movement, attack, shield, dash, and heal mechanics.
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

    // --- SHIELD VARIABLES ---
    public boolean isShielding = false;

    // --- HEAL VARIABLES ---
    public float healTimer = 0f;          // Hitung mundur waktu heal
    public float healCooldown = 5f;       // Cooldown 5 detik

    public Rectangle hitbox;
    float scale = 0.2f;

    Animation<TextureRegion> walkAnim;
    Animation<TextureRegion> attackAnim;
    TextureRegion[] walkFrames;
    TextureRegion[] attackFrames;

    // DASH system
    boolean isDashing = false;
    float dashTime = 0f;
    float dashDuration = 0.15f;
    float dashSpeed = 900f;
    float dashCooldown = 3f;
    float dashCooldownTimer = 0f;

    float stateTime = 0f;

    public Player(float x, float y, Texture spriteSheet, Texture attackSheet) {
        this.x = x;
        this.y = y;

        // character sub-rectangle
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
            walkFrames[i] = new TextureRegion(tmp[1][i], charX, charY, charW, charH);
            attackFrames[i] = new TextureRegion(tmp[0][i], charX, charY, charW, charH);
        }

        walkAnim = new Animation<TextureRegion>(0.1f, walkFrames);
        attackAnim = new Animation<TextureRegion>(0.1f, attackFrames);

        hitbox = new Rectangle(x, y, walkFrames[0].getRegionWidth() * scale - 60, walkFrames[0].getRegionHeight() * scale - 30);
    }

    public void update(float delta) {
        stateTime += delta;

        // --- HEAL LOGIC (TOMBOL H) ---
        // Kurangi timer setiap frame
        if (healTimer > 0) {
            healTimer -= delta;
        }

        // Jika tekan H, timer habis, dan darah belum penuh
        if (Gdx.input.isKeyJustPressed(Input.Keys.H)) {
            if (healTimer <= 0 && health < MAX_HEALTH) {
                health += 30;
                if (health > MAX_HEALTH) health = MAX_HEALTH; // Mentok di 100
                healTimer = healCooldown; // Set cooldown 5 detik
                System.out.println("Healed! Current HP: " + health);
            }
        }
        // -----------------------------

        // --- SHIELD LOGIC ---
        if (Gdx.input.isButtonPressed(Input.Buttons.RIGHT)) {
            isShielding = true;
        } else {
            isShielding = false;
            if (Gdx.input.isKeyPressed(Input.Keys.SHIFT_LEFT)) speed = 550; else speed = 200;
        }

        // Movement
        if (Gdx.input.isKeyPressed(Input.Keys.D)) {
            x += speed * delta;
            facingRight = true;
        }
        if (Gdx.input.isKeyPressed(Input.Keys.A)) {
            x -= speed * delta;
            facingRight = false;
        }

        // Jump
        if (Gdx.input.isKeyPressed(Input.Keys.W) && isGround) { velocityY = 400; isGround = false; }

        // Dash cooldown
        dashCooldownTimer -= delta;
        if (Gdx.input.isKeyJustPressed(Input.Keys.SPACE) && dashCooldownTimer <= 0 && !isDashing) {
            isDashing = true;
            dashTime = 0f;
            dashCooldownTimer = dashCooldown;
        }

        // Attack input
        if (Gdx.input.isButtonJustPressed(Input.Buttons.LEFT) && !isAttack && !isShielding) {
            isAttack = true;
            attackTime = 0f;
            hitRegistered = false;
        }

        // Attack animation logic
        if (isAttack) {
            attackTime += delta;
            if (attackAnim.isAnimationFinished(attackTime)) {
                isAttack = false;
                attackTime = 0f;
            }
        } else {
            hitRegistered = false;
        }

        // Dash execution
        if (isDashing) {
            dashTime += delta;
            if (facingRight) x += dashSpeed * delta; else x -= dashSpeed * delta;
            if (dashTime >= dashDuration) isDashing = false;
            hitbox.setPosition(x + 20, y);
            return;
        }

        // Gravity
        velocityY += gravity * delta;
        y += velocityY * delta;
        if (y <= 0) { y = 0; velocityY = 0; isGround = true; }

        // Update hitbox
        hitbox.setPosition(x + 20, y);
    }

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

    public void updateDialog(float delta) {
        stateTime += delta;
    }

    public Rectangle getAttackHitbox() {
        if (!isAttack) return null;
        float attackWidth = 60;
        float attackHeight = hitbox.height;
        if (facingRight) return new Rectangle(hitbox.x + hitbox.width, hitbox.y, attackWidth, attackHeight);
        else return new Rectangle(hitbox.x - attackWidth, hitbox.y, attackWidth, attackHeight);
    }

    public String getDashStatusString() {
        if (dashCooldownTimer <= 0f) return "DASH: READY";
        return String.format("DASH: cooldown %.1fs", dashCooldownTimer);
    }
}
