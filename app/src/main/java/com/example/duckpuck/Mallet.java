package com.example.duckpuck;

public class Mallet {

    public float x, y;
    public float prevX, prevY;
    public float radius;
    public int color;

    // Zone de restriction (demi-terrain)
    public float minX, maxX, minY, maxY;

    public Mallet(float x, float y, float radius, int color) {
        this.x = x;
        this.y = y;
        this.prevX = x;
        this.prevY = y;
        this.radius = radius;
        this.color = color;
    }

    public void setZone(float minX, float maxX, float minY, float maxY) {
        this.minX = minX;
        this.maxX = maxX;
        this.minY = minY;
        this.maxY = maxY;
    }

    /**
     * Déplace le maillet vers la position tactile tout en restant dans sa zone
     */
    public void moveTo(float targetX, float targetY) {
        prevX = x;
        prevY = y;

        x = Math.max(minX + radius, Math.min(maxX - radius, targetX));
        y = Math.max(minY + radius, Math.min(maxY - radius, targetY));
    }

    /**
     * Vitesse instantanée du maillet (utilisée pour l'impulsion sur le palet)
     */
    public float getVelocityX() {
        return x - prevX;
    }

    public float getVelocityY() {
        return y - prevY;
    }

    public void reset(float rx, float ry) {
        x = rx;
        y = ry;
        prevX = rx;
        prevY = ry;
    }
}
