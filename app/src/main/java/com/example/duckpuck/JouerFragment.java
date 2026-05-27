package com.example.duckpuck;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.fragment.app.Fragment;

public class JouerFragment extends Fragment {

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_jouer, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        view.findViewById(R.id.carte1v1).setOnClickListener(v -> lancerPartie(GameView.MODE_2P));
        view.findViewById(R.id.carte2v2).setOnClickListener(v -> lancerPartie(GameView.MODE_4P));
    }

    private void lancerPartie(int mode) {
        Intent intent = new Intent(requireActivity(), GameActivity.class);
        intent.putExtra(GameActivity.EXTRA_MODE, mode);
        startActivity(intent);
    }
}