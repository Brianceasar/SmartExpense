package com.example.smartexpense;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationBarView;

public class ProfileActivity extends AppCompatActivity {

    private Button btnLogout;
    private TextView profileName, profileEmail, profileAvatar, personalInfoName, personalInfoEmail;
    private View personalInfoRow, personalInfoDetails;
    private BottomNavigationView nav;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (!AuthManager.isSignedIn(this)) {
            openLogin();
            return;
        }

        setContentView(R.layout.activity_profile);
        TopMenu.attach(this);

        profileName = (TextView) findViewById(R.id.profileName);
        profileEmail = (TextView) findViewById(R.id.profileEmail);
        profileAvatar = (TextView) findViewById(R.id.profileAvatar);
        personalInfoName = (TextView) findViewById(R.id.personalInfoName);
        personalInfoEmail = (TextView) findViewById(R.id.personalInfoEmail);
        personalInfoRow = findViewById(R.id.personalInfoRow);
        personalInfoDetails = findViewById(R.id.personalInfoDetails);
        btnLogout = (Button) findViewById(R.id.btnLogout);
        nav = (BottomNavigationView) findViewById(R.id.bottomNavigation);
        nav.setSelectedItemId(R.id.nav_profile);

        String name = AuthManager.getUserName(this);
        String email = AuthManager.getUserEmail(this);
        profileName.setText(name);
        profileEmail.setText(email);
        personalInfoName.setText(name);
        personalInfoEmail.setText(email);
        profileAvatar.setText(getInitial(name, email));

        personalInfoRow.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                personalInfoDetails.setVisibility(
                        personalInfoDetails.getVisibility() == View.VISIBLE ? View.GONE : View.VISIBLE
                );
            }
        });

        btnLogout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AuthManager.logout(ProfileActivity.this);
                openLogin();
            }
        });

        nav.setOnItemSelectedListener(new NavigationBarView.OnItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                int id = item.getItemId();
                if (id == R.id.nav_home) {
                    startActivity(new Intent(ProfileActivity.this, DashboardActivity.class));
                    finish();
                    return true;
                } else if (id == R.id.nav_add) {
                    startActivity(new Intent(ProfileActivity.this, MainActivity.class));
                    finish();
                    return true;
                } else if (id == R.id.nav_history) {
                    startActivity(new Intent(ProfileActivity.this, HistoryActivity.class));
                    finish();
                    return true;
                } else if (id == R.id.nav_insights) {
                    startActivity(new Intent(ProfileActivity.this, InsightsActivity.class));
                    finish();
                    return true;
                } else if (id == R.id.nav_profile) {
                    return true;
                }
                return false;
            }
        });
    }

    private String getInitial(String name, String email) {
        String source = name == null || name.trim().isEmpty() ? email : name;
        if (source == null || source.trim().isEmpty()) {
            return "U";
        }

        return source.trim().substring(0, 1).toUpperCase();
    }

    private void openLogin() {
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
