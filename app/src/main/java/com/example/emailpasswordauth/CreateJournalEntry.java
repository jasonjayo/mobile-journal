package com.example.emailpasswordauth;

import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;


import android.content.pm.PackageManager;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;


import com.example.emailpasswordauth.databinding.ActivityMainBinding;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.slider.Slider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.File;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

public class CreateJournalEntry extends AppCompatActivity {

    private FirebaseAuth auth;

    ActivityResultLauncher<Uri> addPicLauncher;
    Uri imageUri;

    String sentiment;

    // Map Variables
    private FusedLocationProviderClient fusedLocationProviderClient;
    private boolean userLocationPermissionGranted;
    private Location userLocation;
    private LatLng defaultLocation;
    private Double userLat;
    private Double userLong;

    private boolean mapShareEnabled;

    private void registerPicLauncher() {
        addPicLauncher = registerForActivityResult(
                new ActivityResultContracts.TakePicture(),
                o -> {
                    ImageView imgView = findViewById(R.id.imageView);
                    imgView.setImageURI(null);
                    imgView.setImageURI(imageUri);
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
 // adding this for getting users location..
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);

        FirebaseFirestore db = FirebaseFirestore.getInstance();

        auth = FirebaseAuth.getInstance();

        sentiment = "NEUTRAL";

        Button create_btn = findViewById(R.id.create_btn);

        String[] possiblePromptKeys = Prompts.possiblePrompts.keySet().toArray(new String[0]);
        String selectedPromptKey = possiblePromptKeys[(int) (Math.random() * possiblePromptKeys.length)];
        String selectedPrompt = Prompts.possiblePrompts.get(selectedPromptKey);
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
            sentiment = "HAPPY";
        });
        moodNeutral.setOnClickListener(view -> {
            moodHappy.setAlpha(inactiveMoodBtnOpacity);
            moodNeutral.setAlpha(1f);
            moodSad.setAlpha(inactiveMoodBtnOpacity);
            sentiment = "NEUTRAL";
        });
        moodSad.setOnClickListener(view -> {
            moodHappy.setAlpha(inactiveMoodBtnOpacity);
            moodNeutral.setAlpha(inactiveMoodBtnOpacity);
            moodSad.setAlpha(1f);
            sentiment = "SAD";
        });

        create_btn.setOnClickListener(view -> {
            EditText reflection_edit_text = findViewById(R.id.reflection_text);
            String reflection_text = reflection_edit_text.getText().toString();

            Slider slider = findViewById(R.id.prompt_slider);
            int slider_val = (int) slider.getValue();


            //Map Code
            defaultLocation = new LatLng(52.661252, -8.6301239);




            Map<String, Object> entry = new HashMap<>();
            entry.put("sentiment", sentiment);
            entry.put("content", reflection_text);
            entry.put("prompt_key", selectedPromptKey);
            entry.put("prompt_val", slider_val);
            if (userLong != null && userLat != null) {
entry.put("entry_lat", userLat);
entry.put("entry_long", userLong);
            }

            DateTimeFormatter date_formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
            LocalDate today = LocalDate.now();





            // save file
            FirebaseStorage storage = FirebaseStorage.getInstance();
            StorageReference storageRef = storage.getReference();
            StorageReference imageRef = storageRef.child(auth.getUid() + "/" + imageUri.getLastPathSegment());
            UploadTask uploadTask = imageRef.putFile(imageUri);
            uploadTask.addOnFailureListener(exception -> {
                Toast.makeText(CreateJournalEntry.this, "Failed to upload photo " + exception, Toast.LENGTH_SHORT).show();
            }).addOnSuccessListener(taskSnapshot -> {
                // taskSnapshot.getMetadata() contains file metadata such as size, content-type, etc.
            });

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
        });
    }

    private Uri createUri() {
        DateTimeFormatter date_formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        LocalDate today = LocalDate.now();
        File imageFile = new File(getApplicationContext().getFilesDir(), today.format(date_formatter) + ".jpg");
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
        } else if (requestCode == 2) {
            if (grantResults.length > 0
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                userLocationPermissionGranted = true;
                // get location of users device.
                getUserLocationForEntry();
            }
        } else {
            Toast.makeText(this, "Location permission required", Toast.LENGTH_LONG).show();
        }

    }
    private void getUserLocationForEntry() {
        try {
        if (userLocationPermissionGranted) {
            Task<Location> locationResult = fusedLocationProviderClient.getLastLocation();
            locationResult.addOnCompleteListener(this, new OnCompleteListener<Location>() {
                @Override
                public void onComplete(@NonNull Task<Location> task) {
                    if (task.isSuccessful()) {
                        // update the users latitude and longitude values to be the ones retrieved from getting devices location.
                        userLocation = task.getResult();
                        if (userLocation != null) {
                            userLat = userLocation.getLatitude();
                            userLong = userLocation.getLongitude();
                        }
                    } else {
                        Log.d("error", "Current location is null. Using defaults.");
                        userLat = defaultLocation.latitude;
                        userLong = defaultLocation.longitude;
                    }
                }
            });
        }
        } catch(SecurityException e)  {
            Log.e("Exception: %s", e.getMessage(), e);
        }

    }

    private void getPermissionForUserLocation() {
        // if permission granted update variable
        if (ContextCompat.checkSelfPermission(this.getApplicationContext(),
                android.Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            userLocationPermissionGranted = true;
            getUserLocationForEntry();
        } else {
            // make a request for permission
            ActivityCompat.requestPermissions(this,
                    new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION},
                    2);

        }

    }
    public void mapShareToggle(View v){
        CheckBox mapCheckBox = findViewById(R.id.checkBox);
        // Adding entry to map
        // if box is checked and permission granted we get users location
        if (mapCheckBox.isChecked()) {
            if (userLocationPermissionGranted) {
                getUserLocationForEntry();
                //   if permission not granted we go to request it
            } else {
                getPermissionForUserLocation();
            }
        }


        }



}



