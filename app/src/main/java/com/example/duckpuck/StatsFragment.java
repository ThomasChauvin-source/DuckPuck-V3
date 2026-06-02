package com.example.duckpuck;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.fragment.app.Fragment;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class StatsFragment extends Fragment {

    private final ExecutorService dbExecutor = Executors.newSingleThreadExecutor();

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_stats, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        LinearLayout listeTopButeurs = view.findViewById(R.id.listeTopButeurs);
        LinearLayout listeTopParties = view.findViewById(R.id.listeTopParties);
        Spinner spinnerJoueur = view.findViewById(R.id.spinnerJoueurStats);
        TextView tvStatsPerso = view.findViewById(R.id.tvStatsPerso);
        TextView tvVide = view.findViewById(R.id.tvAucunJoueur);

        Context appContext = requireContext().getApplicationContext();

        dbExecutor.execute(() -> {
            AppDao dao = AppDatabase.getInstance(appContext).appDao();
            List<Joueur> joueurs = dao.getAllJoueurs();
            List<Joueur> topButeurs = dao.getTopButeurs();
            List<Joueur> topParties = dao.getTopParties();

            if (getActivity() != null) {
                requireActivity().runOnUiThread(() -> {
                    if (!isAdded()) return;

                    boolean vide = joueurs.isEmpty();
                    if (tvVide != null) tvVide.setVisibility(vide ? View.VISIBLE : View.GONE);
                    if (vide) return;

                    remplirClassement(listeTopButeurs, topButeurs, true);
                    remplirClassement(listeTopParties, topParties, false);
                    configurerStatsPerso(spinnerJoueur, tvStatsPerso, joueurs);
                });
            }
        });
    }

    private void remplirClassement(LinearLayout container, List<Joueur> joueurs, boolean classementButs) {
        container.removeAllViews();
        int rang = 1;
        for (Joueur joueur : joueurs) {
            TextView tv = new TextView(requireContext());
            tv.setPadding(16, 12, 16, 12);
            tv.setTextSize(16f);
            tv.setTextColor(0xFFFFFFFF);
            String valeur = classementButs
                    ? joueur.buts + " buts"
                    : joueur.nbr_parties + " parties";
            tv.setText(rang + ". " + joueur.nom + " - " + valeur);
            container.addView(tv);
            rang++;
        }
    }

    private void configurerStatsPerso(Spinner spinner, TextView tvStatsPerso, List<Joueur> joueurs) {
        List<String> noms = new ArrayList<>();
        for (Joueur joueur : joueurs) {
            noms.add(joueur.nom);
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_spinner_item,
                noms
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);

        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                afficherStatsJoueur(tvStatsPerso, joueurs.get(position));
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        afficherStatsJoueur(tvStatsPerso, joueurs.get(0));
    }

    private void afficherStatsJoueur(TextView tvStatsPerso, Joueur joueur) {
        int winRate = joueur.nbr_parties > 0
                ? Math.round(joueur.win * 100f / joueur.nbr_parties)
                : 0;

        tvStatsPerso.setText(joueur.nom + "\n"
                + "Parties : " + joueur.nbr_parties + "\n"
                + "Victoires : " + joueur.win + " (" + winRate + " %)\n"
                + "Buts : " + joueur.buts);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        dbExecutor.shutdown();
    }
}
