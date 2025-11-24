package com.gdx;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.TimeUtils;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.Viewport;

enum GameState {
    EXPLORE,
    DIALOG,
    GAMEOVER,
    VICTORY
}



/** {@link com.badlogic.gdx.ApplicationListener} implementation shared by all platforms. */
public class Main extends ApplicationAdapter {

    GameState currentState = GameState.DIALOG;
    static final float scale = 0.2f;
    private SpriteBatch batch;
    OrthographicCamera camera;
    Texture background;
    float bgWidth;
    Array<Enemy> enemies = new Array<>();

    Player player;
    NPC npc;

    Array<Bullet> bullets;
    Texture bulletTexture;
    TextureRegion bulletRegion;
    Viewport viewport;

    int dialogIndex = 0;
    float stateTime;
    ShapeRenderer shapeR;
    BitmapFont font;

    String[] dialogList = {
        "Para penjajah berusaha merebut tanah kita.",
        "Jaga tanah ini dengan sepenuh hatimu!",
        "Jangan pernah gentar meskipun nyawa taruhannya!"
    };

    @Override
    public void create() {

        batch = new SpriteBatch();
        player = new Player(100, 0, new Texture("sprite.png"), new Texture("sprite.png"));
        background = new Texture("bg.png");
        bgWidth = background.getWidth();
        background.setWrap(Texture.TextureWrap.Repeat, Texture.TextureWrap.ClampToEdge);

        // ukuran karakter sebenarnya
        int charX = 750;
        int charY = 410;
        int charW = 585;
        int charH = 920;

        shapeR = new ShapeRenderer();

        //bikin bullet
        bullets = new Array<>();
        bulletTexture = new Texture("bullet.png");
        bulletRegion = new TextureRegion(bulletTexture);

        font = new BitmapFont();
        npc = new NPC(-200, -100, new Texture("sprite.png"));
        npc.startEnter();

        camera = new OrthographicCamera();
        viewport = new FitViewport(960, 540, camera);

        stateTime = 0f;

        for (int i = 0; i < 4; i++) {
            enemies.add(new Enemy(1200 + i * 150, 0, new Texture("enemy.png"), 0.4f));
        }


        camera.update();
    }

    @Override
    public void render() {
        ScreenUtils.clear(0, 0, 0, 1);

        float delta = Gdx.graphics.getDeltaTime();
        stateTime += delta;

        if (currentState == GameState.DIALOG) {
            player.updateDialog(delta);
            npc.update(delta);     // NPC bergerak hanya saat dialog
        } else {
            player.update(delta);         // normal
            npc.update(delta);

            for (Enemy e : enemies) {
                e.update(delta, player.hitbox);
            }

        }

        for (Enemy e : enemies) {
            if (e.canShoot()) {
                shootFromEnemy(e);
            }
        }

        if (currentState == GameState.DIALOG) {

            // Klik untuk next dialog
            if (Gdx.input.isButtonJustPressed(Input.Buttons.LEFT)) {
                dialogIndex++;

                // Kalau dialog selesai
                if (dialogIndex >= dialogList.length) {

                    npc.startLeave();      // NPC mundur

                    currentState = GameState.EXPLORE;
                }
            }
        }

        if (player.isAttack) {
            Rectangle atk = player.getAttackHitbox();

            if (atk != null && !player.hitRegistered) {

                for (Enemy e : enemies) {
                    if (!e.dead && atk.overlaps(e.hitbox)) {
                        e.takeDamage(30);

                        player.hitRegistered = true;   // <=== hanya kena sekali
                        break;                         // hentikan loop
                    }
                }
            }
        }


        for (int i = enemies.size - 1; i >= 0; i--) {
            if (enemies.get(i).dead && enemies.get(i).deathTime >= 1f) {
                enemies.removeIndex(i);
            }
        }


        camera.position.x = player.x + player.hitbox.width / 2;
        float halfW = viewport.getWorldWidth() / 2f;
        float worldWidth = 5000;
        if (camera.position.x < halfW) camera.position.x = halfW;
        if (camera.position.x > worldWidth - halfW) camera.position.x = worldWidth - halfW;

        camera.update();

        for (int i = bullets.size - 1; i >= 0; i--) {
            Bullet b = bullets.get(i);

            b.rect.x += b.velX * delta;
            b.rect.y += b.velY * delta;

            // kena player?
            if (b.rect.overlaps(player.hitbox)) {
                if(player.health > 0) {
                    player.health -= 10;
                    bullets.removeIndex(i);
                    continue;
                }

            }

            // keluar layar
            if (b.rect.x < 0 || b.rect.x > 5000 || b.rect.y < 0 || b.rect.y > 2000) {
                bullets.removeIndex(i);
            }
        }


        batch.setProjectionMatrix(camera.combined);
        batch.begin();

        float camLeft = camera.position.x - viewport.getWorldWidth() / 2;

// posisi background mengikuti kamera
        float bgX = -(camLeft % bgWidth);

        batch.draw(background, 0, 0);
        batch.draw(background, 2048, 0);
        batch.draw(background, 4096, 0);

        TextureRegion frame = player.getFrame();
        batch.draw(frame, player.x, player.y,
            frame.getRegionWidth() * scale,
            frame.getRegionHeight() * scale);

        for (Enemy e : enemies) {
            batch.draw(
                e.getSprite(),
                e.x, e.y - 30,
                85, 125,          // originX, originY
                170, 250,         // width height
                1, 1,             // scaleX, scaleY
                e.dead ? e.rotation : 0   // rotate kalau mati
            );
        }

        for (Bullet b : bullets) {
            batch.draw(
                bulletRegion,
                b.rect.x, b.rect.y,
                b.rect.width / 2, b.rect.height / 2,   // origin rotasi
                b.rect.width + 30, b.rect.height + 30,
                1, 1,
                b.rotation                              // rotasi derajat
            );
        }

        TextureRegion npcFrame = npc.getFrame();

        batch.draw(
            npcFrame,
            npc.x,
            npc.y-21,
            npcFrame.getRegionWidth() * 0.2f,
            npcFrame.getRegionHeight() * 0.2f
        );



        batch.end();


        shapeR.setProjectionMatrix(camera.combined);
        shapeR.begin(ShapeRenderer.ShapeType.Filled);
        shapeR.setColor(Color.RED);

// HEALTH BAR
        for (Enemy e : enemies) {
            shapeR.rect(
                e.hitbox.x,
                e.hitbox.y + e.hitbox.height + 10,
                100 * (e.hp / e.maxHp),
                10
            );
        }


        shapeR.rect(
            player.hitbox.x,
            player.hitbox.y + player.hitbox.height + 10,
            100 * (player.health / player.MAX_HEALTH),
            10
        );

        shapeR.end();

        if (currentState == GameState.DIALOG) {
            // kasih background hitam semi transparan
            shapeR.begin(ShapeRenderer.ShapeType.Filled);
            shapeR.setColor(0, 0, 0, 0.5f);
            shapeR.rect(
                camera.position.x - viewport.getWorldWidth()/2 + 20,
                camera.position.y - viewport.getWorldHeight()/2 - 40,
                920, 100
            );
            shapeR.end();

            batch.begin();
            font.setColor(Color.WHITE);
            font.draw(
                batch,
                dialogList[dialogIndex],
                camera.position.x - viewport.getWorldWidth()/2 + 40,
                camera.position.y - viewport.getWorldHeight()/2 + 40
            );
            batch.end();

        }


        //ini buat bikin kotak buat debugging posisi karakter
        shapeR.setProjectionMatrix(camera.combined);
        shapeR.begin(ShapeRenderer.ShapeType.Line);
        shapeR.setColor(Color.RED);
        shapeR.rect(player.hitbox.x, player.hitbox.y, player.hitbox.width, player.hitbox.height);

        shapeR.end();

        shapeR.begin(ShapeRenderer.ShapeType.Line);
        shapeR.setColor(Color.RED);

// hitbox player
// hitbox enemy

        shapeR.end();
    }

    @Override
    public void resize(int width, int height) {
        viewport.update(width, height, true);
    }

    void shootFromEnemy(Enemy e) {

        float startX = e.hitbox.x + e.hitbox.width / 2;
        if(e.facingRight) {
            startX += 30;
        } else {
            startX -= 130;
        }
        float startY = e.hitbox.y + e.hitbox.height / 2 - 25;

        float targetX = player.hitbox.x + player.hitbox.width / 2;

        float bulletSpeed = 500f;

        // arah kiri/kanan
        float velX = (targetX > startX) ? bulletSpeed : -bulletSpeed;
        float velY = 0;

        Bullet bullet = new Bullet(
            startX, startY,
            20, 20,
            velX, velY);

        bullet.rotation = (velX < 0) ? 0 : 180;

        bullets.add(bullet);

        e.lastShootTime = TimeUtils.nanoTime();
    }

    @Override
    public void dispose() { // ini fungsi buat ngehapus texture biar ga ngebug
        batch.dispose();
        shapeR.dispose();
    }
}
