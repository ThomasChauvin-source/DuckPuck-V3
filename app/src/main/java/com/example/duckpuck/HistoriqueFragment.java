package com.example.duckpuck;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
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
            // Appel à votre base de données
            AppDao dao = AppDatabase.getInstance(appContext).appDao();

            // ATTENTION : Si le nom de votre méthode dans AppDao est différent (ex: getAllParties),
            // remplacez simplement le nom de la méthode ci-dessous.
            List<PartieAvecJoueurs> historique = dao.getHistoriqueParties();

            if (isAdded() && getActivity() != null) {
                requireActivity().runOnUiThread(() -> {
                    if (!isAdded()) return;

                    if (historique == null || historique.isEmpty()) {
                        if (tvAucune != null) tvAucune.setVisibility(View.VISIBLE);
                        return;
                    }
                    if (tvAucune != null) tvAucune.setVisibility(View.GONE);

                    listeParties.removeAllViews();

                    // On parcourt votre liste de PartieAvecJoueurs
                    for (PartieAvecJoueurs item : historique) {
                        // On extrait l'objet Partie présent à l'intérieur (votre classe Partie.java)
                        Partie p = item.partie;

                        TextView tv = new TextView(requireContext());
                        tv.setPadding(16, 20, 16, 20);
                        tv.setTextSize(16f);
                        tv.setTextColor(0xFFFFFFFF);

                        // Utilisation de vos vraies variables : id_partie, score_equipe1, score_equipe2, temps
                        String texte = "🎮 Match N° " + p.id_partie + "\n"
                                + "  Score : " + p.score_equipe1 + " - " + p.score_equipe2 + "\n"
                                + "  Durée : " + p.temps + " secondes";

                        // Optionnel : Si vous voulez afficher le nombre de joueurs ayant participé à cette partie
                        if (item.participations != null) {
                            texte += "\n  Nombre de joueurs : " + item.participations.size();
                        }

                        tv.setText(texte);

                        View separator = new View(requireContext());
                        separator.setLayoutParams(new LinearLayout.LayoutParams(
                                LinearLayout.LayoutParams.MATCH_PARENT, 1));
                        separator.setBackgroundColor(0x33FFFFFF);

                        listeParties.addView(tv);
                        listeParties.addView(separator);
                    }
                });
            }
        });
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        dbExecutor.shutdown();
    }
}