package com.example.duckpuck;

import android.content.Context;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

@Database(entities = {Joueur.class, Partie.class, Participer.class}, version = 1)
public abstract class AppDatabase extends RoomDatabase {

    public abstract AppDao appDao();

    // ── Singleton ──────────────────────────────────────────────────────────
    private static volatile AppDatabase INSTANCE;

    public static AppDatabase getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(
                            context.getApplicationContext(),
                            AppDatabase.class,
                            "duckpuck_db"
                    ).build();
                }
            }
        }
        return INSTANCE;
    }
}