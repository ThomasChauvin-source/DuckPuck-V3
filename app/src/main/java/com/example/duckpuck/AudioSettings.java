package com.example.duckpuck;

import android.content.Context;
import android.content.SharedPreferences;

public final class   AudioSettings {

    private static final String PREFS_NAME = "duckpuck_audio";
    private static final String KEY_MUSIC_VOLUME = "music_volume";
    private static final String KEY_SFX_VOLUME = "sfx_volume";
    private static final float DEFAULT_MUSIC_VOLUME = 0.5f;
    private static final float DEFAULT_SFX_VOLUME = 1.0f;

    private AudioSettings() {
    }

    public static float getMusicVolume(Context context) {
        return getPrefs(context).getFloat(KEY_MUSIC_VOLUME, DEFAULT_MUSIC_VOLUME);
    }

    public static void setMusicVolume(Context context, float volume) {
        getPrefs(context).edit().putFloat(KEY_MUSIC_VOLUME, clamp(volume)).apply();
    }

    public static float getSfxVolume(Context context) {
        return getPrefs(context).getFloat(KEY_SFX_VOLUME, DEFAULT_SFX_VOLUME);
    }

    public static void setSfxVolume(Context context, float volume) {
        getPrefs(context).edit().putFloat(KEY_SFX_VOLUME, clamp(volume)).apply();
    }

    private static SharedPreferences getPrefs(Context context) {
        return context.getApplicationContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    private static float clamp(float volume) {
        return Math.max(0f, Math.min(1f, volume));
    }
}
