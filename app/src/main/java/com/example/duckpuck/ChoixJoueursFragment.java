package com.example.duckpuck;

import android.content.Context;
import android.content.Intent;
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
    private List<Joueur> listeTousLesJoueurs = new ArrayList<>();

    private Spinner spinnerEq1J1, spinnerEq1J2, spinnerEq2J1, spinnerEq2J2;
    private int modeJeu;
    private boolean is2v2;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_choix_joueurs, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Récupérer le mode envoyé par le premier fragment
        if (getArguments() != null) {
            modeJeu = getArguments().getInt("game_mode", GameView.MODE_2P);
        }
        is2v2 = (modeJeu == GameView.MODE_4P);

        // Initialisation
        spinnerEq1J1 = view.findViewById(R.id.spinnerEq1Joueur1);
        spinnerEq1J2 = view.findViewById(R.id.spinnerEq1Joueur2);
        spinnerEq2J1 = view.findViewById(R.id.spinnerEq2Joueur1);
        spinnerEq2J2 = view.findViewById(R.id.spinnerEq2Joueur2);
        Button btnLancer = view.findViewById(R.id.btnLancerMatch);

        if (is2v2) {
            spinnerEq1J2.setVisibility(View.VISIBLE);
            spinnerEq2J2.setVisibility(View.VISIBLE);
        }

        chargerJoueursDepuisBDD();

        btnLancer.setOnClickListener(v -> validerEtLancerMatch());
    }

    private void chargerJoueursDepuisBDD() {
        Context appContext = requireContext().getApplicationContext();
        dbExecutor.execute(() -> {
            AppDao dao = AppDatabase.getInstance(appContext).appDao();
            listeTousLesJoueurs = dao.getAllJoueurs();

            if (getActivity() != null) {
                requireActivity().runOnUiThread(() -> {
                    int minimumRequis = is2v2 ? 4 : 2;
                    if (listeTousLesJoueurs.size() < minimumRequis) {
                        Toast.makeText(requireContext(), "Il faut au moins " + minimumRequis + " joueurs créés !", Toast.LENGTH_LONG).show();
                        getParentFragmentManager().popBackStack(); // Retourne automatiquement en arrière
                        return;
                    }

                    List<String> listeNoms = new ArrayList<>();
                    for (Joueur j : listeTousLesJoueurs) {
                        listeNoms.add(j.nom);
                    }

                    ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_item, listeNoms);
                    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

                    spinnerEq1J1.setAdapter(adapter);
                    spinnerEq2J1.setAdapter(adapter);

                    if (is2v2) {
                        spinnerEq1J2.setAdapter(adapter);
                        spinnerEq2J2.setAdapter(adapter);
                        if (listeTousLesJoueurs.size() >= 4) {
                            spinnerEq1J2.setSelection(1);
                            spinnerEq2J1.setSelection(2);
                            spinnerEq2J2.setSelection(3);
                        }
                    } else {
                        if (listeTousLesJoueurs.size() >= 2) spinnerEq2J1.setSelection(1);
                    }
                });
            }
        });
    }

    private void validerEtLancerMatch() {
        if (listeTousLesJoueurs.isEmpty()) return;

        int idEq1J1 = listeTousLesJoueurs.get(spinnerEq1J1.getSelectedItemPosition()).id_joueur;
        int idEq2J1 = listeTousLesJoueurs.get(spinnerEq2J1.getSelectedItemPosition()).id_joueur;
        int[] equipe1Ids;
        int[] equipe2Ids;

        if (is2v2) {
            int idEq1J2 = listeTousLesJoueurs.get(spinnerEq1J2.getSelectedItemPosition()).id_joueur;
            int idEq2J2 = listeTousLesJoueurs.get(spinnerEq2J2.getSelectedItemPosition()).id_joueur;

            if (idEq1J1 == idEq1J2 || idEq1J1 == idEq2J1 || idEq1J1 == idEq2J2 ||
                    idEq1J2 == idEq2J1 || idEq1J2 == idEq2J2 || idEq2J1 == idEq2J2) {
                Toast.makeText(requireContext(), "Erreur : Doublon de joueur détecté !", Toast.LENGTH_SHORT).show();
                return;
            }
            equipe1Ids = new int[]{idEq1J1, idEq1J2};
            equipe2Ids = new int[]{idEq2J1, idEq2J2};
        } else {
            if (idEq1J1 == idEq2J1) {
                Toast.makeText(requireContext(), "Un joueur ne peut pas jouer contre lui-même !", Toast.LENGTH_SHORT).show();
                return;
            }
            equipe1Ids = new int[]{idEq1J1};
            equipe2Ids = new int[]{idEq2J1};
        }

        Intent intent = new Intent(requireContext(), GameActivity.class);
        intent.putExtra(GameActivity.EXTRA_MODE, modeJeu);
        intent.putExtra(GameActivity.EXTRA_EQUIPE_1_IDS, equipe1Ids);
        intent.putExtra(GameActivity.EXTRA_EQUIPE_2_IDS, equipe2Ids);
        startActivity(intent);

        // Optionnel : On quitte l'écran de sélection pour revenir directement aux cartes une fois le match fini
        getParentFragmentManager().popBackStack();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        dbExecutor.shutdown();
    }
}