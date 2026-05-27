package com.example.duckpuck;

import android.media.MediaPlayer;
import android.os.Bundle;
import android.widget.Button;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

public class MainActivity extends AppCompatActivity {

    private MediaPlayer mediaPlayer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Démarrer la musique
        mediaPlayer = MediaPlayer.create(this, R.raw.menu);
        mediaPlayer.setLooping(true);  // boucle infinie
        mediaPlayer.setVolume(0.5f, 0.5f);  // volume 50% gauche/droite
        mediaPlayer.start();

        if (savedInstanceState == null) {
            chargerFragment(new JouerFragment());
        }

        Button btnJouer        = findViewById(R.id.btnJouer);
        Button btnAjouter      = findViewById(R.id.btnAjouterJoueur);
        Button btnHistorique   = findViewById(R.id.btnHistorique);
        Button btnStatistiques = findViewById(R.id.btnStatistiques);
        Button btnParametres   = findViewById(R.id.btnParametres);

        btnJouer.setOnClickListener(v        -> chargerFragment(new JouerFragment()));
        btnAjouter.setOnClickListener(v      -> chargerFragment(new AjouterJoueurFragment()));
        btnHistorique.setOnClickListener(v   -> chargerFragment(new HistoriqueFragment()));
        btnStatistiques.setOnClickListener(v -> chargerFragment(new StatsFragment()));
        btnParametres.setOnClickListener(v   -> chargerFragment(new ParametresFragment()));
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mediaPlayer != null && mediaPlayer.isPlaying()) {
            mediaPlayer.pause();  // pause si l'app passe en arrière-plan
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mediaPlayer != null && !mediaPlayer.isPlaying()) {
            mediaPlayer.start();  // reprend quand on revient
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mediaPlayer != null) {
            mediaPlayer.stop();
            mediaPlayer.release();  // libère la mémoire
            mediaPlayer = null;
        }
    }

    private void chargerFragment(Fragment fragment) {
        FragmentManager fm = getSupportFragmentManager();
        FragmentTransaction ft = fm.beginTransaction();
        ft.replace(R.id.fragmentContainerView, fragment);
        ft.commit();
    }
}