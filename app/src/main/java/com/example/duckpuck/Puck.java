package com.example.duckpuck;

public class Puck {

    public float x, y;
    public float vx, vy;
    public float radius;

    private static final float FAST_FRICTION = 0.994f;
    private static final float SLOW_FRICTION = 0.989f;
    private static final float STOP_SPEED = 0.10f;
    private static final float BASE_MAX_SPEED = 35f;
    private static final float BASE_SPEED_MULTIPLIER = 1.24f;

    private float speedScale = 1f;

    public Puck(float x, float y, float radius) {
        this.x = x;
        this.y = y;
        this.radius = radius;
        this.vx = 0;
        this.vy = 0;
    }

    public void update() {
        x += vx * BASE_SPEED_MULTIPLIER * speedScale;
        y += vy * BASE_SPEED_MULTIPLIER * speedScale;

        float speed = getSpeed();
        float friction = speed > 12f ? FAST_FRICTION : SLOW_FRICTION;
        vx *= friction;
        vy *= friction;

        if (speed < STOP_SPEED) {
            vx = 0;
            vy = 0;
        }
        clampSpeed();
    }

    public void reset(float cx, float cy) {
        x = cx;
        y = cy;
        vx = 0;
        vy = 0;
    }

    public void applyVelocity(float dvx, float dvy) {
        vx += dvx;
        vy += dvy;
        clampSpeed();
    }

    public void setSpeedScale(float speedScale) {
        this.speedScale = Math.max(1f, Math.min(1.75f, speedScale));
        clampSpeed();
    }

    public float getSpeed() {
        return (float) Math.sqrt(vx * vx + vy * vy);
    }

    public void clampSpeed() {
        float speed = getSpeed();
        float maxSpeed = BASE_MAX_SPEED * speedScale;
        if (speed > maxSpeed) {
            vx = (vx / speed) * maxSpeed;
            vy = (vy / speed) * maxSpeed;
        }
    }
}
