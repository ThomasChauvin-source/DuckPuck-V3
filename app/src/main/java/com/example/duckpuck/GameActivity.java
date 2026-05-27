package com.example.duckpuck;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.WindowManager;

import androidx.appcompat.app.AppCompatActivity;

public class GameActivity extends AppCompatActivity implements GameView.GameListener {

    // Clé pour recevoir le mode de jeu depuis le menu
    public static final String EXTRA_MODE = "game_mode";

    private GameView gameView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Plein écran, écran toujours allumé
        getWindow().setFlags(
                WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN
        );
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        // Récupérer le mode (2 ou 4 joueurs) passé depuis le menu
        int mode = getIntent().getIntExtra(EXTRA_MODE, GameView.MODE_2P);

        gameView = new GameView(this, mode);
        gameView.setGameListener(this);
        setContentView(gameView);
    }

    @Override
    protected void onPause() {
        super.onPause();
        gameView.pauseGame();
    }

    @Override
    protected void onResume() {
        super.onResume();
        gameView.resumeGame();
    }

    // ── Callback : fin de partie ──────────────────────────────────────────
    @Override
    public void onGameOver(int winnerTeam, int[] scores) {
        // S'exécute sur le thread principal grâce au Handler dans GameView
        String winnerName = winnerTeam == 1 ? "Équipe Rouge" : "Équipe Bleue";
        String message = winnerName + " gagne !\n\n"
                + "Rouge : " + scores[0] + "  —  Bleu : " + scores[1];

        new AlertDialog.Builder(this)
                .setTitle("Fin de partie 🏆")
                .setMessage(message)
                .setCancelable(false)
                .setPositiveButton("Rejouer", (dialog, which) -> {
                    // Recréer l'activité pour repartir de zéro
                    recreate();
                })
                .setNegativeButton("Menu", (dialog, which) -> {
                    finish();
                })
                .show();
    }
}
