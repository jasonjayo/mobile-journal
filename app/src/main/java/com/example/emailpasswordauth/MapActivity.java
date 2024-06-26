
package com.example.emailpasswordauth;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.location.Location;
import android.media.ExifInterface;
import android.media.Image;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.GoogleMap.OnMarkerClickListener;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;


import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.model.CameraPosition;

import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.net.PlacesClient;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.storage.FileDownloadTask;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.ListResult;
import com.google.firebase.storage.StorageReference;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;


public class MapActivity extends AppCompatActivity implements OnMapReadyCallback {


    private static final String TAG = MapActivity.class.getSimpleName();
    private GoogleMap map;
    private CameraPosition cameraPosition;

    // The entry point to the Places API.
    private PlacesClient placesClient;

    // The entry point to the Fused Location Provider.
    private FusedLocationProviderClient fusedLocationProviderClient;

    // Default location is given as Limerick in case user doesn't grant us location permissions.
    private final LatLng defaultLocation = new LatLng(52.661252, -8.6301239);
    private static final int DEFAULT_ZOOM = 15;
    private static final int PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION = 1;
    private boolean locationPermissionGranted;

    // The geographical location where the device is currently located. That is, the last-known
    // location retrieved by the Fused Location Provider.
    private Location lastKnownLocation;

    // Keys for storing activity state.
    private static final String KEY_CAMERA_POSITION = "camera_position";
    private static final String KEY_LOCATION = "location";

    // Used for selecting the current place.
    private static final int M_MAX_ENTRIES = 5;
    private String[] likelyPlaceNames;
    private String[] likelyPlaceAddresses;
    private List[] likelyPlaceAttributions;
    private LatLng[] likelyPlaceLatLngs;

    // Retrieving Coords Data
    FirebaseFirestore db = FirebaseFirestore.getInstance();
    private FirebaseAuth auth;
    private List<DocumentSnapshot> appUsers;

    private List<DocumentSnapshot> appUserEntries;
    CollectionReference allAppUsers = db.collection("journal_entries");

    FirebaseStorage storage = FirebaseStorage.getInstance();
    StorageReference storageRef = storage.getReference();

    ArrayList<ArrayList> markerLocations = new ArrayList<>();

    ArrayList<LatLng> markers = new ArrayList<>();

    ArrayList<entryInfo> entryMarkers = new ArrayList<>();

    public class entryInfo {
        public Double entryLat;
        public Double entryLong;
        public String entryTitle;
        public String entryContent;

        public Bitmap entryImage;

        public entryInfo(Double latitude, Double longitude, String id, String content, Bitmap img) {
            this.entryLat = latitude;
            this.entryLong = longitude;
            this.entryTitle = id;
            this.entryContent = content;
            this.entryImage = img;
        }

    }

    Bitmap Image;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //Retrieve the content view that renders the map.
        setContentView(R.layout.activity_map);

        //Construct a PlacesClient
        Places.initialize(getApplicationContext(), BuildConfig.MAPS_API_KEY);
        placesClient = Places.createClient(this);

        //Construct a FusedLocationProviderClient
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);


        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        auth = FirebaseAuth.getInstance();


    }

    //Updates the Ui of the map depending on if user has granted permission (enables blue location icon at user location and centres map to user location)
    private void updateLocationUI() {
        if (map == null) {
            return;
        }
        try {
            if (locationPermissionGranted) {
                map.setMyLocationEnabled(true);
                map.getUiSettings().setMyLocationButtonEnabled(true);
            } else {
                map.setMyLocationEnabled(false);
                map.getUiSettings().setMyLocationButtonEnabled(false);
                lastKnownLocation = null;
                getLocationPermission();
            }
        } catch (SecurityException e) {
            Log.e("Exception: %s", e.getMessage());
        }
    }

    private void getDeviceLocation() {
        /*
         * Get the best and most recent location of the device, which may be null in rare
         * cases when a location is not available.
         */
        try {
            if (locationPermissionGranted) {
                // FusedLocationProvider returns a single location fix representing the best estimate of the current location of the device
                Task<Location> locationResult = fusedLocationProviderClient.getLastLocation();
                locationResult.addOnCompleteListener(this, new OnCompleteListener<Location>() {
                    @Override
                    public void onComplete(@NonNull Task<Location> task) {
                        if (task.isSuccessful()) {
                            // Set the map's camera position to the current location of the device.
                            lastKnownLocation = task.getResult();
                            if (lastKnownLocation != null) {
                                map.moveCamera(CameraUpdateFactory.newLatLngZoom(
                                        new LatLng(lastKnownLocation.getLatitude(),
                                                lastKnownLocation.getLongitude()), DEFAULT_ZOOM));
                                map.setMyLocationEnabled(true);
                                map.getUiSettings().setMyLocationButtonEnabled(true);
                            }
                        } else {
                            // if cant get location of user, move map to default location which is Limerick
                            Log.d(TAG, "Current location is null. Using defaults.");
                            Log.e(TAG, "Exception: %s", task.getException());
                            map.moveCamera(CameraUpdateFactory
                                    .newLatLngZoom(defaultLocation, DEFAULT_ZOOM));
                            map.getUiSettings().setMyLocationButtonEnabled(false);
                        }
                    }
                });
            }
        } catch (SecurityException e) {
            Log.e("Exception: %s", e.getMessage(), e);
        }
    }


    private void getLocationPermission() {
        /*
         * Request location permission, so that we can get the location of the
         * device. The result of the permission request is handled by a callback,
         * onRequestPermissionsResult.
         */
        if (ContextCompat.checkSelfPermission(this.getApplicationContext(),
                android.Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            locationPermissionGranted = true;
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION},
                    PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION);
        }
    }

    // Callback function for getLocationPermission function
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        locationPermissionGranted = false;
        if (requestCode
                == PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION) {// If request is cancelled, the result arrays are empty.
            if (grantResults.length > 0
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                locationPermissionGranted = true;
            }
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
        updateLocationUI();
    }

    // Get all Users on the App
    public void getAllAppUsersLocations() {
        // **** Getting all the user's who are on the App
        allAppUsers.get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
            @Override
            public void onComplete(@NonNull Task<QuerySnapshot> task) {
                if (task.isSuccessful()) {
                    List<DocumentSnapshot> documents = task.getResult().getDocuments();
                    appUsers = new ArrayList<>();
                    appUsers.addAll(documents);
                    for (int i = 0; i < appUsers.size(); i++) {
                        String currentId = appUsers.get(i).getId();
                        DocumentReference appUserDoc = appUsers.get(i).getReference(); // Storage reference for each users profile in Firestore

                        //  **** Get all the individual entries within this users entries collection
                        int finalI = i;
                        appUserDoc.collection("entries").get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                            @Override
                            public void onComplete(@NonNull Task<QuerySnapshot> task) {
                                if (task.isSuccessful()) {
                                    Log.d("goodjob", " this worked ");
                                    List<DocumentSnapshot> documents2 = task.getResult().getDocuments();
                                    appUserEntries = new ArrayList<>(); // Contains all the entries for each user of app.
                                    appUserEntries.addAll(documents2); //
                                    for (int j = 0; j < appUserEntries.size(); j++) {


                                        if (appUserEntries.get(j).contains("entry_lat")) { //Check if location fields are in the entry so we can add marker, otherwise ignore

                                            StorageReference imageRef = storageRef.child(currentId + "/" + appUserEntries.get(j).getId() + ".jpg");  // Storage reference for image associated with each entry
                                            Log.d("imageRef", imageRef.toString());
                                            // Getting the image from the reference
                                            try {
                                                File localFile = File.createTempFile("images", "jpg");
                                                int finalJ = j;
                                                DocumentSnapshot userEntry = appUserEntries.get(j);
                                                int userEntriesLength = appUserEntries.size();

                                                imageRef.getFile(localFile).addOnCompleteListener(new OnCompleteListener<FileDownloadTask.TaskSnapshot>() {
                                                    @Override
                                                    public void onComplete(@NonNull Task<FileDownloadTask.TaskSnapshot> task) {
                                                        System.out.println("FILE EXISTS for final j " + finalJ);
                                                        Image = BitmapFactory.decodeFile(localFile.getAbsolutePath());
                                                        if (Image != null) {

                                                            // scale image and maintain aspect ratio
                                                            double ratio = Image.getWidth() / 200.0;
                                                            int newHeight = (int) (Image.getHeight() / ratio);
                                                            Image = Bitmap.createScaledBitmap(Image, 200, newHeight, false);

                                                            // fix rotation
                                                            ExifInterface exif = null;
                                                            try {
                                                                exif = new ExifInterface(localFile);

                                                                int orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);

                                                                Matrix matrix = new Matrix();
                                                                switch (orientation) {
                                                                    case ExifInterface.ORIENTATION_ROTATE_90:
                                                                        matrix.postRotate(90);
                                                                        break;
                                                                    case ExifInterface.ORIENTATION_ROTATE_180:
                                                                        matrix.postRotate(180);
                                                                        break;
                                                                    case ExifInterface.ORIENTATION_ROTATE_270:
                                                                        matrix.postRotate(270);
                                                                        break;
                                                                }
                                                                Image = Bitmap.createBitmap(Image, 0, 0, Image.getWidth(), Image.getHeight(), matrix, true);
                                                            } catch (IOException e) {
//                                                                throw new RuntimeException(e);
                                                            }


                                                        }

                                                        // TBR
                                                        System.out.println("CONTENT: " + userEntry.get("content"));
                                                        entryInfo info = new entryInfo(
                                                                (Double) userEntry.get("entry_lat"),
                                                                (Double) userEntry.get("entry_long"),
                                                                userEntry.getId(),
                                                                (String) userEntry.get("content"),
                                                                Image

                                                        );

                                                        // TBR   entryMarkers.add(info);
                                                        // If image retrieved isnt null we add as icon for marker
                                                        if (Image != null) {
                                                            map.addMarker(new MarkerOptions()
                                                                    .position(new LatLng((Double) userEntry.get("entry_lat"), (Double) userEntry.get("entry_long")))
                                                                    .title(userEntry.getId())
                                                                    .snippet((String) userEntry.get("content"))
                                                                    .icon(BitmapDescriptorFactory.fromBitmap(Image))
                                                            );
                                                            // if image retrieved is null then we dont add icon, and add default marker icon
                                                        } else {
                                                            map.addMarker(new MarkerOptions()
                                                                    .position(new LatLng((Double) userEntry.get("entry_lat"), (Double) userEntry.get("entry_long")))
                                                                    .title(userEntry.getId())
                                                                    .snippet((String) userEntry.get("content"))
                                                            );
                                                        }


                                                        if ((finalI == appUsers.size() - 1) && (finalJ == userEntriesLength - 1)) {
                                                        }

                                                    }
                                                }).addOnFailureListener(new OnFailureListener() {
                                                    @Override
                                                    public void onFailure(@NonNull Exception e) {
                                                        System.out.println("FILE NOT EXISTS for final j " + finalJ);
//                                                        Toast.makeText(MapActivity.this, "failed to retrieve", Toast.LENGTH_SHORT).show();
                                                        //TBR
                                                        entryInfo info = new entryInfo(
                                                                (Double) userEntry.get("entry_lat"),
                                                                (Double) userEntry.get("entry_long"),
                                                                userEntry.getId(),
                                                                (String) userEntry.get("content"),
                                                                null

                                                        );

                                                        //entryMarkers.add(info);

                                                        map.addMarker(new MarkerOptions()
                                                                .position(new LatLng((Double) userEntry.get("entry_lat"), (Double) userEntry.get("entry_long")))
                                                                .title(userEntry.getId())
                                                                .snippet((String) userEntry.get("content"))
                                                        );

                                                    }
                                                });
                                            } catch (IOException e) {
                                                Toast.makeText(getApplicationContext(), "error here" + e, Toast.LENGTH_SHORT).show();
                                            }

                                        }
                                    }
                                } else {
                                    Log.d(TAG, "Error getting documents: ", task.getException());
                                }

                            }

                        });
                    }
                } else {
                    Log.d(TAG, "Error getting documents: ", task.getException());
                }
            }
        });
    }

    // TO BE REMOVED
    public void markersLoaded() {
        System.out.println("============ MARKERS LOADED ============");
//        Log.d("MARKERS", markerLocations.toString());
        System.out.println("Markers length " + entryMarkers.size());
        for (int i = 0; i < entryMarkers.size(); i++) {

            System.out.println("i is " + i);

            System.out.println("LAT LONG: " + entryMarkers.get(i).entryLat + " " + entryMarkers.get(i).entryLong);


//            System.out.println("IMAGE " + entryMarkers.get(i).entryImage);
            if (entryMarkers.get(i).entryImage != null) {
                System.out.println("HAS IMAGE");
                this.map.addMarker(new MarkerOptions()
                                .position(new LatLng(entryMarkers.get(i).entryLat, entryMarkers.get(i).entryLong))
                                .title(entryMarkers.get(i).entryTitle)
                                .snippet(entryMarkers.get(i).entryContent)
//                                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE))
                                .icon(BitmapDescriptorFactory.fromBitmap(entryMarkers.get(i).entryImage))
                );
            } else {
                System.out.println("DOESNT HAVE IMAGE");
//                this.map.addMarker(new MarkerOptions()
//                                .position(new LatLng(entryMarkers.get(i).entryLat, entryMarkers.get(i).entryLong))
                System.out.println(this.map);
                this.map.addMarker(new MarkerOptions()
                        .position(new LatLng(entryMarkers.get(i).entryLat, entryMarkers.get(i).entryLong))
                        .title(entryMarkers.get(i).entryTitle)
                        .snippet(entryMarkers.get(i).entryContent)

                );
                System.out.println("DOESNT HAVE IMAGE ADDED");
            }


            System.out.println("MARKERS PRINTING FINISHED " + i);
        }
    }

    public void getEntryLocations() {
        getAllAppUsersLocations();
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        //LatLng limerick = new LatLng(52.661252,-8.6301239 );
        googleMap.setMapType(GoogleMap.MAP_TYPE_HYBRID);

        this.map = googleMap;
        this.map.setInfoWindowAdapter(new CustomInfoWindowAdapter(MapActivity.this));

        // Turn on the My Location layer and the related control on the map.
        updateLocationUI();

        // Get the current location of the device and set the position of the map.
        getDeviceLocation();

        getEntryLocations();


    }


    @Override
    public void onPointerCaptureChanged(boolean hasCapture) {
        super.onPointerCaptureChanged(hasCapture);
    }
}

