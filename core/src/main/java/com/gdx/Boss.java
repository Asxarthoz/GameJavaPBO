package com.gdx;

import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.utils.Array;

class Bomb {
    public Rectangle rect;
    public TextureRegion sprite;
    public boolean exploded = false;
    public float velX;
    public float velY = -200f;  // jatuh ke bawah

    public Bomb(float x, float y, float targetX, TextureRegion tex) {
        rect = new Rectangle(x, y, 50, 50);
        sprite = tex;
        float speedX = 200f;
        velX = (targetX > x) ? speedX : -speedX;
    }

    public void update(float delta) {
        rect.x += velX * delta;
        rect.y += velY * delta;
        if (rect.y <= 0) {
            rect.y = 0;
            exploded = true;
        }
    }

    public void draw(Batch batch) {
        if (sprite != null) batch.draw(sprite, rect.x, rect.y, rect.width, rect.height);
    }
}

class Explosion {
    public boolean hasDamaged = false;
    public float x, y;
    public float life = 0.3f;
    public float radius = 100f;

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

public class Boss extends Actor {
    public float hp = 500f;
    public float maxHp = 500f;

    public boolean active = false;
    public Rectangle hitbox;

    public enum State { IDLE, RUN, ROLL }
    public State state = State.IDLE;

    TextureRegion idleFrame;
    Animation<TextureRegion> runAnim;
    Animation<TextureRegion> rollAnim;

    private float stateTime = 0f;
    private float speed = 150f;
    private boolean facingRight = true;

    // Bomb & Explosion
    private float bombCooldown = 0f;
    private float bombCooldownMax = 5f;
    private TextureRegion bombTex;
    private Animation<TextureRegion> explosionAnim;

    // Roll mechanics
    private float rollCooldown = 0f;
    private float rollCooldownMax = 12f;
    private float rollSpeed = 450f; // tweak supaya terasa dash
    private boolean rolling = false;
    private int rollDirection = 1;
    private float rollDistanceLeft = 0f;
    private boolean rollHasDamaged = false;

    // Shoot mechanics
    private TextureRegion bulletTex;
    private Sound shootSound;

    // Injected arrays from Main
    private Array<Bullet> bullets;
    private Array<Bomb> bombs;
    private float shootCooldown = 0f;
    private float shootCooldownMax = 2.8f;
    private int burstCount = 0;
    private float burstTimer = 0f;
    private float burstInterval = 0.18f;
    public float dmgCooldown = 0f;

    public void takeDamage(float dmg) {
        if (dmgCooldown > 0) return;
        hp -= dmg;
        dmgCooldown = 0.3f;
    }


    public Boss(float x, float y, Texture idleTex, Texture runTex, Texture rollTex, Texture bulletTex, float scale, Sound shootSound) {
        this.shootSound = shootSound;
        this.bulletTex = new TextureRegion(bulletTex);

        // Idle frame
        idleFrame = new TextureRegion(idleTex);

        // Run animation (4 frames)
        TextureRegion[][] tmp = TextureRegion.split(runTex, runTex.getWidth() / 4, runTex.getHeight());
        TextureRegion[] runFrames = new TextureRegion[4];
        for (int i = 0; i < 4; i++) runFrames[i] = tmp[0][i];
        runAnim = new Animation<>(0.08f, runFrames);

        // Roll animation (8 frames)
        tmp = TextureRegion.split(rollTex, rollTex.getWidth() / 8, rollTex.getHeight());
        TextureRegion[] rollFrames = new TextureRegion[8];
        for (int i = 0; i < 8; i++) rollFrames[i] = tmp[0][i];
        rollAnim = new Animation<>(0.08f, rollFrames);

        // Bomb texture
        bombTex = new TextureRegion(new Texture("bomb.png"));

        // Explosion animation
        Texture exTex = new Texture("bombEx.png");
        tmp = TextureRegion.split(exTex, exTex.getWidth() / 4, exTex.getHeight());
        TextureRegion[] exFrames = new TextureRegion[4];
        for (int i = 0; i < 4; i++) exFrames[i] = tmp[0][i];
        explosionAnim = new Animation<>(0.08f, exFrames);

        // Size & hitbox
        float w = idleTex.getWidth() * scale;
        float h = idleTex.getHeight() * scale;
        setPosition(x, y);
        setSize(w, h);
        hitbox = new Rectangle(x + w * 0.25f, y + h * 0.001f, w * 0.45f, h * 0.55f);
    }

    @Override
    public void act(float delta) {
        super.act(delta);
        if (!active) return;
        // RESETABLE DAMAGE COOLDOWN
        if (dmgCooldown > 0)
            dmgCooldown -= delta;


        // Jika sedang roll: prioritaskan roll (isolasi perilaku)
        if (rolling) {
            stateTime += delta;
            bombCooldown -= delta;
            rollCooldown -= delta;
            doRoll(delta);
            updateHitbox();
            clampPosition();
            return; // penting: hentikan logic lain selama roll
        }

        // Normal flow
        stateTime += delta;
        bombCooldown -= delta;
        rollCooldown -= delta;

        Player p = getStage().getRoot().findActor("player");
        if (p == null) return;

        float bossCenter = getX() + getWidth() / 2f;
        float targetCenter = p.getX() + p.getWidth() / 2f;
        facingRight = targetCenter > bossCenter;

        float dist = Math.abs(targetCenter - bossCenter);
        if (dist <= 800f) {
            if (dist > 300f) {
                chasePlayer(targetCenter, bossCenter, delta);
            } else {
                state = State.IDLE; // dekat → berhenti jalan, siap serang
            }

            // Serangan: prioritas bomb → roll → burst shoot
            if (bombCooldown <= 0f && MathUtils.randomBoolean(0.25f)) {
                spawnBomb(targetCenter);
                bombCooldown = bombCooldownMax;
            } else if (rollCooldown <= 0f && MathUtils.randomBoolean(0.35f) && dist <= 400f) {
                startRoll(targetCenter, bossCenter);
            } else if (dist <= 300f) {
                if (burstCount == 0 && shootCooldown <= 0f) {
                    burstCount = 3;
                    burstTimer = 0f;
                    spawnShoot(p);
                    burstCount--;
                }
            }
        }

        // Burst handling
        if (burstCount > 0) {
            burstTimer -= delta;
            if (burstTimer <= 0f) {
                spawnShoot(p);
                burstCount--;
                burstTimer = burstInterval;
                if (burstCount == 0) shootCooldown = shootCooldownMax;
            }
        } else if (shootCooldown > 0f) {
            shootCooldown -= delta;
        }

        updateHitbox();
        clampPosition();
    }

    private void spawnBomb(float targetX) {
        float bx = getX() + getWidth() / 2f;
        float by = getY() + getHeight() / 2f;
        if (bombs != null) bombs.add(new Bomb(bx, by, targetX, bombTex));
    }

    private void chasePlayer(float targetCenter, float bossCenter, float delta) {
        state = State.RUN;
        if (targetCenter > bossCenter) setX(getX() + speed * delta);
        else setX(getX() - speed * delta);
        clampPosition();
    }

    private void clampPosition() {
        if (getX() < 0) setX(0);
        // 5000 adalah contoh arena width — sesuaikan kalau perlu
        if (getX() + getWidth() > 5000) setX(5000 - getWidth());
    }

    private void updateHitbox() {
        if (!rolling) {
            hitbox.setSize(getWidth() * 0.45f, getHeight() * 0.55f);
            hitbox.setPosition(getX() + getWidth() * 0.25f, getY() + getHeight() * 0.001f);
        } else {
            // saat roll, kita gunakan hitbox lebih tipis dan sedikit maju ke depan
            float newW = getWidth() * 0.9f;
            float newH = getHeight() * 0.6f;
            hitbox.setSize(newW * 0.45f, newH * 0.55f);
            hitbox.setPosition(getX() + getWidth() * 0.05f * (rollDirection == 1 ? 1 : 0.1f), getY() + getHeight() * 0.1f);
        }
    }

    private void startRoll(float targetCenter, float bossCenter) {
        rolling = true;
        state = State.ROLL;
        rollDirection = (targetCenter > bossCenter) ? 1 : -1;
        rollDistanceLeft = 420f;
        rollCooldown = rollCooldownMax;

        // sinkron animasi & orientasi supaya roll terlihat benar
        stateTime = 0f;
        facingRight = (rollDirection == 1);
        rollHasDamaged = false;
    }

    private void doRoll(float delta) {
        float move = rollSpeed * delta;
        if (move > rollDistanceLeft) move = rollDistanceLeft;
        setX(getX() + move * rollDirection);
        rollDistanceLeft -= move;

        // contoh: single-contact damage selama roll (opsional)
        // jika ingin aktifkan, pastikan Player punya method yang sesuai (mis. getBounds() dan takeDamage())
        /*
        Player p = getStage().getRoot().findActor("player");
        if (p != null && !rollHasDamaged) {
            if (hitbox.overlaps(p.getBounds())) {
                p.takeDamage(25f); // sesuaikan nama method & damage
                rollHasDamaged = true;
            }
        }
        */

        if (rollDistanceLeft <= 0f) {
            rolling = false;
            state = State.IDLE;
            rollHasDamaged = false;
            updateHitbox(); // kembalikan hitbox normal
        }
        clampPosition();
    }

    private void spawnShoot(Player target) {
        float bx = getX() + (facingRight ? 70 : -20);
        float by = getY() + hitbox.height * 0.45f + MathUtils.random(-10f, 10f);
        float vel = facingRight ? 350f : -350f;
        if (shootSound != null) shootSound.play();
        if (bullets != null) {
            Bullet bossBullet = new Bullet(bulletTex, bx, by, 40, 40, vel, 0, target);
            bossBullet.rotation = facingRight ? 0 : 180;
            bullets.add(bossBullet);
            getStage().addActor(bossBullet);   // ← WAJIB!
        }

    }

    @Override
    public void draw(Batch batch, float parentAlpha) {
        TextureRegion frame;
        switch (state) {
            case ROLL: frame = rollAnim.getKeyFrame(stateTime, true); break;
            case RUN:  frame = runAnim.getKeyFrame(stateTime, true);  break;
            default:
                float breathe = MathUtils.sin(stateTime * 4f) * 2f;
                frame = idleFrame;
                TextureRegion r = new TextureRegion(frame);
                if (!facingRight && !r.isFlipX()) r.flip(true, false);
                if (facingRight && r.isFlipX()) r.flip(true, false);
                batch.draw(r, getX() + breathe, getY() - 20f + breathe * 0.5f, getWidth(), getHeight());
                return;
        }

        TextureRegion r = new TextureRegion(frame);
        if (!facingRight && !r.isFlipX()) r.flip(true, false);
        if (facingRight && r.isFlipX()) r.flip(true, false);

        batch.draw(r, getX(), getY() - 20f, getWidth(), getHeight());
    }

    public Rectangle getHitbox() { return hitbox; }
    public Animation<TextureRegion> getExplosionAnim() { return explosionAnim; }
    public void setActive(boolean v) { this.active = v; }
    public void setBulletsArray(Array<Bullet> arr) { this.bullets = arr; }
    public void setBombsArray(Array<Bomb> arr) { this.bombs = arr; }
}
