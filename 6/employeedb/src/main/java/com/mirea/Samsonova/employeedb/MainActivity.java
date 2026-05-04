package com.mirea.Samsonova.employeedb;

import android.os.Bundle;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;

import java.util.List;

import com.mirea.Samsonova.employeedb.databinding.ActivityMainBinding;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;
    private static final String TAG = "SUPERHERO_DB";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        AppDatabase db = App.getInstance().getDatabase();
        SuperheroDao superheroDao = db.superheroDao();

        Superhero hero = new Superhero();
        hero.name = "Spider-Man";
        hero.superpower = "Паутина и чутье";

        superheroDao.insert(hero);

        List<Superhero> heroes = superheroDao.getAll();

        Superhero savedHero = heroes.get(heroes.size() - 1);

        savedHero.superpower = "Усиленная паутина";
        superheroDao.update(savedHero);

        Log.d(TAG, savedHero.name + " - " + savedHero.superpower);
        binding.tvDbLog.setText("Герой: " + savedHero.name + "\nСила: " + savedHero.superpower);
    }
}