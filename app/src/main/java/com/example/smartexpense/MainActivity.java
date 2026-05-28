package com.example.smartexpense;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.bottomnavigation.BottomNavigationView;

public class MainActivity extends AppCompatActivity {

    private EditText inputText;
    private Spinner spinnerCategory;
    private Button btnSubmit;
    private BottomNavigationView bottomNavigation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // View Binding
        inputText = findViewById(R.id.inputText);
        spinnerCategory = findViewById(R.id.spinnerCategory);
        btnSubmit = findViewById(R.id.btnSubmit);
        bottomNavigation = findViewById(R.id.bottomNavigation);

        // Spinner Setup
        String[] categories = {"Select Category Override", "Food", "Transport", "Shopping", "Bills"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, categories);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerCategory.setAdapter(adapter);

        // Button Click Listener
        btnSubmit.setOnClickListener(v -> {
            String text = inputText.getText().toString();
            Toast.makeText(MainActivity.this, text, Toast.LENGTH_SHORT).show();
        });

        // Bottom Navigation
        bottomNavigation.setSelectedItemId(R.id.nav_home);
        bottomNavigation.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.nav_home) {
                return true;
            } else if (itemId == R.id.nav_history) {
                startActivity(new Intent(getApplicationContext(), HistoryActivity.class));
                finish();
                return true;
            } else if (itemId == R.id.nav_profile) {
                startActivity(new Intent(getApplicationContext(), ProfileActivity.class));
                finish();
                return true;
            }
            return false;
        });
    }
}
