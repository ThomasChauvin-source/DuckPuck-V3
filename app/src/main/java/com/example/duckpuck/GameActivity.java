package com.example.duckpuck;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class GameActivity extends AppCompatActivity implements GameView.GameListener {

    public static final String EXTRA_MODE = "game_mode";
    public static final String EXTRA_EQUIPE_1_IDS = "equipe_1_ids";
    public static final String EXTRA_EQUIPE_2_IDS = "equipe_2_ids";

    private GameView gameView;
    private final ExecutorService dbExecutor = Executors.newSingleThreadExecutor();
    private long startTimeMillis;
    private boolean partieSauvegardee = false;

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
        startTimeMillis = System.currentTimeMillis();
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
        sauvegarderPartie(winnerTeam, scores, gameView.getPlayerGoals(), false);

        String winnerName = winnerTeam == 1 ? "Equipe Rouge" : "Equipe Bleue";
        String message = winnerName + " gagne !\n\n"
                + "Rouge : " + scores[0] + " - Bleu : " + scores[1];

        new AlertDialog.Builder(this)
                .setTitle("Fin de partie")
                .setMessage(message)
                .setCancelable(false)
                .setPositiveButton("Rejouer", (dialog, which) -> recreate())
                .setNegativeButton("Menu", (dialog, which) -> finish())
                .show();
    }

    @Override
    public void onSettingsRequested() {
        afficherParametresAudio();
    }

    @Override
    public void onQuitRequested() {
        new AlertDialog.Builder(this)
                .setTitle("Quitter la partie")
                .setMessage("La partie sera enregistree comme arretee en cours.")
                .setPositiveButton("Quitter", (dialog, which) -> {
                    sauvegarderPartie(0, gameView.getCurrentScores(), gameView.getPlayerGoals(), true);
                    finish();
                })
                .setNegativeButton("Annuler", null)
                .show();
    }

    private void sauvegarderPartie(int winnerTeam, int[] scores, int[] butsParMallet, boolean arretee) {
        if (partieSauvegardee) return;
        partieSauvegardee = true;
        int dureeSecondes = Math.max(0, (int) ((System.currentTimeMillis() - startTimeMillis) / 1000L));

        dbExecutor.execute(() -> {
            AppDao dao = AppDatabase.getInstance(this).appDao();

            Partie partie = new Partie();
            partie.score_equipe1 = scores[0];
            partie.score_equipe2 = scores[1];
            partie.temps = dureeSecondes;
            partie.arretee = arretee;
            long partieId = dao.insertPartie(partie);

            int[] eq1Ids = getIntent().getIntArrayExtra(EXTRA_EQUIPE_1_IDS);
            int[] eq2Ids = getIntent().getIntArrayExtra(EXTRA_EQUIPE_2_IDS);

            if (eq1Ids != null && eq2Ids != null) {
                enregistrerEquipe(dao, partieId, eq1Ids, eq1Ids, eq2Ids, butsParMallet, 1, winnerTeam);
                enregistrerEquipe(dao, partieId, eq2Ids, eq1Ids, eq2Ids, butsParMallet, 2, winnerTeam);
            } else {
                enregistrerJoueursSecours(dao, partieId, winnerTeam);
            }
        });
    }

    private void enregistrerEquipe(AppDao dao, long partieId, int[] equipeIds, int[] eq1Ids, int[] eq2Ids,
                                   int[] butsParMallet, int equipe, int winnerTeam) {
        for (int id : equipeIds) {
            Participer participation = new Participer();
            participation.id_partie = (int) partieId;
            participation.id_joueur = id;
            participation.equipe = equipe;
            participation.buts = getButsPourJoueur(id, eq1Ids, eq2Ids, butsParMallet);
            participation.a_gagne = winnerTeam == equipe;
            dao.insertParticipation(participation);

            int victoire = participation.a_gagne ? 1 : 0;
            dao.updateStatsJoueur(id, participation.buts, victoire);
        }
    }

    private void enregistrerJoueursSecours(AppDao dao, long partieId, int winnerTeam) {
        java.util.List<Joueur> joueurs = dao.getAllJoueurs();
        if (joueurs.isEmpty()) return;

        int mode = getIntent().getIntExtra(EXTRA_MODE, GameView.MODE_2P);
        int parEquipe = (mode == GameView.MODE_4P) ? 2 : 1;

        for (int i = 0; i < joueurs.size() && i < parEquipe * 2; i++) {
            Joueur joueur = joueurs.get(i);
            int equipe = (i < parEquipe) ? 1 : 2;

            Participer participation = new Participer();
            participation.id_partie = (int) partieId;
            participation.id_joueur = joueur.id_joueur;
            participation.equipe = equipe;
            participation.buts = 0;
            participation.a_gagne = winnerTeam == equipe;
            dao.insertParticipation(participation);

            int victoire = participation.a_gagne ? 1 : 0;
            dao.updateStatsJoueur(joueur.id_joueur, 0, victoire);
        }
    }

    private int getButsPourJoueur(int joueurId, int[] eq1Ids, int[] eq2Ids, int[] butsParMallet) {
        if (butsParMallet == null) return 0;

        if (eq1Ids != null && eq1Ids.length > 0 && joueurId == eq1Ids[0] && butsParMallet.length > 0) {
            return butsParMallet[0];
        }
        if (eq2Ids != null && eq2Ids.length > 0 && joueurId == eq2Ids[0] && butsParMallet.length > 1) {
            return butsParMallet[1];
        }
        if (eq1Ids != null && eq1Ids.length > 1 && joueurId == eq1Ids[1] && butsParMallet.length > 2) {
            return butsParMallet[2];
        }
        if (eq2Ids != null && eq2Ids.length > 1 && joueurId == eq2Ids[1] && butsParMallet.length > 3) {
            return butsParMallet[3];
        }
        return 0;
    }

    private void afficherParametresAudio() {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        int padding = (int) (24 * getResources().getDisplayMetrics().density);
        layout.setPadding(padding, padding, padding, 0);

        TextView tvMusique = new TextView(this);
        SeekBar seekMusique = new SeekBar(this);
        seekMusique.setMax(100);
        seekMusique.setProgress(Math.round(AudioSettings.getMusicVolume(this) * 100f));
        updateVolumeLabel(tvMusique, "Musique", seekMusique.getProgress());

        TextView tvBruitage = new TextView(this);
        SeekBar seekBruitage = new SeekBar(this);
        seekBruitage.setMax(100);
        seekBruitage.setProgress(Math.round(AudioSettings.getSfxVolume(this) * 100f));
        updateVolumeLabel(tvBruitage, "Bruitages", seekBruitage.getProgress());

        layout.addView(tvMusique);
        layout.addView(seekMusique);
        layout.addView(tvBruitage);
        layout.addView(seekBruitage);

        seekMusique.setOnSeekBarChangeListener(new VolumeSeekBarListener(progress -> {
            AudioSettings.setMusicVolume(this, progress / 100f);
            updateVolumeLabel(tvMusique, "Musique", progress);
        }));
        seekBruitage.setOnSeekBarChangeListener(new VolumeSeekBarListener(progress -> {
            AudioSettings.setSfxVolume(this, progress / 100f);
            updateVolumeLabel(tvBruitage, "Bruitages", progress);
        }));

        new AlertDialog.Builder(this)
                .setTitle("Parametres audio")
                .setView(layout)
                .setPositiveButton("OK", null)
                .show();
    }

    private void updateVolumeLabel(TextView textView, String label, int progress) {
        textView.setText(label + " : " + progress + " %");
    }

    private static class VolumeSeekBarListener implements SeekBar.OnSeekBarChangeListener {
        private final VolumeCallback callback;

        VolumeSeekBarListener(VolumeCallback callback) {
            this.callback = callback;
        }

        @Override
        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
            callback.onProgress(progress);
        }

        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {
        }

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {
        }
    }

    private interface VolumeCallback {
        void onProgress(int progress);
    }
}
