package com.example.duckpuck;

import androidx.room.Entity;

@Entity(primaryKeys = {"id_partie", "id_joueur"})
public class Participer {

    public int id_partie;
    public int id_joueur;

    public int equipe; // 1 ou 2
    public int buts;   // buts marqués dans cette partie
    public boolean a_gagne;
}