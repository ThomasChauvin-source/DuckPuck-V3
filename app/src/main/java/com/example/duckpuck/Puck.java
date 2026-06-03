package com.example.duckpuck;

public class Puck {

    public float x, y;
    public float vx, vy;
    public float radius;

    private static final float FAST_FRICTION = 0.992f;
    private static final float SLOW_FRICTION = 0.985f;
    private static final float STOP_SPEED = 0.08f;
    private static final float MAX_SPEED = 32f;
    private static final float SPEED_MULTIPLIER = 1.12f;

    public Puck(float x, float y, float radius) {
        this.x = x;
        this.y = y;
        this.radius = radius;
        this.vx = 0;
        this.vy = 0;
    }

    public void update() {
        x += vx * SPEED_MULTIPLIER;
        y += vy * SPEED_MULTIPLIER;

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

    public float getSpeed() {
        return (float) Math.sqrt(vx * vx + vy * vy);
    }

    public void clampSpeed() {
        float speed = getSpeed();
        if (speed > MAX_SPEED) {
            vx = (vx / speed) * MAX_SPEED;
            vy = (vy / speed) * MAX_SPEED;
        }
    }
}
