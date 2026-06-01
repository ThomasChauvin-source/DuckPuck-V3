package com.example.duckpuck;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

public class JouerFragment extends Fragment {

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_jouer, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Récupération de tes composants d'origine
        View carte1v1 = view.findViewById(R.id.carte1v1);
        View carte2v2 = view.findViewById(R.id.carte2v2);

        // Petite modification : on ouvre l'écran de sélection de joueurs dédié
        carte1v1.setOnClickListener(v -> naviguerVersChoixJoueurs(GameView.MODE_2P));
        carte2v2.setOnClickListener(v -> naviguerVersChoixJoueurs(GameView.MODE_4P));
    }

    private void naviguerVersChoixJoueurs(int mode) {
        ChoixJoueursFragment prochainFragment = new ChoixJoueursFragment();

        Bundle arguments = new Bundle();
        arguments.putInt("game_mode", mode);
        prochainFragment.setArguments(arguments);

        // ATTENTION : Remplace R.id.fragment_container par l'ID de ton conteneur de fragments réel dans activity_main.xml
        getParentFragmentManager().beginTransaction()
                .replace(R.id.fragmentContainerView, prochainFragment)
                .addToBackStack(null) // Permet au bouton "Retour" du téléphone de revenir au choix 1v1/2v2
                .commit();
    }
}