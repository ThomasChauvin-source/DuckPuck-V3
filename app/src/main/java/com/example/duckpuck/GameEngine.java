package com.example.duckpuck;

import android.graphics.RectF;

public class GameEngine {

    public interface OnCollisionListener {
        void onMalletHit(int malletIndex);
        void onWallHit();
    }
    private OnCollisionListener collisionListener;

    private float fLeft, fRight, fTop, fBottom;
    private float goalWidth;
    private RectF[] customWalls = new RectF[0];

    private static final float WALL_RESTITUTION = 0.96f;
    private static final float WALL_TANGENT_DAMPING = 0.992f;
    private static final float MALLET_RESTITUTION = 1.08f;
    private static final float MALLET_TRANSFER = 0.64f;
    private static final float MALLET_SPIN_TRANSFER = 0.16f;
    private static final float MIN_HIT_BOOST = 7.5f;

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
            puck.vy = Math.abs(puck.vy) * WALL_RESTITUTION;
            puck.vx *= WALL_TANGENT_DAMPING;
            notifyWallHit();
        }
        if (puck.y + puck.radius > fBottom) {
            puck.y  = fBottom - puck.radius;
            puck.vy = -Math.abs(puck.vy) * WALL_RESTITUTION;
            puck.vx *= WALL_TANGENT_DAMPING;
            notifyWallHit();
        }

        float fH      = fBottom - fTop;
        float goalTop = fTop + (fH - goalWidth) / 2f;
        float goalBot = goalTop + goalWidth;

        if (puck.x - puck.radius < fLeft) {
            if (puck.y > goalTop && puck.y < goalBot) {
                return 2;
            } else {
                puck.x  = fLeft + puck.radius;
                puck.vx = Math.abs(puck.vx) * WALL_RESTITUTION;
                puck.vy *= WALL_TANGENT_DAMPING;
                notifyWallHit();
            }
        }

        if (puck.x + puck.radius > fRight) {
            if (puck.y > goalTop && puck.y < goalBot) {
                return 1;
            } else {
                puck.x  = fRight - puck.radius;
                puck.vx = -Math.abs(puck.vx) * WALL_RESTITUTION;
                puck.vy *= WALL_TANGENT_DAMPING;
                notifyWallHit();
            }
        }
        puck.clampSpeed();

        for (RectF wall : customWalls) {
            resolveRectCollision(puck, wall);
        }

        for (int i = 0; i < mallets.length; i++) {
            resolveMalletCollision(puck, mallets[i], i);
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
            puck.vx = -Math.abs(puck.vx) * WALL_RESTITUTION;
            puck.vy *= WALL_TANGENT_DAMPING;
        } else if (minD == dRight) {
            puck.x  = right;
            puck.vx = Math.abs(puck.vx) * WALL_RESTITUTION;
            puck.vy *= WALL_TANGENT_DAMPING;
        } else if (minD == dTop) {
            puck.y  = top;
            puck.vy = -Math.abs(puck.vy) * WALL_RESTITUTION;
            puck.vx *= WALL_TANGENT_DAMPING;
        } else {
            puck.y  = bottom;
            puck.vy = Math.abs(puck.vy) * WALL_RESTITUTION;
            puck.vx *= WALL_TANGENT_DAMPING;
        }
        notifyWallHit();
        puck.clampSpeed();
    }

    private void resolveMalletCollision(Puck puck, Mallet mallet, int malletIndex) {
        float dx   = puck.x - mallet.x;
        float dy   = puck.y - mallet.y;
        float dist = (float) Math.sqrt(dx * dx + dy * dy);
        float minD = puck.radius + mallet.radius;
        boolean sweptHit = false;

        if (dist >= minD || dist <= 0) {
            float hitX = mallet.x;
            float hitY = mallet.y;
            float moveX = mallet.x - mallet.prevX;
            float moveY = mallet.y - mallet.prevY;
            float moveLenSq = moveX * moveX + moveY * moveY;
            if (moveLenSq <= 0f) return;

            float t = ((puck.x - mallet.prevX) * moveX + (puck.y - mallet.prevY) * moveY) / moveLenSq;
            t = clamp(t, 0f, 1f);
            hitX = mallet.prevX + moveX * t;
            hitY = mallet.prevY + moveY * t;
            dx = puck.x - hitX;
            dy = puck.y - hitY;
            dist = (float) Math.sqrt(dx * dx + dy * dy);
            if (dist >= minD || dist <= 0) return;

            sweptHit = true;
        }

        float nx = dx / dist;
        float ny = dy / dist;

        float overlap = (minD - dist) + (sweptHit ? puck.radius * 0.35f : 0f);
        puck.x += nx * overlap;
        puck.y += ny * overlap;

        float mvx  = clamp(mallet.getVelocityX(), -34f, 34f);
        float mvy  = clamp(mallet.getVelocityY(), -34f, 34f);
        float malletSpeed = (float) Math.sqrt(mvx * mvx + mvy * mvy);
        float relVn = (puck.vx - mvx) * nx + (puck.vy - mvy) * ny;

        if (relVn < 0 || malletSpeed > 0.45f || sweptHit) {
            float approach = Math.max(0f, -relVn);
            float impulse = Math.max(MIN_HIT_BOOST, approach * (1f + MALLET_RESTITUTION) + malletSpeed * 0.22f);
            float tangentX = -ny;
            float tangentY = nx;
            float tangentSpeed = mvx * tangentX + mvy * tangentY;

            puck.vx += impulse * nx
                    + mvx * MALLET_TRANSFER
                    + tangentX * tangentSpeed * MALLET_SPIN_TRANSFER;
            puck.vy += impulse * ny
                    + mvy * MALLET_TRANSFER
                    + tangentY * tangentSpeed * MALLET_SPIN_TRANSFER;
            puck.clampSpeed();

            if (collisionListener != null) {
                collisionListener.onMalletHit(malletIndex);
            }
        }
    }

    public void setFieldBounds(float left, float right, float top, float bottom) {
        this.fLeft   = left;
        this.fRight  = right;
        this.fTop    = top;
        this.fBottom = bottom;
    }

    private float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }

    private void notifyWallHit() {
        if (collisionListener != null) {
            collisionListener.onWallHit();
        }
    }
}
