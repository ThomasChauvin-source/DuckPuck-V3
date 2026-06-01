package com.example.duckpuck;

import androidx.room.Database;
import androidx.room.RoomDatabase;

@Database(entities = {Joueur.class, Partie.class, Participer.class}, version = 1)
public abstract class AppDatabase extends RoomDatabase {
    public abstract AppDao appDao();
}
