package com.example.duckpuck;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;

public class MenuFragment extends Fragment {

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_menu, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        NavController navController = NavHostFragment.findNavController(this);

        Button btnJouer = view.findViewById(R.id.btnJouer);
        Button btnStats = view.findViewById(R.id.btnStatistiques);
        Button btnHistorique = view.findViewById(R.id.btnHistorique);
        Button btnParam = view.findViewById(R.id.btnParametres);
        Button btnAjout = view.findViewById(R.id.btnAjouterJoueur);

        btnJouer.setOnClickListener(v ->
                navController.navigate(R.id.action_menu_to_jouer));

        btnStats.setOnClickListener(v ->
                navController.navigate(R.id.action_menu_to_stats));

        btnHistorique.setOnClickListener(v ->
                navController.navigate(R.id.action_menu_to_historique));

        btnParam.setOnClickListener(v ->
                navController.navigate(R.id.action_menu_to_parametres));

        btnAjout.setOnClickListener(v ->
                navController.navigate(R.id.action_menu_to_ajouter));
    }
}