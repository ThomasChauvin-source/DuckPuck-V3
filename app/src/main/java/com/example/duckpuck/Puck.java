package com.example.duckpuck;

public class Puck {

    public float x, y;
    public float vx, vy; // vitesse
    public float radius;

    private static final float FRICTION = 0.999f;
    private static final float MAX_SPEED = 40f;
    private static final float SPEED_MULTIPLIER = 1.3f;


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

        // Friction légère
        vx *= FRICTION;
        vy *= FRICTION;

        // Limiter la vitesse max
        float speed = (float) Math.sqrt(vx * vx + vy * vy);
        if (speed > MAX_SPEED) {
            vx = (vx / speed) * MAX_SPEED;
            vy = (vy / speed) * MAX_SPEED;
        }
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
    }
}
