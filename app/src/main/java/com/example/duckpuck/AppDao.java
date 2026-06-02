package com.example.duckpuck;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Transaction;

import java.util.List;

@Dao
public interface AppDao {

    // ── Insertions ────────────────────────────────────────────────────────
    @Insert
    long insertJoueur(Joueur joueur);

    @Insert
    long insertPartie(Partie partie);

    @Insert
    void insertParticipation(Participer participer);

    // ── Historique complet ────────────────────────────────────────────────
    @Transaction
    @Query("SELECT * FROM Partie ORDER BY id_partie DESC")
    List<PartieAvecJoueurs> getHistoriqueParties();

    // ── Participations d'un joueur ────────────────────────────────────────
    @Query("SELECT * FROM Participer WHERE id_joueur = :joueurId")
    List<Participer> getParticipationsJoueur(int joueurId);

    // ── Liste de tous les joueurs ─────────────────────────────────────────
    @Query("SELECT * FROM Joueur ORDER BY nom ASC")
    List<Joueur> getAllJoueurs();

    @Query("SELECT * FROM Joueur ORDER BY buts DESC, nom ASC LIMIT 10")
    List<Joueur> getTopButeurs();

    @Query("SELECT * FROM Joueur ORDER BY nbr_parties DESC, nom ASC LIMIT 10")
    List<Joueur> getTopParties();

    // ── Un joueur par son id ──────────────────────────────────────────────
    @Query("SELECT * FROM Joueur WHERE id_joueur = :id")
    Joueur getJoueurById(int id);

    // ── Nom d'un joueur par son id ────────────────────────────────────────
    @Query("SELECT nom FROM Joueur WHERE id_joueur = :id")
    String getNomJoueur(int id);

    // ── Stats globales d'un joueur ────────────────────────────────────────
    @Query("SELECT COUNT(*) FROM Participer WHERE id_joueur = :joueurId")
    int getNbParties(int joueurId);

    @Query("SELECT COUNT(*) FROM Participer WHERE id_joueur = :joueurId AND a_gagne = 1")
    int getNbVictoires(int joueurId);

    @Query("SELECT SUM(buts) FROM Participer WHERE id_joueur = :joueurId")
    int getTotalButs(int joueurId);

    // ── Mise à jour des stats globales du joueur ──────────────────────────
    @Query("UPDATE Joueur SET buts = buts + :buts, win = win + :win, nbr_parties = nbr_parties + 1 WHERE id_joueur = :id")
    void updateStatsJoueur(int id, int buts, int win);
}
