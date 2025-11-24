package com.gdx;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Rectangle;

public class Player {

    static final int MAX_HEALTH = 100;
    float health = 100f;
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

    // DASH SYSTEM
    boolean isDashing = false;
    float dashTime = 0f;
    float dashDuration = 0.15f;   // waktu dash (singkat)
    float dashSpeed = 900f;       // kecepatan dash
    float dashCooldown = 5f;
    float dashCooldownTimer = 0f;


    float stateTime = 0;

    public Player(float x, float y, Texture spriteSheet, Texture attackSheet) {
        this.x = x;
        this.y = y;
        int charX = 750;
        int charY = 410;
        int charW = 585;
        int charH = 920;

        int frameCols = 7;
        int frameRows = 2;

        int frameWidth  = spriteSheet.getWidth() / frameCols;
        int frameHeight = spriteSheet.getHeight() / frameRows;

        TextureRegion[][] tmp = TextureRegion.split(spriteSheet, frameWidth, frameHeight);

        walkFrames = new TextureRegion[7];
        for (int i = 0; i < 7; i++) {
            walkFrames[i] = new TextureRegion(
                tmp[1][i],   // frame asli
                charX,       // posisi karakter di dalam frame
                charY,
                charW,
                charH
            );

        }

        attackFrames = new TextureRegion[7];
        for (int i = 0; i < 7; i++) {
            attackFrames[i] = new TextureRegion(tmp[0][i], charX, charY, charW, charH);
        }

        walkAnim = new Animation<TextureRegion>(0.1f, walkFrames);
        attackAnim = new Animation<TextureRegion>(0.1f, attackFrames);

        // Hitbox (gunakan frame walk pertama)
        hitbox = new Rectangle(
            x,
            y,
            walkFrames[0].getRegionWidth() * scale - 60,
            walkFrames[0].getRegionHeight() * scale - 30
        );
    }

    public void update(float delta) {
        stateTime += delta;

        boolean moving = false;

        // --- CONTROL ---
        if(Gdx.input.isKeyPressed(Input.Keys.D)) {
            x += speed * delta;
            facingRight = true;
            moving = true;
        }
        if(Gdx.input.isKeyPressed(Input.Keys.A)) {
            x -= speed * delta;
            facingRight = false;
            moving = true;
        }

        if(Gdx.input.isKeyPressed(Input.Keys.W) && isGround) {
            velocityY = 400;
            isGround = false;
        }

        if(Gdx.input.isKeyPressed(Input.Keys.SHIFT_LEFT)) speed = 550;
        else speed = 200;

        dashCooldownTimer -= delta;

        if (Gdx.input.isKeyJustPressed(Input.Keys.SPACE) && dashCooldownTimer <= 0 && !isDashing) {
            isDashing = true;
            dashTime = 0;
            dashCooldownTimer = dashCooldown;
        }

        if(Gdx.input.isButtonJustPressed(Input.Buttons.LEFT) && !isAttack) {
            isAttack = true;
            attackTime = 0;
            hitRegistered = false;

        }

        // --- ATTACK ANIMATION ---
        if (isAttack) {
            attackTime += delta;
            if (attackAnim.isAnimationFinished(attackTime)) {
                isAttack = false;
                attackTime = 0;
            }
        }

        if (!isAttack) {
            hitRegistered = false;
        }


        if (isDashing) {
            dashTime += delta;

            // arah dash sesuai facing
            if (facingRight) x += dashSpeed * delta;
            else x -= dashSpeed * delta;

            // dash selesai?
            if (dashTime >= dashDuration) {
                isDashing = false;
            }

            // saat dash, jangan melakukan hal lain (jalan, lompat, dll)
            hitbox.setPosition(x + 20, y);
            return;
        }


        // --- GRAVITY ---
        velocityY += gravity * delta;
        y += velocityY * delta;

        if (y <= 0) {
            y = 0;
            velocityY = 0;
            isGround = true;
        }

        // Update hitbox
        hitbox.setPosition(x + 20, y);
    }

    public TextureRegion getFrame() {

        TextureRegion rawFrame;

        // idle frame (diam)
        TextureRegion idle = walkFrames[0];

        if (isAttack) {
            rawFrame = attackAnim.getKeyFrame(attackTime, false);
        }
        else if (Gdx.input.isKeyPressed(Input.Keys.A) ||
            Gdx.input.isKeyPressed(Input.Keys.D)) {

            rawFrame = walkAnim.getKeyFrame(stateTime, true);
        }
        else {
            rawFrame = idle;  // player diam → idle frame
        }

        // --- Buat salinan supaya flip tidak merusak frame asli ---
        TextureRegion frame = new TextureRegion(rawFrame);


        // selalu pastikan orientasi benar
        if (facingRight && frame.isFlipX()) frame.flip(true, false);
        if (!facingRight && !frame.isFlipX()) frame.flip(true, false);

        return frame;
    }

    public void updateDialog(float delta) {
        stateTime += delta;
        // supaya animasi idle/jalan tetap hidup
    }

    public Rectangle getAttackHitbox() {
        if (!isAttack) return null;

        // Posisi serangan ada di depan player
        float attackWidth = 60;
        float attackHeight = hitbox.height;

        if (facingRight) {
            return new Rectangle(hitbox.x + hitbox.width, hitbox.y, attackWidth, attackHeight);
        } else {
            return new Rectangle(hitbox.x - attackWidth, hitbox.y, attackWidth, attackHeight);
        }
    }



}

