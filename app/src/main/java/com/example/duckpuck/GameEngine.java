package com.example.duckpuck;

public class GameEngine {

    private float tableWidth, tableHeight;
    private float goalWidth;
    private float wallThickness;

    // Restitution = rebond (1.0 = parfaitement élastique)
    private static final float RESTITUTION = 0.85f;
    // Transfert d'impulsion maillet -> palet
    private static final float IMPULSE_FACTOR = 1.4f;

    public GameEngine(float tableWidth, float tableHeight, float goalWidth, float wallThickness) {
        this.tableWidth = tableWidth;
        this.tableHeight = tableHeight;
        this.goalWidth = goalWidth;
        this.wallThickness = wallThickness;
    }

    /**
     * Met à jour la physique : déplace le palet et gère toutes les collisions.
     * Retourne : 0 = pas de but, 1 = but joueur 1 (gauche), 2 = but joueur 2 (droite)
     */
    public int update(Puck puck, Mallet[] mallets) {
        puck.update();

        // Collisions avec les murs (haut / bas)
        if (puck.y - puck.radius < wallThickness) {
            puck.y = wallThickness + puck.radius;
            puck.vy = Math.abs(puck.vy) * RESTITUTION;
        }
        if (puck.y + puck.radius > tableHeight - wallThickness) {
            puck.y = tableHeight - wallThickness - puck.radius;
            puck.vy = -Math.abs(puck.vy) * RESTITUTION;
        }

        // Collisions avec les bords gauche/droite (hors cage)
        float goalTop = (tableHeight - goalWidth) / 2f;
        float goalBottom = (tableHeight + goalWidth) / 2f;

        // Mur gauche
        if (puck.x - puck.radius < wallThickness) {
            // Est-ce dans la cage ?
            if (puck.y > goalTop && puck.y < goalBottom) {
                // BUT pour le joueur 2 (droite)
                return 2;
            } else {
                puck.x = wallThickness + puck.radius;
                puck.vx = Math.abs(puck.vx) * RESTITUTION;
            }
        }

        // Mur droit
        if (puck.x + puck.radius > tableWidth - wallThickness) {
            if (puck.y > goalTop && puck.y < goalBottom) {
                // BUT pour le joueur 1 (gauche)
                return 1;
            } else {
                puck.x = tableWidth - wallThickness - puck.radius;
                puck.vx = -Math.abs(puck.vx) * RESTITUTION;
            }
        }

        // Collisions avec les maillets
        for (Mallet mallet : mallets) {
            resolveMalletCollision(puck, mallet);
        }

        return 0; // pas de but
    }

    /**
     * Détecte et résout la collision entre le palet et un maillet.
     */
    private void resolveMalletCollision(Puck puck, Mallet mallet) {
        float dx = puck.x - mallet.x;
        float dy = puck.y - mallet.y;
        float dist = (float) Math.sqrt(dx * dx + dy * dy);
        float minDist = puck.radius + mallet.radius;

        if (dist < minDist && dist > 0) {
            // Vecteur normal de collision (normalisé)
            float nx = dx / dist;
            float ny = dy / dist;

            // Séparer les objets pour éviter l'interpénétration
            float overlap = minDist - dist;
            puck.x += nx * overlap;
            puck.y += ny * overlap;

            // Vitesse relative le long de la normale
            float malletVx = mallet.getVelocityX();
            float malletVy = mallet.getVelocityY();

            float relVx = puck.vx - malletVx;
            float relVy = puck.vy - malletVy;
            float relVn = relVx * nx + relVy * ny;

            // Ne résoudre que si le palet se rapproche du maillet
            if (relVn < 0) {
                // Impulsion
                float impulse = -(1 + RESTITUTION) * relVn;
                puck.vx += impulse * nx;
                puck.vy += impulse * ny;

                // Ajouter l'impulsion du mouvement du maillet
                puck.vx += malletVx * IMPULSE_FACTOR;
                puck.vy += malletVy * IMPULSE_FACTOR;
            }
        }
    }

    public void setTableSize(float width, float height) {
        this.tableWidth = width;
        this.tableHeight = height;
    }
}
