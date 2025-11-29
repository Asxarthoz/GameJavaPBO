package com.gdx;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputMultiplexer;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.TimeUtils;
import com.badlogic.gdx.utils.viewport.ExtendViewport;
import com.badlogic.gdx.utils.viewport.ScreenViewport;

import java.util.Random;

public class GameScreen implements Screen {
    private final Main game;

    // ---------- constants ----------
    private static final int WORLD_WIDTH = 5000;
    private static final int WORLD_HEIGHT = 2000;
    private static final int V_WIDTH = 800;
    private static final int V_HEIGHT = 640;

    // Wave and kill tracking
    private int killCount = 0;
    private static final int KILL_TARGET = 20;
    private static final int WIN_TARGET = 21; // Target untuk menang
    private static final int WAVE_SIZE = 4;

    // Core rendering utilities
    private Stage gameStage;
    private Stage uiStage;
    private ShapeRenderer shapeR;
    private BitmapFont font;

    // Assets loaded once for reuse
    private Texture background;
    private Texture enemyTexture;
    private Texture playerSheet;
    private Texture npcSheet;
    private Texture bulletTexture;
    private TextureRegion bulletRegion;
    private Texture shieldTexture;
    private TextureRegion shieldRegion;

    // --- SOUND VARIABLES ---
    Sound s_shot, s_hit, s_dash;
    Sound s_heal;
    Sound s_die;

    // --- MUSIC VARIABLES ---
    Music bossMusic;

    private Texture healTexture;
    private Texture gameOverTexture;

    // Game objects collections
    private Player player;
    private NPC npc;
    private Array<Enemy> enemies = new Array<>();
    private Array<Bullet> bullets = new Array<>();
    private Array<Bullet> bossBullets = new Array<>();
    private Boss boss;
    private Array<Bomb> bossBombs = new Array<>();
    private Array<Explosion> explosions = new Array<>();

    // Enemy
    private Texture enemyIdle, enemyWalk, enemyRun, enemyShoot, enemyDie;
    private TextureRegion[] idleFrames, walkFrames, runFrames, shootFrames, dieFrames;

    // State and utilities
    private enum GameState {
        EXPLORE,
        DIALOG,
        GAMEOVER,
        VICTORY
    }
    private GameState currentState = GameState.DIALOG;
    private String[] dialogList = {
        "Para penjajah berusaha merebut tanah kita.",
        "Jaga tanah ini dengan sepenuh hatimu!",
        "Jangan pernah gentar meskipun nyawa taruhannya!"
    };
    private int dialogIndex = 0;
    private float stateTime = 0f;
    private Random rand = new Random();

    public GameScreen(Main game) {
        this.game = game;
        preloadAsset();
    }

    private void preloadAsset() {
        shapeR = new ShapeRenderer();
        font = new BitmapFont();

        // Load Sound (SFX)
        s_shot = Gdx.audio.newSound(Gdx.files.internal("sound/shot.mp3"));
        s_hit = Gdx.audio.newSound(Gdx.files.internal("sound/hit.mp3"));
        s_dash = Gdx.audio.newSound(Gdx.files.internal("sound/dash.mp3"));
        s_heal = Gdx.audio.newSound(Gdx.files.internal("sound/heal.ogg"));
        s_die = Gdx.audio.newSound(Gdx.files.internal("sound/mati.ogg"));

        // Load Music (Boss)
        bossMusic = Gdx.audio.newMusic(Gdx.files.internal("sound/boss.ogg"));
        bossMusic.setLooping(true);

        // Load textures once
        background = new Texture("bg.png");
        enemyTexture = new Texture("enemy.png");
        playerSheet = new Texture("sprite.png");
        npcSheet = new Texture("sprite.png");
        bulletTexture = new Texture("bullet.png");
        bulletRegion = new TextureRegion(bulletTexture);
        shieldTexture = new Texture("shield.png");
        shieldRegion = new TextureRegion(shieldTexture);
        healTexture = new Texture("heal.png");
        gameOverTexture = new Texture("gameover.png");

        enemyIdle  = new Texture("shooter/Soldier_1/Idle.png");
        enemyWalk  = new Texture("shooter/Soldier_1/Walk.png");
        enemyRun   = new Texture("shooter/Soldier_1/Run.png");
        enemyShoot = new Texture("shooter/Soldier_1/Shot_1.png");
        enemyDie   = new Texture("shooter/Soldier_1/Dead.png");

        boss = new Boss(
            5100, 0,
            new Texture("Standing.png"),
            new Texture("Run.png"),
            new Texture("Roll.png"),
            new Texture("bullet.png"),
            4.25f, s_shot
        );
    }

    @Override
    public void show() {
        // Camera & viewport
        gameStage = new Stage(new ExtendViewport(V_WIDTH, V_HEIGHT, new OrthographicCamera()));
        uiStage = new Stage(new ScreenViewport());
        InputMultiplexer im = new InputMultiplexer(uiStage, gameStage);
        Gdx.input.setInputProcessor(im);

        // Create main actor and NPC
        player = new Player(100, 0, playerSheet, playerSheet, s_hit, s_dash, s_heal, shieldRegion);
        player.setName("player");
        gameStage.addActor(player);

        npc = new NPC(-200, -150, npcSheet);
        npc.setName("npc");
        npc.startEnter();
        gameStage.addActor(npc);

        boss.setBulletsArray(bossBullets);
        boss.setBombsArray(bossBombs);
        boss.setName("boss");
        gameStage.addActor(boss);

        // Spawn first wave
        idleFrames  = new TextureRegion[7];
        walkFrames  = new TextureRegion[8];
        runFrames   = new TextureRegion[8];
        shootFrames = new TextureRegion[4];
        dieFrames   = new TextureRegion[5];

        for (int i = 0; i < 7; i++) idleFrames[i]  = new TextureRegion(enemyIdle,  i * 128, 0, 128, 128);
        for (int i = 0; i < 8; i++) walkFrames[i]  = new TextureRegion(enemyWalk,  i * 128, 0, 128, 128);
        for (int i = 0; i < 8; i++) runFrames[i]   = new TextureRegion(enemyRun,   i * 128, 0, 128, 128);
        for (int i = 0; i < 4; i++) shootFrames[i] = new TextureRegion(enemyShoot, i * 128, 0, 128, 128);
        for (int i = 0; i < 5; i++) dieFrames[i]   = new TextureRegion(enemyDie,   i * 128, 0, 128, 128);
        spawnWave();

        background.setWrap(Texture.TextureWrap.Repeat, Texture.TextureWrap.ClampToEdge);
    }

    private void spawnWave() {
        for (Enemy e : enemies) e.remove();
        enemies.clear();
        for (int i = 0; i < WAVE_SIZE; i++) {
            int x = (killCount == 0) ? rand.nextInt(1000, 2000) : rand.nextInt(200, 2000);
            float scale = 2f;
            Enemy e = new Enemy(x, 0, idleFrames, walkFrames, runFrames, shootFrames, dieFrames, scale);
            gameStage.addActor(e);
            enemies.add(e);
        }
    }

    @Override
    public void render(float d) {
        ScreenUtils.clear(0, 0, 0, 1);
        float delta = Gdx.graphics.getDeltaTime();
        stateTime += delta;

        // --- CEK GAME OVER ---
        if (player.health <= 0 && currentState != GameState.GAMEOVER) {
            currentState = GameState.GAMEOVER;
            s_die.play();
            if (bossMusic.isPlaying()) bossMusic.stop();
        }

        // --- UPDATE LOGIC (Hanya jalan jika TIDAK gameover) ---
        if (currentState != GameState.GAMEOVER) {

            // Update phase
            if (currentState == GameState.DIALOG) {
                player.updateDialog(delta);
            }
            gameStage.act(delta);
            uiStage.act(delta);

            // Enemies shoot
            for (Enemy e : enemies) {
                if (e.shouldFire()) {
                    shootFromEnemy(e);
                    e.lastShootTime = TimeUtils.nanoTime();
                }
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
                        if (!e.dead && atk.overlaps(e.getHitbox())) {
                            e.takeDamage(100);
                            player.hitRegistered = true;
                            break;
                        }
                    }
                }
            }

            if (player.isAttack && boss.active) {
                Rectangle atk = player.getAttackHitbox();
                if (atk != null && atk.overlaps(boss.hitbox) && !player.hitRegistered) {
                    boss.takeDamage(30);
                    player.hitRegistered = true;
                }
            }


            // Process dead enemies
            for (int i = enemies.size - 1; i >= 0; i--) {
                Enemy en = enemies.get(i);
                if (en.isDead()) {
                    if (!en.hasCountedKill()) {
                        en.setCountedKill(true);
                        killCount++;

                        // --- LOGIKA MUNCUL BOSS & MUSIK ---
                        if (killCount >= KILL_TARGET && !boss.active) {
                            boss.setActive(true);
                            boss.state = Boss.State.RUN;
                            bossMusic.play();
                        }

                    }
                    en.remove();
                    enemies.removeIndex(i);
                }
            }

            // --- LOGIKA BOSS MATI & END SCREEN ---
            if (boss.active && boss.hp <= 0) {
                boss.remove();
                boss.active = false;

                // Ubah kill count jadi 21 sesuai request
                killCount = WIN_TARGET;
                currentState = GameState.VICTORY;

                // Matikan musik boss
                if (bossMusic.isPlaying()) bossMusic.stop();

                // Pindah ke EndScreen
                game.setScreen(new EndScreen(game));
                dispose(); // Bersihkan GameScreen
                return;    // Hentikan render frame ini
            }

            // Spawn next wave
            if (enemies.size == 0 && killCount < KILL_TARGET && currentState == GameState.EXPLORE) {
                spawnWave();
            }

            // Bullets update
            for (int i = bullets.size - 1; i >= 0; i--) {
                Bullet b = bullets.get(i);
                b.rect.x += b.velX * delta;
                b.rect.y += b.velY * delta;

                if (b.rect.overlaps(player.hitbox)) {
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
            // ==== BOMB UPDATE ====
            for (int i = bossBombs.size - 1; i >= 0; i--) {
                Bomb b = bossBombs.get(i);
                b.update(delta);

                if (b.rect.overlaps(player.hitbox) && player.isShielding) {
                    explosions.add(new Explosion(b.rect.x + 25, b.rect.y + 25, boss.getExplosionAnim()));
                    bossBombs.removeIndex(i);
                    continue;
                }
                if (b.rect.overlaps(player.hitbox) && !player.isShielding) {
                    player.health -= 20;
                    if (player.health < 0) player.health = 0;
                }
                if (b.exploded) {
                    explosions.add(new Explosion(b.rect.x + 25, b.rect.y + 25, boss.getExplosionAnim()));
                    s_hit.play();
                    bossBombs.removeIndex(i);
                }
            }
            // == EXPLOSION ==
            for (int i = explosions.size - 1; i >= 0; i--) {
                Explosion ex = explosions.get(i);
                ex.update(delta);

                if (ex.life <= 0) {
                    explosions.removeIndex(i);
                    continue;
                }
                float dx = (player.hitbox.x + player.hitbox.width/2) - ex.x;
                float dy = (player.hitbox.y + player.hitbox.height/2) - ex.y;
                float dist = (float)Math.sqrt(dx*dx + dy*dy);

                if (!ex.hasDamaged && dist <= ex.radius) {
                    if (!player.isShielding && player.health > 0) {
                        player.health -= 40;
                    }
                    ex.hasDamaged = true;
                }
                if (ex.life <= 0) explosions.removeIndex(i);
            }
        } // End of if(!GAMEOVER)

        // Camera Update
        Camera cam = gameStage.getCamera();
        float halfW = ((ExtendViewport)gameStage.getViewport()).getWorldWidth() / 2f;
        float px = player.getX() + player.getWidth() / 2f;
        cam.position.x = MathUtils.clamp(px, halfW, WORLD_WIDTH - halfW);
        cam.update();

        // ---------- Rendering ----------
        SpriteBatch batch = (SpriteBatch)gameStage.getBatch();
        batch.setProjectionMatrix(cam.combined);
        batch.begin();
        batch.draw(background, 0, 0);
        batch.draw(background, background.getWidth(), 0);
        batch.draw(background, 2 * background.getWidth(), 0);
        batch.end();

        // draw stages
        gameStage.draw();

        // boss batch
        batch.begin();
        for (Bomb b : bossBombs) {
            if (!b.exploded) batch.draw(b.sprite, b.rect.x, b.rect.y, 50, 50);
        }
        for (int i = explosions.size - 1; i >= 0; i--) {
            Explosion ex = explosions.get(i);
            ex.update(delta);
            if (ex.life <= 0) {
                explosions.removeIndex(i);
                continue;
            }
            TextureRegion frame = ex.getFrame();
            batch.draw(frame, ex.x - 64, ex.y - 64, 128, 128);
        }
        batch.end();

        // --- HUD SECTION ---
        SpriteBatch uiBatch = (SpriteBatch)uiStage.getBatch();
        uiBatch.setProjectionMatrix(uiStage.getCamera().combined);

        uiBatch.begin();
        float worldWidth = uiStage.getViewport().getWorldWidth();
        float worldHeight = uiStage.getViewport().getWorldHeight();

        font.setColor(Color.WHITE);
        font.draw(uiStage.getBatch(), "Kills: " + killCount + " / " + KILL_TARGET, 20, worldHeight - 20);
        font.draw(uiStage.getBatch(), player.getDashStatusString(), 20, worldHeight - 40);

        // --- HEAL UI LOGIC ---
        float healIconX = 150;
        float healIconY = worldHeight - 90;
        float healIconSize = 90;

        uiStage.getBatch().draw(healTexture, healIconX, healIconY, healIconSize, healIconSize);

        if (player.healTimer > 0) {
            font.setColor(Color.YELLOW);
            String cdText = String.format("%.0f", player.healTimer);
            font.getData().setScale(2.0f);
            font.draw(uiStage.getBatch(), cdText, healIconX + 35, healIconY + 10);
            font.getData().setScale(1.0f);
        }

        // Dialog text
        if (currentState == GameState.DIALOG) {
            font.setColor(Color.WHITE);
            font.draw(uiStage.getBatch(), dialogList[dialogIndex], 40, 80);
        }

        // --- GAMEOVER OVERLAY ---
        if (currentState == GameState.GAMEOVER) {
            uiBatch.draw(gameOverTexture, 0, 0, worldWidth, worldHeight);
        }

        uiBatch.end();

        // --- INPUT LOGIC GAMEOVER (Outside Batch) ---
        if (currentState == GameState.GAMEOVER) {
            if (Gdx.input.justTouched()) {
                game.setScreen(new GameScreen(game));
                dispose();
            }
        }

        // Draw health bars
        if (currentState != GameState.GAMEOVER) {
            shapeR.setProjectionMatrix(cam.combined);
            shapeR.begin(ShapeRenderer.ShapeType.Filled);
            if (boss.active) {
                float pct = boss.hp / boss.maxHp;
                shapeR.setColor(Color.RED);
                shapeR.rect(boss.getHitbox().x, boss.getHitbox().y + boss.getHitbox().height + 5, boss.getHitbox().width * pct, 12);
            }
            for (Enemy e : enemies) {
                float pct = (e.maxHp <= 0) ? 0f : (e.hp / e.maxHp);
                shapeR.setColor(Color.RED);
                shapeR.rect(e.getHitbox().x, e.getHitbox().y + e.getHitbox().height + 10, 100 * pct, 10);
            }
            shapeR.setColor(Color.RED);
            shapeR.rect(player.getHitbox().x, player.getHitbox().y + player.getHitbox().height + 10, 100 * (player.health / player.MAX_HEALTH), 10);
            shapeR.end();

            shapeR.begin(ShapeRenderer.ShapeType.Line);
            shapeR.setColor(Color.YELLOW);
            shapeR.rect(player.getHitbox().x, player.getHitbox().y, player.getHitbox().width, player.getHitbox().height);
            shapeR.rect(boss.getHitbox().x, boss.getHitbox().y, boss.getHitbox().width, boss.getHitbox().height);
            for (Enemy e : enemies) shapeR.rect(e.getHitbox().x, e.getHitbox().y, e.getHitbox().width, e.getHitbox().height);
            shapeR.end();
        }
    }

    void shootFromEnemy(Enemy e) {
        float startX = e.getHitbox().x + e.getHitbox().width / 2;
        if (e.facingRight) startX += 30; else startX -= 130;
        float startY = e.getHitbox().y + e.getHitbox().height / 2 - 25;

        float targetX = player.getHitbox().x + player.getHitbox().width / 2;
        float bulletSpeed = 500f;
        float velX = (targetX > startX) ? bulletSpeed : -bulletSpeed;
        float velY = 0;

        Bullet bullet = new Bullet(bulletRegion, startX, startY, 20, 20, velX, velY, player);
        bullet.rotation = (velX < 0) ? 0 : 180;
        bullets.add(bullet);
        s_shot.play();
        gameStage.addActor(bullet);

        e.lastShootTime = TimeUtils.nanoTime();
    }

    @Override
    public void resize(int width, int height) {
        gameStage.getViewport().update(width, height, true);
        uiStage.getViewport().update(width, height, true);
    }

    @Override public void hide() {}
    @Override public void pause() {}
    @Override public void resume() {}

    @Override
    public void dispose() {
        shapeR.dispose();
        font.dispose();
        gameStage.dispose();
        uiStage.dispose();
        background.dispose();
        enemyTexture.dispose();
        playerSheet.dispose();
        npcSheet.dispose();
        bulletTexture.dispose();
        shieldTexture.dispose();
        healTexture.dispose();
        gameOverTexture.dispose();

        s_shot.dispose();
        s_hit.dispose();
        s_dash.dispose();
        s_heal.dispose();
        s_die.dispose();

        bossMusic.dispose();
    }
}
