package com.example.duckpuck;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.content.Intent;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    private Button btnJouer, btnAjouterJoueur, btnHistorique, btnStatistiques, btnParametres;
    private FrameLayout carte1v1, carte2v2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Liaison des boutons du menu
        btnJouer         = findViewById(R.id.btnJouer);
        btnAjouterJoueur = findViewById(R.id.btnAjouterJoueur);
        btnHistorique    = findViewById(R.id.btnHistorique);
        btnStatistiques  = findViewById(R.id.btnStatistiques);
        btnParametres    = findViewById(R.id.btnParametres);

        // Liaison des cartes de mode de jeu
        carte1v1 = findViewById(R.id.carte1v1);
        carte2v2 = findViewById(R.id.carte2v2);

        // Listeners
        btnJouer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // TODO : lancer la partie
            }
        });

        btnAjouterJoueur.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, com.example.duckpuck.AjouterUnJoueur.class);
                startActivity(intent);
            }
        });

        btnHistorique.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, com.example.duckpuck.HistoriqueDesParties.class);
                startActivity(intent);
            }
        });

        btnStatistiques.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, com.example.duckpuck.Statistiques.class);
                startActivity(intent);
            }
        });

        btnParametres.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, com.example.duckpuck.Parametre.class);
                startActivity(intent);
            }
        });

        carte1v1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // TODO : sélectionner le mode 1v1
            }
        });

        carte2v2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // TODO : sélectionner le mode 2v2
            }
        });
    }
}