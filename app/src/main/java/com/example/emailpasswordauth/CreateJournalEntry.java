package com.example.emailpasswordauth;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;


import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.slider.Slider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

public class CreateJournalEntry extends AppCompatActivity {

    private FirebaseAuth auth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_journal_entry);

        FirebaseFirestore db = FirebaseFirestore.getInstance();

        auth = FirebaseAuth.getInstance();

        Button create_btn = findViewById(R.id.create_btn);
        create_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                EditText reflection_edit_text = findViewById(R.id.reflection_text);
                String reflection_text = reflection_edit_text.getText().toString();

                Slider slider = findViewById(R.id.prompt_slider);
                int slider_val = (int) slider.getValue();

                Map<String, Object> entry = new HashMap<>();
                entry.put("content", reflection_text);
                entry.put("prompt_val", slider_val);

                DateTimeFormatter date_formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
                LocalDate today = LocalDate.now();


                db.collection("journal_entries").document(auth.getUid()).collection("entries").document(today.format(date_formatter))
                        .set(entry)
                        .addOnSuccessListener(new OnSuccessListener<Void>() {
                            @Override
                            public void onSuccess(Void aVoid) {
                                Toast.makeText(CreateJournalEntry.this, "Journal entry created",
                                        Toast.LENGTH_SHORT).show();
                                finish();
                            }
                        })
                        .addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                Toast.makeText(CreateJournalEntry.this, "Failed to create journal entry " + e,
                                        Toast.LENGTH_SHORT).show();
                            }
                        });
            }
        });
    }


}