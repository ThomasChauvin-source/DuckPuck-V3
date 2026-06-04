package com.example.duckpuck;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ChoixJoueursFragment extends Fragment {

    private final ExecutorService dbExecutor = Executors.newSingleThreadExecutor();
    private final List<Joueur> joueurs = new ArrayList<>();
    private Spinner spinnerEq1J1, spinnerEq1J2, spinnerEq2J1, spinnerEq2J2;
    private CheckBox checkReplay;
    private boolean joueursCharges = false;
    private boolean is2v2 = false;

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
        checkReplay = view.findViewById(R.id.checkReplay);
        View btnLancer = view.findViewById(R.id.btnLancerMatch);

        is2v2 = getArguments() != null && getArguments().getBoolean("is2v2", false);

        if (is2v2) {
            //TextView labelEq1J2 = view.findViewById(R.id.labelEq1Joueur2);
            //TextView labelEq2J2 = view.findViewById(R.id.labelEq2Joueur2);
            //labelEq1J2.setVisibility(View.VISIBLE);
            //labelEq2J2.setVisibility(View.VISIBLE);
            spinnerEq1J2.setVisibility(View.VISIBLE);
            spinnerEq2J2.setVisibility(View.VISIBLE);
        }

        Context appContext = requireContext().getApplicationContext();

        dbExecutor.execute(() -> {
            AppDao dao = AppDatabase.getInstance(appContext).appDao();
            List<Joueur> listeJoueurs = dao.getAllJoueurs();
            List<String> noms = new ArrayList<>();

            for (Joueur joueur : listeJoueurs) {
                noms.add(joueur.nom);
            }

            if (isAdded() && getActivity() != null) {
                requireActivity().runOnUiThread(() -> {
                    if (!isAdded()) return;

                    joueurs.clear();
                    joueurs.addAll(listeJoueurs);
                    joueursCharges = true;

                    if (joueurs.size() < getNombreJoueursNecessaires()) {
                        String message = is2v2
                                ? "Ajoutez au moins 4 joueurs pour lancer un 2v2."
                                : "Ajoutez au moins 2 joueurs pour lancer un 1v1.";
                        Toast.makeText(appContext, message, Toast.LENGTH_LONG).show();
                    }

                    List<String> choix = new ArrayList<>();
                    choix.add("Selectionner un joueur");
                    choix.addAll(noms);

                    ArrayAdapter<String> adapter = new ArrayAdapter<>(
                            requireContext(),
                            android.R.layout.simple_spinner_item,
                            choix
                    );
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

        btnLancer.setOnClickListener(v -> lancerPartie());
    }

    private void lancerPartie() {
        if (!joueursCharges) {
            Toast.makeText(requireContext(), "Chargement des joueurs en cours...", Toast.LENGTH_SHORT).show();
            return;
        }

        if (joueurs.size() < getNombreJoueursNecessaires()) {
            String message = is2v2
                    ? "Il faut 4 joueurs differents pour lancer un 2v2."
                    : "Il faut 2 joueurs differents pour lancer un 1v1.";
            Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
            return;
        }

        int eq1J1 = getSelectedJoueurId(spinnerEq1J1);
        int eq2J1 = getSelectedJoueurId(spinnerEq2J1);
        int eq1J2 = is2v2 ? getSelectedJoueurId(spinnerEq1J2) : -1;
        int eq2J2 = is2v2 ? getSelectedJoueurId(spinnerEq2J2) : -1;

        if (eq1J1 == -1 || eq2J1 == -1 || (is2v2 && (eq1J2 == -1 || eq2J2 == -1))) {
            Toast.makeText(requireContext(), "Selectionnez tous les joueurs avant de commencer.", Toast.LENGTH_SHORT).show();
            return;
        }

        Set<Integer> idsSelectionnes = new HashSet<>();
        idsSelectionnes.add(eq1J1);
        idsSelectionnes.add(eq2J1);
        if (is2v2) {
            idsSelectionnes.add(eq1J2);
            idsSelectionnes.add(eq2J2);
        }

        if (idsSelectionnes.size() != getNombreJoueursNecessaires()) {
            Toast.makeText(requireContext(), "Un joueur ne peut pas etre selectionne deux fois.", Toast.LENGTH_SHORT).show();
            return;
        }

        int[] equipe1Ids = is2v2 ? new int[]{eq1J1, eq1J2} : new int[]{eq1J1};
        int[] equipe2Ids = is2v2 ? new int[]{eq2J1, eq2J2} : new int[]{eq2J1};

        Intent intent = new Intent(requireContext(), GameActivity.class);
        intent.putExtra(GameActivity.EXTRA_MODE, is2v2 ? GameView.MODE_4P : GameView.MODE_2P);
        intent.putExtra(GameActivity.EXTRA_REPLAY_ENABLED, checkReplay.isChecked());
        intent.putExtra(GameActivity.EXTRA_EQUIPE_1_IDS, equipe1Ids);
        intent.putExtra(GameActivity.EXTRA_EQUIPE_2_IDS, equipe2Ids);
        startActivity(intent);
    }

    private int getNombreJoueursNecessaires() {
        return is2v2 ? 4 : 2;
    }

    private int getSelectedJoueurId(Spinner spinner) {
        int position = spinner.getSelectedItemPosition();
        if (position <= 0 || position > joueurs.size()) {
            return -1;
        }
        return joueurs.get(position - 1).id_joueur;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        dbExecutor.shutdown();
    }
}
