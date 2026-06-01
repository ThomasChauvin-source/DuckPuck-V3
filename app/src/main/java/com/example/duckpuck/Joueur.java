package com.example.duckpuck;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity
public class Joueur {

    @PrimaryKey(autoGenerate = true)
    public int id_joueur;

    public String nom;
    public int buts;
    public int win;
    public int nbr_parties;
}
