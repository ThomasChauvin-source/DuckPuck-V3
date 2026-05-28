package com.example.duckpuck;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RadialGradient;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.os.Handler;
import android.os.Looper;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

public class GameView extends SurfaceView implements SurfaceHolder.Callback {

    // ── Modes de jeu ──────────────────────────────────────────────────────
    public static final int MODE_2P = 2;
    public static final int MODE_4P = 4;

    // ── Paramètres de jeu ─────────────────────────────────────────────────
    private static final int   MAX_SCORE    = 7;
    private static final long  FRAME_MS     = 16;   // ~60 FPS

    // Ratios mesurés sur l'image 1642×958
    // Terrain jouable (intérieur des cordes)
    private static final float FIELD_LEFT_R   = 0.085f;
    private static final float FIELD_RIGHT_R  = 0.914f;
    private static final float FIELD_TOP_R    = 0.120f;
    private static final float FIELD_BOTTOM_R = 0.960f;

    // HUD bar (barre en bois en haut)
    private static final float HUD_TOP_R    = 0.008f;
    private static final float HUD_BOTTOM_R = 0.104f;

    // Cases HUD  (x ratios par rapport à W)
    private static final float HUD_BOX1_L = 0.301f;  // Score gauche
    private static final float HUD_BOX1_R = 0.423f;
    private static final float HUD_BOX2_L = 0.434f;  // Palets restants
    private static final float HUD_BOX2_R = 0.555f;
    private static final float HUD_BOX3_L = 0.565f;  // Score droit
    private static final float HUD_BOX3_R = 0.683f;
    private static final float HUD_BOX4_L = 0.684f;  // Pause
    private static final float HUD_BOX4_R = 0.812f;

    // Taille des éléments de jeu (relatifs au terrain)
    private static final float PUCK_RADIUS_RATIO   = 0.045f;
    private static final float MALLET_RADIUS_RATIO = 0.070f;

    // Couleurs joueurs
    private static final int COLOR_PUCK = 0xFFE0E0E0;
    private static final int COLOR_P1   = 0xFFFF5252;
    private static final int COLOR_P2   = 0xFF40C4FF;
    private static final int COLOR_P3   = 0xFF69F0AE;
    private static final int COLOR_P4   = 0xFFFFD740;

    // ── État du jeu ───────────────────────────────────────────────────────
    private enum GameState { COUNTDOWN, PLAYING, GOAL, PAUSED, GAME_OVER }
    private GameState state      = GameState.COUNTDOWN;
    private GameState stateBeforePause;

    private int    mode;
    private int[]  scores;
    private int    countdownValue  = 3;
    private long   lastCountdownTime;
    private long   goalDisplayTime;
    private int    lastScorer      = 0;

    // ── Objets du jeu ─────────────────────────────────────────────────────
    private Puck    puck;
    private Mallet[] mallets;
    private GameEngine engine;

    // ── Dimensions écran et terrain ───────────────────────────────────────
    private float W, H;          // dimensions SurfaceView
    private float fLeft, fRight, fTop, fBottom;   // bords intérieurs terrain
    private float fW, fH;        // largeur/hauteur terrain
    private float goalWidth;
    private float puckRadius, malletRadius;

    // ── Rendu ─────────────────────────────────────────────────────────────
    private Bitmap  bgBitmap;
    private Paint   paintPuck, paintShadow, paintGoalFlash;
    private Paint   paintHudText, paintHudSub;
    private Paint   paintOverlay;
    private Paint[] paintMallets;
    private RectF   screenRect;

    // Rectangles HUD (calculés dans setupGame)
    private RectF hudBox1, hudBox2, hudBox3, hudBox4;
    private float hudCY;   // centre y de la barre HUD

    // ── Boucle de jeu ─────────────────────────────────────────────────────
    private GameThread gameThread;

    // ── Pointeurs tactiles ────────────────────────────────────────────────
    private int[] malletPointerIds;

    // ── Callback vers l'activité ──────────────────────────────────────────
    public interface GameListener {
        void onGameOver(int winnerTeam, int[] scores);
    }
    private GameListener listener;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    // ══════════════════════════════════════════════════════════════════════
    public GameView(Context context, int mode) {
        super(context);
        this.mode   = mode;
        this.scores = new int[2];
        getHolder().addCallback(this);
        setFocusable(true);
        initPaints();
    }

    public void setGameListener(GameListener l) { this.listener = l; }

    // ── Chargement du fond ────────────────────────────────────────────────
    private void loadBackground() {
        // L'image doit être dans res/drawable sous le nom "game_bg"
        int resId = getResources().getIdentifier("game_bg", "drawable",
                getContext().getPackageName());
        if (resId != 0) {
            BitmapFactory.Options opts = new BitmapFactory.Options();
            opts.inScaled = false;
            Bitmap raw = BitmapFactory.decodeResource(getResources(), resId, opts);
            bgBitmap = Bitmap.createScaledBitmap(raw, (int) W, (int) H, true);
            if (raw != bgBitmap) raw.recycle();
        }
    }

    // ── Initialisation des Paints ─────────────────────────────────────────
    private void initPaints() {
        paintShadow = new Paint(Paint.ANTI_ALIAS_FLAG);
        paintShadow.setColor(Color.BLACK);
        paintShadow.setAlpha(80);

        paintPuck = new Paint(Paint.ANTI_ALIAS_FLAG);
        paintPuck.setColor(COLOR_PUCK);

        paintGoalFlash = new Paint(Paint.ANTI_ALIAS_FLAG);
        paintGoalFlash.setColor(Color.WHITE);
        paintGoalFlash.setAlpha(60);

        paintOverlay = new Paint(Paint.ANTI_ALIAS_FLAG);
        paintOverlay.setColor(Color.BLACK);
        paintOverlay.setAlpha(140);

        // Texte HUD principal (grands chiffres)
        paintHudText = new Paint(Paint.ANTI_ALIAS_FLAG);
        paintHudText.setColor(Color.WHITE);
        paintHudText.setTextAlign(Paint.Align.CENTER);
        paintHudText.setFakeBoldText(true);
        paintHudText.setShadowLayer(3f, 2f, 2f, Color.BLACK);

        // Texte HUD sous-titre
        paintHudSub = new Paint(Paint.ANTI_ALIAS_FLAG);
        paintHudSub.setColor(0xFFFFD700);  // or
        paintHudSub.setTextAlign(Paint.Align.CENTER);
        paintHudSub.setFakeBoldText(true);
        paintHudSub.setShadowLayer(2f, 1f, 1f, Color.BLACK);

        paintMallets = new Paint[4];
        int[] colors = {COLOR_P1, COLOR_P2, COLOR_P3, COLOR_P4};
        for (int i = 0; i < 4; i++) {
            paintMallets[i] = new Paint(Paint.ANTI_ALIAS_FLAG);
            paintMallets[i].setColor(colors[i]);
        }
    }

    // ── Setup après connaissance des dimensions ───────────────────────────
    private void setupGame() {
        loadBackground();

        // Terrain intérieur (pixels absolus)
        fLeft   = W * FIELD_LEFT_R;
        fRight  = W * FIELD_RIGHT_R;
        fTop    = H * FIELD_TOP_R;
        fBottom = H * FIELD_BOTTOM_R;
        fW      = fRight - fLeft;
        fH      = fBottom - fTop;

        // But : 35% de la hauteur de terrain, centré
        goalWidth = fH * 0.35f;

        // Tailles des éléments
        puckRadius   = Math.min(fW, fH) * PUCK_RADIUS_RATIO;
        malletRadius = Math.min(fW, fH) * MALLET_RADIUS_RATIO;

        screenRect = new RectF(0, 0, W, H);

        // Cases HUD
        float hudY1 = H * HUD_TOP_R;
        float hudY2 = H * HUD_BOTTOM_R;
        hudCY = (hudY1 + hudY2) / 2f;
        hudBox1 = new RectF(W * HUD_BOX1_L, hudY1, W * HUD_BOX1_R, hudY2);
        hudBox2 = new RectF(W * HUD_BOX2_L, hudY1, W * HUD_BOX2_R, hudY2);
        hudBox3 = new RectF(W * HUD_BOX3_L, hudY1, W * HUD_BOX3_R, hudY2);
        hudBox4 = new RectF(W * HUD_BOX4_L, hudY1, W * HUD_BOX4_R, hudY2);

        // Centre du terrain
        float cx = fLeft + fW / 2f;
        float cy = fTop  + fH / 2f;

        // Palet au centre du terrain
        puck = new Puck(cx, cy, puckRadius);

        // Maillets (positions relatives au terrain)
        if (mode == MODE_2P) {
            mallets = new Mallet[2];
            mallets[0] = new Mallet(fLeft + fW * 0.18f, cy, malletRadius, COLOR_P1);
            mallets[0].setZone(fLeft, cx, fTop, fBottom);
            mallets[1] = new Mallet(fLeft + fW * 0.82f, cy, malletRadius, COLOR_P2);
            mallets[1].setZone(cx, fRight, fTop, fBottom);
        } else {
            mallets = new Mallet[4];
            mallets[0] = new Mallet(fLeft + fW * 0.25f, fTop + fH * 0.35f, malletRadius, COLOR_P1);
            mallets[0].setZone(fLeft, cx, fTop, fBottom);
            mallets[2] = new Mallet(fLeft + fW * 0.10f, fTop + fH * 0.65f, malletRadius, COLOR_P3);
            mallets[2].setZone(fLeft, cx, fTop, fBottom);
            mallets[1] = new Mallet(fLeft + fW * 0.75f, fTop + fH * 0.35f, malletRadius, COLOR_P2);
            mallets[1].setZone(cx, fRight, fTop, fBottom);
            mallets[3] = new Mallet(fLeft + fW * 0.90f, fTop + fH * 0.65f, malletRadius, COLOR_P4);
            mallets[3].setZone(cx, fRight, fTop, fBottom);
        }

        malletPointerIds = new int[mallets.length];
        for (int i = 0; i < malletPointerIds.length; i++) malletPointerIds[i] = -1;

        engine = new GameEngine(fLeft, fRight, fTop, fBottom, goalWidth);

        updatePuckGradient();
    }

    private void updatePuckGradient() {
        RadialGradient gradient = new RadialGradient(
                puck.x - puckRadius * 0.3f,
                puck.y - puckRadius * 0.3f,
                puckRadius * 1.2f,
                new int[]{Color.WHITE, COLOR_PUCK, 0xFFBDBDBD},
                new float[]{0f, 0.4f, 1f},
                Shader.TileMode.CLAMP
        );
        paintPuck.setShader(gradient);
    }

    // ── Boucle de jeu ─────────────────────────────────────────────────────
    private class GameThread extends Thread {
        private volatile boolean running = true;
        @Override
        public void run() {
            while (running) {
                long start = System.currentTimeMillis();
                tick();
                draw();
                long elapsed = System.currentTimeMillis() - start;
                long sleep   = FRAME_MS - elapsed;
                if (sleep > 0) {
                    try { Thread.sleep(sleep); } catch (InterruptedException ignored) {}
                }
            }
        }
        void stopThread() { running = false; }
    }

    private void tick() {
        long now = System.currentTimeMillis();
        switch (state) {
            case COUNTDOWN:
                if (now - lastCountdownTime >= 1000) {
                    countdownValue--;
                    lastCountdownTime = now;
                    if (countdownValue <= 0) state = GameState.PLAYING;
                }
                break;

            case PLAYING:
                int result = engine.update(puck, mallets);
                updatePuckGradient();
                if (result == 1) { scores[0]++; lastScorer = 1; onGoal(); }
                else if (result == 2) { scores[1]++; lastScorer = 2; onGoal(); }
                break;

            case GOAL:
                if (now - goalDisplayTime >= 2000) {
                    if (scores[0] >= MAX_SCORE || scores[1] >= MAX_SCORE) {
                        state = GameState.GAME_OVER;
                        notifyGameOver();
                    } else {
                        resetPositions();
                        startCountdown();
                    }
                }
                break;

            case PAUSED:
            case GAME_OVER:
                break;
        }
    }

    private void onGoal() {
        state = GameState.GOAL;
        goalDisplayTime = System.currentTimeMillis();
    }

    private void resetPositions() {
        float cx = fLeft + fW / 2f;
        float cy = fTop  + fH / 2f;
        puck.reset(cx, cy);
        if (mode == MODE_2P) {
            mallets[0].reset(fLeft + fW * 0.18f, cy);
            mallets[1].reset(fLeft + fW * 0.82f, cy);
        } else {
            mallets[0].reset(fLeft + fW * 0.25f, fTop + fH * 0.35f);
            mallets[2].reset(fLeft + fW * 0.10f, fTop + fH * 0.65f);
            mallets[1].reset(fLeft + fW * 0.75f, fTop + fH * 0.35f);
            mallets[3].reset(fLeft + fW * 0.90f, fTop + fH * 0.65f);
        }
        for (int i = 0; i < malletPointerIds.length; i++) malletPointerIds[i] = -1;
    }

    private void startCountdown() {
        countdownValue    = 3;
        lastCountdownTime = System.currentTimeMillis();
        state             = GameState.COUNTDOWN;
    }

    private void notifyGameOver() {
        if (listener == null) return;
        int winner = scores[0] >= MAX_SCORE ? 1 : 2;
        mainHandler.post(() -> listener.onGameOver(winner, scores));
    }

    // ── DESSIN ────────────────────────────────────────────────────────────
    private void draw() {
        SurfaceHolder holder = getHolder();
        if (!holder.getSurface().isValid()) return;
        Canvas canvas = holder.lockCanvas();
        if (canvas == null) return;
        try {
            drawBackground(canvas);
            drawMallets(canvas);
            drawPuck(canvas);
            drawHUD(canvas);
            drawOverlay(canvas);
        } finally {
            holder.unlockCanvasAndPost(canvas);
        }
    }

    /** Fond : bitmap si disponible, sinon fallback couleur */
    private void drawBackground(Canvas canvas) {
        if (bgBitmap != null) {
            canvas.drawBitmap(bgBitmap, 0, 0, null);
        } else {
            // Fallback : fond bleu foncé simple
            Paint p = new Paint();
            p.setColor(0xFF0A3D6B);
            canvas.drawRect(screenRect, p);
        }
    }

    private void drawMallets(Canvas canvas) {
        for (int i = 0; i < mallets.length; i++) {
            Mallet m = mallets[i];
            canvas.drawCircle(m.x + 4, m.y + 4, m.radius, paintShadow);
            canvas.drawCircle(m.x, m.y, m.radius, paintMallets[i]);
            // Reflet
            Paint shine = new Paint(Paint.ANTI_ALIAS_FLAG);
            shine.setColor(Color.WHITE);
            shine.setAlpha(80);
            canvas.drawCircle(m.x - m.radius * 0.25f, m.y - m.radius * 0.25f,
                    m.radius * 0.35f, shine);
            // Bouton central
            Paint center = new Paint(Paint.ANTI_ALIAS_FLAG);
            center.setColor(Color.BLACK);
            center.setAlpha(60);
            canvas.drawCircle(m.x, m.y, m.radius * 0.25f, center);
        }
    }

    private void drawPuck(Canvas canvas) {
        canvas.drawCircle(puck.x + 5, puck.y + 5, puck.radius, paintShadow);
        canvas.drawCircle(puck.x, puck.y, puck.radius, paintPuck);
    }

    /** HUD : les 4 cases de la barre en bois */
    private void drawHUD(Canvas canvas) {
        float boxH   = hudBox1.height();
        float hudCYr = hudCY;  // centre vertical de la barre

        // ── Case 1 : Score équipe gauche ──
        float scoreSize = boxH * 0.62f;
        float labelSize = boxH * 0.22f;
        paintHudText.setTextSize(scoreSize);
        paintHudSub.setTextSize(labelSize);

        float cx1 = hudBox1.centerX();
        // Label "ROUGE" en haut
        paintHudSub.setColor(COLOR_P1);
        canvas.drawText("ROUGE", cx1, hudCYr - boxH * 0.08f, paintHudSub);
        // Score centré
        paintHudText.setColor(Color.WHITE);
        canvas.drawText(String.valueOf(scores[0]), cx1, hudCYr + boxH * 0.28f, paintHudText);

        // ── Case 2 : Palets restants ──
        float cx2 = hudBox2.centerX();
        // Nombre de palets = MAX_SCORE - total buts marqués
        int totalGoals   = scores[0] + scores[1];
        int paletsLeft   = (MAX_SCORE * 2) - totalGoals;  // ou autre logique métier
        paintHudText.setTextSize(scoreSize * 0.75f);
        paintHudSub.setColor(0xFFFFD700);
        canvas.drawText("PALETS", cx2, hudCYr - boxH * 0.08f, paintHudSub);
        paintHudText.setColor(0xFFFFD700);
        canvas.drawText(String.valueOf(paletsLeft), cx2, hudCYr + boxH * 0.28f, paintHudText);
        paintHudText.setTextSize(scoreSize);  // restore

        // ── Case 3 : Score équipe droite ──
        float cx3 = hudBox3.centerX();
        paintHudSub.setColor(COLOR_P2);
        canvas.drawText("BLEU", cx3, hudCYr - boxH * 0.08f, paintHudSub);
        paintHudText.setColor(Color.WHITE);
        canvas.drawText(String.valueOf(scores[1]), cx3, hudCYr + boxH * 0.28f, paintHudText);

        // ── Case 4 : Bouton Pause ──
        drawPauseButton(canvas, hudBox4, hudCYr, boxH);
    }

    private void drawPauseButton(Canvas canvas, RectF box, float cy, float boxH) {
        float cx = box.centerX();
        // Fond semi-transparent arrondi dans la case beige
        Paint bg = new Paint(Paint.ANTI_ALIAS_FLAG);
        bg.setColor(0x55000000);
        float pad = boxH * 0.12f;
        canvas.drawRoundRect(
                box.left + pad, box.top + pad, box.right - pad, box.bottom - pad,
                8f, 8f, bg);

        if (state == GameState.PAUSED) {
            // Icône "play" (triangle)
            paintHudText.setTextSize(boxH * 0.55f);
            paintHudText.setColor(0xFF00FF88);
            canvas.drawText("▶", cx, cy + boxH * 0.22f, paintHudText);
        } else {
            // Icône "pause" (deux barres)
            paintHudText.setTextSize(boxH * 0.55f);
            paintHudText.setColor(Color.WHITE);
            canvas.drawText("⏸", cx, cy + boxH * 0.22f, paintHudText);
        }
    }

    private void drawOverlay(Canvas canvas) {
        switch (state) {
            case COUNTDOWN: {
                // Voile léger
                paintGoalFlash.setAlpha(25);
                canvas.drawRect(screenRect, paintGoalFlash);
                // Chiffre / GO!
                float sz = fH * 0.42f;
                paintHudText.setTextSize(sz);
                paintHudText.setColor(Color.WHITE);
                String cd = countdownValue > 0 ? String.valueOf(countdownValue) : "GO!";
                canvas.drawText(cd, fLeft + fW / 2f, fTop + fH * 0.62f, paintHudText);
                break;
            }
            case GOAL: {
                paintGoalFlash.setAlpha(45);
                canvas.drawRect(screenRect, paintGoalFlash);
                // "BUT !"
                paintHudText.setTextSize(fH * 0.20f);
                paintHudText.setColor(Color.WHITE);
                canvas.drawText("BUT !", fLeft + fW / 2f, fTop + fH * 0.42f, paintHudText);
                // Équipe
                paintHudText.setTextSize(fH * 0.09f);
                int teamColor = lastScorer == 1 ? COLOR_P1 : COLOR_P2;
                paintHudText.setColor(teamColor);
                String team = lastScorer == 1 ? "Équipe Rouge" : "Équipe Bleue";
                canvas.drawText(team, fLeft + fW / 2f, fTop + fH * 0.58f, paintHudText);
                break;
            }
            case PAUSED: {
                // Overlay sombre
                paintOverlay.setAlpha(160);
                canvas.drawRect(screenRect, paintOverlay);
                paintHudText.setTextSize(fH * 0.14f);
                paintHudText.setColor(Color.WHITE);
                canvas.drawText("PAUSE", fLeft + fW / 2f, fTop + fH * 0.48f, paintHudText);
                paintHudText.setTextSize(fH * 0.07f);
                paintHudText.setColor(0xFFAAAAAA);
                canvas.drawText("Appuie sur ⏸ pour reprendre",
                        fLeft + fW / 2f, fTop + fH * 0.60f, paintHudText);
                break;
            }
            case GAME_OVER: {
                paintOverlay.setAlpha(130);
                canvas.drawRect(screenRect, paintOverlay);
                paintHudText.setTextSize(fH * 0.14f);
                paintHudText.setColor(Color.WHITE);
                canvas.drawText("FIN DE PARTIE", fLeft + fW / 2f, fTop + fH / 2f, paintHudText);
                break;
            }
            default:
                break;
        }
    }

    // ── Entrées tactiles ──────────────────────────────────────────────────
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        int action       = event.getActionMasked();
        int pointerIndex = event.getActionIndex();
        int pointerId    = event.getPointerId(pointerIndex);
        float tx = event.getX(pointerIndex);
        float ty = event.getY(pointerIndex);

        // Tap sur le bouton Pause (case 4)
        if (action == MotionEvent.ACTION_DOWN || action == MotionEvent.ACTION_POINTER_DOWN) {
            if (hudBox4 != null && hudBox4.contains(tx, ty)) {
                togglePause();
                return true;
            }
        }

        if (state != GameState.PLAYING && state != GameState.COUNTDOWN) return true;

        switch (action) {
            case MotionEvent.ACTION_DOWN:
            case MotionEvent.ACTION_POINTER_DOWN:
                assignPointerToMallet(pointerId, tx, ty);
                break;

            case MotionEvent.ACTION_MOVE:
                for (int i = 0; i < event.getPointerCount(); i++) {
                    int pid = event.getPointerId(i);
                    for (int m = 0; m < mallets.length; m++) {
                        if (malletPointerIds[m] == pid) {
                            mallets[m].moveTo(event.getX(i), event.getY(i));
                        }
                    }
                }
                break;

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_POINTER_UP:
                for (int m = 0; m < mallets.length; m++) {
                    if (malletPointerIds[m] == pointerId) malletPointerIds[m] = -1;
                }
                break;

            case MotionEvent.ACTION_CANCEL:
                for (int m = 0; m < mallets.length; m++) malletPointerIds[m] = -1;
                break;
        }
        return true;
    }

    private void togglePause() {
        if (state == GameState.PAUSED) {
            state = (stateBeforePause != null) ? stateBeforePause : GameState.PLAYING;
        } else if (state == GameState.PLAYING || state == GameState.COUNTDOWN) {
            stateBeforePause = state;
            state = GameState.PAUSED;
        }
    }

    private void assignPointerToMallet(int pointerId, float x, float y) {
        float minDist   = Float.MAX_VALUE;
        int   bestMallet = -1;
        for (int m = 0; m < mallets.length; m++) {
            if (malletPointerIds[m] != -1) continue;
            Mallet mallet = mallets[m];
            if (x < mallet.minX || x > mallet.maxX ||
                    y < mallet.minY || y > mallet.maxY) continue;
            float dx = x - mallet.x;
            float dy = y - mallet.y;
            float dist = dx * dx + dy * dy;
            if (dist < minDist) { minDist = dist; bestMallet = m; }
        }
        if (bestMallet == -1) {
            for (int m = 0; m < mallets.length; m++) {
                if (malletPointerIds[m] != -1) continue;
                Mallet mallet = mallets[m];
                if (x >= mallet.minX && x <= mallet.maxX &&
                        y >= mallet.minY && y <= mallet.maxY) {
                    bestMallet = m; break;
                }
            }
        }
        if (bestMallet != -1) {
            malletPointerIds[bestMallet] = pointerId;
            mallets[bestMallet].moveTo(x, y);
        }
    }

    // ── Cycle de vie SurfaceView ──────────────────────────────────────────
    @Override public void surfaceCreated(SurfaceHolder holder) {}

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        W = width;
        H = height;
        if (gameThread != null) {
            gameThread.stopThread();
            try { gameThread.join(500); } catch (InterruptedException ignored) {}
        }
        setupGame();
        startCountdown();
        gameThread = new GameThread();
        gameThread.start();
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        if (gameThread != null) {
            gameThread.stopThread();
            try { gameThread.join(1000); } catch (InterruptedException ignored) {}
            gameThread = null;
        }
    }

    // ── API publique ──────────────────────────────────────────────────────
    public void pauseGame()  { if (gameThread != null) gameThread.stopThread(); }
    public void resumeGame() {
        if (gameThread == null || !gameThread.isAlive()) {
            gameThread = new GameThread();
            gameThread.start();
        }
    }
}