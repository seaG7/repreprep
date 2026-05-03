package com.mirea.Samsonova.mireaproject.ui.audio;

import android.Manifest;
import android.content.pm.PackageManager;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.mirea.Samsonova.mireaproject.R;

import java.io.File;
import java.io.IOException;

public class AudioFragment extends Fragment {

    private String recordFilePath;
    private Button btnRecord;
    private Button btnPlay;
    private MediaRecorder recorder;
    private MediaPlayer player;
    private boolean isRecording = false;
    private boolean isPlaying = false;

    private final ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    toggleRecording();
                }
            });

    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_audio, container, false);

        btnRecord = root.findViewById(R.id.btnRecord);
        btnPlay = root.findViewById(R.id.btnPlay);

        recordFilePath = new File(requireActivity().getExternalFilesDir(Environment.DIRECTORY_MUSIC),
                "/audionote.3gp").getAbsolutePath();

        btnRecord.setOnClickListener(v -> {
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                toggleRecording();
            } else {
                requestPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO);
            }
        });

        btnPlay.setOnClickListener(v -> togglePlaying());

        return root;
    }

    private void toggleRecording() {
        if (isRecording) {
            btnRecord.setText("Начать запись");
            btnPlay.setEnabled(true);
            stopRecording();
        } else {
            btnRecord.setText("Остановить запись");
            btnPlay.setEnabled(false);
            startRecording();
        }
        isRecording = !isRecording;
    }

    private void togglePlaying() {
        if (isPlaying) {
            btnPlay.setText("Слушать заметку");
            btnRecord.setEnabled(true);
            stopPlaying();
        } else {
            btnPlay.setText("Остановить прослушивание");
            btnRecord.setEnabled(false);
            startPlaying();
        }
        isPlaying = !isPlaying;
    }

    private void startRecording() {
        recorder = new MediaRecorder();
        recorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        recorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
        recorder.setOutputFile(recordFilePath);
        recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
        try {
            recorder.prepare();
            recorder.start();
        } catch (IOException e) {
            Log.e("AudioFragment", "prepare() failed");
        }
    }

    private void stopRecording() {
        if (recorder != null) {
            recorder.stop();
            recorder.release();
            recorder = null;
        }
    }

    private void startPlaying() {
        player = new MediaPlayer();
        try {
            player.setDataSource(recordFilePath);
            player.prepare();
            player.start();

            player.setOnCompletionListener(mp -> {
                isPlaying = false;
                btnPlay.setText("Слушать заметку");
                btnRecord.setEnabled(true);
            });
        } catch (IOException e) {
            Log.e("AudioFragment", "prepare() failed");
        }
    }

    private void stopPlaying() {
        if (player != null) {
            player.release();
            player = null;
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        stopRecording();
        stopPlaying();
    }
}