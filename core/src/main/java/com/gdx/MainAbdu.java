package com.gdx;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.TimeUtils;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.Viewport;

public class MainAbdu extends ApplicationAdapter {

    // --- CLASS PELURU (Static agar lebih aman) ---
    static class Bullet {
        Rectangle rect;
        float velX, velY;

        public Bullet(float x, float y, float width, float height, float vX, float vY) {
            this.rect = new Rectangle(x, y, width, height);
            this.velX = vX;
            this.velY = vY;
        }
    }

    // --- CLASS MUSUH ---
    static class Enemy {
        Rectangle rect;
        float hp;
        float maxHp;
        boolean facingRight;
        long lastShootTime;
        float speed;

        public Enemy(float x, float y) {
            this.rect = new Rectangle(x, y, 100, 200);
            this.maxHp = 100;
            this.hp = this.maxHp;
            this.facingRight = false;
            this.speed = MathUtils.random(60f, 90f);
            this.lastShootTime = TimeUtils.nanoTime() + MathUtils.random(0, 1000000000L);
        }
    }

    // --- ENUM STATUS GAME ---
    enum GameState {
        EXPLORE, DIALOG, COMBAT, GAMEOVER, VICTORY
    }

    GameState currentState = GameState.EXPLORE;

    private SpriteBatch batch;
    private Texture playerText, grassText, attackText;
    private Texture[] bgLayers;

    // --- ASET SOEKARNO ---
    private Texture soekarnoText;
    private Rectangle soekarnoRect;
    private boolean soekarnoActive = true;

    // --- ASET MUSUH ---
    private Texture enemyText;
    private Array<Enemy> enemies;
    private boolean enemiesActive = false;

    // --- PELURU ---
    private Texture bulletText;
    private Array<Bullet> bullets;

    // --- UI & DIALOG ---
    private BitmapFont font;
    private int dialogIndex = 0;
    private String[] dialogList = {
        "Soekarno:\nAh, kau datang juga. Aku sudah mendengar tentang keberanianmu.\nApa kau siap menanggung konsekuensi dari perjuangan ini, anak muda?",
        "Player:\nSaya siap, Bung Karno. Apapun yang harus saya hadapi\ndemi Indonesia merdeka.",
        "Soekarno:\n(Senyum tipis) Bagus. Kemerdekaan bukan hadiah.\nIa harus direbut—dengan tenaga, pikiran, dan tekad yang tak pernah padam.\nIngat itu baik-baik.",
        "Player:\nBung, apa yang harus saya lakukan?",
        "Soekarno:\nBelanda semakin memperketat penjagaan. Banyak rakyat kita ditangkap\nhanya karena berani berbicara. Kita butuh orang seperti kau—\nyang tak gentar, meski bayangan laras senapan mengintai.",
        "Player:\nSaya tidak akan mundur.",
        "Soekarno:\nBaik. Tapi dengarkan aku... Perjuangan bukan hanya tentang mengangkat senjata.\nIni tentang keyakinan bahwa bangsa yang besar harus berdiri di atas kakinya sendiri.\nKau adalah bagian dari itu.",
        "Soekarno:\nNamun... malam ini situasinya berubah.",
        "Player:\nApa yang terjadi, Bung?",
        "Soekarno:\nPasukan Belanda sudah menemukan lokasi markas ini.\nKita tidak punya banyak waktu. Kau harus membantu menahan mereka\nagar rakyat yang berada di belakang rumah ini bisa melarikan diri.",
        "Player:\nSaya akan bertempur, Bung!",
        "Soekarno:\n(Suara tegas) Dengarkan! Kau bukan hanya bertempur untukku,\natau untuk dirimu sendiri. Kau bertempur untuk jutaan jiwa\nyang menunggu hari kemerdekaan.\nPergilah! Tunjukkan bahwa bangsa ini tidak akan tunduk!",
        "Player:\nBaik, Bung Karno. Saya akan melindungi semua orang yang ada di sini!",
        "Soekarno:\nSemoga semangatmu menerobos gelapnya penjajahan ini.\nIndonesia... ada di ujung tombakmu, anak muda.",
        "(Percakapan berakhir. Tiba-tiba terdengar suara teriakan dan tembakan di luar.)"
    };

    // --- PLAYER STATS ---
    OrthographicCamera camera;
    float playerX, playerY;
    float speed = 200f;
    int gravity = -800;
    Rectangle playerRect;
    float playerHp = 100;
    float playerMaxHp = 100;

    float velocityY = 0;
    boolean isGround = true;
    boolean isAttack = false;
    boolean hasDealtDamage = false;
    boolean facingRight = true;
    float attTime = 0f;

    Viewport viewport;
    TextureRegion[] walkFrames;
    TextureRegion[] attFrames;
    Animation<TextureRegion> walkAnimation;
    Animation<TextureRegion> attAnimation;
    float stateTime;
    ShapeRenderer shapeR;

    float playerScale = 0.2f;
    final float SECTION_WIDTH = 960f * 3f;
    final float WORLD_WIDTH = SECTION_WIDTH * 5;

    Vector3 touchPoint;

    // --- HITBOX SETTINGS ---
    float manualHitboxWidth = 75f;
    float manualHitboxHeight = 200f;
    float hitboxOffsetY = 0f;
    float attackAdjustY = 30f;
    float attackAdjustX = -10f;

    @Override
    public void create() {
        shapeR = new ShapeRenderer();
        batch = new SpriteBatch();
        font = new BitmapFont();
        font.getData().setScale(1.3f);
        font.setColor(Color.WHITE);
        touchPoint = new Vector3();

        bullets = new Array<>();
        enemies = new Array<>();

        // --- LOAD ASSETS DENGAN SAFETY CHECK ---
        // Jika file tidak ada, buat kotak warna di RAM (tidak simpan file, biar ga crash permission)
        playerText = loadTextureOrDummy("player_walk.png", Color.BLUE);
        attackText = loadTextureOrDummy("attackSpriteC.png", Color.CYAN);
        grassText  = loadTextureOrDummy("grass.jpg", Color.GREEN);
        soekarnoText = loadTextureOrDummy("soekarno.png", Color.ORANGE);
        enemyText  = loadTextureOrDummy("enemy.png", Color.RED);

        // Buat tekstur peluru
        Pixmap pixmap = new Pixmap(10, 10, Pixmap.Format.RGBA8888);
        pixmap.setColor(Color.YELLOW);
        pixmap.fillCircle(5, 5, 5);
        bulletText = new Texture(pixmap);
        pixmap.dispose();

        // Background
        bgLayers = new Texture[5];
        for (int i = 0; i < 5; i++) {
            try {
                bgLayers[i] = new Texture("layer" + (i + 1) + ".png");
                bgLayers[i].setWrap(Texture.TextureWrap.Repeat, Texture.TextureWrap.Repeat);
            } catch (Exception e) {
                // Jika background kosong, biarkan null (nanti di handle di draw)
                bgLayers[i] = null;
            }
        }

        // --- SETUP ANIMASI ---
        // Kita cek dulu apakah playerText itu dummy (kecil) atau asli (besar)
        // untuk menghindari error split
        TextureRegion[][] tmp;
        int frameCols = 7;

        if (playerText.getWidth() < 100) { // Berarti dummy
            tmp = TextureRegion.split(playerText, playerText.getWidth(), playerText.getHeight());
            frameCols = 1;
        } else {
            try {
                tmp = TextureRegion.split(playerText, playerText.getWidth() / frameCols, playerText.getHeight());
            } catch(Exception e) {
                // Fallback kalau dimensi tidak pas
                tmp = TextureRegion.split(playerText, playerText.getWidth(), playerText.getHeight());
                frameCols = 1;
            }
        }

        walkFrames = new TextureRegion[frameCols];
        for (int i = 0; i < frameCols; i++) walkFrames[i] = tmp[0][i];

        // Setup Animasi Attack
        TextureRegion[][] tmpAtt;
        try {
            tmpAtt = TextureRegion.split(attackText, attackText.getWidth() / 5, attackText.getHeight());
        } catch(Exception e) {
            tmpAtt = TextureRegion.split(attackText, attackText.getWidth(), attackText.getHeight());
        }

        attFrames = new TextureRegion[Math.min(5, tmpAtt[0].length)];
        for (int i = 0; i < attFrames.length; i++) attFrames[i] = tmpAtt[0][i];

        walkAnimation = new Animation<TextureRegion>(0.1f, walkFrames);
        attAnimation = new Animation<TextureRegion>(0.1f, attFrames);
        stateTime = 0f;

        camera = new OrthographicCamera();
        viewport = new FitViewport(960, 540, camera);

        resetGame();
    }

    // Fungsi Helper Aman: Load file, jika gagal buat kotak warna
    private Texture loadTextureOrDummy(String fileName, Color fallbackColor) {
        try {
            return new Texture(fileName);
        } catch (Exception e) {
            // Buat tekstur di memori saja
            Pixmap p = new Pixmap(100, 100, Pixmap.Format.RGBA8888);
            p.setColor(fallbackColor);
            p.fill();
            Texture t = new Texture(p);
            p.dispose();
            return t;
        }
    }

    private void resetGame() {
        playerX = 100;
        playerY = 100;
        playerHp = playerMaxHp;
        playerRect = new Rectangle(playerX, playerY, manualHitboxWidth, manualHitboxHeight);

        soekarnoActive = true;
        soekarnoRect = new Rectangle(400, 0, 100, 200);

        enemies.clear();
        enemiesActive = false;

        bullets.clear();
        currentState = GameState.EXPLORE;
        dialogIndex = 0;
        isAttack = false;
        facingRight = true;
        velocityY = 0;

        camera.position.set(viewport.getWorldWidth() / 2, viewport.getWorldHeight() / 2, 0);
    }

    @Override
    public void render() {
        float delta = Gdx.graphics.getDeltaTime();

        if (currentState == GameState.GAMEOVER) {
            renderGameOver();
            return;
        }

        updatePlayer(delta);
        updateCamera();

        // Logika Combat hanya jalan jika sudah dipicu dialog
        if (currentState == GameState.COMBAT) {
            updateCombat(delta);
        }

        ScreenUtils.clear(0, 0, 0, 1);
        batch.setProjectionMatrix(camera.combined);
        shapeR.setProjectionMatrix(camera.combined);

        batch.begin();
        drawBackground();
        drawEntities(delta);
        drawUI();
        batch.end();

        drawHealthBars();
    }

    private void updatePlayer(float delta) {
        stateTime += delta;
        boolean gerak = false;

        if(Gdx.input.isKeyPressed(Input.Keys.D) && playerX < WORLD_WIDTH) {
            playerX += speed * delta;
            facingRight = true;
            gerak = true;
        }
        if(Gdx.input.isKeyPressed(Input.Keys.A) && playerX > 0) {
            playerX -= speed * delta;
            facingRight = false;
            gerak = true;
        }
        if(Gdx.input.isKeyPressed(Input.Keys.W) && playerY < 355 && isGround) {
            velocityY = 600;
            isGround = false;
        }
        if(Gdx.input.isKeyPressed(Input.Keys.SHIFT_LEFT)) speed = 550;
        else speed = 200;

        if(Gdx.input.isButtonJustPressed(Input.Buttons.LEFT)) {
            touchPoint.set(Gdx.input.getX(), Gdx.input.getY(), 0);
            camera.unproject(touchPoint);

            if (soekarnoActive && soekarnoRect.contains(touchPoint.x, touchPoint.y)) {
                handleDialog();
            } else {
                if(!isAttack) {
                    isAttack = true;
                    attTime = 0f;
                    hasDealtDamage = false;
                }
            }
        }

        velocityY += gravity * delta;
        playerY += velocityY * delta;
        if(playerY <= 0) { playerY = 0; velocityY = 0; isGround = true; }

        TextureRegion currentFrame = getCurrentFrame(gerak);
        float visualWidth = currentFrame.getRegionWidth() * playerScale;
        float hitboxOffsetX = (visualWidth - manualHitboxWidth) / 2;
        playerRect.setPosition(playerX + hitboxOffsetX, playerY + hitboxOffsetY);
    }

    private void handleDialog() {
        currentState = GameState.DIALOG;
        dialogIndex++;
        if (dialogIndex >= dialogList.length) {
            currentState = GameState.COMBAT;
            soekarnoActive = false;
            spawnEnemies();
            dialogIndex = 0;
        }
    }

    private void spawnEnemies() {
        enemiesActive = true;
        enemies.clear(); // Pastikan bersih
        for(int i = 0; i < 3; i++) {
            float spawnX = playerX + 600 + (i * 150);
            Enemy e = new Enemy(spawnX, 0);
            enemies.add(e);
        }
    }

    private void updateCombat(float delta) {
        if (!enemiesActive) return;

        if (enemies.size == 0) {
            currentState = GameState.VICTORY;
            enemiesActive = false;
            return;
        }

        // --- LOOP MUSUH (MENGGUNAKAN INDEX AGAR AMAN) ---
        for (int i = 0; i < enemies.size; i++) {
            Enemy e = enemies.get(i);

            // 1. AI Gerak
            float rangeStop = 200f;
            float centerXPlayer = playerRect.x + playerRect.width/2;
            float centerXEnemy = e.rect.x + e.rect.width/2;
            float distance = centerXEnemy - centerXPlayer;

            if (Math.abs(distance) > rangeStop) {
                if (distance > 0) {
                    e.rect.x -= e.speed * delta;
                    e.facingRight = false;
                } else {
                    e.rect.x += e.speed * delta;
                    e.facingRight = true;
                }
            } else {
                e.facingRight = (distance < 0);
            }

            // 2. Separasi (Biar ga numpuk)
            for (int j = 0; j < enemies.size; j++) {
                if (i == j) continue;
                Enemy other = enemies.get(j);
                if (e.rect.overlaps(other.rect)) {
                    float distBetween = e.rect.x - other.rect.x;
                    if (distBetween > 0) e.rect.x += 100 * delta;
                    else e.rect.x -= 100 * delta;
                }
            }

            // 3. Tembak
            if (TimeUtils.nanoTime() - e.lastShootTime > 2000000000L) {
                shootBullet(e);
                e.lastShootTime = TimeUtils.nanoTime();
            }
        }

        // --- UPDATE PELURU (Loop Terbalik untuk Remove Aman) ---
        for (int i = bullets.size - 1; i >= 0; i--) {
            Bullet b = bullets.get(i);
            b.rect.x += b.velX * delta;
            b.rect.y += b.velY * delta;

            if (b.rect.overlaps(playerRect)) {
                playerHp -= 5;
                bullets.removeIndex(i);
                if (playerHp <= 0) currentState = GameState.GAMEOVER;
            }
            else if (b.rect.x < 0 || b.rect.x > WORLD_WIDTH || b.rect.y < 0) {
                bullets.removeIndex(i);
            }
        }

        // --- MELEE ATTACK PLAYER ---
        if (isAttack && !hasDealtDamage) {
            Rectangle attackHitbox = new Rectangle(playerRect);
            if (facingRight) attackHitbox.x += 50;
            else attackHitbox.x -= 50;

            boolean hitAnyone = false;
            // Loop terbalik untuk menghapus musuh yang mati
            for (int i = enemies.size - 1; i >= 0; i--) {
                Enemy e = enemies.get(i);
                if (attackHitbox.overlaps(e.rect)) {
                    e.hp -= 25;
                    hitAnyone = true;
                    if(e.hp <= 0) {
                        enemies.removeIndex(i);
                    }
                }
            }
            if (hitAnyone) hasDealtDamage = true;
        }
    }

    private void shootBullet(Enemy shooter) {
        float startX = shooter.rect.x + shooter.rect.width / 2;
        float startY = shooter.rect.y + shooter.rect.height / 2;
        float targetX = playerRect.x + playerRect.width / 2;
        float targetY = playerRect.y + playerRect.height / 2;

        float angle = MathUtils.atan2(targetY - startY, targetX - startX);
        float bulletSpeed = 400f;

        bullets.add(new Bullet(startX, startY, 20, 20,
            bulletSpeed * MathUtils.cos(angle),
            bulletSpeed * MathUtils.sin(angle)));
    }

    private void updateCamera() {
        TextureRegion frame = getCurrentFrame(false);
        float visualWidth = frame.getRegionWidth() * playerScale;
        camera.position.x = playerX + visualWidth / 2;

        float halfW = viewport.getWorldWidth() / 2f;
        if (camera.position.x < halfW) camera.position.x = halfW;
        if (camera.position.x > WORLD_WIDTH - halfW) camera.position.x = WORLD_WIDTH - halfW;
        camera.update();
    }

    private TextureRegion getCurrentFrame(boolean gerak) {
        TextureRegion currentFrame;
        if (isAttack) {
            attTime += Gdx.graphics.getDeltaTime();
            currentFrame = attAnimation.getKeyFrame(attTime, false);
            if (attAnimation.isAnimationFinished(attTime)) { isAttack = false; attTime = 0f; }
        } else if (gerak) {
            currentFrame = walkAnimation.getKeyFrame(stateTime, true);
        } else {
            currentFrame = walkFrames[0];
        }

        if (facingRight && currentFrame.isFlipX()) currentFrame.flip(true, false);
        if (!facingRight && !currentFrame.isFlipX()) currentFrame.flip(true, false);
        return currentFrame;
    }

    private void drawEntities(float delta) {
        if (soekarnoActive) {
            batch.draw(soekarnoText, soekarnoRect.x, soekarnoRect.y, soekarnoRect.width, soekarnoRect.height);
        }

        if (enemiesActive) {
            for (Enemy e : enemies) {
                boolean flipX = !e.facingRight;
                // Gunakan fungsi draw aman
                batch.draw(enemyText,
                    e.rect.x, e.rect.y,
                    e.rect.width, e.rect.height,
                    0, 0,
                    enemyText.getWidth(), enemyText.getHeight(),
                    flipX, false);
            }
        }

        for (Bullet b : bullets) {
            batch.draw(bulletText, b.rect.x, b.rect.y, b.rect.width, b.rect.height);
        }

        for(int i = 0; i < WORLD_WIDTH; i+=22) {
            batch.draw(grassText, i, 0, 22, 22);
        }

        TextureRegion currentFrame = getCurrentFrame(false);
        float vWidth = currentFrame.getRegionWidth() * playerScale;
        float vHeight = currentFrame.getRegionHeight() * playerScale;
        float drawX = playerX;
        float drawY = playerY;
        if (isAttack) { drawX += attackAdjustX; drawY += attackAdjustY; }
        batch.draw(currentFrame, drawX, drawY, vWidth, vHeight);
    }

    private void drawUI() {
        if (currentState == GameState.DIALOG || (currentState == GameState.EXPLORE && soekarnoActive && dialogIndex == 0)) {
            if (soekarnoActive && dialogIndex > 0) {
                float textX = camera.position.x - 300;
                float textY = camera.position.y + 200;
                font.draw(batch, dialogList[dialogIndex], textX, textY);
            }
        }
        if (currentState == GameState.VICTORY) {
            font.setColor(Color.GREEN);
            font.draw(batch, "MERDEKA! Musuh Kalah!", camera.position.x - 100, camera.position.y + 100);
            font.setColor(Color.WHITE);
        }
    }

    private void drawHealthBars() {
        shapeR.begin(ShapeRenderer.ShapeType.Filled);

        float uiX = camera.position.x - viewport.getWorldWidth()/2 + 20;
        float uiY = camera.position.y + viewport.getWorldHeight()/2 - 40;

        shapeR.setColor(Color.GRAY);
        shapeR.rect(uiX, uiY, 200, 20);
        shapeR.setColor(Color.GREEN);
        shapeR.rect(uiX, uiY, 200 * (playerHp / playerMaxHp), 20);

        if (enemiesActive) {
            for (Enemy e : enemies) {
                shapeR.setColor(Color.GRAY);
                shapeR.rect(e.rect.x, e.rect.y + e.rect.height + 10, 100, 10);
                shapeR.setColor(Color.RED);
                shapeR.rect(e.rect.x, e.rect.y + e.rect.height + 10, 100 * (e.hp / e.maxHp), 10);
            }
        }
        shapeR.end();
    }

    private void drawBackground() {
        float screenHeight = viewport.getWorldHeight();
        for (int i = 0; i < 5; i++) {
            if (bgLayers[i] != null) {
                Texture t = bgLayers[i];
                batch.draw(t, i * SECTION_WIDTH, 0, SECTION_WIDTH, screenHeight,
                    0, 0, t.getWidth() * 3, t.getHeight(), false, false);
            }
        }
    }

    private void renderGameOver() {
        ScreenUtils.clear(0, 0, 0, 1);
        batch.begin();
        font.setColor(Color.RED);
        font.getData().setScale(3);
        GlyphLayout layout = new GlyphLayout(font, "GAME OVER\nKlik untuk Ulang");
        float textX = camera.position.x - layout.width / 2;
        float textY = camera.position.y + layout.height / 2;
        font.draw(batch, layout, textX, textY);
        batch.end();
        if (Gdx.input.isTouched()) {
            font.getData().setScale(1.3f);
            font.setColor(Color.WHITE);
            resetGame();
        }
    }

    @Override
    public void resize(int width, int height) { viewport.update(width, height, true); }

    @Override
    public void dispose() {
        batch.dispose();
        if(playerText!=null) playerText.dispose();
        if(grassText!=null) grassText.dispose();
        if(attackText!=null) attackText.dispose();
        shapeR.dispose();
        font.dispose();
        if(soekarnoText != null) soekarnoText.dispose();
        if(enemyText != null) enemyText.dispose();
        if(bulletText != null) bulletText.dispose();
        for(Texture t : bgLayers) { if(t != null) t.dispose(); }
    }
}
