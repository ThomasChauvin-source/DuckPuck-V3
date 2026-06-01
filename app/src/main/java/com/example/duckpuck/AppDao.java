package com.example.duckpuck;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Transaction;

import java.util.List;

@Dao
public interface AppDao {

    // Insertions
    @Insert
    long insertJoueur(Joueur joueur);

    @Insert
    long insertPartie(Partie partie);

    @Insert
    void insertParticipation(Participer participer);

    // Historique complet
    @Transaction
    @Query("SELECT * FROM Partie")
    List<PartieAvecJoueurs> getHistoriqueParties();

    // Parties d’un joueur
    @Query("SELECT * FROM Participer WHERE id_joueur = :joueurId")
    List<Participer> getParticipationsJoueur(int joueurId);
}