package com.example.duckpuck;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class HistoriqueFragment extends Fragment {

    private final ExecutorService dbExecutor = Executors.newSingleThreadExecutor();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_historique, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        LinearLayout listeParties = view.findViewById(R.id.listeParties);
        TextView tvAucune = view.findViewById(R.id.tvAucunePartie);

        Context appContext = requireContext().getApplicationContext();

        dbExecutor.execute(() -> {
            AppDao dao = AppDatabase.getInstance(appContext).appDao();
            List<PartieAvecJoueurs> historique = dao.getHistoriqueParties();
            List<LigneHistorique> lignes = new ArrayList<>();

            if (historique != null) {
                for (PartieAvecJoueurs item : historique) {
                    lignes.add(new LigneHistorique(
                            item.partie.id_partie,
                            formatPartie(dao, item),
                            item.partie.replay_data != null && !item.partie.replay_data.trim().isEmpty()
                    ));
                }
            }

            if (isAdded() && getActivity() != null) {
                requireActivity().runOnUiThread(() -> {
                    if (!isAdded()) return;

                    if (lignes.isEmpty()) {
                        if (tvAucune != null) tvAucune.setVisibility(View.VISIBLE);
                        return;
                    }
                    if (tvAucune != null) tvAucune.setVisibility(View.GONE);

                    listeParties.removeAllViews();

                    for (LigneHistorique ligne : lignes) {
                        TextView tv = new TextView(requireContext());
                        tv.setPadding(16, 20, 16, 20);
                        tv.setTextSize(16f);
                        tv.setTextColor(0xFFFFFFFF);
                        tv.setText(ligne.texte);

                        listeParties.addView(tv);

                        if (ligne.hasReplay) {
                            Button btnReplay = new Button(requireContext());
                            btnReplay.setText("Voir les replays");
                            btnReplay.setOnClickListener(v -> {
                                Intent intent = new Intent(requireContext(), ReplayActivity.class);
                                intent.putExtra(ReplayActivity.EXTRA_PARTIE_ID, ligne.partieId);
                                startActivity(intent);
                            });
                            listeParties.addView(btnReplay);
                        }

                        View separator = new View(requireContext());
                        separator.setLayoutParams(new LinearLayout.LayoutParams(
                                LinearLayout.LayoutParams.MATCH_PARENT, 1));
                        separator.setBackgroundColor(0x33FFFFFF);

                        listeParties.addView(separator);
                    }
                });
            }
        });
    }

    private String formatPartie(AppDao dao, PartieAvecJoueurs item) {
        Partie partie = item.partie;
        StringBuilder equipeRouge = new StringBuilder();
        StringBuilder equipeBleue = new StringBuilder();

        if (item.participations != null) {
            for (Participer participation : item.participations) {
                String nom = dao.getNomJoueur(participation.id_joueur);
                String ligne = nom + " (" + participation.buts + " buts)";
                if (participation.equipe == 1) {
                    appendJoueur(equipeRouge, ligne);
                } else {
                    appendJoueur(equipeBleue, ligne);
                }
            }
        }

        String statut = partie.arretee ? "Arretee en cours" : "Terminee";

        return "Match N " + partie.id_partie + " - " + statut + "\n"
                + "Score : " + partie.score_equipe1 + " - " + partie.score_equipe2 + "\n"
                + "Duree : " + partie.temps + " secondes\n"
                + "Rouge : " + valueOrDash(equipeRouge) + "\n"
                + "Bleu : " + valueOrDash(equipeBleue);
    }

    private void appendJoueur(StringBuilder builder, String joueur) {
        if (builder.length() > 0) {
            builder.append(", ");
        }
        builder.append(joueur);
    }

    private String valueOrDash(StringBuilder builder) {
        return builder.length() == 0 ? "-" : builder.toString();
    }

    private static class LigneHistorique {
        final int partieId;
        final String texte;
        final boolean hasReplay;

        LigneHistorique(int partieId, String texte, boolean hasReplay) {
            this.partieId = partieId;
            this.texte = texte;
            this.hasReplay = hasReplay;
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        dbExecutor.shutdown();
    }
}
