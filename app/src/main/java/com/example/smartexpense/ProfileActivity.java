package com.example.smartexpense;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationBarView;

public class ProfileActivity extends AppCompatActivity {

    private Button btnWeb, btnEmail;
    private BottomNavigationView nav;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        btnWeb = (Button) findViewById(R.id.btnWeb);
        btnEmail = (Button) findViewById(R.id.btnEmail);
        nav = (BottomNavigationView) findViewById(R.id.bottomNavigation);
        nav.setSelectedItemId(R.id.nav_profile);

        btnWeb.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent webIntent = new Intent(Intent.ACTION_VIEW);
                webIntent.setData(Uri.parse("https://developer.android.com/guide"));
                startActivity(webIntent);
            }
        });

        btnEmail.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent emailIntent = new Intent(Intent.ACTION_SENDTO);
                emailIntent.setData(Uri.parse("mailto:support@smartexpense.local"));
                emailIntent.putExtra(Intent.EXTRA_SUBJECT, "Smart Expense AI Feedback");

                if (emailIntent.resolveActivity(getPackageManager()) != null) {
                    startActivity(emailIntent);
                } else {
                    Toast.makeText(ProfileActivity.this, "No email app found", Toast.LENGTH_SHORT).show();
                }
            }
        });

        nav.setOnItemSelectedListener(new NavigationBarView.OnItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                int id = item.getItemId();
                if (id == R.id.nav_home) {
                    startActivity(new Intent(ProfileActivity.this, MainActivity.class));
                    finish();
                    return true;
                } else if (id == R.id.nav_history) {
                    startActivity(new Intent(ProfileActivity.this, HistoryActivity.class));
                    finish();
                    return true;
                } else if (id == R.id.nav_profile) {
                    return true;
                }
                return false;
            }
        });
    }
}
