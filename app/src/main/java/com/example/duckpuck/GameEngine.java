package com.example.duckpuck;

import android.graphics.RectF;

public class GameEngine {

    public interface OnCollisionListener {
        void onMalletHit();
    }
    private OnCollisionListener collisionListener;

    private float fLeft, fRight, fTop, fBottom;
    private float goalWidth;
    private RectF[] customWalls = new RectF[0];

    private static final float RESTITUTION    = 1.0f;
    private static final float IMPULSE_FACTOR = 2.0f;

    public GameEngine(float fLeft, float fRight, float fTop, float fBottom, float goalWidth) {
        this.fLeft     = fLeft;
        this.fRight    = fRight;
        this.fTop      = fTop;
        this.fBottom   = fBottom;
        this.goalWidth = goalWidth;
    }

    public void setOnCollisionListener(OnCollisionListener listener) {
        this.collisionListener = listener;
    }

    public void setCustomWalls(RectF[] walls) {
        this.customWalls = (walls != null) ? walls : new RectF[0];
    }

    public int update(Puck puck, Mallet[] mallets) {
        puck.update();

        if (puck.y - puck.radius < fTop) {
            puck.y  = fTop + puck.radius;
            puck.vy = Math.abs(puck.vy) * RESTITUTION;
        }
        if (puck.y + puck.radius > fBottom) {
            puck.y  = fBottom - puck.radius;
            puck.vy = -Math.abs(puck.vy) * RESTITUTION;
        }

        float fH      = fBottom - fTop;
        float goalTop = fTop + (fH - goalWidth) / 2f;
        float goalBot = goalTop + goalWidth;

        if (puck.x - puck.radius < fLeft) {
            if (puck.y > goalTop && puck.y < goalBot) {
                return 2;
            } else {
                puck.x  = fLeft + puck.radius;
                puck.vx = Math.abs(puck.vx) * RESTITUTION;
            }
        }

        if (puck.x + puck.radius > fRight) {
            if (puck.y > goalTop && puck.y < goalBot) {
                return 1;
            } else {
                puck.x  = fRight - puck.radius;
                puck.vx = -Math.abs(puck.vx) * RESTITUTION;
            }
        }

        for (RectF wall : customWalls) {
            resolveRectCollision(puck, wall);
        }

        for (Mallet mallet : mallets) {
            resolveMalletCollision(puck, mallet);
        }

        return 0;
    }

    private void resolveRectCollision(Puck puck, RectF wall) {
        float left   = wall.left   - puck.radius;
        float right  = wall.right  + puck.radius;
        float top    = wall.top    - puck.radius;
        float bottom = wall.bottom + puck.radius;

        if (puck.x < left || puck.x > right || puck.y < top  || puck.y > bottom) return;

        float dLeft   = puck.x - left;
        float dRight  = right  - puck.x;
        float dTop    = puck.y - top;
        float dBottom = bottom - puck.y;

        float minD = Math.min(Math.min(dLeft, dRight), Math.min(dTop, dBottom));

        if (minD == dLeft) {
            puck.x  = left;
            puck.vx = -Math.abs(puck.vx) * RESTITUTION;
        } else if (minD == dRight) {
            puck.x  = right;
            puck.vx = Math.abs(puck.vx) * RESTITUTION;
        } else if (minD == dTop) {
            puck.y  = top;
            puck.vy = -Math.abs(puck.vy) * RESTITUTION;
        } else {
            puck.y  = bottom;
            puck.vy = Math.abs(puck.vy) * RESTITUTION;
        }
    }

    private void resolveMalletCollision(Puck puck, Mallet mallet) {
        float dx   = puck.x - mallet.x;
        float dy   = puck.y - mallet.y;
        float dist = (float) Math.sqrt(dx * dx + dy * dy);
        float minD = puck.radius + mallet.radius;

        if (dist >= minD || dist <= 0) return;

        float nx = dx / dist;
        float ny = dy / dist;

        float overlap = minD - dist;
        puck.x += nx * overlap;
        puck.y += ny * overlap;

        float mvx  = mallet.getVelocityX();
        float mvy  = mallet.getVelocityY();
        float relVn = (puck.vx - mvx) * nx + (puck.vy - mvy) * ny;

        if (relVn < 0) {
            float impulse = -(1 + RESTITUTION) * relVn;
            puck.vx += impulse * nx + mvx * IMPULSE_FACTOR;
            puck.vy += impulse * ny + mvy * IMPULSE_FACTOR;

            if (collisionListener != null) {
                collisionListener.onMalletHit();
            }
        }
    }

    public void setFieldBounds(float left, float right, float top, float bottom) {
        this.fLeft   = left;
        this.fRight  = right;
        this.fTop    = top;
        this.fBottom = bottom;
    }
}