package com.example.duckpuck;

import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.media.SoundPool;
import android.os.Bundle;
import android.widget.Button;
import androidx.appcompat.app.AppCompatActivity;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;

public class MainActivity extends AppCompatActivity {

    private MediaPlayer mediaPlayer;
    private SoundPool menuSoundPool;
    private int menuClickSoundId;
    private NavController navController;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Gestion de la musique de fond
        mediaPlayer = MediaPlayer.create(this, R.raw.menu);
        if (mediaPlayer != null) {
            mediaPlayer.setLooping(true);
            applyMusicVolume();
            mediaPlayer.start();
        }
        initMenuClickSound();

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
            btnJouer.setOnClickListener(v -> navigateWithMenuClick(R.id.jouerFragment));
            btnAjouter.setOnClickListener(v -> navigateWithMenuClick(R.id.ajouterJoueurFragment));
            btnHistorique.setOnClickListener(v -> navigateWithMenuClick(R.id.historiqueFragment));
            btnStatistiques.setOnClickListener(v -> navigateWithMenuClick(R.id.statistiquesFragment));
            btnParametres.setOnClickListener(v -> navigateWithMenuClick(R.id.parametresFragment));
        }
    }

    private void initMenuClickSound() {
        AudioAttributes audioAttributes = new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build();

        menuSoundPool = new SoundPool.Builder()
                .setMaxStreams(2)
                .setAudioAttributes(audioAttributes)
                .build();
        menuClickSoundId = menuSoundPool.load(this, R.raw.clic_bouton_menu, 1);
    }

    private void navigateWithMenuClick(int destinationId) {
        playMenuClickSound();
        navController.navigate(destinationId);
    }

    private void playMenuClickSound() {
        if (menuSoundPool == null || menuClickSoundId == 0) return;
        float volume = AudioSettings.getSfxVolume(this);
        menuSoundPool.play(menuClickSoundId, volume, volume, 1, 0, 1.0f);
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
        applyMusicVolume();
        if (mediaPlayer != null && !mediaPlayer.isPlaying()) {
            mediaPlayer.start();
        }
    }

    public void applyMusicVolume() {
        if (mediaPlayer == null) return;
        float volume = AudioSettings.getMusicVolume(this);
        mediaPlayer.setVolume(volume, volume);
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
        if (menuSoundPool != null) {
            menuSoundPool.release();
            menuSoundPool = null;
        }
    }
}
