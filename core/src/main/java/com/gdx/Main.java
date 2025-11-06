package com.gdx;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
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
    Viewport viewport;

    @Override
    public void create() {

        playerRect = new Rectangle(playerX, playerY, 64, 128);

        batch = new SpriteBatch();
        playerText = new Texture("player/player1.png");
        camera = new OrthographicCamera();
        viewport = new FitViewport(960, 540, camera);
        grassText = new Texture("grass.jpg");



        camera.update();


    }

    @Override
    public void render() {
        float oldX = playerX;
        float oldY = playerY;

        ScreenUtils.clear(0.15f, 0.15f, 0.2f, 1f);
        float delta = Gdx.graphics.getDeltaTime();

        if(Gdx.input.isKeyPressed(Input.Keys.D) && playerX < 5000) {
            playerX += speed * delta;
            System.out.println("PlayerX: " + playerX + " | CameraX: " + camera.position.x);

        }

        if(Gdx.input.isKeyPressed(Input.Keys.A) && playerX > -12) {
            playerX -= speed * delta;
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

        batch.draw(playerText, playerX, playerY,64,128);

        batch.end();
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
    }
}
