package com.gdx;

import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.utils.TimeUtils;

public class Enemy extends Actor {

    public float hp = 100f;
    public float maxHp = 100f;

    public boolean dead = false;
    public float deathTime = 0f;
    public long lastShootTime = 0;

    public float speed = 80f;
    public boolean facingRight = true;

    private Rectangle hitbox;
    private float scale = 1.0f;

    // === ANIMASI ===
    private Animation<TextureRegion> idleAnim;
    private Animation<TextureRegion> walkAnim;
    private Animation<TextureRegion> runAnim;
    private Animation<TextureRegion> shootAnim;
    private Animation<TextureRegion> dieAnim;

    private float stateTime = 0f;
    private EnemyState state = EnemyState.IDLE;
    private EnemyState prevState = EnemyState.IDLE;

    // AI
    private float patrolTargetX;
    private float detectDelay = 1.0f; // bisa disesuaikan
    private float delayTimer = 0f;
    private boolean detectedPlayer = false;
    private float damageCooldown = 0f;
    private boolean countedKill = false;

    private enum EnemyState {
        IDLE, WALK, RUN, SHOOT, DIE
    }

    // SHOOT timing
    private final float fireTime = 0.12f;
    private boolean firedThisShot = false;

    public Enemy(float startX, float startY,
                 TextureRegion[] idleFrames,
                 TextureRegion[] walkFrames,
                 TextureRegion[] runFrames,
                 TextureRegion[] shootFrames,
                 TextureRegion[] dieFrames,
                 float scale) {

        this.scale = scale;
        setPosition(startX, startY);

        // Buat animasi
        idleAnim   = new Animation<>(0.2f, idleFrames);
        idleAnim.setPlayMode(Animation.PlayMode.LOOP);
        walkAnim   = new Animation<>(0.08f, walkFrames);
        walkAnim.setPlayMode(Animation.PlayMode.LOOP);
        runAnim    = new Animation<>(0.08f, runFrames);
        runAnim.setPlayMode(Animation.PlayMode.LOOP);
        shootAnim  = new Animation<>(0.08f, shootFrames);
        shootAnim.setPlayMode(Animation.PlayMode.NORMAL);
        dieAnim    = new Animation<>(0.12f, dieFrames);
        dieAnim.setPlayMode(Animation.PlayMode.NORMAL);


        TextureRegion firstFrame = idleFrames[0];
        float w = firstFrame.getRegionWidth() * scale;
        float h = firstFrame.getRegionHeight() * scale;
        setSize(w, h);

        hitbox = new Rectangle(getX() + getWidth() * 0.25f,
                getY() + getHeight() * 0.001f,
                getWidth() * 0.5f,
                getHeight() * 0.75f);

        // inisialisasi lastShootTime biar ga langsung menembak
        lastShootTime = TimeUtils.nanoTime();

        pickNewPatrolTarget();
    }

    @Override
    public void act(float delta) {
        super.act(delta);
        if (dead) {
            deathTime += delta;
            state = EnemyState.DIE;
            return;
        }

        // update waktu state
        stateTime += delta;

        Player player = findPlayer();
        if (player == null) {
            // tidak ada player di stage: patrol
            state = EnemyState.WALK;
            patrol(delta);
            updateHitbox();
            return;
        }

        // jarak ke player (center)
        float dist = Math.abs(getCenterX() - (player.getHitbox().x + player.getHitbox().width/2f));

        if (dist <= 450f) detectedPlayer = true;

        if (detectedPlayer) {
            delayTimer += delta;
            if (delayTimer < detectDelay) {
                state = EnemyState.IDLE;
            } else {
                // jika dekat cukup untuk menembak -> berhenti di tempat dan mulai animasi SHOOT
                if (dist <= 250f) {
                    // Jika dalam radius menembak, berhenti; jika cooldown ready -> SHOOT, else tetap IDLE
                    if (canShoot()) {
                        if (state != EnemyState.SHOOT) {
                            stateTime = 0f;
                            firedThisShot = false;
                        }
                        state = EnemyState.SHOOT;
                    } else {
                        // cooldown belum selesai -> tetap diam (IDLE)
                        state = EnemyState.IDLE;
                    }
                } else if (dist <= 600f) {
                    // player agak jauh: kejar
                    state = EnemyState.RUN;
                    chasePlayer(player, delta);
                } else {
                    // player terlihat tapi jauh -> patrol (atau jalan)
                    state = EnemyState.WALK;
                    patrol(delta);
                }
            }
        } else {
            state = EnemyState.WALK;
            patrol(delta);
        }

        // jika sedang dalam state SHOOT, cek apakah animasi sudah melewati titik fireTime
        if (state == EnemyState.SHOOT) {
            // tidak bergerak saat menembak (patrol/chase berhenti)
            // setelah animasi SHOOT selesai, reset ke IDLE dan set lastShootTime
            if (shootAnim.isAnimationFinished(stateTime)) {
                stateTime = 0f;
                state = EnemyState.IDLE;
                lastShootTime = TimeUtils.nanoTime();
                // setelah selesai, delayTimer tetap (biar tidak langsung reload)
            }
        }

        if (damageCooldown > 0) damageCooldown -= delta;

        updateHitbox();
        prevState = state;
        clampPosition();
    }

    private Player findPlayer() {
        if (getStage() == null) return null;
        Actor a = getStage().getRoot().findActor("player");
        return (a instanceof Player) ? (Player) a : null;
    }

    private float getCenterX() {
        return getX() + getWidth() / 2f;
    }

    private void clampPosition() {
        if (getX() < 0) setX(0);
        if (getX() + getWidth() > 5000) setX(5000 - getWidth());
    }

    private void chasePlayer(Player player, float delta) {
        float playerCenter = player.getHitbox().x + player.getHitbox().width / 2f;
        if (playerCenter > getCenterX()) {
            setX(getX() + speed * 1.5f * delta); // run lebih cepat
            facingRight = true;
        } else {
            setX(getX() - speed * 1.5f * delta);
            facingRight = false;
        }
        clampPosition();
    }


    public boolean shouldFire() {
        if (state != EnemyState.SHOOT) return false;
        if (firedThisShot) return false;

        if (stateTime >= fireTime) {
            firedThisShot = true;
            return true;
        }
        return false;
    }

    private void patrol(float delta) {
        if (facingRight) {
            setX(getX() + speed * delta);
            if (getX() >= patrolTargetX) pickNewPatrolTarget();
        } else {
            setX(getX() - speed * delta);
            if (getX() <= patrolTargetX) pickNewPatrolTarget();
        }
        clampPosition();
    }

    private void pickNewPatrolTarget() {
        float move = MathUtils.random(150, 400);
        if (MathUtils.randomBoolean()) {
            patrolTargetX = getX() + move;
            facingRight = true;
        } else {
            patrolTargetX = Math.max(0, getX() - move);
            facingRight = false;
        }
    }

    public boolean canShoot() {
        return TimeUtils.nanoTime() - lastShootTime > 1_800_000_000L; // 1.8 detik cooldown
    }

    public void takeDamage(float dmg) {
        if (dead || damageCooldown > 0) return;
        damageCooldown = 0.3f;
        hp -= dmg;
        if (hp <= 0) {
            dead = true;
            hp = 0;
            stateTime = 0f;
            deathTime = 0f;
        }
    }

    private void updateHitbox() {
        // hitbox selalu berada relatif ke pos actor — tidak mengambang lagi
        hitbox.setPosition(getX() + getWidth() * 0.25f, getY() + getHeight() * 0.001f);
    }

    @Override
    public void draw(Batch batch, float parentAlpha) {
        TextureRegion frame;

        if (state == EnemyState.DIE) {
            frame = dieAnim.getKeyFrame(deathTime, false);
        } else if (state == EnemyState.SHOOT) {
            frame = shootAnim.getKeyFrame(stateTime, false);
        } else if (state == EnemyState.RUN) {
            frame = runAnim.getKeyFrame(stateTime, true);
        } else if (state == EnemyState.WALK) {
            frame = walkAnim.getKeyFrame(stateTime, true);
        } else {
            frame = idleAnim.getKeyFrame(stateTime, true);
        }

        // buat salinan region sehingga flip tidak merusak region sumber
        TextureRegion r = new TextureRegion(frame);
        if (!facingRight && !r.isFlipX()) r.flip(true, false);
        if (facingRight && r.isFlipX()) r.flip(true, false);

        // gambar tepat di getX(), getY() — jangan offset, supaya hitbox sinkron
        batch.draw(r, getX(), getY(), getWidth(), getHeight());
    }

    public Rectangle getHitbox() { return hitbox; }
    public boolean isDead() { return dead && dieAnim.isAnimationFinished(deathTime); }

    public boolean hasCountedKill() { return countedKill; }
    public void setCountedKill(boolean v) { countedKill = v; }
}
