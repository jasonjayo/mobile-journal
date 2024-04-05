package com.example.emailpasswordauth;

import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.FileProvider;


import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.emailpasswordauth.databinding.ActivityMainBinding;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.slider.Slider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.io.File;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

public class CreateJournalEntry extends AppCompatActivity {

    private FirebaseAuth auth;

    private final String[] possiblePrompts = {
            "How are your stress levels?",
            "How grateful are you?",
            "How calm are you?",
            "How happy do you feel?",
            "How connected do you feel to those around you?",
            "How balanced is your life?",
            "How well are you making time to pursue your interests?",
            "How grounded do you feel in your daily life?",
            "How well are you handling life's challenges?",
            "How satisfied does you lifestyle make you feel?"
    };

    ActivityMainBinding mainBinding;
    ActivityResultLauncher<Uri> addPicLauncher;
    Uri imageUri;

    private void registerPicLauncher() {
        addPicLauncher = registerForActivityResult(
                new ActivityResultContracts.TakePicture(),
                new ActivityResultCallback<Boolean>() {
                    @Override
                    public void onActivityResult(Boolean o) {
                        try {
                            if (o) {
                                ImageView imgView = findViewById(R.id.imageView);
                                imgView.setImageURI(null);
                                imgView.setImageURI(imageUri);
                            }
                        } catch (Exception e) {
                            // todo tidy this
                            e.getStackTrace();
                        }
                    }
                }
        );
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_journal_entry);

        imageUri = createUri();

        registerPicLauncher();

        Button addPicBtn = findViewById(R.id.addPicBtn);
        addPicBtn.setOnClickListener(view -> {
            openCam();
        });

        FirebaseFirestore db = FirebaseFirestore.getInstance();

        auth = FirebaseAuth.getInstance();

        Button create_btn = findViewById(R.id.create_btn);

        String selectedPrompt = possiblePrompts[(int) (Math.random() * possiblePrompts.length)];
        System.out.println(selectedPrompt);
        TextView promptTextView = findViewById(R.id.prompt);
        promptTextView.setText(selectedPrompt);

        ImageButton moodHappy = findViewById(R.id.sentimentHappy);
        ImageButton moodNeutral = findViewById(R.id.sentimentNeutral);
        ImageButton moodSad = findViewById(R.id.sentimentSad);

        final float inactiveMoodBtnOpacity = 0.05f;

        moodHappy.setOnClickListener(view -> {
            moodHappy.setAlpha(1f);
            moodNeutral.setAlpha(inactiveMoodBtnOpacity);
            moodSad.setAlpha(inactiveMoodBtnOpacity);
        });
        moodNeutral.setOnClickListener(view -> {
            moodHappy.setAlpha(inactiveMoodBtnOpacity);
            moodNeutral.setAlpha(1f);
            moodSad.setAlpha(inactiveMoodBtnOpacity);
        });
        moodSad.setOnClickListener(view -> {
            moodHappy.setAlpha(inactiveMoodBtnOpacity);
            moodNeutral.setAlpha(inactiveMoodBtnOpacity);
            moodSad.setAlpha(1f);
        });

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

    private Uri createUri() {
        File imageFile = new File(getApplicationContext().getFilesDir(), "camera_photo.jpg");
        return FileProvider.getUriForFile(getApplicationContext(), "com.example.emailpasswordauth.provider", imageFile);
    }

    private void openCam() {
        // open camera if permission granted, else ask for it
        if (ActivityCompat.checkSelfPermission(CreateJournalEntry.this, android.Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(CreateJournalEntry.this, new String[]{android.Manifest.permission.CAMERA}, 1);
        } else {
            addPicLauncher.launch(imageUri);
        }
    }

    @Override
    // called with result of the requestPermissions above
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == 1) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                addPicLauncher.launch(imageUri);
            } else {
                Toast.makeText(this, "Camera permission required", Toast.LENGTH_LONG).show();
            }
        }
    }
}