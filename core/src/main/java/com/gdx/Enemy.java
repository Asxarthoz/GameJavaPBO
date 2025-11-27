    package com.gdx;

    import com.badlogic.gdx.graphics.g2d.TextureRegion;
    import com.badlogic.gdx.math.MathUtils;
    import com.badlogic.gdx.math.Rectangle;
    import com.badlogic.gdx.utils.TimeUtils;

    /**
     * Enemy AI and state.
     * Hybrid comments: explain intent and non-obvious choices (patrol, chase delay, damage cooldown).
     */
    public class Enemy {

        // Position and basic state (public for simple access from Main)
        public float x, y;
        public float hp = 100;
        public float maxHp = 100;

        // Death and removal
        public boolean dead = false;  // set true in takeDamage when hp <= 0
        public float deathTime = 0f;  // used to delay removal for death animation
        public float rotation = 0f;   // visual rotation when dying
        public long lastShootTime = 0;

        // Movement & facing
        public float speed = 100;
        public boolean facingRight = true;

        // Rendering and collision
        public TextureRegion sprite;
        public Rectangle hitbox;

        // Patrol & chase parameters
        float patrolTargetX;
        float patrolMinMove = 100;
        float patrolMaxMove = 300;

        // Delay before starting chase when player detected (gives 'look' animation)
        float detectDelay = 2f;
        float delayTimer = 0f;
        boolean detectedPlayer = false;

        // Prevents rapid repeated damage
        float damageCooldown = 0f;

        // Ensure kill counted only once
        private boolean countedKill = false;

        /**
         * Constructor expects a TextureRegion and scale so sprite and hitbox match render size.
         * Keeping scale out of Main gives better control per-enemy if desired later.
         */
        public Enemy(float x, float y, TextureRegion enemyRegion, float scale) {
            this.x = x;
            this.y = y;
            this.sprite = enemyRegion;
            this.maxHp = 100;
            this.hp = maxHp;
            float w = (sprite.getRegionWidth() * scale);
            float h = sprite.getRegionHeight() * scale;
            this.hitbox = new Rectangle(x, y, w, h);
            pickNewPatrolTarget();
        }

        // Choose a new random patrol endpoint near current x
        private void pickNewPatrolTarget() {
            float move = MathUtils.random(patrolMinMove, patrolMaxMove);
            if (MathUtils.randomBoolean()) {
                patrolTargetX = x + move;
                facingRight = true;
            } else {
                patrolTargetX = x - move;
                facingRight = false;
            }
        }

        /**
         * Update enemy each frame.
         * - If dead: increment deathTime (used by Main to remove after animation)
         * - If player detected: wait for detectDelay then chase
         * - Otherwise: patrol between random targets
         */
        public void update(float delta, Rectangle playerRect) {
            if (dead) {
                deathTime += delta;
                return;
            }

            float centerEnemy = x + hitbox.width / 2;
            float centerPlayer = playerRect.x + playerRect.width / 2;
            float dist = Math.abs(centerEnemy - centerPlayer);
            boolean playerInRange = dist <= 450;

            if (playerInRange) detectedPlayer = true;

            if (detectedPlayer) {
                delayTimer += delta;
                if (delayTimer < detectDelay) {
                    // remain idle during detection delay so player can react
                    hitbox.setPosition(x, y);
                    return;
                }
                chasePlayer(playerRect, delta);
            } else {
                patrol(delta);
            }

            if (damageCooldown > 0) damageCooldown -= delta;

            // Keep sprite orientation consistent (flip if needed)
            if (facingRight && sprite.isFlipX()) sprite.flip(true, false);
            if (!facingRight && !sprite.isFlipX()) sprite.flip(true, false);

            hitbox.setPosition(x, y);
        }

        // Simple horizontal chase: stops when close enough to avoid overlap
        private void chasePlayer(Rectangle playerRect, float delta) {
            float centerEnemy = x + hitbox.width / 2;
            float centerPlayer = playerRect.x + playerRect.width / 2;
            float distance = Math.abs(centerEnemy - centerPlayer);

            if (distance <= 300f) return;

            if (centerPlayer > centerEnemy-50) {
                x += speed * delta;
                facingRight = true;
            } else {
                x -= speed * delta;
                facingRight = false;
            }
        }

        // Patrol movement between random targets
        private void patrol(float delta) {
            if (facingRight) {
                x += speed * delta;
                if (x >= patrolTargetX) pickNewPatrolTarget();
            } else {
                x -= speed * delta;
                if (x <= patrolTargetX) pickNewPatrolTarget();
            }
        }

        public TextureRegion getSprite() {
            return sprite;
        }

        /**
         * Can shoot only when detected and sufficient time passed since last shot.
         * Also prevents shooting when dead.
         */
        public boolean canShoot() {
            return detectedPlayer && TimeUtils.nanoTime() - lastShootTime > 2_000_000_000L && !dead;
        }

        /**
         * Apply damage with a brief damageCooldown to avoid multi-hit in one frame.
         * On death we set dead=true and begin death animation parameters.
         */
        public void takeDamage(float dmg) {
            if (dead) return;
            if (damageCooldown > 0) return;
            damageCooldown = 0.35f;
            hp -= dmg;
            if (hp <= 0) {
                dead = true;
                hp = 0;
                // stop behavior and set visual death state
    //            canShoot = false;
                speed = 0;
                rotation = 90f;
                y -= 90; // visually sink a bit
                deathTime = 0f;
                hitbox.setPosition(x, y);
            }
        }

        // Accessors for Main to mark counted kills
        public boolean hasCountedKill() {
            return countedKill;
        }

        public void setCountedKill(boolean v) {
            countedKill = v;
        }
    }
