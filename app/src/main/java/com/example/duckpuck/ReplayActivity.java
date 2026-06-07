package com.example.duckpuck;

import static android.content.Intent.getIntent;import android.os.Bundle;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ReplayActivity extends AppCompatActivity {

    public static final String EXTRA_PARTIE_ID = "partie_id";

    private final ExecutorService dbExecutor = Executors.newSingleThreadExecutor();
    private ReplayView replayView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setFlags(
                WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN
        );
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        TextView loading = new TextView(this);
        loading.setText("Chargement des replays...");
        loading.setTextColor(0xFFFFFFFF);
        loading.setTextSize(22f);
        loading.setGravity(android.view.Gravity.CENTER);
        loading.setBackgroundColor(0xFF0A3D6B);
        setContentView(loading);

        int partieId = getIntent().getIntExtra(EXTRA_PARTIE_ID, -1);
        dbExecutor.execute(() -> {
            Partie partie = AppDatabase.getInstance(this).appDao().getPartieById(partieId);
            List<ReplayData.Goal> goals = partie != null ? ReplayData.fromJson(partie.replay_data) : java.util.Collections.emptyList();
            runOnUiThread(() -> showReplay(goals));
        });
    }

    private void showReplay(List<ReplayData.Goal> goals) {
        if (goals == null || goals.isEmpty()) {
            FrameLayout layout = new FrameLayout(this);
            layout.setBackgroundColor(0xFF0A3D6B);

            TextView empty = new TextView(this);
            empty.setText("Aucun replay disponible");
            empty.setTextColor(0xFFFFFFFF);
            empty.setTextSize(22f);
            empty.setGravity(android.view.Gravity.CENTER);
            FrameLayout.LayoutParams tvParams = new FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT);
            layout.addView(empty, tvParams);

            addBackButton(layout);
            setContentView(layout);
            return;
        }

        replayView = new ReplayView(this, goals);
        FrameLayout layout = new FrameLayout(this);
        layout.addView(replayView);
        addBackButton(layout);
        setContentView(layout);
    }

    private void addBackButton(FrameLayout parent) {
        Button btnBack = new Button(this);
        btnBack.setText("← Retour");
        btnBack.setTextColor(0xFFFFFFFF);
        btnBack.setBackgroundColor(0xCC000000);
        btnBack.setOnClickListener(v -> finish());

        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT);
        params.gravity = android.view.Gravity.TOP | android.view.Gravity.START;
        params.setMargins(16, 48, 0, 0);
        parent.addView(btnBack, params);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (replayView != null) replayView.stop();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (replayView != null) replayView.start();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        dbExecutor.shutdown();
    }
}