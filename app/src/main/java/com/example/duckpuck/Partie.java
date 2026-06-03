package com.example.duckpuck;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity
public class Partie {

    @PrimaryKey(autoGenerate = true)
    public int id_partie;

    public int score_equipe1;
    public int score_equipe2;
    public int temps;
    public boolean arretee;
    public String replay_data;
}
