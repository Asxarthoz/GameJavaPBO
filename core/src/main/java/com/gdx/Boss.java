package com.gdx;

import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.scenes.scene2d.Actor;
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

/**
 * Boss sebagai Actor (Scene2D).
 * - act(float delta) dipanggil oleh Stage.act(delta).
 * - Main dapat meng-inject referensi arrays bombs/bullets agar Boss menambahkan objek ke daftar tersebut.
 * - Untuk menemukan Player, Boss mencari Actor dengan nama playerName di Stage (jika Main tidak meng-inject langsung).
 */
public class Boss extends Actor {

    // Bomb system
    float bombTimer = 0f;
    float bombInterval = 6f;
    TextureRegion bombTex;
    Animation<TextureRegion> explosionAnim;

    public enum State { IDLE, RUN, ROLL, ATTACK }

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
    float shootInterval = 0.9f;
    TextureRegion bulletTex;

    // Animations / frames
    TextureRegion idleFrame;
    Animation<TextureRegion> runAnim;
    Animation<TextureRegion> rollAnim;

    float stateTime = 0f;
    public State state = State.IDLE;

    public Rectangle hitbox;
    public boolean active = false; // aktif setelah kondisi tertentu (mis. kill target)

    // REFERENSI EXTERNAL (di-inject dari Main)
    private Array<BossBullet> bullets = null;
    private Array<Bomb> bombs = null;
    private String playerName = "player"; // default, Main bisa setName("player") ke PlayerActor

    /**
     * Constructor
     */
    public Boss(float x, float y,
                Texture idleTex,
                Texture runSheet,
                Texture rollSheet,
                Texture bossBulletTex,
                float scale,
                Sound sound)
    {
        // set posisi awal Boss
        setPosition(x, y);

        this.shootSound = sound;
        this.bulletTex = new TextureRegion(bossBulletTex);
        this.idleFrame = new TextureRegion(idleTex);

        // bomb texture and explosion animation (load internally)
        this.bombTex = new TextureRegion(new Texture("bomb.png"));
        Texture expSheet = new Texture("bombEx.png");
        TextureRegion[][] expSplit = TextureRegion.split(
            expSheet,
            expSheet.getWidth() / 3,
            expSheet.getHeight()
        );
        TextureRegion[] expFrames = new TextureRegion[3];
        for (int i = 0; i < 3; i++) expFrames[i] = expSplit[0][i];
        explosionAnim = new Animation<>(0.1f, expFrames);

        // run animation (assume 4 frames)
        TextureRegion[][] runSplit = TextureRegion.split(runSheet, runSheet.getWidth()/4, runSheet.getHeight());
        TextureRegion[] runFrames = new TextureRegion[4];
        for (int i = 0; i < 4; i++) runFrames[i] = runSplit[0][i];
        runAnim = new Animation<>(0.12f, runFrames);

        // roll animation (assume 8 frames)
        TextureRegion[][] rollSplit = TextureRegion.split(rollSheet, rollSheet.getWidth()/8, rollSheet.getHeight());
        TextureRegion[] rollFrames = new TextureRegion[8];
        for (int i = 0; i < 8; i++) rollFrames[i] = rollSplit[0][i];
        rollAnim = new Animation<>(0.08f, rollFrames);

        // hitbox berdasarkan ukuran idle texture (skalakan jika perlu)
        hitbox = new Rectangle(x, y, idleTex.getWidth() * scale, idleTex.getHeight() * scale);

        // set size actor agar getWidth/getHeight berguna
        setSize(hitbox.width, hitbox.height);
    }

    /** Main dapat inject nama actor player bila berbeda */
    public void setPlayerName(String name) {
        this.playerName = name;
    }

    /** Main inject array bullets agar Boss bisa menambah BossBullet ke daftar itu */
    public void setBulletsArray(Array<BossBullet> bullets) {
        this.bullets = bullets;
    }

    /** Main inject array bombs agar Boss bisa menambah Bomb ke daftar itu */
    public void setBombsArray(Array<Bomb> bombs) {
        this.bombs = bombs;
    }

    @Override
    public void act(float delta) {
        super.act(delta);

        if (!active) return;

        stateTime += delta;
        shootTimer += delta;
        bombTimer += delta;

        if (rollCooldown > 0) rollCooldown -= delta;

        // spawn bomb periodik
        if (bombs != null && bombTimer >= bombInterval) {
            bombTimer = 0f;
            float throwX = getX() + (facingRight ? 80 : -20);
            // cari player X
            Rectangle playerRect = findPlayerHitbox();
            float targetX = (playerRect != null) ? playerRect.x + playerRect.width/2f : getX();
            bombs.add(new Bomb(throwX, getY() + 100, targetX, bombTex));
        }

        // cari player untuk decision making
        Rectangle playerRect = findPlayerHitbox();
        float playerCenter = (playerRect != null) ? playerRect.x + playerRect.width/2f : getX();
        float bossCenter = getX() + hitbox.width / 2f;
        float dist = Math.abs(playerCenter - bossCenter);

        updateFacing(playerCenter);

        // jika rolling => eksekusi roll
        if (rolling) {
            doRoll(delta);
            return;
        }

        // keputusan berdasarkan jarak
        if (dist <= 100 && rollCooldown <= 0) {
            startRoll(playerCenter);
            return;
        }

        if (dist <= 500) {
            state = State.IDLE;
            if (shootTimer >= shootInterval) {
                shootTimer = 0f;
                spawnShoot();
            }
            hitbox.setPosition(getX(), getY());
            return;
        }

        if (dist <= 800) {
            state = State.ATTACK;
            if (shootTimer >= shootInterval) {
                shootTimer = 0f;
                spawnShoot();
            }
            hitbox.setPosition(getX(), getY());
            return;
        }

        // otherwise chase
        chasePlayer(playerCenter, delta);
        hitbox.setPosition(getX(), getY());
    }

    // cari player di stage berdasarkan playerName -> ambil hitbox
    private Rectangle findPlayerHitbox() {
        if (getStage() == null) return null;
        Actor a = getStage().getRoot().findActor(playerName);
        if (a instanceof Player) return ((Player) a).getHitbox();
        return null;
    }

    private void updateFacing(float targetCenter) {
        facingRight = targetCenter > getX();
    }

    private void chasePlayer(float targetX, float delta) {
        state = State.RUN;
        if (targetX > getX()) setX(getX() + speed * delta);
        else setX(getX() - speed * delta);
    }

    private void startRoll(float targetCenter) {
        rolling = true;
        state = State.ROLL;
        rollDirection = (targetCenter > getX()) ? 1 : -1;
        rollDistanceLeft = 300;
        rollCooldown = rollCooldownMax;
    }

    private void doRoll(float delta) {
        float move = rollSpeed * delta;
        if (move > rollDistanceLeft) move = rollDistanceLeft;
        setX(getX() + move * rollDirection);
        rollDistanceLeft -= move;
        if (rollDistanceLeft <= 0) {
            rolling = false;
            state = State.IDLE;
        }
        hitbox.setPosition(getX(), getY());
    }

    private void spawnShoot() {
        // spawn BossBullet ke daftar bullets (jika di-inject)
        float bx = getX() + (facingRight ? 70 : -20);
        float by = getY() + hitbox.height * 0.45f;
        float vel = facingRight ? 350 : -350;
        if (shootSound != null) shootSound.play();
        if (bullets != null) bullets.add(new BossBullet(bx, by, vel, 0, bulletTex));
    }

    @Override
    public void draw(Batch batch, float parentAlpha) {
        // pilih frame sesuai state
        TextureRegion frame;
        switch (state) {
            case ROLL: frame = rollAnim.getKeyFrame(stateTime, true); break;
            case RUN:  frame = runAnim.getKeyFrame(stateTime, true);  break;
            default:   frame = idleFrame; break;
        }

        // gunakan salinan region sebelum flip agar tidak merusak sumber
        TextureRegion r = new TextureRegion(frame);
        if (facingRight && r.isFlipX()) r.flip(true, false);
        if (!facingRight && !r.isFlipX()) r.flip(true, false);

        // gambar dengan ukuran yang sudah diset (setSize di constructor)
        batch.draw(r, getX(), getY(), getWidth(), getHeight());
    }

    // akses hitbox dari luar
    public Rectangle getHitbox() { return hitbox; }

    // helper diberi nama agar Main bisa set active dll.
    public void setActive(boolean v) { this.active = v; }
}
