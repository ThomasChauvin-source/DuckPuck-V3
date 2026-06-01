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

        // Le layout doit contenir un LinearLayout avec id "listeStats"
        LinearLayout liste  = view.findViewById(R.id.listeStats);
        TextView     tvVide = view.findViewById(R.id.tvAucunJoueur);

        dbExecutor.execute(() -> {
            AppDao dao = AppDatabase.getInstance(requireContext()).appDao();
            List<Joueur> joueurs = dao.getAllJoueurs();

            requireActivity().runOnUiThread(() -> {
                if (joueurs.isEmpty()) {
                    if (tvVide != null) tvVide.setVisibility(View.VISIBLE);
                    return;
                }
                if (tvVide != null) tvVide.setVisibility(View.GONE);

                for (Joueur j : joueurs) {
                    TextView tv = new TextView(requireContext());
                    tv.setPadding(16, 24, 16, 24);
                    tv.setTextSize(16f);
                    tv.setTextColor(0xFFFFFFFF);

                    int winRate = j.nbr_parties > 0
                            ? Math.round(j.win * 100f / j.nbr_parties) : 0;

                    String texte = "👤 " + j.nom + "\n"
                            + "  Parties  : " + j.nbr_parties + "\n"
                            + "  Victoires: " + j.win
                            + "  (" + winRate + " %)\n"
                            + "  Buts     : " + j.buts;

                    tv.setText(texte);

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