package com.example.duckpuck;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RadialGradient;
import android.graphics.RectF;
import android.graphics.Shader;
import android.os.Handler;
import android.os.Looper;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

public class GameView extends SurfaceView implements SurfaceHolder.Callback {

    // ── Paramètres de jeu ─────────────────────────────────────────────────
    public static final int MODE_2P = 2;
    public static final int MODE_4P = 4;

    private static final int MAX_SCORE = 7;
    private static final long FRAME_MS = 16; // ~60 FPS
    private static final float WALL_THICKNESS_RATIO = 0.04f;
    private static final float GOAL_WIDTH_RATIO = 0.35f;
    private static final float PUCK_RADIUS_RATIO = 0.045f;
    private static final float MALLET_RADIUS_RATIO = 0.07f;

    // ── Couleurs ──────────────────────────────────────────────────────────
    private static final int COLOR_TABLE     = 0xFF0A3D6B;
    private static final int COLOR_CENTER    = 0xFF0D4F8A;
    private static final int COLOR_WALL      = 0xFF1565C0;
    private static final int COLOR_GOAL_LINE = 0xFF42A5F5;
    private static final int COLOR_PUCK      = 0xFFE0E0E0;
    private static final int COLOR_P1        = 0xFFFF5252; // rouge
    private static final int COLOR_P2        = 0xFF40C4FF; // bleu ciel
    private static final int COLOR_P3        = 0xFF69F0AE; // vert
    private static final int COLOR_P4        = 0xFFFFD740; // or

    // ── État du jeu ───────────────────────────────────────────────────────
    private enum GameState { COUNTDOWN, PLAYING, GOAL, GAME_OVER }
    private GameState state = GameState.COUNTDOWN;

    private int mode;
    private int[] scores;         // scores[0] = équipe gauche, scores[1] = équipe droite
    private int countdownValue = 3;
    private long lastCountdownTime;
    private long goalDisplayTime;
    private int lastScorer = 0;   // 1 ou 2

    // ── Objets du jeu ─────────────────────────────────────────────────────
    private Puck puck;
    private Mallet[] mallets;
    private GameEngine engine;

    // ── Dimensions ────────────────────────────────────────────────────────
    private float W, H;
    private float wallThick;
    private float goalWidth;
    private float puckRadius;
    private float malletRadius;

    // ── Rendu ─────────────────────────────────────────────────────────────
    private Paint paintTable, paintWall, paintGoal, paintCenter,
                  paintPuck, paintText, paintShadow, paintGoalFlash;
    private Paint[] paintMallets;
    private RectF tableRect;

    // ── Boucle de jeu ─────────────────────────────────────────────────────
    private GameThread gameThread;

    // ── Pointeurs tactiles ────────────────────────────────────────────────
    // On associe chaque maillet à un pointerId (-1 = libre)
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
        this.mode = mode;
        scores = new int[2];
        getHolder().addCallback(this);
        setFocusable(true);
        initPaints();
    }

    public void setGameListener(GameListener listener) {
        this.listener = listener;
    }

    // ── Initialisation des Paints ─────────────────────────────────────────
    private void initPaints() {
        paintTable = new Paint(Paint.ANTI_ALIAS_FLAG);
        paintTable.setColor(COLOR_TABLE);

        paintWall = new Paint(Paint.ANTI_ALIAS_FLAG);
        paintWall.setColor(COLOR_WALL);

        paintGoal = new Paint(Paint.ANTI_ALIAS_FLAG);
        paintGoal.setColor(COLOR_GOAL_LINE);
        paintGoal.setStrokeWidth(4f);
        paintGoal.setStyle(Paint.Style.STROKE);

        paintCenter = new Paint(Paint.ANTI_ALIAS_FLAG);
        paintCenter.setColor(COLOR_CENTER);
        paintCenter.setStyle(Paint.Style.STROKE);
        paintCenter.setStrokeWidth(3f);

        paintPuck = new Paint(Paint.ANTI_ALIAS_FLAG);
        paintPuck.setColor(COLOR_PUCK);

        paintShadow = new Paint(Paint.ANTI_ALIAS_FLAG);
        paintShadow.setColor(Color.BLACK);
        paintShadow.setAlpha(80);

        paintText = new Paint(Paint.ANTI_ALIAS_FLAG);
        paintText.setColor(Color.WHITE);
        paintText.setTextAlign(Paint.Align.CENTER);
        paintText.setFakeBoldText(true);

        paintGoalFlash = new Paint(Paint.ANTI_ALIAS_FLAG);
        paintGoalFlash.setColor(Color.WHITE);
        paintGoalFlash.setAlpha(60);

        paintMallets = new Paint[4];
        int[] colors = {COLOR_P1, COLOR_P2, COLOR_P3, COLOR_P4};
        for (int i = 0; i < 4; i++) {
            paintMallets[i] = new Paint(Paint.ANTI_ALIAS_FLAG);
            paintMallets[i].setColor(colors[i]);
        }
    }

    // ── Setup après connaissance des dimensions ───────────────────────────
    private void setupGame() {
        wallThick  = W * WALL_THICKNESS_RATIO;
        goalWidth  = H * GOAL_WIDTH_RATIO;
        puckRadius = Math.min(W, H) * PUCK_RADIUS_RATIO;
        malletRadius = Math.min(W, H) * MALLET_RADIUS_RATIO;

        tableRect = new RectF(0, 0, W, H);

        // Palet au centre
        puck = new Puck(W / 2f, H / 2f, puckRadius);

        // Maillets
        if (mode == MODE_2P) {
            mallets = new Mallet[2];
            // Joueur 1 (gauche)
            mallets[0] = new Mallet(W * 0.18f, H / 2f, malletRadius, COLOR_P1);
            mallets[0].setZone(wallThick, W / 2f, wallThick, H - wallThick);
            // Joueur 2 (droite)
            mallets[1] = new Mallet(W * 0.82f, H / 2f, malletRadius, COLOR_P2);
            mallets[1].setZone(W / 2f, W - wallThick, wallThick, H - wallThick);
        } else {
            // Mode 4 joueurs : 2 maillets par équipe
            mallets = new Mallet[4];
            // Équipe gauche : P1 attaque, P3 défense
            mallets[0] = new Mallet(W * 0.25f, H * 0.35f, malletRadius, COLOR_P1);
            mallets[0].setZone(wallThick, W / 2f, wallThick, H - wallThick);
            mallets[2] = new Mallet(W * 0.10f, H * 0.65f, malletRadius, COLOR_P3);
            mallets[2].setZone(wallThick, W / 2f, wallThick, H - wallThick);
            // Équipe droite : P2 attaque, P4 défense
            mallets[1] = new Mallet(W * 0.75f, H * 0.35f, malletRadius, COLOR_P2);
            mallets[1].setZone(W / 2f, W - wallThick, wallThick, H - wallThick);
            mallets[3] = new Mallet(W * 0.90f, H * 0.65f, malletRadius, COLOR_P4);
            mallets[3].setZone(W / 2f, W - wallThick, wallThick, H - wallThick);
        }

        malletPointerIds = new int[mallets.length];
        for (int i = 0; i < malletPointerIds.length; i++) malletPointerIds[i] = -1;

        engine = new GameEngine(W, H, goalWidth, wallThick);

        // Gradient radial pour le palet
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
                long sleep = FRAME_MS - elapsed;
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
                    if (countdownValue <= 0) {
                        state = GameState.PLAYING;
                    }
                }
                break;

            case PLAYING:
                int result = engine.update(puck, mallets);
                updatePuckGradient();
                if (result == 1) {
                    scores[0]++;
                    lastScorer = 1;
                    onGoal();
                } else if (result == 2) {
                    scores[1]++;
                    lastScorer = 2;
                    onGoal();
                }
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

            case GAME_OVER:
                // géré par l'activité
                break;
        }
    }

    private void onGoal() {
        state = GameState.GOAL;
        goalDisplayTime = System.currentTimeMillis();
    }

    private void resetPositions() {
        puck.reset(W / 2f, H / 2f);
        if (mode == MODE_2P) {
            mallets[0].reset(W * 0.18f, H / 2f);
            mallets[1].reset(W * 0.82f, H / 2f);
        } else {
            mallets[0].reset(W * 0.25f, H * 0.35f);
            mallets[2].reset(W * 0.10f, H * 0.65f);
            mallets[1].reset(W * 0.75f, H * 0.35f);
            mallets[3].reset(W * 0.90f, H * 0.65f);
        }
        for (int i = 0; i < malletPointerIds.length; i++) malletPointerIds[i] = -1;
    }

    private void startCountdown() {
        countdownValue = 3;
        lastCountdownTime = System.currentTimeMillis();
        state = GameState.COUNTDOWN;
    }

    private void notifyGameOver() {
        if (listener == null) return;
        int winner = scores[0] >= MAX_SCORE ? 1 : 2;
        mainHandler.post(() -> listener.onGameOver(winner, scores));
    }

    // ── Dessin ────────────────────────────────────────────────────────────
    private void draw() {
        SurfaceHolder holder = getHolder();
        if (!holder.getSurface().isValid()) return;
        Canvas canvas = holder.lockCanvas();
        if (canvas == null) return;

        try {
            drawTable(canvas);
            drawMallets(canvas);
            drawPuck(canvas);
            drawScores(canvas);
            drawOverlay(canvas);
        } finally {
            holder.unlockCanvasAndPost(canvas);
        }
    }

    private void drawTable(Canvas canvas) {
        // Fond
        canvas.drawRect(tableRect, paintTable);

        // Murs
        canvas.drawRect(0, 0, W, wallThick, paintWall);
        canvas.drawRect(0, H - wallThick, W, H, paintWall);

        float goalTop = (H - goalWidth) / 2f;
        float goalBottom = (H + goalWidth) / 2f;

        // Mur gauche (2 segments autour du but)
        canvas.drawRect(0, wallThick, wallThick, goalTop, paintWall);
        canvas.drawRect(0, goalBottom, wallThick, H - wallThick, paintWall);

        // Mur droit
        canvas.drawRect(W - wallThick, wallThick, W, goalTop, paintWall);
        canvas.drawRect(W - wallThick, goalBottom, W, H - wallThick, paintWall);

        // Buts (cadres)
        paintGoal.setStrokeWidth(3f);
        // Gauche
        canvas.drawLine(0, goalTop, wallThick, goalTop, paintGoal);
        canvas.drawLine(0, goalBottom, wallThick, goalBottom, paintGoal);
        // Droite
        canvas.drawLine(W, goalTop, W - wallThick, goalTop, paintGoal);
        canvas.drawLine(W, goalBottom, W - wallThick, goalBottom, paintGoal);

        // Ligne centrale
        canvas.drawLine(W / 2f, wallThick, W / 2f, H - wallThick, paintCenter);

        // Cercle central
        float centerR = Math.min(W, H) * 0.13f;
        canvas.drawCircle(W / 2f, H / 2f, centerR, paintCenter);

        // Point central
        paintCenter.setStyle(Paint.Style.FILL);
        canvas.drawCircle(W / 2f, H / 2f, 6f, paintCenter);
        paintCenter.setStyle(Paint.Style.STROKE);
    }

    private void drawMallets(Canvas canvas) {
        for (int i = 0; i < mallets.length; i++) {
            Mallet m = mallets[i];
            // Ombre
            canvas.drawCircle(m.x + 4, m.y + 4, m.radius, paintShadow);
            // Corps
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
        // Ombre
        canvas.drawCircle(puck.x + 5, puck.y + 5, puck.radius, paintShadow);
        // Corps
        canvas.drawCircle(puck.x, puck.y, puck.radius, paintPuck);
    }

    private void drawScores(Canvas canvas) {
        paintText.setTextSize(H * 0.14f);
        paintText.setAlpha(180);
        canvas.drawText(String.valueOf(scores[0]), W * 0.25f, H * 0.22f, paintText);
        canvas.drawText(String.valueOf(scores[1]), W * 0.75f, H * 0.22f, paintText);
        paintText.setAlpha(255);
    }

    private void drawOverlay(Canvas canvas) {
        switch (state) {
            case COUNTDOWN:
                // Flash mi-terrain semi-transparent
                paintGoalFlash.setAlpha(30);
                canvas.drawRect(tableRect, paintGoalFlash);

                paintText.setTextSize(H * 0.5f);
                paintText.setAlpha(200);
                String cd = countdownValue > 0 ? String.valueOf(countdownValue) : "GO!";
                canvas.drawText(cd, W / 2f, H / 2f + H * 0.18f, paintText);
                paintText.setAlpha(255);
                break;

            case GOAL:
                paintGoalFlash.setAlpha(50);
                canvas.drawRect(tableRect, paintGoalFlash);

                paintText.setTextSize(H * 0.22f);
                String goalMsg = "BUT !";
                canvas.drawText(goalMsg, W / 2f, H * 0.42f, paintText);

                paintText.setTextSize(H * 0.1f);
                String team = lastScorer == 1 ? "Équipe Rouge" : "Équipe Bleue";
                canvas.drawText(team, W / 2f, H * 0.62f, paintText);
                break;

            case GAME_OVER:
                paintGoalFlash.setAlpha(100);
                canvas.drawRect(tableRect, paintGoalFlash);
                paintText.setTextSize(H * 0.18f);
                canvas.drawText("FIN !", W / 2f, H / 2f, paintText);
                break;

            default:
                break;
        }
    }

    // ── Entrées tactiles ──────────────────────────────────────────────────
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (state != GameState.PLAYING && state != GameState.COUNTDOWN) return true;

        int action = event.getActionMasked();
        int pointerIndex = event.getActionIndex();
        int pointerId = event.getPointerId(pointerIndex);

        switch (action) {
            case MotionEvent.ACTION_DOWN:
            case MotionEvent.ACTION_POINTER_DOWN:
                assignPointerToMallet(pointerId,
                        event.getX(pointerIndex),
                        event.getY(pointerIndex));
                break;

            case MotionEvent.ACTION_MOVE:
                // Mettre à jour tous les pointeurs actifs
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
                // Libérer le maillet associé
                for (int m = 0; m < mallets.length; m++) {
                    if (malletPointerIds[m] == pointerId) {
                        malletPointerIds[m] = -1;
                    }
                }
                break;

            case MotionEvent.ACTION_CANCEL:
                for (int m = 0; m < mallets.length; m++) malletPointerIds[m] = -1;
                break;
        }

        return true;
    }

    /**
     * Associe un nouveau pointeur au maillet le plus proche dans la zone correspondante.
     */
    private void assignPointerToMallet(int pointerId, float x, float y) {
        float minDist = Float.MAX_VALUE;
        int bestMallet = -1;

        for (int m = 0; m < mallets.length; m++) {
            if (malletPointerIds[m] != -1) continue; // déjà pris

            // Vérifier que le toucher est dans la zone du maillet
            Mallet mallet = mallets[m];
            if (x < mallet.minX || x > mallet.maxX ||
                y < mallet.minY || y > mallet.maxY) continue;

            float dx = x - mallet.x;
            float dy = y - mallet.y;
            float dist = dx * dx + dy * dy;

            if (dist < minDist) {
                minDist = dist;
                bestMallet = m;
            }
        }

        // Si aucun maillet proche, assigner au premier libre de la zone
        if (bestMallet == -1) {
            for (int m = 0; m < mallets.length; m++) {
                if (malletPointerIds[m] != -1) continue;
                Mallet mallet = mallets[m];
                if (x >= mallet.minX && x <= mallet.maxX &&
                    y >= mallet.minY && y <= mallet.maxY) {
                    bestMallet = m;
                    break;
                }
            }
        }

        if (bestMallet != -1) {
            malletPointerIds[bestMallet] = pointerId;
            mallets[bestMallet].moveTo(x, y);
        }
    }

    // ── Cycle de vie SurfaceView ──────────────────────────────────────────
    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        // Rien ici, on attend surfaceChanged pour les dimensions
    }

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
    public void pauseGame() {
        if (gameThread != null) gameThread.stopThread();
    }

    public void resumeGame() {
        if (gameThread == null || !gameThread.isAlive()) {
            gameThread = new GameThread();
            gameThread.start();
        }
    }
}
