package com.example.duckpuck;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "joueurs")
public class Player {

    @PrimaryKey(autoGenerate = true)
    public int id;

    @NonNull
    @ColumnInfo(name = "nom_joueur")
    public String name;

    @ColumnInfo(name = "parties_jouees")
    public int gamesPlayed;

    @ColumnInfo(name = "parties_gagnees")
    public int gamesWon;

    public Player(@NonNull String name, int gamesPlayed, int gamesWon) {
        this.name = name;
        this.gamesPlayed = gamesPlayed;
        this.gamesWon = gamesWon;
    }
}