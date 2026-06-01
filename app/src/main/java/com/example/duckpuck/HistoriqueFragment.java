package com.example.duckpuck;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.fragment.app.Fragment;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class HistoriqueFragment extends Fragment {

    private final ExecutorService dbExecutor = Executors.newSingleThreadExecutor();

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_historique, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Le layout doit contenir un LinearLayout avec id "listeParties"
        LinearLayout liste = view.findViewById(R.id.listeParties);
        TextView     tvVide = view.findViewById(R.id.tvAucunePartie);

        dbExecutor.execute(() -> {
            AppDao dao = AppDatabase.getInstance(requireContext()).appDao();
            List<PartieAvecJoueurs> historique = dao.getHistoriqueParties();

            requireActivity().runOnUiThread(() -> {
                if (historique.isEmpty()) {
                    if (tvVide != null) tvVide.setVisibility(View.VISIBLE);
                    return;
                }
                if (tvVide != null) tvVide.setVisibility(View.GONE);

                for (PartieAvecJoueurs paj : historique) {
                    Partie p = paj.partie;

                    // Construire dynamiquement une ligne par partie
                    TextView tv = new TextView(requireContext());
                    tv.setPadding(16, 24, 16, 24);
                    tv.setTextSize(16f);
                    tv.setTextColor(0xFFFFFFFF);

                    String equipeGagnante = p.score_equipe1 > p.score_equipe2
                            ? "🔴 Rouge" : "🔵 Bleu";

                    StringBuilder sb = new StringBuilder();
                    sb.append("Partie #").append(p.id_partie).append("\n");
                    sb.append("Rouge ").append(p.score_equipe1)
                            .append("  —  ").append(p.score_equipe2).append(" Bleu\n");
                    sb.append("Vainqueur : ").append(equipeGagnante);

                    // Ajouter les noms des joueurs si disponibles
                    if (paj.participations != null && !paj.participations.isEmpty()) {
                        sb.append("\n");
                        for (Participer part : paj.participations) {
                            String nom = dao.getNomJoueur(part.id_joueur);
                            if (nom != null) {
                                String equipe = part.equipe == 1 ? "Rouge" : "Bleu";
                                sb.append("  • ").append(nom)
                                        .append(" (").append(equipe).append(")");
                                if (part.a_gagne) sb.append(" ✓");
                                sb.append("\n");
                            }
                        }
                    }

                    tv.setText(sb.toString());

                    // Séparateur visuel
                    View separator = new View(requireContext());
                    separator.setLayoutParams(new LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT, 1));
                    separator.setBackgroundColor(0x44FFFFFF);

                    liste.addView(tv);
                    liste.addView(separator);
                }
            });
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        dbExecutor.shutdown();
    }
}