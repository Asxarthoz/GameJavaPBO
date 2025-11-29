package com.gdx;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.scenes.scene2d.Actor;

/**
 * Player: movement, attack, shield, dash, and heal mechanics.
 */
public class Player extends Actor {

    // Health player
    static final int MAX_HEALTH = 100;
    public float health = 100f;

    // Physics
    public float x, y;
    public float speed = 200f;
    public float gravity = -800f;
    public float velocityY = 0;
    public boolean isGround = true;

    // Arah & Serangan & sound
    public boolean facingRight = true;
    public boolean isAttack = false;
    public float attackTime = 0f;
    public boolean hitRegistered = false;

    Sound hitSound;
    Sound dashSound;
    Sound healSound; // <-- Variabel Sound Heal

    // --- SHIELD VARIABLES ---
    public boolean isShielding = false;

    // --- HEAL VARIABLES ---
    public float healTimer = 0f;
    public float healCooldown = 5f;

    public Rectangle hitbox;
    float scale = 0.2f;

    Animation<TextureRegion> walkAnim;
    Animation<TextureRegion> attackAnim;
    TextureRegion[] walkFrames;
    TextureRegion[] attackFrames;
    private TextureRegion shieldRegion;

    // DASH system
    boolean isDashing = false;
    float dashTime = 0f;
    float dashDuration = 0.15f;
    float dashSpeed = 900f;
    float dashCooldown = 3f;
    float dashCooldownTimer = 0f;

    float stateTime = 0f;

    // Constructor Updated: Menambahkan parameter healSound
    public Player(float x, float y, Texture spriteSheet, Texture attackSheet, Sound sound, Sound soundDash, Sound healSound, TextureRegion shieldRegion) {
        this.setPosition(x, y);
        this.hitSound = sound;
        this.dashSound = soundDash;
        this.healSound = healSound; // <-- Assign sound ke variabel
        this.shieldRegion = shieldRegion;

        // character sub-rectangle
        int charX = 750;
        int charY = 410;
        int charW = 585;
        int charH = 920;

        int frameCols = 7;
        int frameRows = 2;
        int frameWidth = spriteSheet.getWidth() / frameCols;
        int frameHeight = spriteSheet.getHeight() / frameRows;
        TextureRegion[][] tmpWalk = TextureRegion.split(spriteSheet, frameWidth, frameHeight);
        TextureRegion[][] tmpAtk = TextureRegion.split(spriteSheet, frameWidth, frameHeight);

        walkFrames = new TextureRegion[7];
        attackFrames = new TextureRegion[7];

        for (int i = 0; i < 7; i++) {
            walkFrames[i] = new TextureRegion(tmpWalk[1][i], charX, charY, charW, charH);
            attackFrames[i] = new TextureRegion(tmpAtk[0][i], charX, charY, charW, charH);
        }

        walkAnim = new Animation<TextureRegion>(0.1f, walkFrames);
        attackAnim = new Animation<TextureRegion>(0.1f, attackFrames);

        float w = walkFrames[0].getRegionWidth() * scale;
        float h = walkFrames[0].getRegionHeight() * scale;
        setSize(w, h);

        hitbox = new Rectangle(getX(), getY(), w - 60, h - 30);
    }

    @Override
    public void act(float delta) {
        super.act(delta);
        stateTime += delta;

        // Heal Logic
        if (healTimer > 0) {
            healTimer -= delta;
        }

        if (Gdx.input.isKeyJustPressed(Input.Keys.H)) {
            if (healTimer <= 0 && health < MAX_HEALTH) {
                health += 30;
                if (health > MAX_HEALTH) health = MAX_HEALTH;
                healTimer = healCooldown;

                // Play Heal Sound
                if (healSound != null) {
                    healSound.play(); // <-- Sound Heal Berbunyi di sini
                }

                System.out.println("Healed! Current HP: " + health);
            }
        }

        // Shield Logic Input
        if (Gdx.input.isButtonPressed(Input.Buttons.RIGHT)) {
            isShielding = true;
        } else {
            isShielding = false;
        }

        // Speed Adjustment Logic
        if (isShielding) {
            speed = 100f; // Melambat saat shield
        } else {
            // Jika tidak shield, cek sprint
            if (Gdx.input.isKeyPressed(Input.Keys.SHIFT_LEFT)) {
                speed = 550f;
            } else {
                speed = 200f;
            }
        }

        // Movement
        if (Gdx.input.isKeyPressed(Input.Keys.D)) {
            setX(getX() + speed * delta);
            facingRight = true;
        }
        if (Gdx.input.isKeyPressed(Input.Keys.A)) {
            setX(getX() - speed * delta);
            facingRight = false;
        }

        // Jump
        if (Gdx.input.isKeyPressed(Input.Keys.W) && isGround) {
            velocityY = 400;
            isGround = false;
        }

        // Dash cooldown
        dashCooldownTimer -= delta;
        if (Gdx.input.isKeyJustPressed(Input.Keys.SPACE) && dashCooldownTimer <= 0 && !isDashing) {
            isDashing = true;
            this.dashSound.play();
            dashTime = 0f;
            dashCooldownTimer = dashCooldown;
        }

        // Attack input (Hanya bisa attack jika TIDAK shielding)
        if (Gdx.input.isButtonJustPressed(Input.Buttons.LEFT) && !isAttack && !isShielding) {
            isAttack = true;
            attackTime = 0f;
            hitRegistered = false;
            hitSound.play();
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
            if (facingRight) {
                setX(getX() + dashSpeed * delta);
            } else {
                setX(getX() - dashSpeed * delta);
            }
            if (dashTime >= dashDuration){
                isDashing = false;
            }
            hitbox.setPosition(getX() + 15, getY());
            return;
        }

        // Gravity
        velocityY += gravity * delta;
        setY(getY() + velocityY * delta);
        if (getY() <= 0) { setY(0); velocityY = 0; isGround = true; }

        // CLAMP
        if (getX() < 0) setX(0);
        if (health > MAX_HEALTH) health = MAX_HEALTH;
        if (health < 0) health = 0f;

        // Update hitbox
        hitbox.setPosition(getX() + 15, getY());
    }

    /**
     * Mengambil frame animasi
     */
    public TextureRegion getFrame() {
        TextureRegion rawFrame;
        TextureRegion idle = walkFrames[0];

        if (isAttack) {
            rawFrame = attackAnim.getKeyFrame(attackTime, false);
        } else if (Gdx.input.isKeyPressed(Input.Keys.A) || Gdx.input.isKeyPressed(Input.Keys.D)) {
            rawFrame = walkAnim.getKeyFrame(stateTime, true);
        } else {
            rawFrame = idle;
        }

        TextureRegion frame = new TextureRegion(rawFrame);
        if (facingRight && frame.isFlipX()) frame.flip(true, false);
        if (!facingRight && !frame.isFlipX()) frame.flip(true, false);
        return frame;
    }

    @Override
    public void draw(Batch batch, float parentAlpha) {
        // --- BAGIAN SAAT KLIK KANAN (SHIELD) ---
        if (isShielding) {
            // Logika flip
            boolean flip = !facingRight;
            if (shieldRegion.isFlipX() != flip) {
                shieldRegion.flip(true, false);
            }

            // --- ATUR POSISI DI SINI ---
            float penyesuaianY = -55f;
            float penyesuaianX = -50f;

            batch.draw(
                shieldRegion,
                getX() + penyesuaianX,
                getY() + penyesuaianY,
                shieldRegion.getRegionWidth() * scale * 2.2f,
                shieldRegion.getRegionHeight() * scale * 2.2f
            );
        }
        // --- BAGIAN NORMAL ---
        else {
            TextureRegion frame = getFrame();
            batch.draw(
                frame,
                getX(), getY(),
                frame.getRegionWidth() * scale,
                frame.getRegionHeight() * scale
            );
        }
    }

    public void updateDialog(float delta) {
        stateTime += delta;
    }

    public Rectangle getAttackHitbox() {
        if (!isAttack) return null;
        float attackWidth = 60;
        float attackHeight = hitbox.height;
        if (facingRight) {
            return new Rectangle(hitbox.x + hitbox.width, hitbox.y, attackWidth, attackHeight);
        } else {
            return new Rectangle(hitbox.x - attackWidth, hitbox.y, attackWidth, attackHeight);
        }
    }

    public Rectangle getHitbox() { return hitbox; }

    public String getDashStatusString() {
        if (dashCooldownTimer <= 0f) return "DASH: READY";
        return String.format("DASH: cooldown %.1fs", dashCooldownTimer);
    }
}
