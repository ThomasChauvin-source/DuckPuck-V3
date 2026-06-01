package com.example.duckpuck;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.WindowManager;

import androidx.appcompat.app.AppCompatActivity;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class GameActivity extends AppCompatActivity implements GameView.GameListener {

    public static final String EXTRA_MODE = "game_mode";
    // Petites clés ajoutées pour recevoir les IDs des joueurs sélectionnés
    public static final String EXTRA_EQUIPE_1_IDS = "equipe_1_ids";
    public static final String EXTRA_EQUIPE_2_IDS = "equipe_2_ids";

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

    @Override
    public void onGameOver(int winnerTeam, int[] scores) {
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

    private void sauvegarderPartie(int winnerTeam, int[] scores) {
        dbExecutor.execute(() -> {
            AppDao dao = AppDatabase.getInstance(this).appDao();

            // 1. Créer l'entrée Partie
            Partie partie = new Partie();
            partie.score_equipe1 = scores[0];
            partie.score_equipe2 = scores[1];
            partie.temps = 0;
            long partieId = dao.insertPartie(partie);

            // 2. Récupérer les vrais identifiants choisis dans le fragment de sélection
            int[] eq1Ids = getIntent().getIntArrayExtra(EXTRA_EQUIPE_1_IDS);
            int[] eq2Ids = getIntent().getIntArrayExtra(EXTRA_EQUIPE_2_IDS);

            if (eq1Ids != null && eq2Ids != null) {
                // Enregistrer et mettre à jour les joueurs de l'équipe 1 (Rouge)
                for (int id : eq1Ids) {
                    Participer p = new Participer();
                    p.id_partie  = (int) partieId;
                    p.id_joueur  = id;
                    p.equipe     = 1;
                    p.buts       = 0;
                    p.a_gagne    = (winnerTeam == 1);
                    dao.insertParticipation(p);

                    int victoire = p.a_gagne ? 1 : 0;
                    dao.updateStatsJoueur(id, 0, victoire); // +0 match nul, +1 ou 0 victoire
                }

                // Enregistrer et mettre à jour les joueurs de l'équipe 2 (Bleue)
                for (int id : eq2Ids) {
                    Participer p = new Participer();
                    p.id_partie  = (int) partieId;
                    p.id_joueur  = id;
                    p.equipe     = 2;
                    p.buts       = 0;
                    p.a_gagne    = (winnerTeam == 2);
                    dao.insertParticipation(p);

                    int victoire = p.a_gagne ? 1 : 0;
                    dao.updateStatsJoueur(id, 0, victoire);
                }
            } else {
                // Système de secours d'origine (si lancée sans sélection préalable)
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
                    p.buts       = 0;
                    p.a_gagne    = (equipe == winnerTeam);
                    dao.insertParticipation(p);

                    int victoire = p.a_gagne ? 1 : 0;
                    dao.updateStatsJoueur(j.id_joueur, 0, victoire);
                }
            }
        });
    }
}