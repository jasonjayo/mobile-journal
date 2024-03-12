package com.example.emailpasswordauth;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class MainActivity extends AppCompatActivity {

    private FirebaseAuth auth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button registerLink = findViewById(R.id.loginLink);
        registerLink.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(MainActivity.this, RegisterActivity.class));
            }
        });

        Button signInButton = findViewById(R.id.signUpButton);

        signInButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                EditText emailInput = findViewById(R.id.editTextTextEmailAddress);
                String email = emailInput.getText().toString();
                EditText passwordInput = findViewById(R.id.editTextTextPassword);
                String password = passwordInput.getText().toString();

                signIn(email, password);
            }
        });

        auth = FirebaseAuth.getInstance();
    }

    private void signIn(String email, String password) {


        auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            // Sign in success, update UI with the signed-in user's information
                            FirebaseUser user = auth.getCurrentUser();
                            Toast.makeText(MainActivity.this, "Authentication successful.",
                                    Toast.LENGTH_SHORT).show();
                            System.out.println("Successfully signed in as " + user.getEmail());
                            startActivity(new Intent(MainActivity.this, HomeActivity.class));
                        } else {
                            // If sign in fails, display a message to the user.
                            Toast.makeText(MainActivity.this, "Authentication failed.",
                                    Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }
}