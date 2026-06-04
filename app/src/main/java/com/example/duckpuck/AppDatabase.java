package com.example.duckpuck;

import android.content.Context;
import androidx.room.Database;
import androidx.room.migration.Migration;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.sqlite.db.SupportSQLiteDatabase;

import java.io.File;

@Database(entities = {Joueur.class, Partie.class, Participer.class}, version = 3)
public abstract class AppDatabase extends RoomDatabase {

    public abstract AppDao appDao();

    private static final Migration MIGRATION_1_2 = new Migration(1, 2) {
        @Override
        public void migrate(SupportSQLiteDatabase database) {
            database.execSQL("ALTER TABLE Partie ADD COLUMN arretee INTEGER NOT NULL DEFAULT 0");
        }
    };

    private static final Migration MIGRATION_2_3 = new Migration(2, 3) {
        @Override
        public void migrate(SupportSQLiteDatabase database) {
            database.execSQL("ALTER TABLE Partie ADD COLUMN replay_data TEXT");
        }
    };

    // ── Singleton ──────────────────────────────────────────────────────────
    private static volatile AppDatabase INSTANCE;

    public static AppDatabase getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    Context appContext = context.getApplicationContext();
                    String databaseName = getDatabaseName(appContext);

                    INSTANCE = Room.databaseBuilder(
                            appContext,
                            AppDatabase.class,
                            databaseName
                    )
                            .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                            .build();
                }
            }
        }
        return INSTANCE;
    }

    private static String getDatabaseName(Context context) {
        File externalDirectory = context.getExternalFilesDir("databases");
        if (externalDirectory != null && (externalDirectory.exists() || externalDirectory.mkdirs())) {
            return new File(externalDirectory, "duckpuck_db").getAbsolutePath();
        }

        ensureDatabaseDirectory(context);
        return "duckpuck_db";
    }

    private static void ensureDatabaseDirectory(Context context) {
        File databaseFile = context.getDatabasePath("duckpuck_db");
        File databaseDirectory = databaseFile.getParentFile();
        if (databaseDirectory != null && !databaseDirectory.exists()) {
            databaseDirectory.mkdirs();
        }
    }
}
