package com.example.duckpuck;

import android.media.MediaPlayer;
import android.os.Bundle;
import android.widget.Button;
import androidx.appcompat.app.AppCompatActivity;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;

public class MainActivity extends AppCompatActivity {

    private MediaPlayer mediaPlayer;
    private NavController navController;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Gestion de la musique de fond
        mediaPlayer = MediaPlayer.create(this, R.raw.menu);
        if (mediaPlayer != null) {
            mediaPlayer.setLooping(true);
            mediaPlayer.setVolume(0.5f, 0.5f);
            mediaPlayer.start();
        }

        // Récupération propre du NavController
        NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager()
                .findFragmentById(R.id.fragmentContainerView);
        if (navHostFragment != null) {
            navController = navHostFragment.getNavController();
        }

        // Liaison des boutons du menu latéral gauche
        Button btnJouer = findViewById(R.id.btnJouer);
        Button btnAjouter = findViewById(R.id.btnAjouterJoueur);
        Button btnHistorique = findViewById(R.id.btnHistorique);
        Button btnStatistiques = findViewById(R.id.btnStatistiques);
        Button btnParametres = findViewById(R.id.btnParametres);

        // Navigation sécurisée utilisant les ID du nav_graph.xml
        if (navController != null) {
            btnJouer.setOnClickListener(v -> navController.navigate(R.id.jouerFragment));
            btnAjouter.setOnClickListener(v -> navController.navigate(R.id.ajouterJoueurFragment));
            btnHistorique.setOnClickListener(v -> navController.navigate(R.id.historiqueFragment));
            btnStatistiques.setOnClickListener(v -> navController.navigate(R.id.statistiquesFragment));
            btnParametres.setOnClickListener(v -> navController.navigate(R.id.parametresFragment));
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mediaPlayer != null && mediaPlayer.isPlaying()) {
            mediaPlayer.pause();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mediaPlayer != null && !mediaPlayer.isPlaying()) {
            mediaPlayer.start();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mediaPlayer != null) {
            try {
                if (mediaPlayer.isPlaying()) {
                    mediaPlayer.stop();
                }
            } catch (IllegalStateException e) {
                // Ignore
            }
            mediaPlayer.release();
            mediaPlayer = null;
        }
    }
}