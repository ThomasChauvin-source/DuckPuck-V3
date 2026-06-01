package com.example.duckpuck;

import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.fragment.app.Fragment;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AjouterJoueurFragment extends Fragment {

    private final ExecutorService dbExecutor = Executors.newSingleThreadExecutor();

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_ajouter_joueur, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        EditText editNom  = view.findViewById(R.id.editNomJoueur);
        Button   btnAjout = view.findViewById(R.id.btnConfirmerAjout);

        btnAjout.setOnClickListener(v -> {
            String nom = editNom.getText().toString().trim();

            if (TextUtils.isEmpty(nom)) {
                Toast.makeText(requireContext(), "Veuillez entrer un nom.", Toast.LENGTH_SHORT).show();
                return;
            }

            // Sécurité : On capture le contexte de l'application AVANT d'entrer dans le thread alternatif
            Context appContext = requireContext().getApplicationContext();

            dbExecutor.execute(() -> {
                AppDao dao = AppDatabase.getInstance(appContext).appDao();

                // Vérifier si un joueur avec ce nom existe déjà
                boolean doublon = dao.getAllJoueurs().stream()
                        .anyMatch(j -> j.nom.equalsIgnoreCase(nom));

                if (doublon) {
                    if (getActivity() != null) {
                        requireActivity().runOnUiThread(() -> {
                            Toast.makeText(appContext,
                                    "Un joueur nommé \"" + nom + "\" existe déjà.",
                                    Toast.LENGTH_SHORT).show();
                        });
                    }
                } else {
                    // On est déjà sur le thread background, on insère directement !
                    Joueur joueur = new Joueur();
                    joueur.nom        = nom;
                    joueur.buts       = 0;
                    joueur.win        = 0;
                    joueur.nbr_parties = 0;
                    dao.insertJoueur(joueur);

                    if (getActivity() != null) {
                        requireActivity().runOnUiThread(() -> {
                            Toast.makeText(appContext,
                                    "Joueur \"" + nom + "\" ajouté !",
                                    Toast.LENGTH_SHORT).show();
                            editNom.setText("");
                        });
                    }
                }
            });
        });
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        // Corrigé : On détruit l'exécuteur dans onDestroy pour éviter les RejectedExecutionException
        dbExecutor.shutdown();
    }
}