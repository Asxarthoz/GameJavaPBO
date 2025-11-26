package com.gdx;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Rectangle;

/**
 * Player: movement, attack, and dash mechanics.
 * Adapted to use separate Idle / Run / Attack sprite sheets.
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
    float playerScale = 2f;

    Animation<TextureRegion> idleAnim;
    Animation<TextureRegion> runAnim;
    Animation<TextureRegion> attackAnim;
    TextureRegion[] idleFrames;
    TextureRegion[] runFrames;
    TextureRegion[] attackFrames;

    // DASH system (seconds)
    boolean isDashing = false;
    float dashTime = 0f;
    float dashDuration = 0.15f; // short burst
    float dashSpeed = 900f;
    float dashCooldown = 3f;    // 3 seconds cooldown
    float dashCooldownTimer = 0f;

    float stateTime = 0f;

    /**
     * Constructor menggunakan 3 sheet terpisah: idle, run, attack.
     * Contoh path file yang kamu upload:
     * /mnt/data/Idle-Sheet.png
     * /mnt/data/Run-Sheet.png
     * /mnt/data/Attack-01-Sheet.png
     *
     * Jika jumlah kolom/frame berbeda dari asumsi di bawah, ubah konstanta IDLE_COLS/RUN_COLS/ATTACK_COLS.
     */
    public Player(float x, float y, Texture idleSheet, Texture runSheet, Texture attackSheet) {
        this.x = x;
        this.y = y;

        // -> Ubah angka ini bila sheet-mu memiliki jumlah kolom berbeda
        final int IDLE_COLS = 4;   // asumsi: Idle sheet 4 frame
        final int RUN_COLS  = 10;   // asumsi: Run sheet 10 frame
        final int ATTACK_COLS = 8; // asumsi: Attack sheet 8 frame
        final int ROWS = 1;        // semua sheet 1 row (ubah bila berbeda)

        // --- Idle frames ---
        int idleFrameW = idleSheet.getWidth() / IDLE_COLS;
        int idleFrameH = idleSheet.getHeight() / ROWS;
        TextureRegion[][] idleTmp = TextureRegion.split(idleSheet, idleFrameW, idleFrameH);
        idleFrames = new TextureRegion[IDLE_COLS];
        for (int i = 0; i < IDLE_COLS; i++) {
            idleFrames[i] = idleTmp[0][i];
        }
        idleAnim = new Animation<TextureRegion>(0.15f, idleFrames); // lambat untuk idle

        // --- Run frames ---
        int runFrameW = runSheet.getWidth() / RUN_COLS;
        int runFrameH = runSheet.getHeight() / ROWS;
        TextureRegion[][] runTmp = TextureRegion.split(runSheet, runFrameW, runFrameH);
        runFrames = new TextureRegion[RUN_COLS];
        for (int i = 0; i < RUN_COLS; i++) {
            runFrames[i] = runTmp[0][i];
        }
        runAnim = new Animation<TextureRegion>(0.08f, runFrames); // cepat untuk lari

        // --- Attack frames ---
        int attackFrameW = attackSheet.getWidth() / ATTACK_COLS;
        int attackFrameH = attackSheet.getHeight() / ROWS;
        TextureRegion[][] attackTmp = TextureRegion.split(attackSheet, attackFrameW, attackFrameH);
        attackFrames = new TextureRegion[ATTACK_COLS];
        for (int i = 0; i < ATTACK_COLS; i++) {
            attackFrames[i] = attackTmp[0][i];
        }
        attackAnim = new Animation<TextureRegion>(0.06f, attackFrames); // trim/tempo animasi serangan

        // Hitbox: gunakan ukuran frame run (umumnya mewakili postur berjalan)
        float hbWidth = runFrames[0].getRegionWidth() * playerScale - 60;  // offset kecil untuk menyesuaikan bentuk
        float hbHeight = runFrames[0].getRegionHeight() * playerScale - 30;
        if (hbWidth < 10) hbWidth = runFrames[0].getRegionWidth() * playerScale * 0.6f; // fallback aman
        if (hbHeight < 10) hbHeight = runFrames[0].getRegionHeight() * playerScale * 0.6f;
        hitbox = new Rectangle(x, y, hbWidth, hbHeight);
    }

    /**
     * update: movement, jump, sprint, dash, attack.
     * Delta dipakai untuk independensi frame rate.
     */
    public void update(float delta) {
        stateTime += delta;
        boolean moving = false;

        // Horizontal movement input
        if (Gdx.input.isKeyPressed(Input.Keys.D)) { x += speed * delta; facingRight = true; moving = true; }
        if (Gdx.input.isKeyPressed(Input.Keys.A)) { x -= speed * delta; facingRight = false; moving = true; }

        // Jump
        if (Gdx.input.isKeyPressed(Input.Keys.W) && isGround) { velocityY = 400; isGround = false; }

        // Sprint (temporary speed increase)
        if (Gdx.input.isKeyPressed(Input.Keys.SHIFT_LEFT)) speed = 550; else speed = 200;

        // Dash cooldown timer (counts down)
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
            hitRegistered = false;
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

        // Update hitbox position (offset agar anchor sprite sesuai)
        hitbox.setPosition(x + 20, y);
    }

    /**
     * Mengembalikan frame yang siap digambar sesuai state:
     * - attackAnim saat menyerang
     * - runAnim saat menekan A/D
     * - idleAnim saat tidak bergerak
     *
     * Membuat salinan TextureRegion supaya flip tidak merusak array asli.
     */
    public TextureRegion getFrame() {
        TextureRegion rawFrame;
        if (isAttack) rawFrame = attackAnim.getKeyFrame(attackTime, false);
        else if (Gdx.input.isKeyPressed(Input.Keys.A) || Gdx.input.isKeyPressed(Input.Keys.D)) rawFrame = runAnim.getKeyFrame(stateTime, true);
        else rawFrame = idleAnim.getKeyFrame(stateTime, true);

        TextureRegion frame = new TextureRegion(rawFrame);
        if (facingRight && frame.isFlipX()) frame.flip(true, false);
        if (!facingRight && !frame.isFlipX()) frame.flip(true, false);
        return frame;
    }

    // Lightweight update used while dialog is active so animations still progress
    public void updateDialog(float delta) {
        stateTime += delta;
    }

    // Attack hitbox adalah rectangle pendek di depan player. Facing mempengaruhi sisi.
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
