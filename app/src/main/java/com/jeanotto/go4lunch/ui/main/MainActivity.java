package com.jeanotto.go4lunch.ui.main;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.jeanotto.go4lunch.R;
import com.jeanotto.go4lunch.ui.auth.LoginActivity;
import com.jeanotto.go4lunch.ui.list.ListFragment;
import com.jeanotto.go4lunch.ui.map.MapFragment;
import com.jeanotto.go4lunch.ui.workmates.WorkmatesFragment;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        BottomNavigationView bottomNavigationView = findViewById(R.id.bottom_navigation);
        bottomNavigationView.setOnItemSelectedListener(this::onNavigationItemSelected);

        if (savedInstanceState == null) {
            replaceFragment(new MapFragment());
        }
    }

    private boolean onNavigationItemSelected(@NonNull MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == R.id.nav_map) {
            replaceFragment(new MapFragment());
            return true;
        } else if (itemId == R.id.nav_list) {
            replaceFragment(new ListFragment());
            return true;
        } else if (itemId == R.id.nav_workmates) {
            replaceFragment(new WorkmatesFragment());
            return true;
        }
        return false;
    }

    private void replaceFragment(@NonNull Fragment fragment) {
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .commit();
    }
}
