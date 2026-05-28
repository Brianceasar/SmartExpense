package com.example.smartexpense;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationBarView;

import java.util.ArrayList;

public class HistoryActivity extends AppCompatActivity {

    private BottomNavigationView nav;
    private ListView listHistory;
    private TextView emptyText;

    private static final String PREFS_NAME = "expense_data";
    private static final String KEY_HISTORY = "history";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);

        nav = (BottomNavigationView) findViewById(R.id.bottomNavigation);
        listHistory = (ListView) findViewById(R.id.listHistory);
        emptyText = (TextView) findViewById(R.id.emptyText);

        loadHistory();

        nav.setSelectedItemId(R.id.nav_history);

        nav.setOnItemSelectedListener(new NavigationBarView.OnItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                int id = item.getItemId();
                if (id == R.id.nav_home) {
                    startActivity(new Intent(HistoryActivity.this, MainActivity.class));
                    finish();
                    return true;
                } else if (id == R.id.nav_history) {
                    return true;
                } else if (id == R.id.nav_profile) {
                    startActivity(new Intent(HistoryActivity.this, ProfileActivity.class));
                    finish();
                    return true;
                }
                return false;
            }
        });
    }

    private void loadHistory() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        String savedHistory = prefs.getString(KEY_HISTORY, "");
        ArrayList<String> records = new ArrayList<String>();

        if (!savedHistory.trim().isEmpty()) {
            String[] lines = savedHistory.split("\\n");
            for (String line : lines) {
                if (!line.trim().isEmpty()) {
                    records.add(line);
                }
            }
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<String>(
                this,
                android.R.layout.simple_list_item_1,
                records
        );
        listHistory.setAdapter(adapter);
        listHistory.setEmptyView(emptyText);
    }
}
