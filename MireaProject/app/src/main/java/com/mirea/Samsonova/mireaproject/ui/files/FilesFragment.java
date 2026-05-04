package com.mirea.Samsonova.mireaproject.ui.files;

import android.app.AlertDialog;
import android.content.Context;
import android.os.Bundle;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.mirea.Samsonova.mireaproject.R;

import java.io.FileInputStream;
import java.io.FileOutputStream;

public class FilesFragment extends Fragment {

    private TextView tvDecryptedContent;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_files, container, false);

        tvDecryptedContent = root.findViewById(R.id.tvDecryptedContent);
        FloatingActionButton fabAddFile = root.findViewById(R.id.fabAddFile);

        fabAddFile.setOnClickListener(v -> showCreateFileDialog());

        return root;
    }

    private void showCreateFileDialog() {
        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_new_file, null);
        EditText editFileName = dialogView.findViewById(R.id.dialogFileName);
        EditText editFileContent = dialogView.findViewById(R.id.dialogFileContent);

        new AlertDialog.Builder(requireContext())
                .setTitle("Новая крипто-запись")
                .setView(dialogView)
                .setPositiveButton("Зашифровать и сохранить", (dialog, which) -> {
                    String fileName = editFileName.getText().toString();
                    String content = editFileContent.getText().toString();

                    if (!fileName.isEmpty() && !content.isEmpty()) {
                        saveEncryptedFile(fileName, content);
                    } else {
                        Toast.makeText(requireContext(), "Заполните все поля", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Отмена", null)
                .show();
    }

    private void saveEncryptedFile(String fileName, String content) {
        try {
            String encryptedBase64 = Base64.encodeToString(content.getBytes(), Base64.DEFAULT);

            FileOutputStream outputStream = requireActivity().openFileOutput(fileName, Context.MODE_PRIVATE);
            outputStream.write(encryptedBase64.getBytes());
            outputStream.close();

            Toast.makeText(requireContext(), "Файл " + fileName + " сохранен!", Toast.LENGTH_SHORT).show();

            readAndDecryptFile(fileName);

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(requireContext(), "Ошибка при сохранении", Toast.LENGTH_SHORT).show();
        }
    }

    private void readAndDecryptFile(String fileName) {
        try {
            FileInputStream fin = requireActivity().openFileInput(fileName);
            byte[] bytes = new byte[fin.available()];
            fin.read(bytes);
            fin.close();

            String encryptedText = new String(bytes);

            byte[] decryptedBytes = Base64.decode(encryptedText, Base64.DEFAULT);
            String decryptedText = new String(decryptedBytes);

            tvDecryptedContent.setText("Файл: " + fileName + "\n\nЗашифрованный текст в файле:\n"
                    + encryptedText + "\nРасшифрованный текст:\n" + decryptedText);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}