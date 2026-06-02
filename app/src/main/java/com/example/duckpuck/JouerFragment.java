package com.example.duckpuck;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;

public class JouerFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_jouer, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        CardView carte1v1 = view.findViewById(R.id.carte1v1);
        CardView carte2v2 = view.findViewById(R.id.carte2v2);

        // Récupération sécurisée du NavController propre à ce fragment
        NavController navController = NavHostFragment.findNavController(this);

        carte1v1.setOnClickListener(v -> {
            Bundle args = new Bundle();
            args.putBoolean("is2v2", false);
            navController.navigate(R.id.choixJoueursFragment, args);
        });

        carte2v2.setOnClickListener(v -> {
            Bundle args = new Bundle();
            args.putBoolean("is2v2", true);
            navController.navigate(R.id.choixJoueursFragment, args);
        });
    }
}