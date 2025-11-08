package com.gdx;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.Viewport;


/** {@link com.badlogic.gdx.ApplicationListener} implementation shared by all platforms. */
public class Main extends ApplicationAdapter {
    private SpriteBatch batch;
    private Texture playerText, grassText;
    OrthographicCamera camera;

    float playerX = 100, playerY = 100;
    float speed = 200f;
    int stamina = 1000;
    int gravity = -800;
    Rectangle playerRect;
    float velocityY = 0;
    boolean isGround = true;
    boolean isAttack = false;
    boolean facingRight = true;
    float attTime = 0f;
    Viewport viewport;
    TextureRegion[] walkFrames;
    TextureRegion[] attFrames;
    Animation<TextureRegion> walkAnimation;
    Animation<TextureRegion> attAnimation;
    float stateTime;
    ShapeRenderer shapeR;
    Texture attackText;

    @Override
    public void create() {

        shapeR = new ShapeRenderer();
        playerRect = new Rectangle(playerX, playerY, 64, 128);
        attackText = new Texture("attackSpriteC.png");
        batch = new SpriteBatch();
        playerText = new Texture("player_walk.png");
        int frameCols = 7;
        int frameRows = 1;

        TextureRegion[][] tmp = TextureRegion.split(
            playerText,
            playerText.getWidth() / frameCols,
            playerText.getHeight()
        );
        TextureRegion[][] tmpAtt = TextureRegion.split(
            attackText,
            attackText.getWidth() / 5,
            attackText.getHeight()
        );

        camera = new OrthographicCamera();
        viewport = new FitViewport(960, 540, camera);
        grassText = new Texture("grass.jpg");

        walkFrames = new TextureRegion[frameCols];
        for (int i = 0; i < frameCols; i++) {
            walkFrames[i] = tmp[0][i];
        }
        attFrames = new TextureRegion[5];
        for (int i = 0; i < 5; i++) {
            attFrames[i] = tmpAtt[0][i];
        }

        walkAnimation = new Animation<TextureRegion>(0.1f, walkFrames);
        attAnimation = new Animation<TextureRegion>(0.1f, attFrames);
        stateTime = 0f;



        camera.update();


    }

    @Override
    public void render() {
        ScreenUtils.clear(0, 0, 0, 1);
        float oldX = playerX;
        float oldY = playerY;

        float delta = Gdx.graphics.getDeltaTime();
        stateTime += delta;
        boolean gerak = false;

        if(Gdx.input.isKeyPressed(Input.Keys.D) && playerX < 5000) {
            playerX += speed * delta;
            facingRight = true;
            System.out.println("PlayerX: " + playerX + " | CameraX: " + camera.position.x);
            gerak = true;
        }

        if(Gdx.input.isKeyPressed(Input.Keys.A) && playerX > 23) {
            playerX -= speed * delta;
            facingRight = false;
            gerak = true;
            System.out.println(playerX);
        }

        if(Gdx.input.isKeyPressed(Input.Keys.W) && playerY < 355 && isGround) {
            velocityY = 400;
            isGround = false;
            System.out.println(playerY);
        }

        if(Gdx.input.isKeyPressed(Input.Keys.S) && playerY > 0) {
//            playerY -= speed * 4 * delta;
            System.out.println(playerY);
        }

        if(Gdx.input.isKeyPressed(Input.Keys.SHIFT_LEFT)) {
            speed = 550;
        } else {
            speed = 200;
        }

        if(Gdx.input.isButtonJustPressed(Input.Buttons.LEFT) && !isAttack) {
            isAttack = true;
            attTime = 0f;
        }


        TextureRegion currentFrame;
        if (isAttack) {
            attTime += delta;
            currentFrame = attAnimation.getKeyFrame(attTime, false);
            if (attAnimation.isAnimationFinished(attTime)) {
                isAttack = false;
                attTime = 0f;
            }
        } else if (gerak) {
            currentFrame = walkAnimation.getKeyFrame(stateTime, true);
        } else {
            currentFrame = walkFrames[0];
        }



        TextureRegion frame = walkAnimation.getKeyFrame(stateTime, true);

        // buat ngadep kanan kiri
        if (facingRight && currentFrame.isFlipX()) currentFrame.flip(true, false);
        if (!facingRight && !currentFrame.isFlipX()) currentFrame.flip(true, false);





        velocityY += gravity * delta;
        playerY += velocityY * delta;

        //cek tabrakan ke tembok
        playerRect.setPosition(playerX, playerY);


        if(playerY <= 0) {
            playerY = 0;
            velocityY = 0;
            isGround = true;
        }

        camera.position.x = playerX + 64 / 2;
        float halfW = viewport.getWorldWidth() / 2f;
        float worldWidth = 5000; // sesuai panjang dunia (rumput)
        if (camera.position.x < halfW) camera.position.x = halfW;
        if (camera.position.x > worldWidth - halfW) camera.position.x = worldWidth - halfW;

        camera.update();

        batch.setProjectionMatrix(camera.combined);



        batch.begin();

        for(int i = 0; i < 5000; i+=22) {
            batch.draw(grassText, i, 0, 22, 22);
        }

        float scale = 0.2f; // 30% dari ukuran aslinya
        batch.draw(currentFrame, playerX, playerY,
            (currentFrame.getRegionWidth() * scale),
            (currentFrame.getRegionHeight() * scale));


        batch.end();
        shapeR.setProjectionMatrix(camera.combined);
        shapeR.begin(ShapeRenderer.ShapeType.Line);
        shapeR.setColor(Color.RED);
        shapeR.rect(playerRect.x + 168, playerRect.y + 30, playerRect.width - 5, playerRect.height + 22);
        shapeR.end();
    }

    @Override
    public void resize(int width, int height) {
        viewport.update(width, height, true);
    }

    @Override
    public void dispose() {

        batch.dispose();
        playerText.dispose();
        grassText.dispose();
        attackText.dispose();
        shapeR.dispose();
    }
}
