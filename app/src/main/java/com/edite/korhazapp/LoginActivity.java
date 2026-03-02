package com.edite.korhazapp;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class LoginActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // EZ A SOR HIÁNYZOTT: Összeköti a Java kódot az XML felülettel
        setContentView(R.layout.activity_login);

        // Most már megtalálja az ID alapján a feliratot
        TextView goToRegister = findViewById(R.id.tvGoToRegister);

        goToRegister.setOnClickListener(v -> {
            // Csak akkor nem lesz piros, ha már létrehoztad a RegisterActivity-t!
            Intent intent = new Intent(LoginActivity.this, RegisterActivity.class);
            startActivity(intent);
        });
    }
}