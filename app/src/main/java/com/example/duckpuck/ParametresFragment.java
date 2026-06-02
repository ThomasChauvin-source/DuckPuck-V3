package com.example.duckpuck;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.fragment.app.Fragment;

public class ParametresFragment extends Fragment {

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_parametres, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        TextView tvMusique = view.findViewById(R.id.tvVolumeMusique);
        TextView tvBruitage = view.findViewById(R.id.tvVolumeBruitage);
        SeekBar seekMusique = view.findViewById(R.id.seekVolumeMusique);
        SeekBar seekBruitage = view.findViewById(R.id.seekVolumeBruitage);

        int musique = Math.round(AudioSettings.getMusicVolume(requireContext()) * 100f);
        int bruitage = Math.round(AudioSettings.getSfxVolume(requireContext()) * 100f);

        seekMusique.setProgress(musique);
        seekBruitage.setProgress(bruitage);
        updateLabel(tvMusique, "Musique", musique);
        updateLabel(tvBruitage, "Bruitages", bruitage);

        seekMusique.setOnSeekBarChangeListener(new SimpleSeekBarListener(progress -> {
            AudioSettings.setMusicVolume(requireContext(), progress / 100f);
            updateLabel(tvMusique, "Musique", progress);
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).applyMusicVolume();
            }
        }));

        seekBruitage.setOnSeekBarChangeListener(new SimpleSeekBarListener(progress -> {
            AudioSettings.setSfxVolume(requireContext(), progress / 100f);
            updateLabel(tvBruitage, "Bruitages", progress);
        }));
    }

    private void updateLabel(TextView textView, String label, int progress) {
        textView.setText(label + " : " + progress + " %");
    }

    private static class SimpleSeekBarListener implements SeekBar.OnSeekBarChangeListener {
        private final ProgressCallback callback;

        SimpleSeekBarListener(ProgressCallback callback) {
            this.callback = callback;
        }

        @Override
        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
            callback.onProgress(progress);
        }

        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {
        }

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {
        }
    }

    private interface ProgressCallback {
        void onProgress(int progress);
    }
}
