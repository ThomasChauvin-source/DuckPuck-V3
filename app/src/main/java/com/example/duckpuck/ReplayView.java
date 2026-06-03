package com.example.duckpuck;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import java.util.List;

public class ReplayView extends SurfaceView implements SurfaceHolder.Callback {

    private static final long FRAME_MS = 36;
    private static final long GAP_MS = 900;
    private static final int COLOR_PUCK = 0xFFE0E0E0;
    private static final int[] MALLET_COLORS = {0xFFFF5252, 0xFF40C4FF, 0xFF69F0AE, 0xFFFFD740};

    private final List<ReplayData.Goal> goals;
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint shadowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint[] malletPaints = new Paint[MALLET_COLORS.length];

    private ReplayThread thread;
    private Bitmap bgBitmap;
    private Bitmap[] malletBitmaps;
    private float W;
    private float H;
    private float puckRadius;
    private float malletRadius;
    private int goalIndex;
    private long goalStartTime;

    public ReplayView(Context context, List<ReplayData.Goal> goals) {
        super(context);
        this.goals = goals;
        getHolder().addCallback(this);
        setFocusable(true);
        initPaints();
    }

    private void initPaints() {
        shadowPaint.setColor(Color.BLACK);
        shadowPaint.setAlpha(80);

        textPaint.setColor(Color.WHITE);
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setFakeBoldText(true);
        textPaint.setShadowLayer(3f, 2f, 2f, Color.BLACK);

        for (int i = 0; i < malletPaints.length; i++) {
            malletPaints[i] = new Paint(Paint.ANTI_ALIAS_FLAG);
            malletPaints[i].setColor(MALLET_COLORS[i]);
        }
    }

    private void loadBackground() {
        int resId = getResources().getIdentifier("game_bg", "drawable", getContext().getPackageName());
        if (resId != 0 && W > 0 && H > 0) {
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
        BitmapFactory.Options opts = new BitmapFactory.Options();
        opts.inScaled = false;

        for (int i = 0; i < resIds.length; i++) {
            malletBitmaps[i] = BitmapFactory.decodeResource(getResources(), resIds[i], opts);
        }
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        goalIndex = 0;
        goalStartTime = System.currentTimeMillis();
        start();
    }

    public void start() {
        if (thread != null && thread.isAlive()) return;
        thread = new ReplayThread();
        thread.start();
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        W = width;
        H = height;
        float fieldW = W * (0.914f - 0.085f);
        float fieldH = H * (0.960f - 0.120f);
        puckRadius = Math.min(fieldW, fieldH) * 0.045f;
        malletRadius = Math.min(fieldW, fieldH) * 0.070f;
        loadBackground();
        loadMalletImages();
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        stop();
    }

    public void stop() {
        if (thread != null) {
            thread.stopThread();
            try { thread.join(500); } catch (InterruptedException ignored) {}
            thread = null;
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        return true;
    }

    private void tick() {
        if (goals == null || goals.isEmpty()) return;

        ReplayData.Goal goal = goals.get(goalIndex);
        long elapsed = System.currentTimeMillis() - goalStartTime;
        long duration = getDuration(goal) + GAP_MS;
        if (elapsed >= duration) {
            goalIndex++;
            if (goalIndex >= goals.size()) goalIndex = 0;
            goalStartTime = System.currentTimeMillis();
        }
    }

    private void draw() {
        SurfaceHolder holder = getHolder();
        if (!holder.getSurface().isValid()) return;

        Canvas canvas = holder.lockCanvas();
        if (canvas == null) return;
        try {
            drawBackground(canvas);
            if (goals != null && !goals.isEmpty()) {
                ReplayData.Goal goal = goals.get(goalIndex);
                ReplayData.Frame frame = getFrameForTime(goal, System.currentTimeMillis() - goalStartTime);
                if (frame != null) drawFrame(canvas, frame);
                drawOverlay(canvas, goal);
            }
        } finally {
            holder.unlockCanvasAndPost(canvas);
        }
    }

    private void drawBackground(Canvas canvas) {
        if (bgBitmap != null) {
            canvas.drawBitmap(bgBitmap, 0, 0, null);
        } else {
            paint.setColor(0xFF0A3D6B);
            canvas.drawRect(new RectF(0, 0, W, H), paint);
        }
    }

    private void drawFrame(Canvas canvas, ReplayData.Frame frame) {
        int malletCount = Math.min(frame.malletX.length, frame.malletY.length);
        for (int i = 0; i < malletCount && i < malletPaints.length; i++) {
            drawMallet(canvas, frame.malletX[i] * W, frame.malletY[i] * H, malletPaints[i], i);
        }

        paint.setShader(null);
        paint.setColor(COLOR_PUCK);
        canvas.drawCircle(frame.puckX * W + 5, frame.puckY * H + 5, puckRadius, shadowPaint);
        canvas.drawCircle(frame.puckX * W, frame.puckY * H, puckRadius, paint);
    }

    private void drawMallet(Canvas canvas, float x, float y, Paint malletPaint, int malletIndex) {
        Bitmap malletBitmap = getMalletBitmap(malletIndex);
        if (malletBitmap != null) {
            canvas.drawCircle(x + malletRadius * 0.08f, y + malletRadius * 0.08f, malletRadius, shadowPaint);
            RectF dst = new RectF(x - malletRadius, y - malletRadius, x + malletRadius, y + malletRadius);
            canvas.drawBitmap(malletBitmap, null, dst, null);
            return;
        }

        canvas.drawCircle(x + 4, y + 4, malletRadius, shadowPaint);
        canvas.drawCircle(x, y, malletRadius, malletPaint);
        paint.setColor(Color.WHITE);
        paint.setAlpha(80);
        canvas.drawCircle(x - malletRadius * 0.25f, y - malletRadius * 0.25f, malletRadius * 0.35f, paint);
        paint.setColor(Color.BLACK);
        paint.setAlpha(60);
        canvas.drawCircle(x, y, malletRadius * 0.25f, paint);
        paint.setAlpha(255);
    }

    private Bitmap getMalletBitmap(int malletIndex) {
        if (malletBitmaps == null || malletBitmaps.length == 0) return null;
        if (malletIndex < 0) return null;
        return malletBitmaps[malletIndex % malletBitmaps.length];
    }

    private void drawOverlay(Canvas canvas, ReplayData.Goal goal) {
        paint.setShader(null);
        paint.setColor(Color.BLACK);
        paint.setAlpha(95);
        canvas.drawRect(0, 0, W, H * 0.18f, paint);
        paint.setAlpha(255);

        textPaint.setTextSize(H * 0.055f);
        textPaint.setColor(Color.WHITE);
        canvas.drawText("REPLAY DES BUTS", W / 2f, H * 0.065f, textPaint);

        textPaint.setTextSize(H * 0.035f);
        textPaint.setColor(goal.scorer == 1 ? MALLET_COLORS[0] : MALLET_COLORS[1]);
        String team = goal.scorer == 1 ? "Rouge" : "Bleu";
        canvas.drawText("But " + (goalIndex + 1) + "/" + goals.size() + " - " + team
                + " (" + goal.scoreRed + " - " + goal.scoreBlue + ")", W / 2f, H * 0.125f, textPaint);
    }

    private ReplayData.Frame getFrameForTime(ReplayData.Goal goal, long elapsed) {
        if (goal.frames.isEmpty()) return null;
        long replayTime = goal.frames.get(0).timeMs + elapsed;
        ReplayData.Frame selected = goal.frames.get(goal.frames.size() - 1);
        for (ReplayData.Frame frame : goal.frames) {
            if (frame.timeMs >= replayTime) {
                selected = frame;
                break;
            }
        }
        return selected;
    }

    private long getDuration(ReplayData.Goal goal) {
        if (goal.frames.size() < 2) return 1200;
        return Math.max(1200, goal.frames.get(goal.frames.size() - 1).timeMs - goal.frames.get(0).timeMs);
    }

    private class ReplayThread extends Thread {
        private volatile boolean running = true;

        @Override
        public void run() {
            while (running) {
                long start = System.currentTimeMillis();
                tick();
                draw();
                long sleep = FRAME_MS - (System.currentTimeMillis() - start);
                if (sleep > 0) {
                    try { Thread.sleep(sleep); } catch (InterruptedException ignored) {}
                }
            }
        }

        void stopThread() {
            running = false;
        }
    }
}
