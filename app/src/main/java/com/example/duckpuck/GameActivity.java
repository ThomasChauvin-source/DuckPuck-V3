package com.example.duckpuck;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.WindowManager;

import androidx.appcompat.app.AppCompatActivity;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class GameActivity extends AppCompatActivity implements GameView.GameListener {

    public static final String EXTRA_MODE = "game_mode";

    private GameView gameView;
    private final ExecutorService dbExecutor = Executors.newSingleThreadExecutor();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().setFlags(
                WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN
        );
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

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

    @Override
    protected void onDestroy() {
        super.onDestroy();
        dbExecutor.shutdown();
    }

    // ── Callback : fin de partie ──────────────────────────────────────────
    @Override
    public void onGameOver(int winnerTeam, int[] scores) {
        // Sauvegarder la partie en BDD (thread background obligatoire avec Room)
        sauvegarderPartie(winnerTeam, scores);

        String winnerName = winnerTeam == 1 ? "Équipe Rouge" : "Équipe Bleue";
        String message = winnerName + " gagne !\n\n"
                + "Rouge : " + scores[0] + "  —  Bleu : " + scores[1];

        new AlertDialog.Builder(this)
                .setTitle("Fin de partie 🏆")
                .setMessage(message)
                .setCancelable(false)
                .setPositiveButton("Rejouer", (dialog, which) -> recreate())
                .setNegativeButton("Menu",    (dialog, which) -> finish())
                .show();
    }

    /**
     * Enregistre la partie terminée dans la base de données.
     * Crée une entrée Partie + une entrée Participer par joueur enregistré.
     * Si aucun joueur n'est enregistré, seul le score est sauvegardé.
     */
    private void sauvegarderPartie(int winnerTeam, int[] scores) {
        dbExecutor.execute(() -> {
            AppDao dao = AppDatabase.getInstance(this).appDao();

            // 1. Créer l'entrée Partie
            Partie partie = new Partie();
            partie.score_equipe1 = scores[0];
            partie.score_equipe2 = scores[1];
            partie.temps = 0; // pas de chrono pour l'instant
            long partieId = dao.insertPartie(partie);

            // 2. Relier les joueurs existants à cette partie
            //    Convention : joueurs triés par id, les premiers vont en équipe 1
            java.util.List<Joueur> joueurs = dao.getAllJoueurs();
            if (joueurs.isEmpty()) return;

            int mode = getIntent().getIntExtra(EXTRA_MODE, GameView.MODE_2P);
            int parEquipe = (mode == GameView.MODE_4P) ? 2 : 1;

            for (int i = 0; i < joueurs.size() && i < parEquipe * 2; i++) {
                Joueur j = joueurs.get(i);
                int equipe = (i < parEquipe) ? 1 : 2;

                Participer p = new Participer();
                p.id_partie  = (int) partieId;
                p.id_joueur  = j.id_joueur;
                p.equipe     = equipe;
                p.buts       = 0; // buts individuels non trackés pour l'instant
                p.a_gagne    = (equipe == winnerTeam);
                dao.insertParticipation(p);

                // Mettre à jour les stats globales du joueur
                int victoire = p.a_gagne ? 1 : 0;
                dao.updateStatsJoueur(j.id_joueur, 0, victoire);
            }
        });
    }
}