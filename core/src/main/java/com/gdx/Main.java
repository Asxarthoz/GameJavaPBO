package com.gdx;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.TimeUtils;
import com.badlogic.gdx.utils.viewport.ExtendViewport;
import com.badlogic.gdx.utils.viewport.Viewport;

import java.util.Random;

enum GameState {
    EXPLORE,
    DIALOG,
    GAMEOVER,
    VICTORY
}

public class Main extends ApplicationAdapter {

    // ---------- constants ----------
    private static final float GLOBAL_SCALE = 0.2f; // uniform scale for character frames
    private static final int WORLD_WIDTH = 5000;    // clamp camera movement
    private static final int WORLD_HEIGHT = 2000;
    private static final int V_WIDTH = 800;
    private static final int V_HEIGHT = 640;

    // Wave and kill tracking
    private int killCount = 0;
    private static final int KILL_TARGET = 20; // level goal
    private static final int WAVE_SIZE = 4;    // enemies per wave

    // Core rendering utilities
    private SpriteBatch batch;
    private ShapeRenderer shapeR;
    private BitmapFont font;
    private OrthographicCamera camera;
    private Viewport viewport;

    // Assets loaded once for reuse
    private Texture background;
    private Texture enemyTexture;
    private Texture playerSheet;
    private Texture npcSheet;
    private Texture bulletTexture;
    private TextureRegion bulletRegion;

    // --- SHIELD ASSETS ---
    private Texture shieldTexture;
    private TextureRegion shieldRegion;

    // --- HEAL ASSETS ---
    private Texture healTexture;

    // Game objects collections
    private Player player;
    private NPC npc;
    private Array<Enemy> enemies = new Array<>();
    private Array<Bullet> bullets = new Array<>();

    // State and utilities
    private GameState currentState = GameState.DIALOG;
    private String[] dialogList = {
        "Para penjajah berusaha merebut tanah kita.",
        "Jaga tanah ini dengan sepenuh hatimu!",
        "Jangan pernah gentar meskipun nyawa taruhannya!"
    };
    private int dialogIndex = 0;
    private float stateTime = 0f;
    private Random rand = new Random();

    @Override
    public void create() {
        // Basic lib setup
        batch = new SpriteBatch();
        shapeR = new ShapeRenderer();
        font = new BitmapFont();

        // Load textures once
        background = new Texture("bg.png");
        enemyTexture = new Texture("enemy.png");
        playerSheet = new Texture("sprite.png");
        npcSheet = new Texture("sprite.png");
        bulletTexture = new Texture("bullet.png");
        bulletRegion = new TextureRegion(bulletTexture);
        background.setWrap(Texture.TextureWrap.Repeat, Texture.TextureWrap.ClampToEdge);

        // Load Shield & Heal
        shieldTexture = new Texture("shield.png"); // Gambar player pegang shield
        shieldRegion = new TextureRegion(shieldTexture);

        healTexture = new Texture("heal.png"); // Gambar icon heal (plus/hati)

        // Camera & viewport
        camera = new OrthographicCamera();
        viewport = new ExtendViewport(V_WIDTH, V_HEIGHT, camera);

        // Create main actor and NPC
        player = new Player(100, 0, playerSheet, playerSheet);
        npc = new NPC(-200, -100, npcSheet);
        npc.startEnter();

        // Spawn first wave
        spawnWave();

        camera.update();
    }

    private void spawnWave() {
        enemies.clear();
        TextureRegion enemyRegion = new TextureRegion(enemyTexture);
        for (int i = 0; i < WAVE_SIZE; i++) {
            int x;
            if(killCount == 0) {
                x = rand.nextInt(1001) + 1000;
            } else {
                x = rand.nextInt(2001);
            }

            int y = 0;
            float scale = 0.4f;
            enemies.add(new Enemy(x, y, enemyRegion, scale));
        }
    }

    @Override
    public void render() {
        ScreenUtils.clear(0, 0, 0, 1);
        float delta = Gdx.graphics.getDeltaTime();
        stateTime += delta;

        // Update phase
        if (currentState == GameState.DIALOG) {
            player.updateDialog(delta);
            npc.update(delta);
        } else {
            player.update(delta);
            npc.update(delta);
            for (Enemy e : enemies) e.update(delta, player.hitbox);
        }

        // Enemies shoot
        for (Enemy e : enemies) {
            if (e.canShoot()) shootFromEnemy(e);
        }

        // Dialog progression
        if (currentState == GameState.DIALOG) {
            if (Gdx.input.isButtonJustPressed(Input.Buttons.LEFT)) {
                dialogIndex++;
                if (dialogIndex >= dialogList.length) {
                    npc.startLeave();
                    currentState = GameState.EXPLORE;
                }
            }
        }

        // Player attack detection
        if (player.isAttack) {
            Rectangle atk = player.getAttackHitbox();
            if (atk != null && !player.hitRegistered) {
                for (Enemy e : enemies) {
                    if (!e.dead && atk.overlaps(e.hitbox)) {
                        e.takeDamage(30);
                        player.hitRegistered = true;
                        break;
                    }
                }
            }
        }

        // Process dead enemies
        for (int i = enemies.size - 1; i >= 0; i--) {
            Enemy en = enemies.get(i);
            if (en.dead && en.deathTime >= 1f) {
                if (!en.hasCountedKill()) {
                    en.setCountedKill(true);
                    killCount++;
                }
                enemies.removeIndex(i);
            }
        }

        // Spawn next wave
        if (enemies.size == 0 && killCount < KILL_TARGET && currentState == GameState.EXPLORE) {
            spawnWave();
        }

        // Victory
        if (killCount >= KILL_TARGET && currentState != GameState.VICTORY) {
            currentState = GameState.VICTORY;
            System.out.println("Victory! Kills = " + killCount);
        }

        // Bullets update
        for (int i = bullets.size - 1; i >= 0; i--) {
            Bullet b = bullets.get(i);
            b.rect.x += b.velX * delta;
            b.rect.y += b.velY * delta;

            if (b.rect.overlaps(player.hitbox)) {
                // LOGIKA KEBAL SHIELD
                if (player.isShielding) {
                    bullets.removeIndex(i);
                    continue;
                }

                if (player.health > 0) player.health -= 10;
                bullets.removeIndex(i);
                continue;
            }

            if (b.rect.x < 0 || b.rect.x > WORLD_WIDTH || b.rect.y < 0 || b.rect.y > WORLD_HEIGHT) {
                bullets.removeIndex(i);
            }
        }

        // Camera Update
        camera.position.x = player.x + player.hitbox.width / 2;
        float halfW = viewport.getWorldWidth() / 2f;
        if (camera.position.x < halfW) camera.position.x = halfW;
        if (camera.position.x > WORLD_WIDTH - halfW) camera.position.x = WORLD_WIDTH - halfW;
        camera.update();

        // ---------- Rendering ----------
        batch.setProjectionMatrix(camera.combined);
        batch.begin();

        // Background
        batch.draw(background, 0, 0);
        batch.draw(background,  background.getWidth(), 0);
        batch.draw(background, 2 * background.getWidth(), 0);

        // --- PLAYER & SHIELD RENDER LOGIC ---
        if (player.isShielding) {
            // Mode Shield: Gambar Player Memegang Shield

            // 1. Reset flip jika perlu
            if (shieldRegion.isFlipX()) shieldRegion.flip(true, false);

            // 2. Flip sesuai arah
            if (!player.facingRight) shieldRegion.flip(true, false);

            // 3. Gambar dengan ukuran khusus (jika ingin diubah ukurannya, ubah GLOBAL_SCALE di sini)
            float scaleKhusus = 0.4f; // Tentukan skala khusus shield

            batch.draw(shieldRegion,
                player.x - 40,  // Geser X jika perlu (minus ke kiri, plus ke kanan)
                player.y - 50,   // Geser Y jika perlu (minus ke bawah, plus ke atas)
                shieldRegion.getRegionWidth() * scaleKhusus,
                shieldRegion.getRegionHeight() * scaleKhusus
            );

        } else {
            // Mode Normal: Gambar Animasi Biasa
            TextureRegion frame = player.getFrame();
            batch.draw(frame, player.x, player.y, frame.getRegionWidth() * GLOBAL_SCALE, frame.getRegionHeight() * GLOBAL_SCALE);
        }

        // Enemies
        for (Enemy e : enemies) {
            batch.draw(
                e.getSprite(),
                e.x, e.y - 30,
                85, 125,
                170, 250,
                1, 1,
                e.dead ? e.rotation : 0
            );
        }

        // Bullets
        for (Bullet b : bullets) {
            batch.draw(
                bulletRegion,
                b.rect.x, b.rect.y,
                b.rect.width / 2, b.rect.height / 2,
                b.rect.width + 30, b.rect.height + 30,
                1, 1,
                b.rotation
            );
        }

        // NPC
        TextureRegion npcFrame = npc.getFrame();
        batch.draw(npcFrame, npc.x, npc.y - 21, npcFrame.getRegionWidth() * 0.2f, npcFrame.getRegionHeight() * 0.2f);

        // --- HUD SECTION ---
        // Hitung posisi pojok kiri atas layar berdasarkan kamera
        float camLeft = camera.position.x - viewport.getWorldWidth() / 2;
        float camTop = camera.position.y + viewport.getWorldHeight() / 2;

        font.setColor(Color.WHITE);
        // Teks Kills
        font.draw(batch, "Kills: " + killCount + " / " + KILL_TARGET, camLeft + 20, camTop - 20);
        // Teks Dash
        font.draw(batch, player.getDashStatusString(), camLeft + 20, camTop - 40);

        // --- HEAL UI LOGIC ---
        // Gambar Icon Heal di samping teks (misal geser 250 pixel ke kanan)
        float healIconX = camLeft + 150;
        float healIconY = camTop - 90; // Posisi Y icon
        float healIconSize = 90; // Ukuran icon 40x40

        batch.draw(healTexture, healIconX, healIconY, healIconSize, healIconSize);

        // Gambar Counter Cooldown di bawah icon Heal
        if (player.healTimer > 0) {
            font.setColor(Color.YELLOW);
            String cdText = String.format("%.0f", player.healTimer);

            font.getData().setScale(2.0f);

            // Gambar text (sesuaikan posisi X dan Y agar pas di tengah jika ukurannya berubah)
            font.draw(batch, cdText, healIconX + 35, healIconY + 10);

            // [PENTING: KEMBALIKAN KE UKURAN NORMAL]
            // Wajib di-reset ke 1.0f agar tulisan lain (seperti Kills/Dialog) tidak ikut membesar
            font.getData().setScale(1.0f);
        }
        // ---------------------

        // Dialog text
        if (currentState == GameState.DIALOG) {
            font.setColor(Color.WHITE);
            font.draw(batch, dialogList[dialogIndex], camLeft + 40, camera.position.y - viewport.getWorldHeight() / 2 + 40);
        }

        batch.end();

        // Draw health bars
        shapeR.setProjectionMatrix(camera.combined);
        shapeR.begin(ShapeRenderer.ShapeType.Filled);

        // Enemy HP
        for (Enemy e : enemies) {
            float pct = (e.maxHp <= 0) ? 0f : (e.hp / e.maxHp);
            shapeR.setColor(Color.RED);
            shapeR.rect(e.hitbox.x, e.hitbox.y + e.hitbox.height + 10, 100 * pct, 10);
        }

        // Player HP
        shapeR.setColor(Color.RED);
        shapeR.rect(player.hitbox.x, player.hitbox.y + player.hitbox.height + 10, 100 * (player.health / player.MAX_HEALTH), 10);

        shapeR.end();

        // Debug outlines
        shapeR.begin(ShapeRenderer.ShapeType.Line);
        shapeR.setColor(Color.YELLOW);
        shapeR.rect(player.hitbox.x, player.hitbox.y, player.hitbox.width, player.hitbox.height);
        for (Enemy e : enemies) shapeR.rect(e.hitbox.x, e.hitbox.y, e.hitbox.width, e.hitbox.height);
        shapeR.end();
    }

    void shootFromEnemy(Enemy e) {
        float startX = e.hitbox.x + e.hitbox.width / 2;
        if (e.facingRight) startX += 30; else startX -= 130;
        float startY = e.hitbox.y + e.hitbox.height / 2 - 25;

        float targetX = player.hitbox.x + player.hitbox.width / 2;
        float bulletSpeed = 500f;
        float velX = (targetX > startX) ? bulletSpeed : -bulletSpeed;
        float velY = 0;

        Bullet bullet = new Bullet(startX, startY, 20, 20, velX, velY);
        bullet.rotation = (velX < 0) ? 0 : 180;
        bullets.add(bullet);

        e.lastShootTime = TimeUtils.nanoTime();
    }

    @Override
    public void resize(int width, int height) {
        viewport.update(width, height, true);
    }

    @Override
    public void dispose() {
        batch.dispose();
        shapeR.dispose();
        font.dispose();

        background.dispose();
        enemyTexture.dispose();
        playerSheet.dispose();
        npcSheet.dispose();
        bulletTexture.dispose();
        shieldTexture.dispose();
        healTexture.dispose(); // Dispose heal texture
    }

    class Bullet {
        Rectangle rect;
        float velX, velY;
        float rotation;

        public Bullet(float x, float y, float width, float height, float vX, float vY) {
            this.rect = new Rectangle(x, y, width, height);
            this.velX = vX;
            this.velY = vY;
        }
    }
}
