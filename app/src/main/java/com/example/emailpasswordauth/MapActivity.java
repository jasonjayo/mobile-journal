package com.example.emailpasswordauth;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.tasks.OnCompleteListener;
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

import java.util.ArrayList;
import java.util.List;


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


    ArrayList<ArrayList> markerLocations = new ArrayList<>();

    ArrayList<Object> markers = new ArrayList<>();


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
                            }
                        } else {
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
                        Log.d("userIds", appUsers.get(i).getId()); // will show all ids of users
                        DocumentReference appUserDoc = appUsers.get(i).getReference(); // reference to all these user profs

                        //  **** Get all the individual entries within this users entries collection (TBD: where the location field exists)
                        appUserDoc.collection("entries").get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                            @Override
                            public void onComplete(@NonNull Task<QuerySnapshot> task) {
                                if (task.isSuccessful()) {
                                    Log.d("goodjob", " this worked bro");
                                    List<DocumentSnapshot> documents2 = task.getResult().getDocuments();
                                    appUserEntries = new ArrayList<>();
                                    appUserEntries.addAll(documents2);
                                    for (int i = 0; i < appUserEntries.size(); i++) {
                                        if (appUserEntries.get(i).contains("entry_lat")) {
                                            Log.d("entryIds", appUserEntries.get(i).getId() + " " + appUserEntries.get(i).get("entry_lat"));
                                            ArrayList<Double> userCoords = new ArrayList<Double>();
                                            userCoords.add((Double) appUserEntries.get(i).get("entry_lat"));
                                            userCoords.add((Double) appUserEntries.get(i).get("entry_long"));
                                            markerLocations.add(userCoords);
                                        }
                                        Log.d("mate", markerLocations.toString());

                                    }
                                } else {
                                    Log.d("bruh", "there is an error brudda");
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
        Log.d("mama", markers.toString());
    }




    public void getEntryLocations() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        // Getting all the user's who are on the App
//                journalEntries.get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
//            @Override
//            public void onComplete(@NonNull Task<QuerySnapshot> task) {
//                if (task.isSuccessful()) {
//                    List<DocumentSnapshot> documents = task.getResult().getDocuments();
//                    dataList = new ArrayList<>();
//                    dataList.addAll(documents);
//                   for (int i = 0; i < dataList.size(); i++){
//                       Log.d("gettingDocs", dataList.get(i).getId());
//                       // Gettings all the app user's entries.
//                       db.collection("journal_entries").document( dataList.get(i).getId()).collection("entries")
//                   }
////                    for (QueryDocumentSnapshot document : task.getResult()) {
////                        Log.d("gettingDocs", document.getId());
////                    }
//                } else {
//                    Log.d(TAG, "Error getting documents: ", task.getException());
//                }
//            }
//        });

        // *** Problem here is that I am not authorised to view anyone else's journal entries, only my own, so cant access the location of anyone else's entries :(.
        getAllAppUsersLocations();
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        ArrayList<LatLng> markers = new ArrayList<>();
        //LatLng limerick = new LatLng(52.661252,-8.6301239 );
        googleMap.setMapType(GoogleMap.MAP_TYPE_HYBRID);
        this.map = googleMap;
        Log.d("point", markerLocations.toString());
//        for (int i = 0; i < markerLocations.size(); i++) {


//            for (int j = 0; j < markerLocations.get(i).size();){
//            }
//                Double lat = (Double) markerLocations.get(i).get(j);
//                Double lon = (Double) markerLocations.get(i).get(j++);
//
//                LatLng marker = new LatLng(lat, lon);
//                markers.add(marker);
//           }






        // Turn on the My Location layer and the related control on the map.
        updateLocationUI();

        // Get the current location of the device and set the position of the map.
        getDeviceLocation();

        getEntryLocations();
        Log.d("point", markerLocations.toString());
//        for(int k = 0; k < markers.size(); k++){
//            googleMap.addMarker(new MarkerOptions()
//                    .position(markers.get(k)));
//        }
        // googleMap.addMarker(new MarkerOptions()
        //        .position(limerick).title("Limerick"));
//        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(markers.get(1), 15));
    }




    @Override
    public void onPointerCaptureChanged(boolean hasCapture) {
        super.onPointerCaptureChanged(hasCapture);
    }

}