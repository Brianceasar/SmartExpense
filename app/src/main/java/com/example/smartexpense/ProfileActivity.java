package com.example.smartexpense;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationBarView;

public class ProfileActivity extends AppCompatActivity {

    private Button btnLogout;
    private TextView profileName, profileEmail, profileAvatar, personalInfoName, personalInfoEmail, profileBudgetSummary;
    private View personalInfoRow, personalInfoDetails, budgetRow;
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
        profileBudgetSummary = (TextView) findViewById(R.id.profileBudgetSummary);
        personalInfoRow = findViewById(R.id.personalInfoRow);
        personalInfoDetails = findViewById(R.id.personalInfoDetails);
        budgetRow = findViewById(R.id.budgetRow);
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
        updateBudgetSummary();

        personalInfoRow.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                personalInfoDetails.setVisibility(
                        personalInfoDetails.getVisibility() == View.VISIBLE ? View.GONE : View.VISIBLE
                );
            }
        });

        budgetRow.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showBudgetDialog();
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

    private void updateBudgetSummary() {
        profileBudgetSummary.setText(BudgetManager.getBudgetSummary(this));
    }

    private void showBudgetDialog() {
        final LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        int padding = dp(20);
        content.setPadding(padding, dp(10), padding, 0);

        final EditText amountInput = new EditText(this);
        amountInput.setHint(R.string.profile_budget_amount_hint);
        amountInput.setInputType(InputType.TYPE_CLASS_NUMBER);
        amountInput.setSingleLine(true);
        amountInput.setText(String.valueOf(BudgetManager.getBudgetAmount(this)));
        content.addView(amountInput, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        ));

        final Spinner periodSpinner = new Spinner(this);
        final String[] periods = {
                BudgetManager.PERIOD_DAILY,
                BudgetManager.PERIOD_WEEKLY,
                BudgetManager.PERIOD_MONTHLY,
                BudgetManager.PERIOD_YEARLY
        };
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(
                this,
                android.R.layout.simple_spinner_item,
                periods
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        periodSpinner.setAdapter(adapter);
        String currentPeriod = BudgetManager.getBudgetPeriod(this);
        for (int i = 0; i < periods.length; i++) {
            if (periods[i].equals(currentPeriod)) {
                periodSpinner.setSelection(i);
                break;
            }
        }
        LinearLayout.LayoutParams spinnerParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        spinnerParams.setMargins(0, dp(12), 0, 0);
        content.addView(periodSpinner, spinnerParams);

        final AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(R.string.profile_budget_dialog_title)
                .setView(content)
                .setNegativeButton(R.string.profile_budget_cancel, null)
                .setPositiveButton(R.string.profile_budget_save, null)
                .create();

        dialog.setOnShowListener(dialogInterface -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String amountText = amountInput.getText().toString().trim();
                if (amountText.isEmpty()) {
                    amountInput.setError("Enter a budget amount");
                    return;
                }

                int amount;
                try {
                    amount = Integer.parseInt(amountText);
                } catch (NumberFormatException e) {
                    amountInput.setError("Enter a valid amount");
                    return;
                }

                if (amount <= 0) {
                    amountInput.setError("Budget must be greater than 0");
                    return;
                }

                BudgetManager.saveBudget(
                        ProfileActivity.this,
                        amount,
                        periodSpinner.getSelectedItem().toString()
                );
                updateBudgetSummary();
                Toast.makeText(ProfileActivity.this, "Budget updated", Toast.LENGTH_SHORT).show();
                dialog.dismiss();
            }
        }));

        dialog.show();
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }

    private void openLogin() {
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
