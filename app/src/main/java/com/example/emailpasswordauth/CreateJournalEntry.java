package com.example.emailpasswordauth;

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

    // photos variables
    ActivityResultLauncher<Uri> addPicLauncher;
    Uri imageUri;
    private boolean hasPhoto = false;

    String sentiment;


    // map Variables
    private FusedLocationProviderClient fusedLocationProviderClient;
    private boolean userLocationPermissionGranted;
    private Location userLocation;
    private LatLng defaultLocation;
    private Double userLat;
    private Double userLong;

    private void registerPicLauncher() {
        Button addPicBtn = findViewById(R.id.addPicBtn);
        // open system camera UI to take photo
        addPicLauncher = registerForActivityResult(
                new ActivityResultContracts.TakePicture(),
                success -> {
                    if (success) {
                        ImageView imgView = findViewById(R.id.imageView);
                        // get rid of any existing image first, then display image user just took
                        imgView.setImageURI(null);
                        imgView.setImageURI(imageUri);
                        // change text of button from Add Photo to Replace Photo
                        addPicBtn.setText("Replace image");
                        hasPhoto = true;
                    } else {
                        Toast.makeText(this, "Unable to take photo.", Toast.LENGTH_SHORT).show();
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

        // for getting users location
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);

        // auth
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        Button create_btn = findViewById(R.id.create_btn);

        // choose random prompt and display
        String[] possiblePromptKeys = Prompts.possiblePrompts.keySet().toArray(new String[0]);
        String selectedPromptKey = possiblePromptKeys[(int) (Math.random() * possiblePromptKeys.length)];
        String selectedPrompt = Prompts.possiblePrompts.get(selectedPromptKey);
        TextView promptTextView = findViewById(R.id.prompt);
        promptTextView.setText(selectedPrompt);


        // sentiments
        sentiment = "NEUTRAL"; // default sentiment

        ImageButton moodHappy = findViewById(R.id.sentimentHappy);
        ImageButton moodNeutral = findViewById(R.id.sentimentNeutral);
        ImageButton moodSad = findViewById(R.id.sentimentSad);

        // set up click event listeners so that when user clicks a sentiment, the others change to 50% opacity
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

        // submit new journal entry
        create_btn.setOnClickListener(view -> {
            // main entry content
            EditText reflection_edit_text = findViewById(R.id.reflection_text);
            String reflection_text = reflection_edit_text.getText().toString();

            // get prompt value from slider
            Slider slider = findViewById(R.id.prompt_slider);
            int slider_val = (int) slider.getValue();

            // map
            defaultLocation = new LatLng(52.661252, -8.6301239);

            // set up Map to represent this entry, will be passed to Firebase
            Map<String, Object> entry = new HashMap<>();
            entry.put("sentiment", sentiment);
            entry.put("content", reflection_text);
            entry.put("prompt_key", selectedPromptKey);
            entry.put("prompt_val", slider_val);
            if (userLong != null && userLat != null) {
                entry.put("entry_lat", userLat);
                entry.put("entry_long", userLong);
            }

            // upload photo if present
            if (hasPhoto) {
                FirebaseStorage storage = FirebaseStorage.getInstance();
                StorageReference storageRef = storage.getReference();
                // set remote storage location for photo
                StorageReference imageRef = storageRef.child(auth.getUid() + "/" + imageUri.getLastPathSegment());

                UploadTask uploadTask = imageRef.putFile(imageUri);
                uploadTask.addOnFailureListener(exception -> {
                    Toast.makeText(CreateJournalEntry.this, "Failed to upload photo " + exception, Toast.LENGTH_SHORT).show();
                }).addOnSuccessListener(t -> {
                    Toast.makeText(CreateJournalEntry.this, "Photo upload complete", Toast.LENGTH_SHORT).show();
                });
            }

            // entry name will be today's date
            DateTimeFormatter date_formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
            LocalDate today = LocalDate.now();

            // store document on Firestore under journal_entries/<userId>/entries/<currentDate>
            db.collection("journal_entries")
                    .document(auth.getUid())
                    .collection("entries")
                    .document(today.format(date_formatter))
                    .set(entry)
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(CreateJournalEntry.this, "Journal entry created",
                                Toast.LENGTH_SHORT).show();
                        finish(); // go back to dashboard
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(CreateJournalEntry.this, "Failed to create journal entry " + e,
                                Toast.LENGTH_SHORT).show();
                        Log.d("ENTRY FAIL", e.toString());
                    });
        });
    }

    private Uri createUri() {
        // create an empty file and return its URI
        // later, we'll use this file to store the photo taken by the camera locally before upload
        DateTimeFormatter date_formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        LocalDate today = LocalDate.now();
        File imageFile = new File(getApplicationContext().getFilesDir(), today.format(date_formatter) + ".jpg");
        return FileProvider.getUriForFile(getApplicationContext(), "com.example.emailpasswordauth.provider", imageFile);
    }

    private void openCam() {
        // open camera if permission granted, else ask for it

        if (ActivityCompat.checkSelfPermission(CreateJournalEntry.this, android.Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            // ask for camera permission
            ActivityCompat.requestPermissions(CreateJournalEntry.this, new String[]{android.Manifest.permission.CAMERA}, 1);
        } else {
            // launch camera - image will stored after capture in URI specified by imageUri
            addPicLauncher.launch(imageUri);
        }
    }

    @Override
    // called with result of any requestPermissions calls (either for camera or locations in our case)
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        /*
            general format is we check if permissions was actually granted, if so continue with next task
            else create toast message saying permission is required
        */
        if (requestCode == 1) { // if requested camera permissions
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                addPicLauncher.launch(imageUri); // launch camera
            } else {
                Toast.makeText(this, "Camera permission required", Toast.LENGTH_LONG).show();
            }
        } else if (requestCode == 2) { // if requested location permissions
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                userLocationPermissionGranted = true;
                // get location of user's device
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
                locationResult.addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        // update the users latitude and longitude values to be the ones retrieved from getting device's location
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
                });
            }
        } catch (SecurityException e) {
            Log.e("Exception: %s", e.getMessage(), e);
        }
    }

    private void getPermissionForUserLocation() {
        // if permission granted update variable
        if (ContextCompat.checkSelfPermission(this.getApplicationContext(),
                android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            userLocationPermissionGranted = true;
            getUserLocationForEntry();
        } else {
            // else make a request for permission
            ActivityCompat.requestPermissions(this,
                    new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION},
                    2);
        }
    }

    public void mapShareToggle(View v) {
        CheckBox mapCheckBox = findViewById(R.id.checkBox);
        // adding entry to map
        // if box is checked and permission granted we get user's location
        if (mapCheckBox.isChecked()) {
            if (userLocationPermissionGranted) {
                getUserLocationForEntry();
            } else {
                // if permission not granted we go to request it
                getPermissionForUserLocation();
            }
        }
    }

}



