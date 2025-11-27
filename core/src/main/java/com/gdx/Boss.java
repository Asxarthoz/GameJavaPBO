package com.gdx;

import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.utils.Array;

class Bomb {
    public Rectangle rect;
    public TextureRegion sprite;

    public boolean exploded = false;

    public float velX;
    public float velY = -200;  // jatuh ke bawah

    public Bomb(float x, float y, float targetX, TextureRegion tex) {
        rect = new Rectangle(x, y, 50, 50);
        sprite = tex;

        // Tentukan kecepatan horizontal (lempar)
        float speedX = 200;
        velX = (targetX > x) ? speedX : -speedX;
    }

    public void update(float delta) {
        rect.x += velX * delta;
        rect.y += velY * delta;

        // Jika sudah jatuh ke tanah (y <= 0)
        if (rect.y <= 0) {
            rect.y = 0;
            exploded = true;   // meledak di tanah
        }
    }
}


class Explosion {
    public boolean hasDamaged = false;
    public float x, y;
    public float life = 0.3f;
    public float radius = 200;

    float stateTime = 0f;
    Animation<TextureRegion> anim;

    public Explosion(float x, float y, Animation<TextureRegion> anim) {
        this.x = x;
        this.y = y;
        this.anim = anim;
    }

    public void update(float delta) {
        stateTime += delta;
        life -= delta;
    }

    public TextureRegion getFrame() {
        return anim.getKeyFrame(stateTime, false);
    }
}



class BossBullet {

    public Rectangle rect;
    public float velX, velY;
    public TextureRegion sprite;

    public BossBullet(float x, float y, float vx, float vy, TextureRegion tex) {
        rect = new Rectangle(x, y, 40, 40);
        velX = vx;
        velY = vy;
        sprite = tex;
    }

    public void update(float delta) {
        rect.x += velX * delta;
        rect.y += velY * delta;
    }
}

public class Boss {

    //ini buat bomb system
    // Bomb system
    float bombTimer = 0f;
    float bombInterval = 6f;

    TextureRegion bombTex;
    TextureRegion explosionTex;
    //======================

    public enum State { IDLE, RUN, ROLL, ATTACK }

    public float x, y;
    public boolean facingRight = true;

    public float hp = 300;
    public float maxHp = 300;

    public float speed = 120;
    public float rollSpeed = 600;

    Sound shootSound;

    // Roll system
    boolean rolling = false;
    float rollDirection = 1;
    float rollDistanceLeft = 0;

    float rollCooldown = 0f;
    float rollCooldownMax = 5f;

    // Shooting
    float shootTimer = 0f;
    float shootInterval = 0.9f;   // rate tembak
    TextureRegion bulletTex;

    // Animations
    TextureRegion idleFrame;
    Animation<TextureRegion> runAnim;
    Animation<TextureRegion> rollAnim;
    Animation<TextureRegion> explosionAnim;


    float stateTime = 0f;
    public State state = State.IDLE;

    public Rectangle hitbox;

    public boolean active = false;  // HANYA aktif setelah kill 20


    public Boss(float x, float y,
                Texture idleTex,
                Texture runSheet,
                Texture rollSheet,
                Texture bossBulletTex,
                float scale, Sound sound)
    {
        this.x = x;
        this.y = y;
        this.shootSound = sound;

        bulletTex = new TextureRegion(bossBulletTex);
        idleFrame = new TextureRegion(idleTex);
        bombTex = new TextureRegion(new Texture("bomb.png"));
        Texture expSheet = new Texture("bombEx.png");
        TextureRegion[][] expSplit = TextureRegion.split(
            expSheet,
            expSheet.getWidth() / 3,   // 3 kolom
            expSheet.getHeight()      // 1 baris
        );

        TextureRegion[] expFrames = new TextureRegion[3];
        for (int i = 0; i < 3; i++) expFrames[i] = expSplit[0][i];

        explosionAnim = new Animation<>(0.1f, expFrames); // speed anim


        // Run (4 frame)
        TextureRegion[][] runSplit =
            TextureRegion.split(runSheet, runSheet.getWidth() / 4, runSheet.getHeight());
        TextureRegion[] runFrames = new TextureRegion[4];
        for (int i = 0; i < 4; i++) runFrames[i] = runSplit[0][i];
        runAnim = new Animation<>(0.12f, runFrames);

        // Roll (8 frame)
        TextureRegion[][] rollSplit =
            TextureRegion.split(rollSheet, rollSheet.getWidth() / 8, rollSheet.getHeight());
        TextureRegion[] rollFrames = new TextureRegion[8];
        for (int i = 0; i < 8; i++) rollFrames[i] = rollSplit[0][i];
        rollAnim = new Animation<>(0.08f, rollFrames);

        // Hitbox
        hitbox = new Rectangle(
            x, y,
            idleTex.getWidth() * 1,
            idleTex.getHeight() * 2
        );
    }


    // ==============================================================
    //                        UPDATE BOSS
    // ==============================================================

    public void update(float delta, Rectangle playerRect,
                       Array<BossBullet> bullets,
                       Array<Bomb> bombs) {

        if (!active) return;  // TIDAK update sampai kill >= 20
        if (active && x > 5000) {
            state = State.RUN;
            x -= speed * delta;     // jalan ke kiri
            hitbox.setPosition(x, y);
            return;
        }


        stateTime += delta;
        shootTimer += delta;

        // Spawn bomb tiap 3 detik

        bombTimer += delta;
        if (bombTimer >= bombInterval) {
            bombTimer = 0;
            float throwX = x + (facingRight ? 80 : -20);
            float targetX = playerRect.x + playerRect.width / 2;

            bombs.add(new Bomb(throwX, y + 100, targetX, bombTex));

        }



        if (rollCooldown > 0) rollCooldown -= delta;

        float playerCenter = playerRect.x + playerRect.width / 2;
        float bossCenter = x + hitbox.width / 2;
        float dist = Math.abs(playerCenter - bossCenter);

        updateFacing(playerCenter);

        // =============================================================
        // 1. IF ROLLING → lakukan roll saja
        // =============================================================
        if (rolling) {
            doRoll(delta);
            return;
        }

        // =============================================================
        // 2. RANGE DETECTION MODE
        // =============================================================

        // A. Jika jarak <= 100 → roll (dan cooldown ready)
        if (dist <= 100 && rollCooldown <= 0) {
            startRoll(playerCenter);
            return;
        }

        // B. Jika jarak <= 500 → boss berhenti & nembak (standing idle)
        if (dist <= 500) {
            state = State.IDLE;
            if (shootTimer >= shootInterval) {
                shootTimer = 0f;
                bullets.add(shoot());
            }
            hitbox.setPosition(x, y);
            return;
        }

        // C. Jika jarak <= 800 → boss boleh menembak sambil diam
        if (dist <= 800) {
            state = State.ATTACK;
            if (shootTimer >= shootInterval) {
                shootTimer = 0f;
                bullets.add(shoot());
            }
            hitbox.setPosition(x, y);
            return;
        }

        // =============================================================
        // 3.FAR → kejar player
        // =============================================================
        chasePlayer(playerCenter, delta);
        hitbox.setPosition(x, y);
    }


    // ==============================================================
    //                        MOVE & ROLL
    // ==============================================================

    private void updateFacing(float targetCenter) {
        facingRight = targetCenter > x;
    }

    private void chasePlayer(float targetX, float delta) {
        state = State.RUN;

        if (targetX > x) x += speed * delta;
        else x -= speed * delta;
    }

    private void startRoll(float targetCenter) {
        rolling = true;
        state = State.ROLL;

        rollDirection = (targetCenter > x) ? 1 : -1;
        rollDistanceLeft = 300;

        rollCooldown = rollCooldownMax; // reset cooldown 5 sec
    }

    private void doRoll(float delta) {
        float move = rollSpeed * delta;

        if (move > rollDistanceLeft)
            move = rollDistanceLeft;

        x += move * rollDirection;
        rollDistanceLeft -= move;

        if (rollDistanceLeft <= 0) {
            rolling = false;
            state = State.IDLE;
        }

        hitbox.setPosition(x, y);
    }


    // ==============================================================
    //                        SHOOT
    // ==============================================================

    private BossBullet shoot() {

        float bx = x + (facingRight ? 70 : -20);
        float by = y + hitbox.height * 0.45f;

        float vel = facingRight ? 350 : -350;
        shootSound.play();
        return new BossBullet(bx, by, vel, 0, bulletTex);
    }



    // ==============================================================
    //                        GET FRAME
    // ==============================================================

    public TextureRegion getFrame() {

        TextureRegion frame;

        switch (state) {
            case ROLL: frame = rollAnim.getKeyFrame(stateTime, true); break;
            case RUN:  frame = runAnim.getKeyFrame(stateTime, true);  break;
            default:   frame = idleFrame; break;
        }

        // Flip
        if (facingRight && frame.isFlipX()) frame.flip(true, false);
        if (!facingRight && !frame.isFlipX()) frame.flip(true, false);

        return frame;
    }
}
