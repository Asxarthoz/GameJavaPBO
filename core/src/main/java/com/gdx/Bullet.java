package com.gdx;

import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.scenes.scene2d.Actor;

/**
 * Bullet sederhana sebagai Actor.
 * Gerak berdasarkan velX/velY, cek tabrakan ke player, dan menghapus diri saat keluar world.
 */
public class Bullet extends Actor {
    private TextureRegion region;
    public Rectangle rect;
    public float velX, velY;
    public float rotation = 0f;
    private Player target;
    private int worldWidth = 5000;
    private int worldHeight = 2000;

    public Bullet(TextureRegion region, float x, float y, float w, float h, float vX, float vY, Player target) {
        this.region = region;
        setBounds(x, y, w, h);
        rect = new Rectangle(x, y, w, h);
        this.velX = vX;
        this.velY = vY;
        this.target = target;
    }

    @Override
    public void act(float delta) {
        super.act(delta);
        // update posisi
        moveBy(velX * delta, velY * delta);
        rect.setPosition(getX(), getY());

        // tabrak player
        if (target != null && rect.overlaps(target.getHitbox())) {
            // jika player shield aktif, peluru hilang tanpa damage
            if (!target.isShielding &&  target.health > 0) {
                target.health -= 10;
                if (target.health < 0) target.health = 0;
            }
            remove(); // hapus diri
            return;
        }

        // keluar batas world -> hapus
        if (getX() < 0 || getX() > worldWidth || getY() < 0 || getY() > worldHeight) {
            remove();
        }
    }

    @Override
    public void draw(Batch batch, float parentAlpha) {
        batch.draw(region, getX(), getY(), getWidth(), getHeight());
    }

    public Rectangle getRect() {
        return rect;
    }
}
