package com.example.smartexpense;

import android.content.Intent;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.PopupMenu;

final class TopMenu {

    private TopMenu() {
    }

    static void attach(final AppCompatActivity activity) {
        View menuButton = activity.findViewById(R.id.topMenuButton);
        if (menuButton == null) {
            return;
        }

        menuButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                PopupMenu popupMenu = new PopupMenu(activity, v);
                popupMenu.getMenu().add("Settings");
                popupMenu.getMenu().add("Logout");
                popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem item) {
                        String title = item.getTitle().toString();
                        if ("Settings".equals(title)) {
                            Toast.makeText(activity, "Settings coming soon", Toast.LENGTH_SHORT).show();
                            return true;
                        }

                        if ("Logout".equals(title)) {
                            AuthManager.logout(activity);
                            Intent intent = new Intent(activity, LoginActivity.class);
                            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                            activity.startActivity(intent);
                            activity.finish();
                            return true;
                        }

                        return false;
                    }
                });
                popupMenu.show();
            }
        });
    }
}
