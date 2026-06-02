package com.example.duckpuck;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ChoixJoueursFragment extends Fragment {

    private final ExecutorService dbExecutor = Executors.newSingleThreadExecutor();
    private Spinner spinnerEq1J1, spinnerEq1J2, spinnerEq2J1, spinnerEq2J2;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_choix_joueurs, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        spinnerEq1J1 = view.findViewById(R.id.spinnerEq1Joueur1);
        spinnerEq1J2 = view.findViewById(R.id.spinnerEq1Joueur2);
        spinnerEq2J1 = view.findViewById(R.id.spinnerEq2Joueur1);
        spinnerEq2J2 = view.findViewById(R.id.spinnerEq2Joueur2);
        Button btnLancer = view.findViewById(R.id.btnLancerMatch);

        boolean is2v2 = getArguments() != null && getArguments().getBoolean("is2v2", false);

        if (is2v2) {
            spinnerEq1J2.setVisibility(View.VISIBLE);
            spinnerEq2J2.setVisibility(View.VISIBLE);
        }

        Context appContext = requireContext().getApplicationContext();

        // Chargement asynchrone sécurisé des joueurs depuis la base Room
        dbExecutor.execute(() -> {
            AppDao dao = AppDatabase.getInstance(appContext).appDao();
            List<Joueur> listeJoueurs = dao.getAllJoueurs();
            List<String> noms = new ArrayList<>();

            for (Joueur j : listeJoueurs) {
                noms.add(j.nom);
            }

            if (isAdded() && getActivity() != null) {
                requireActivity().runOnUiThread(() -> {
                    if (!isAdded()) return;

                    if (noms.isEmpty()) {
                        Toast.makeText(appContext, "Veuillez d'abord ajouter des joueurs dans le menu !", Toast.LENGTH_LONG).show();
                        return;
                    }

                    ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(),
                            android.R.layout.simple_spinner_item, noms);
                    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

                    spinnerEq1J1.setAdapter(adapter);
                    spinnerEq2J1.setAdapter(adapter);
                    if (is2v2) {
                        spinnerEq1J2.setAdapter(adapter);
                        spinnerEq2J2.setAdapter(adapter);
                    }
                });
            }
        });

        btnLancer.setOnClickListener(v -> {
            // Logique de lancement de votre jeu (ex: Intent vers GameActivity)
            Toast.makeText(requireContext(), "Lancement de la partie !", Toast.LENGTH_SHORT).show();
        });
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        dbExecutor.shutdown();
    }
}