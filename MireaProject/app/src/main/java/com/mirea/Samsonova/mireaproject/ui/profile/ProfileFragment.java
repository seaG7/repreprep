package com.mirea.Samsonova.mireaproject.ui.profile;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.mirea.Samsonova.mireaproject.R;

public class ProfileFragment extends Fragment {

    private EditText editProfileName, editProfileAge, editProfileHobby;
    private SharedPreferences sharedPreferences;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_profile, container, false);

        editProfileName = root.findViewById(R.id.editProfileName);
        editProfileAge = root.findViewById(R.id.editProfileAge);
        editProfileHobby = root.findViewById(R.id.editProfileHobby);
        Button btnSaveProfile = root.findViewById(R.id.btnSaveProfile);

        sharedPreferences = requireActivity().getSharedPreferences("mirea_project_profile", Context.MODE_PRIVATE);

        editProfileName.setText(sharedPreferences.getString("NAME", ""));
        editProfileAge.setText(sharedPreferences.getString("AGE", ""));
        editProfileHobby.setText(sharedPreferences.getString("HOBBY", ""));

        btnSaveProfile.setOnClickListener(v -> {
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putString("NAME", editProfileName.getText().toString());
            editor.putString("AGE", editProfileAge.getText().toString());
            editor.putString("HOBBY", editProfileHobby.getText().toString());
            editor.apply();

            Toast.makeText(requireContext(), "Профиль сохранен!", Toast.LENGTH_SHORT).show();
        });

        return root;
    }
}