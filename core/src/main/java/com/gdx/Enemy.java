package com.gdx;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.utils.TimeUtils;



class Bullet {
    Rectangle rect;
    float velX, velY;
    float rotation;
    float offsetX;

    public Bullet(float x, float y, float width, float height, float vX, float vY) {
        this.rect = new Rectangle(x, y, width, height);
        this.velX = vX;
        this.velY = vY;
    }
}

public class Enemy {

    public long lastShootTime = 0;
    float damageCooldown = 0f;


    public boolean dead = false;
    float deathTime = 0f;
    public boolean canShoot = true;
    public float rotation = 0f;


    public float hp = 100;
    public float maxHp = 100;

    float x, y;
    float speed = 100;

    boolean facingRight = true;

    TextureRegion sprite;
    public Rectangle hitbox;

    // --- Patrol Data ---
    float patrolTargetX;
    float patrolMinMove = 100;
    float patrolMaxMove = 300;

    // --- Chase Delay ---
    float detectDelay = 2f;      // BERHENTI 2 DETIK setelah lihat player
    float delayTimer = 0f;
    boolean detectedPlayer = false;

    public Enemy(float x, float y, Texture enemyTexture, float scale) {
        this.x = x;
        this.y = y;

        sprite = new TextureRegion(enemyTexture);

        hitbox = new Rectangle(
            x, y,
            sprite.getRegionWidth() * scale,
            sprite.getRegionHeight() * scale
        );

        pickNewPatrolTarget();
    }

    private void pickNewPatrolTarget() {
        float move = MathUtils.random(patrolMinMove, patrolMaxMove);

        if (MathUtils.randomBoolean()) {
            patrolTargetX = x + move;
            facingRight = true;
        } else {
            patrolTargetX = x - move;
            facingRight = false;
        }
    }

    /** Update AI */
    public void update(float delta, Rectangle playerRect) {

        float centerEnemy = x + hitbox.width / 2;
        float centerPlayer = playerRect.x + playerRect.width / 2;
        float dist = Math.abs(centerEnemy - centerPlayer);

        boolean playerInRange = dist <= 450;

        if (dead) {
            deathTime += delta;
            return; // berhenti bergerak
        }

        // ----------- DETEKSI PLAYER & DELAY -----------
        if (playerInRange) {
            detectedPlayer = true;
        }

        if (detectedPlayer) {

            delayTimer += delta;

            // selama delay 2 detik → DIAM
            if (delayTimer < detectDelay) {
                hitbox.setPosition(x, y);
                return;
            }

            // setelah delay → mulai chase
            chasePlayer(playerRect, delta);

        } else {
            // kalau belum mendeteksi player → PATROL
            patrol(delta);
        }

        // kurangi cooldown tiap frame
        if (damageCooldown > 0) {
            damageCooldown -= delta;
        }


        // update sprite flip
        if (facingRight && sprite.isFlipX()) sprite.flip(true, false);
        if (!facingRight && !sprite.isFlipX()) sprite.flip(true, false);

        hitbox.setPosition(x, y);
    }

    private void chasePlayer(Rectangle playerRect, float delta) {

        float centerEnemy = x + hitbox.width / 2;
        float centerPlayer = playerRect.x + playerRect.width / 2;

        float distance = Math.abs(centerEnemy - centerPlayer);

        // --- Jika sudah terlalu dekat → berhenti ---
        if (distance <= 300f) {
            return; // tidak bergerak
        }

        // --- Jika masih jauh → kejar ---
        if (centerPlayer > centerEnemy) {
            x += speed * delta;
            facingRight = true;
        } else {
            x -= speed * delta;
            facingRight = false;
        }
    }


    private void patrol(float delta) {

        if (facingRight) {
            x += speed * delta;

            if (x >= patrolTargetX)
                pickNewPatrolTarget();
        } else {
            x -= speed * delta;

            if (x <= patrolTargetX)
                pickNewPatrolTarget();
        }
    }

    public TextureRegion getSprite() {
        return sprite;
    }

    public boolean canShoot() {
        return detectedPlayer &&
            TimeUtils.nanoTime() - lastShootTime > 2_000_000_000L;
    }

    public void takeDamage(float dmg) {
        if (dead) return;

        if (damageCooldown > 0) return;  // masih cooldown, tidak kena hit
        damageCooldown = 0.35f;

        hp -= dmg;

        if (hp <= 0) {
            dead = true;
            hp = 0;

            // Hentikan gerakan & tembakan
            canShoot = false;
            speed = 0;

            // Rotate enemy jadi jatuh
            rotation = 90f;

            // Turunin sedikit biar keliatan jatuh
            y -= 90;

            deathTime = 0;
            hitbox.setPosition(x, y);
        }
    }


}

