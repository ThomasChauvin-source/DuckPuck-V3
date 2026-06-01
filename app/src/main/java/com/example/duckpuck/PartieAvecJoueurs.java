package com.example.duckpuck;

import androidx.room.Embedded;
import androidx.room.Relation;

import java.util.List;

public class PartieAvecJoueurs {

    @Embedded
    public Partie partie;

    @Relation(
            parentColumn = "id_partie",
            entityColumn = "id_partie",
            entity = Participer.class
    )
    public List<Participer> participations;
}
