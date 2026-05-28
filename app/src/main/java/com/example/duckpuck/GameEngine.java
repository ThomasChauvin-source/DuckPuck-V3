package com.example.duckpuck;

import android.graphics.RectF;

public class GameEngine {

    // ── Limites du terrain (bords intérieurs des cordes) ──────────────────
    private float fLeft, fRight, fTop, fBottom;
    private float goalWidth;

    // ── Murs custom invisibles (obstacles décoratifs) ─────────────────────
    // Ces RectF représentent les zones de collision pixelées par-dessus l'image.
    // Remplis via setCustomWalls() depuis setupGame() si nécessaire.
    private RectF[] customWalls = new RectF[0];

    // Physique
    private static final float RESTITUTION    = 1.0f;
    private static final float IMPULSE_FACTOR = 2.0f;

    // ══════════════════════════════════════════════════════════════════════
    /**
     * @param fLeft   bord gauche intérieur du terrain (pixels absolus)
     * @param fRight  bord droit
     * @param fTop    bord haut
     * @param fBottom bord bas
     * @param goalWidth largeur de la cage (en pixels, centré verticalement)
     */
    public GameEngine(float fLeft, float fRight, float fTop, float fBottom, float goalWidth) {
        this.fLeft     = fLeft;
        this.fRight    = fRight;
        this.fTop      = fTop;
        this.fBottom   = fBottom;
        this.goalWidth = goalWidth;
    }

    /**
     * Définit des murs supplémentaires invisibles (obstacles décoratifs).
     * Appelle cette méthode depuis GameView.setupGame() pour coller aux décors.
     *
     * Exemple d'utilisation dans setupGame() :
     *   engine.setCustomWalls(new RectF[]{
     *       new RectF(W*0.20f, H*0.30f, W*0.22f, H*0.45f),  // rocher gauche
     *       new RectF(W*0.78f, H*0.55f, W*0.80f, H*0.70f),  // château sable
     *   });
     */
    public void setCustomWalls(RectF[] walls) {
        this.customWalls = (walls != null) ? walls : new RectF[0];
    }

    // ── Mise à jour physique principale ───────────────────────────────────
    /**
     * Retourne : 0 = pas de but, 1 = but équipe gauche, 2 = but équipe droite.
     */
    public int update(Puck puck, Mallet[] mallets) {
        puck.update();

        // ── Mur haut / bas ────────────────────────────────────────────────
        if (puck.y - puck.radius < fTop) {
            puck.y  = fTop + puck.radius;
            puck.vy = Math.abs(puck.vy) * RESTITUTION;
        }
        if (puck.y + puck.radius > fBottom) {
            puck.y  = fBottom - puck.radius;
            puck.vy = -Math.abs(puck.vy) * RESTITUTION;
        }

        // ── Buts ──────────────────────────────────────────────────────────
        float fH      = fBottom - fTop;
        float goalTop = fTop + (fH - goalWidth) / 2f;
        float goalBot = goalTop + goalWidth;

        // Mur gauche
        if (puck.x - puck.radius < fLeft) {
            if (puck.y > goalTop && puck.y < goalBot) {
                return 2;   // but pour équipe droite
            } else {
                puck.x  = fLeft + puck.radius;
                puck.vx = Math.abs(puck.vx) * RESTITUTION;
            }
        }

        // Mur droit
        if (puck.x + puck.radius > fRight) {
            if (puck.y > goalTop && puck.y < goalBot) {
                return 1;   // but pour équipe gauche
            } else {
                puck.x  = fRight - puck.radius;
                puck.vx = -Math.abs(puck.vx) * RESTITUTION;
            }
        }

        // ── Murs custom (obstacles décoratifs) ────────────────────────────
        for (RectF wall : customWalls) {
            resolveRectCollision(puck, wall);
        }

        // ── Maillets ──────────────────────────────────────────────────────
        for (Mallet mallet : mallets) {
            resolveMalletCollision(puck, mallet);
        }

        return 0;
    }

    // ── Collision palet <-> rectangle (mur custom) ────────────────────────
    /**
     * Rebond simple AABB : repousse le palet hors du rectangle,
     * en inversant la composante de vitesse selon la face touchée.
     */
    private void resolveRectCollision(Puck puck, RectF wall) {
        // Étendre le rectangle par le rayon du palet
        float left   = wall.left   - puck.radius;
        float right  = wall.right  + puck.radius;
        float top    = wall.top    - puck.radius;
        float bottom = wall.bottom + puck.radius;

        if (puck.x < left || puck.x > right ||
                puck.y < top  || puck.y > bottom) return;  // pas de collision

        // Pénétrations sur chaque face
        float dLeft   = puck.x - left;
        float dRight  = right  - puck.x;
        float dTop    = puck.y - top;
        float dBottom = bottom - puck.y;

        // Face avec pénétration minimale = face touchée
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

    // ── Collision palet <-> maillet ───────────────────────────────────────
    private void resolveMalletCollision(Puck puck, Mallet mallet) {
        float dx   = puck.x - mallet.x;
        float dy   = puck.y - mallet.y;
        float dist = (float) Math.sqrt(dx * dx + dy * dy);
        float minD = puck.radius + mallet.radius;

        if (dist >= minD || dist <= 0) return;

        float nx = dx / dist;
        float ny = dy / dist;

        // Séparation
        float overlap = minD - dist;
        puck.x += nx * overlap;
        puck.y += ny * overlap;

        // Vitesse relative
        float mvx  = mallet.getVelocityX();
        float mvy  = mallet.getVelocityY();
        float relVn = (puck.vx - mvx) * nx + (puck.vy - mvy) * ny;

        if (relVn < 0) {
            float impulse = -(1 + RESTITUTION) * relVn;
            puck.vx += impulse * nx + mvx * IMPULSE_FACTOR;
            puck.vy += impulse * ny + mvy * IMPULSE_FACTOR;
        }
    }

    // ── Redimensionnement dynamique ───────────────────────────────────────
    public void setFieldBounds(float left, float right, float top, float bottom) {
        this.fLeft   = left;
        this.fRight  = right;
        this.fTop    = top;
        this.fBottom = bottom;
    }
}