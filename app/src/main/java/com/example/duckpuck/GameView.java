package com.example.duckpuck;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.RadialGradient;
import android.graphics.RectF;
import android.graphics.Shader;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.media.AudioAttributes;
import android.media.SoundPool;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;

public class GameView extends SurfaceView implements SurfaceHolder.Callback {

    // ── Modes de jeu ──────────────────────────────────────────────────────
    public static final int MODE_2P = 2;
    public static final int MODE_4P = 4;

    // ── Paramètres de jeu ─────────────────────────────────────────────────
    private static final int   TOTAL_PUCKS  = 10;
    private static final long  FRAME_MS     = 36;   // ~60 FPS
    private static final long  REPLAY_MS    = 5000;

    private static final float FIELD_LEFT_R   = 0.085f;
    private static final float FIELD_RIGHT_R  = 0.914f;
    private static final float FIELD_TOP_R    = 0.120f;
    private static final float FIELD_BOTTOM_R = 0.960f;

    private static final float HUD_TOP_R    = 0.008f;
    private static final float HUD_BOTTOM_R = 0.104f;

    private static final float HUD_BOX1_L = 0.301f;
    private static final float HUD_BOX1_R = 0.423f;
    private static final float HUD_BOX2_L = 0.434f;
    private static final float HUD_BOX2_R = 0.555f;
    private static final float HUD_BOX3_L = 0.565f;
    private static final float HUD_BOX3_R = 0.683f;
    private static final float HUD_BOX4_L = 0.684f;
    private static final float HUD_BOX4_R = 0.812f;

    private static final float PUCK_RADIUS_RATIO   = 0.045f;
    private static final float MALLET_RADIUS_RATIO = 0.070f;
    private static final float PUCK_SPIN_HIT_FACTOR = 2.0f;
    private static final float PUCK_SPIN_SPEED_FACTOR = 2.6f;
    private static final float PUCK_SPIN_MAX = 70f;
    private static final float PUCK_SPIN_DAMPING = 0.86f;
    private static final float PUCK_SPIN_STOP = 0.08f;

    private static final int COLOR_PUCK = 0xFFE0E0E0;
    private static final int COLOR_P1   = 0xFFFF5252;
    private static final int COLOR_P2   = 0xFF40C4FF;
    private static final int COLOR_P3   = 0xFF69F0AE;
    private static final int COLOR_P4   = 0xFFFFD740;

    // ── GESTION AUDIO (SoundPool) ─────────────────────────────────────────
    private SoundPool soundPool;
    private int soundHitId = 0;
    private int soundWallId = 0;
    private int soundGoalId = 0;
    private long lastWallSoundTime = 0;
    private static final long WALL_SOUND_COOLDOWN_MS = 120;

    // ── État du jeu ───────────────────────────────────────────────────────
    private enum GameState { COUNTDOWN, PLAYING, REPLAY, GOAL, PAUSED, GAME_OVER }
    private GameState state      = GameState.COUNTDOWN;
    private GameState stateBeforePause;

    private int    mode;
    private int[]  scores;
    private int    countdownValue  = 3;
    private long   lastCountdownTime;
    private long   goalDisplayTime;
    private int    lastScorer      = 0;

    private Puck    puck;
    private Mallet[] mallets;
    private GameEngine engine;

    private float W, H;
    private float fLeft, fRight, fTop, fBottom;
    private float fW, fH;
    private float goalWidth;
    private float puckRadius, malletRadius;
    private float puckRotationDegrees = 0f;
    private float puckSpinVelocity = 0f;
    private float puckSpinDirection = 1f;

    private Bitmap  bgBitmap;
    private Bitmap  puckBitmap;
    private Bitmap[] malletBitmaps;
    private Bitmap[] flippedMalletBitmaps;
    private Paint   paintPuck, paintShadow, paintGoalFlash;
    private Paint   paintHudText, paintHudSub, paintHudPanel, paintHudBorder, paintHudAccent;
    private Paint   paintOverlay;
    private Paint[] paintMallets;
    private RectF   screenRect;

    private RectF hudBox1, hudBox2, hudBox3, hudBox4;
    private RectF pauseButtonRect;
    private RectF btnResume, btnSettings, btnQuit;
    private float hudCY;

    private static final float HUD_PANEL_INSET_RATIO = 0.08f;
    private static final float PAUSE_ICON_SIZE_RATIO = 0.52f;

    private GameThread gameThread;
    private int[] malletPointerIds;
    private int[] playerGoals;
    private int lastHitMalletIndex = -1;
    private final boolean replayEnabled;
    private final ArrayDeque<ReplayData.Frame> replayBuffer = new ArrayDeque<>();
    private final ArrayList<ReplayData.Goal> goalReplays = new ArrayList<>();
    private List<ReplayData.Frame> activeReplayFrames = new ArrayList<>();
    private long replayStartTime;
    private long activeReplayDuration;
    private ReplayData.Frame currentReplayFrame;

    public interface GameListener {
        void onGameOver(int winnerTeam, int[] scores);
        void onSettingsRequested();
        void onQuitRequested();
    }
    private GameListener listener;
    private final android.os.Handler mainHandler = new android.os.Handler(android.os.Looper.getMainLooper());

    public GameView(Context context, int mode) {
        this(context, mode, true);
    }

    public GameView(Context context, int mode, boolean replayEnabled) {
        super(context);
        this.mode   = mode;
        this.replayEnabled = replayEnabled;
        this.scores = new int[2];
        getHolder().addCallback(this);
        setFocusable(true);
        initPaints();
        initAudio(context); // Initialisation du SoundPool
    }

    public void setGameListener(GameListener l) { this.listener = l; }

    // Initialisation propre du moteur Audio d'Android
    private void initAudio(Context context) {
        AudioAttributes audioAttributes = new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build();

        soundPool = new SoundPool.Builder()
                .setMaxStreams(4)
                .setAudioAttributes(audioAttributes)
                .build();

        // Son joue quand un striker touche le palet.
        soundHitId = soundPool.load(context, R.raw.coincoinvf, 1);
        soundWallId = soundPool.load(context, R.raw.boing, 1);
        soundGoalId = soundPool.load(context, R.raw.wowpublic, 1);
    }

    private void loadBackground() {
        int resId = getResources().getIdentifier("game_bg", "drawable", getContext().getPackageName());
        if (resId != 0) {
            BitmapFactory.Options opts = new BitmapFactory.Options();
            opts.inScaled = false;
            Bitmap raw = BitmapFactory.decodeResource(getResources(), resId, opts);
            bgBitmap = Bitmap.createScaledBitmap(raw, (int) W, (int) H, true);
            if (raw != bgBitmap) raw.recycle();
        }
    }

    private void loadMalletImages() {
        if (malletBitmaps != null) return;

        int[] resIds = {
                R.drawable.rougecanard,
                R.drawable.bleucanard,
                R.drawable.rougecanard,
                R.drawable.bleucanard
        };
        malletBitmaps = new Bitmap[resIds.length];
        flippedMalletBitmaps = new Bitmap[resIds.length];
        BitmapFactory.Options opts = new BitmapFactory.Options();
        opts.inScaled = false;

        for (int i = 0; i < resIds.length; i++) {
            malletBitmaps[i] = BitmapFactory.decodeResource(getResources(), resIds[i], opts);
            flippedMalletBitmaps[i] = createFlippedBitmap(malletBitmaps[i]);
        }
    }

    private void loadPuckImage() {
        if (puckBitmap != null) return;

        BitmapFactory.Options opts = new BitmapFactory.Options();
        opts.inScaled = false;
        puckBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.ballduckpuck, opts);
    }

    private Bitmap createFlippedBitmap(Bitmap source) {
        if (source == null) return null;

        Matrix matrix = new Matrix();
        matrix.preScale(-1f, 1f);
        return Bitmap.createBitmap(source, 0, 0, source.getWidth(), source.getHeight(), matrix, true);
    }

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

        paintHudPanel = new Paint(Paint.ANTI_ALIAS_FLAG);
        paintHudPanel.setColor(0xAA142436);

        paintHudBorder = new Paint(Paint.ANTI_ALIAS_FLAG);
        paintHudBorder.setStyle(Paint.Style.STROKE);
        paintHudBorder.setStrokeWidth(3f);
        paintHudBorder.setColor(0xFFD6A94A);

        paintHudAccent = new Paint(Paint.ANTI_ALIAS_FLAG);
        paintHudAccent.setColor(0xFFD6A94A);

        paintHudText = new Paint(Paint.ANTI_ALIAS_FLAG);
        paintHudText.setColor(Color.WHITE);
        paintHudText.setTextAlign(Paint.Align.CENTER);
        paintHudText.setFakeBoldText(true);
        paintHudText.setShadowLayer(3f, 2f, 2f, Color.BLACK);

        paintHudSub = new Paint(Paint.ANTI_ALIAS_FLAG);
        paintHudSub.setColor(0xFFFFD700);
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

    private void setupGame() {
        loadBackground();
        loadMalletImages();
        loadPuckImage();

        fLeft   = W * FIELD_LEFT_R;
        fRight  = W * FIELD_RIGHT_R;
        fTop    = H * FIELD_TOP_R;
        fBottom = H * FIELD_BOTTOM_R;
        fW      = fRight - fLeft;
        fH      = fBottom - fTop;

        goalWidth = fH * 0.35f;

        puckRadius   = Math.min(fW, fH) * PUCK_RADIUS_RATIO;
        malletRadius = Math.min(fW, fH) * MALLET_RADIUS_RATIO;

        screenRect = new RectF(0, 0, W, H);

        float hudY1 = H * HUD_TOP_R;
        float hudY2 = H * HUD_BOTTOM_R;
        hudCY = (hudY1 + hudY2) / 2f;
        hudBox1 = new RectF(W * HUD_BOX1_L, hudY1, W * HUD_BOX1_R, hudY2);
        hudBox2 = new RectF(W * HUD_BOX2_L, hudY1, W * HUD_BOX2_R, hudY2);
        hudBox3 = new RectF(W * HUD_BOX3_L, hudY1, W * HUD_BOX3_R, hudY2);
        hudBox4 = new RectF(W * HUD_BOX4_L, hudY1, W * HUD_BOX4_R, hudY2);
        pauseButtonRect = buildPauseButtonRect(hudBox4);

        float cx = fLeft + fW / 2f;
        float cy = fTop  + fH / 2f;

        puck = new Puck(cx, cy, puckRadius);
        puck.setSpeedScale(getPuckSpeedScale());

        if (mode == MODE_2P) {
            mallets = new Mallet[2];
            playerGoals = new int[2];
            mallets[0] = new Mallet(fLeft + fW * 0.18f, cy, malletRadius, COLOR_P1);
            mallets[0].setZone(fLeft, cx, fTop, fBottom);
            mallets[1] = new Mallet(fLeft + fW * 0.82f, cy, malletRadius, COLOR_P2);
            mallets[1].setZone(cx, fRight, fTop, fBottom);
        } else {
            mallets = new Mallet[4];
            playerGoals = new int[4];
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

        // Configuration de l'engine physique
        engine = new GameEngine(fLeft, fRight, fTop, fBottom, goalWidth);

        // ── CONNEXION DE L'ÉCOUTEUR AUDIO ENTRE L'ENGINE ET LA VUE ──
        engine.setOnCollisionListener(new GameEngine.OnCollisionListener() {
            @Override
            public void onMalletHit(int malletIndex) {
                lastHitMalletIndex = malletIndex;
                startPuckSpin(malletIndex);
                if (soundPool != null && soundHitId != 0) {
                    // Lecture instantanée du bruitage sans latence
                    float volume = AudioSettings.getSfxVolume(getContext());
                    soundPool.play(soundHitId, volume, volume, 1, 0, 1.0f);
                }
            }

            @Override
            public void onWallHit() {
                long now = System.currentTimeMillis();
                if (now - lastWallSoundTime < WALL_SOUND_COOLDOWN_MS) return;
                lastWallSoundTime = now;

                if (soundPool != null && soundWallId != 0) {
                    float volume = AudioSettings.getSfxVolume(getContext());
                    soundPool.play(soundWallId, volume, volume, 1, 0, 1.0f);
                }
            }
        });

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

    private void startPuckSpin(int malletIndex) {
        if (puck == null) return;

        float speed = puck.getSpeed();
        puckSpinDirection = (malletIndex == 1 || malletIndex == 3) ? -1f : 1f;
        if (Math.abs(puck.vy) > Math.abs(puck.vx)) {
            puckSpinDirection *= puck.vy >= 0f ? 1f : -1f;
        }
        puckSpinVelocity = clamp(speed * PUCK_SPIN_HIT_FACTOR * puckSpinDirection, -PUCK_SPIN_MAX, PUCK_SPIN_MAX);
    }

    private void updatePuckRotation() {
        if (puck != null) {
            float targetSpin = clamp(
                    puck.getSpeed() * PUCK_SPIN_SPEED_FACTOR * puckSpinDirection,
                    -PUCK_SPIN_MAX,
                    PUCK_SPIN_MAX
            );
            puckSpinVelocity = puckSpinVelocity * PUCK_SPIN_DAMPING + targetSpin * (1f - PUCK_SPIN_DAMPING);
        }
        puckRotationDegrees = (puckRotationDegrees + puckSpinVelocity) % 360f;
        if (Math.abs(puckSpinVelocity) < PUCK_SPIN_STOP) {
            puckSpinVelocity = 0f;
        }
    }

    private float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }

    private float getPuckSpeedScale() {
        float fieldBase = Math.min(fW, fH);
        return clamp(fieldBase / 950f, 1.15f, 1.75f);
    }

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
                updatePuckRotation();
                updatePuckGradient();
                if (replayEnabled) {
                    recordReplayFrame(now);
                }
                if (result == 1) { scores[0]++; lastScorer = 1; creditGoal(result); onGoal(); }
                else if (result == 2) { scores[1]++; lastScorer = 2; creditGoal(result); onGoal(); }
                break;

            case REPLAY:
                updateReplayFrame(now);
                if (now - replayStartTime >= activeReplayDuration) {
                    currentReplayFrame = null;
                    state = GameState.GOAL;
                    goalDisplayTime = now;
                }
                break;

            case GOAL:
                if (now - goalDisplayTime >= 2000) {
                    if (getTotalGoals() >= TOTAL_PUCKS) {
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
        playGoalSound();
        if (!replayEnabled) {
            state = GameState.GOAL;
            goalDisplayTime = System.currentTimeMillis();
            return;
        }

        ReplayData.Goal replay = buildGoalReplay();
        if (!replay.frames.isEmpty()) {
            goalReplays.add(replay);
            activeReplayFrames = replay.frames;
            activeReplayDuration = getReplayDuration(activeReplayFrames);
            replayStartTime = System.currentTimeMillis();
            currentReplayFrame = activeReplayFrames.get(0);
            state = GameState.REPLAY;
        } else {
            state = GameState.GOAL;
            goalDisplayTime = System.currentTimeMillis();
        }
    }

    private void playGoalSound() {
        if (soundPool == null || soundGoalId == 0) return;
        float volume = AudioSettings.getSfxVolume(getContext());
        soundPool.play(soundGoalId, volume, volume, 1, 0, 1.0f);
    }

    private void recordReplayFrame(long now) {
        if (puck == null || mallets == null || W <= 0 || H <= 0) return;

        float[] mx = new float[mallets.length];
        float[] my = new float[mallets.length];
        for (int i = 0; i < mallets.length; i++) {
            mx[i] = mallets[i].x / W;
            my[i] = mallets[i].y / H;
        }

        replayBuffer.addLast(new ReplayData.Frame(now, puck.x / W, puck.y / H, mx, my));
        while (!replayBuffer.isEmpty() && now - replayBuffer.peekFirst().timeMs > REPLAY_MS) {
            replayBuffer.removeFirst();
        }
    }

    private ReplayData.Goal buildGoalReplay() {
        ReplayData.Goal replay = new ReplayData.Goal();
        replay.scorer = lastScorer;
        replay.scoreRed = scores[0];
        replay.scoreBlue = scores[1];
        replay.frames.addAll(replayBuffer);
        return replay;
    }

    private long getReplayDuration(List<ReplayData.Frame> frames) {
        if (frames == null || frames.size() < 2) return 1200;
        long duration = frames.get(frames.size() - 1).timeMs - frames.get(0).timeMs;
        return Math.max(1200, duration);
    }

    private void updateReplayFrame(long now) {
        if (activeReplayFrames == null || activeReplayFrames.isEmpty()) return;

        long replayTime = activeReplayFrames.get(0).timeMs + (now - replayStartTime);
        ReplayData.Frame selected = activeReplayFrames.get(activeReplayFrames.size() - 1);
        for (ReplayData.Frame frame : activeReplayFrames) {
            if (frame.timeMs >= replayTime) {
                selected = frame;
                break;
            }
        }
        currentReplayFrame = selected;
    }

    private void creditGoal(int scoringTeam) {
        if (lastHitMalletIndex < 0 || playerGoals == null || lastHitMalletIndex >= playerGoals.length) {
            return;
        }

        boolean redMallet = lastHitMalletIndex == 0 || (mode == MODE_4P && lastHitMalletIndex == 2);
        boolean blueMallet = lastHitMalletIndex == 1 || (mode == MODE_4P && lastHitMalletIndex == 3);

        if ((scoringTeam == 1 && redMallet) || (scoringTeam == 2 && blueMallet)) {
            playerGoals[lastHitMalletIndex]++;
        }
        lastHitMalletIndex = -1;
    }

    private void resetPositions() {
        float cx = fLeft + fW / 2f;
        float cy = fTop  + fH / 2f;
        puck.reset(cx, cy);
        puck.setSpeedScale(getPuckSpeedScale());
        puckRotationDegrees = 0f;
        puckSpinVelocity = 0f;
        puckSpinDirection = 1f;
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
        replayBuffer.clear();
    }

    private void startCountdown() {
        countdownValue    = 3;
        lastCountdownTime = System.currentTimeMillis();
        state             = GameState.COUNTDOWN;
        replayBuffer.clear();
    }

    private void notifyGameOver() {
        if (listener == null) return;
        int winner = scores[0] > scores[1] ? 1 : (scores[1] > scores[0] ? 2 : 0);
        mainHandler.post(() -> listener.onGameOver(winner, scores));
    }

    private int getTotalGoals() {
        return scores[0] + scores[1];
    }

    private void draw() {
        SurfaceHolder holder = getHolder();
        if (!holder.getSurface().isValid()) return;
        Canvas canvas = holder.lockCanvas();
        if (canvas == null) return;
        try {
            drawBackground(canvas);
            if (state == GameState.REPLAY && currentReplayFrame != null) {
                drawReplayFrame(canvas, currentReplayFrame);
            } else {
                drawMallets(canvas);
                drawPuck(canvas);
            }
            drawHUD(canvas);
            drawOverlay(canvas);
        } finally {
            holder.unlockCanvasAndPost(canvas);
        }
    }

    private void drawBackground(Canvas canvas) {
        if (bgBitmap != null) {
            canvas.drawBitmap(bgBitmap, 0, 0, null);
        } else {
            Paint p = new Paint();
            p.setColor(0xFF0A3D6B);
            canvas.drawRect(screenRect, p);
        }
    }

    private void drawMallets(Canvas canvas) {
        for (int i = 0; i < mallets.length; i++) {
            Mallet m = mallets[i];
            drawMalletAt(canvas, m.x, m.y, m.radius, paintMallets[i], i);
        }
    }

    private void drawPuck(Canvas canvas) {
        drawPuckAt(canvas, puck.x, puck.y, puck.radius, paintPuck, puckRotationDegrees);
    }

    private void drawReplayFrame(Canvas canvas, ReplayData.Frame frame) {
        int malletCount = Math.min(frame.malletX.length, frame.malletY.length);
        for (int i = 0; i < malletCount && i < paintMallets.length; i++) {
            drawMalletAt(canvas, frame.malletX[i] * W, frame.malletY[i] * H, malletRadius, paintMallets[i], i);
        }
        drawPuckAt(canvas, frame.puckX * W, frame.puckY * H, puckRadius, paintPuck, 0f);
    }

    private void drawMalletAt(Canvas canvas, float x, float y, float radius, Paint paint, int malletIndex) {
        Bitmap malletBitmap = getMalletBitmap(malletIndex);
        if (malletBitmap != null) {
            canvas.drawCircle(x + radius * 0.08f, y + radius * 0.08f, radius, paintShadow);
            RectF dst = new RectF(x - radius, y - radius, x + radius, y + radius);
            canvas.drawBitmap(malletBitmap, null, dst, null);
            return;
        }

        canvas.drawCircle(x + 4, y + 4, radius, paintShadow);
        canvas.drawCircle(x, y, radius, paint);
        Paint shine = new Paint(Paint.ANTI_ALIAS_FLAG);
        shine.setColor(Color.WHITE);
        shine.setAlpha(80);
        canvas.drawCircle(x - radius * 0.25f, y - radius * 0.25f, radius * 0.35f, shine);
        Paint center = new Paint(Paint.ANTI_ALIAS_FLAG);
        center.setColor(Color.BLACK);
        center.setAlpha(60);
        canvas.drawCircle(x, y, radius * 0.25f, center);
    }

    private Bitmap getMalletBitmap(int malletIndex) {
        if (malletBitmaps == null || malletBitmaps.length == 0) return null;
        if (malletIndex < 0) return null;

        boolean blueSide = malletIndex == 1 || malletIndex == 3;
        Bitmap[] bitmaps = blueSide ? flippedMalletBitmaps : malletBitmaps;
        if (bitmaps == null || bitmaps.length == 0) return null;
        return bitmaps[malletIndex % bitmaps.length];
    }

    private void drawPuckAt(Canvas canvas, float x, float y, float radius, Paint paint, float rotationDegrees) {
        canvas.drawCircle(x + 5, y + 5, radius, paintShadow);
        if (puckBitmap != null) {
            RectF dst = new RectF(x - radius, y - radius, x + radius, y + radius);
            canvas.save();
            canvas.rotate(rotationDegrees, x, y);
            canvas.drawBitmap(puckBitmap, null, dst, null);
            canvas.restore();
            return;
        }
        canvas.drawCircle(x, y, radius, paint);
    }

    private void drawHUD(Canvas canvas) {
        float boxH   = hudBox1.height();
        float hudCYr = hudCY;

        drawScorePanel(canvas, hudBox1, "ROUGE", scores[0], COLOR_P1, boxH, hudCYr);
        int paletsLeft = Math.max(0, TOTAL_PUCKS - getTotalGoals());
        drawCenterPanel(canvas, hudBox2, paletsLeft, boxH, hudCYr);
        drawScorePanel(canvas, hudBox3, "BLEU", scores[1], COLOR_P2, boxH, hudCYr);

        drawPauseButton(canvas, pauseButtonRect);
    }

    private RectF buildPauseButtonRect(RectF slot) {
        float inset = slot.height() * HUD_PANEL_INSET_RATIO;
        float size = slot.height() - inset * 2f;
        float right = slot.right - inset;
        float top = slot.top + inset;
        return new RectF(right - size, top, right, top + size);
    }

    private void drawScorePanel(Canvas canvas, RectF box, String label, int score, int color, float boxH, float cy) {
        RectF panel = insetRect(box, boxH * HUD_PANEL_INSET_RATIO);
        drawHudPanel(canvas, panel);

        paintHudAccent.setColor(color);
        float dotRadius = boxH * 0.12f;
        canvas.drawCircle(panel.left + dotRadius * 1.7f, panel.centerY(), dotRadius, paintHudAccent);

        paintHudSub.setTextSize(boxH * 0.20f);
        paintHudSub.setColor(color);
        canvas.drawText(label, panel.centerX(), cy - boxH * 0.13f, paintHudSub);

        paintHudText.setTextSize(boxH * 0.54f);
        paintHudText.setColor(Color.WHITE);
        canvas.drawText(String.valueOf(score), panel.centerX(), cy + boxH * 0.28f, paintHudText);
    }

    private void drawCenterPanel(Canvas canvas, RectF box, int paletsLeft, float boxH, float cy) {
        RectF panel = insetRect(box, boxH * HUD_PANEL_INSET_RATIO);
        drawHudPanel(canvas, panel);

        paintHudSub.setTextSize(boxH * 0.18f);
        paintHudSub.setColor(0xFFD6A94A);
        canvas.drawText("PALETS", panel.centerX(), cy - boxH * 0.13f, paintHudSub);

        paintHudText.setTextSize(boxH * 0.46f);
        paintHudText.setColor(0xFFFFE08A);
        canvas.drawText(paletsLeft + "/" + TOTAL_PUCKS, panel.centerX(), cy + boxH * 0.25f, paintHudText);
    }

    private void drawHudPanel(Canvas canvas, RectF rect) {
        paintHudPanel.setStyle(Paint.Style.FILL);
        canvas.drawRoundRect(rect, 12f, 12f, paintHudPanel);
        paintHudBorder.setStrokeWidth(Math.max(2f, rect.height() * 0.035f));
        paintHudBorder.setColor(0xFFD6A94A);
        canvas.drawRoundRect(rect, 12f, 12f, paintHudBorder);
    }

    private RectF insetRect(RectF rect, float inset) {
        return new RectF(rect.left + inset, rect.top + inset, rect.right - inset, rect.bottom - inset);
    }

    private void drawPauseButton(Canvas canvas, RectF panel) {
        if (panel == null) return;

        drawHudPanel(canvas, panel);

        String icon = state == GameState.PAUSED ? "▶" : "⏸";
        paintHudText.setTextSize(panel.height() * PAUSE_ICON_SIZE_RATIO);
        paintHudText.setColor(state == GameState.PAUSED ? 0xFF00FF88 : 0xFFFFE08A);

        Paint.FontMetrics metrics = paintHudText.getFontMetrics();
        float textY = panel.centerY() - (metrics.ascent + metrics.descent) / 2f;
        canvas.drawText(icon, panel.centerX(), textY, paintHudText);
    }

    private void drawOverlay(Canvas canvas) {
        switch (state) {
            case COUNTDOWN: {
                paintGoalFlash.setAlpha(25);
                canvas.drawRect(screenRect, paintGoalFlash);
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
                paintHudText.setTextSize(fH * 0.20f);
                paintHudText.setColor(Color.WHITE);
                canvas.drawText("BUT !", fLeft + fW / 2f, fTop + fH * 0.42f, paintHudText);
                paintHudText.setTextSize(fH * 0.09f);
                int teamColor = lastScorer == 1 ? COLOR_P1 : COLOR_P2;
                paintHudText.setColor(teamColor);
                String team = lastScorer == 1 ? "Équipe Rouge" : "Équipe Bleue";
                canvas.drawText(team, fLeft + fW / 2f, fTop + fH * 0.58f, paintHudText);
                break;
            }
            case REPLAY: {
                paintOverlay.setAlpha(70);
                canvas.drawRect(screenRect, paintOverlay);
                paintHudText.setTextSize(fH * 0.12f);
                paintHudText.setColor(Color.WHITE);
                canvas.drawText("REPLAY", fLeft + fW / 2f, fTop + fH * 0.20f, paintHudText);
                paintHudText.setTextSize(fH * 0.055f);
                int teamColor = lastScorer == 1 ? COLOR_P1 : COLOR_P2;
                paintHudText.setColor(teamColor);
                String team = lastScorer == 1 ? "But Rouge" : "But Bleu";
                canvas.drawText(team, fLeft + fW / 2f, fTop + fH * 0.29f, paintHudText);
                break;
            }
            case PAUSED: {
                paintOverlay.setAlpha(160);
                canvas.drawRect(screenRect, paintOverlay);
                paintHudText.setTextSize(fH * 0.14f);
                paintHudText.setColor(Color.WHITE);
                canvas.drawText("PAUSE", fLeft + fW / 2f, fTop + fH * 0.32f, paintHudText);
                drawPauseMenuButtons(canvas);
                paintHudText.setTextSize(1f);
                paintHudText.setColor(0x00000000);
                canvas.drawText("Appuie sur ⏸ pour reprendre", fLeft + fW / 2f, fTop + fH * 0.60f, paintHudText);
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

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        int action       = event.getActionMasked();
        int pointerIndex = event.getActionIndex();
        int pointerId    = event.getPointerId(pointerIndex);
        float tx = event.getX(pointerIndex);
        float ty = event.getY(pointerIndex);

        if (action == MotionEvent.ACTION_DOWN || action == MotionEvent.ACTION_POINTER_DOWN) {
            if (pauseButtonRect != null && pauseButtonRect.contains(tx, ty)) {
                togglePause();
                return true;
            }
            if (state == GameState.PAUSED) {
                handlePauseButtonTouch(tx, ty);
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

    private void handlePauseButtonTouch(float x, float y) {
        if (btnResume != null && btnResume.contains(x, y)) {
            togglePause();
        } else if (btnSettings != null && btnSettings.contains(x, y)) {
            if (listener != null) {
                mainHandler.post(() -> listener.onSettingsRequested());
            }
        } else if (btnQuit != null && btnQuit.contains(x, y)) {
            if (listener != null) {
                mainHandler.post(() -> listener.onQuitRequested());
            }
        }
    }

    private void drawPauseMenuButtons(Canvas canvas) {
        float buttonW = fW * 0.44f;
        float buttonH = fH * 0.10f;
        float left = fLeft + (fW - buttonW) / 2f;
        float top = fTop + fH * 0.42f;
        float gap = buttonH * 0.24f;

        btnResume = new RectF(left, top, left + buttonW, top + buttonH);
        btnSettings = new RectF(left, top + buttonH + gap, left + buttonW, top + buttonH * 2f + gap);
        btnQuit = new RectF(left, top + (buttonH + gap) * 2f, left + buttonW, top + buttonH * 3f + gap * 2f);

        drawMenuButton(canvas, btnResume, "REPRENDRE", 0xFF2E7D32);
        drawMenuButton(canvas, btnSettings, "PARAMETRES", 0xFF1565C0);
        drawMenuButton(canvas, btnQuit, "QUITTER", 0xFFC62828);
    }

    private void drawMenuButton(Canvas canvas, RectF rect, String label, int color) {
        Paint bg = new Paint(Paint.ANTI_ALIAS_FLAG);
        bg.setColor(color);
        canvas.drawRoundRect(rect, 12f, 12f, bg);

        paintHudBorder.setStrokeWidth(Math.max(2f, rect.height() * 0.045f));
        paintHudBorder.setColor(0xFFD6A94A);
        canvas.drawRoundRect(rect, 12f, 12f, paintHudBorder);

        paintHudText.setTextSize(rect.height() * 0.38f);
        paintHudText.setColor(Color.WHITE);
        Paint.FontMetrics metrics = paintHudText.getFontMetrics();
        float textY = rect.centerY() - (metrics.ascent + metrics.descent) / 2f;
        canvas.drawText(label, rect.centerX(), textY, paintHudText);
    }

    private void assignPointerToMallet(int pointerId, float x, float y) {
        float minDist   = Float.MAX_VALUE;
        int   bestMallet = -1;
        for (int m = 0; m < mallets.length; m++) {
            if (malletPointerIds[m] != -1) continue;
            Mallet mallet = mallets[m];
            if (x < mallet.minX || x > mallet.maxX || y < mallet.minY || y > mallet.maxY) continue;
            float dx = x - mallet.x;
            float dy = y - mallet.y;
            float dist = dx * dx + dy * dy;
            if (dist < minDist) { minDist = dist; bestMallet = m; }
        }
        if (bestMallet == -1) {
            for (int m = 0; m < mallets.length; m++) {
                if (malletPointerIds[m] != -1) continue;
                Mallet mallet = mallets[m];
                if (x >= mallet.minX && x <= mallet.maxX && y >= mallet.minY && y <= mallet.maxY) {
                    bestMallet = m; break;
                }
            }
        }
        if (bestMallet != -1) {
            malletPointerIds[bestMallet] = pointerId;
            mallets[bestMallet].moveTo(x, y);
        }
    }

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
        // Libération de la mémoire audio à la destruction du plateau de jeu
        if (soundPool != null) {
            soundPool.release();
            soundPool = null;
        }
    }

    public void pauseGame()  { if (gameThread != null) gameThread.stopThread(); }
    public void resumeGame() {
        if (gameThread == null || !gameThread.isAlive()) {
            gameThread = new GameThread();
            gameThread.start();
        }
    }

    public int[] getCurrentScores() {
        return scores != null ? scores.clone() : new int[]{0, 0};
    }

    public int[] getPlayerGoals() {
        return playerGoals != null ? playerGoals.clone() : new int[0];
    }

    public String getReplayData() {
        if (!replayEnabled) return "";
        return ReplayData.toJson(goalReplays);
    }
}
