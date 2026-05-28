package com.example.smartexpense;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationBarView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MainActivity extends AppCompatActivity {

    private EditText inputText;
    private Spinner spinnerCategory;
    private Button btnSubmit;
    private BottomNavigationView bottomNavigation;
    private String selectedCategory = "";

    private static final String PREFS_NAME = "expense_data";
    private static final String KEY_HISTORY = "history";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // View Mapping
        inputText = (EditText) findViewById(R.id.inputText);
        spinnerCategory = (Spinner) findViewById(R.id.spinnerCategory);
        btnSubmit = (Button) findViewById(R.id.btnSubmit);
        bottomNavigation = (BottomNavigationView) findViewById(R.id.bottomNavigation);

        // Spinner Setup
        final String[] categories = {"Auto Category", "Food", "Transport", "Shopping", "Bills"};
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,
                android.R.layout.simple_spinner_item, categories);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerCategory.setAdapter(adapter);

        spinnerCategory.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position > 0) {
                    selectedCategory = categories[position];
                } else {
                    selectedCategory = "";
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // No action needed
            }
        });

        // Button Click Listener
        btnSubmit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String text = inputText.getText().toString().trim();
                if (!text.isEmpty()) {
                    String amount = findAmount(text);
                    String category = selectedCategory.isEmpty() ? findCategory(text) : selectedCategory;
                    String date = new SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault()).format(new Date());
                    String record = date + " | " + amount + " TZS | " + category + " | " + text;

                    saveRecord(record);
                    inputText.setText("");
                    spinnerCategory.setSelection(0);
                    Toast.makeText(MainActivity.this, "Saved: " + amount + " TZS, " + category, Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(MainActivity.this, "Please enter an expense", Toast.LENGTH_SHORT).show();
                }
            }
        });

        // Bottom Navigation Listener
        bottomNavigation.setSelectedItemId(R.id.nav_home);
        bottomNavigation.setOnItemSelectedListener(new NavigationBarView.OnItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                int itemId = item.getItemId();
                if (itemId == R.id.nav_home) {
                    return true;
                } else if (itemId == R.id.nav_history) {
                    Intent historyIntent = new Intent(MainActivity.this, HistoryActivity.class);
                    startActivity(historyIntent);
                    finish();
                    return true;
                } else if (itemId == R.id.nav_profile) {
                    Intent profileIntent = new Intent(MainActivity.this, ProfileActivity.class);
                    startActivity(profileIntent);
                    finish();
                    return true;
                }
                return false;
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == R.id.action_settings) {
            Toast.makeText(this, "Settings clicked", Toast.LENGTH_SHORT).show();
            return true;
        } else if (itemId == R.id.action_about) {
            Toast.makeText(this, "About clicked", Toast.LENGTH_SHORT).show();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private String findAmount(String text) {
        Pattern pattern = Pattern.compile("(\\d[\\d,]*)");
        Matcher matcher = pattern.matcher(text);
        if (matcher.find()) {
            return matcher.group(1).replace(",", "");
        }
        return "0";
    }

    private String findCategory(String text) {
        String lowerText = text.toLowerCase(Locale.ROOT);

        if (lowerText.contains("pizza") || lowerText.contains("food")
                || lowerText.contains("lunch") || lowerText.contains("dinner")
                || lowerText.contains("restaurant")) {
            return "Food";
        }

        if (lowerText.contains("bus") || lowerText.contains("taxi")
                || lowerText.contains("uber") || lowerText.contains("fuel")
                || lowerText.contains("transport")) {
            return "Transport";
        }

        if (lowerText.contains("shop") || lowerText.contains("clothes")
                || lowerText.contains("market") || lowerText.contains("shoes")) {
            return "Shopping";
        }

        if (lowerText.contains("bill") || lowerText.contains("rent")
                || lowerText.contains("water") || lowerText.contains("electric")) {
            return "Bills";
        }

        return "General";
    }

    private void saveRecord(String record) {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        String oldHistory = prefs.getString(KEY_HISTORY, "");
        String newHistory = record + "\n" + oldHistory;
        prefs.edit().putString(KEY_HISTORY, newHistory).apply();
    }
}
