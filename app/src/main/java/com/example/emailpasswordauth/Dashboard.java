package com.example.emailpasswordauth;

import static android.content.ContentValues.TAG;

import android.app.PendingIntent;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Paint;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.Manifest;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;


import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * A simple {@link Fragment} subclass.
 */
public class Dashboard extends Fragment {

    private FirebaseAuth auth;
    ViewJournalEntryAdapter adapter;
    private RecyclerView recyclerView;
    private List<DocumentSnapshot> dataList;

    public Dashboard() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_dashboard, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        auth = FirebaseAuth.getInstance();

        // display user's email
        TextView userEmail = getView().findViewById(R.id.userEmail);
        userEmail.setText(auth.getCurrentUser().getEmail());
        // set email as shortcut to profile page
        userEmail.setOnClickListener(view1 -> {
            // using a bundle to pass data (email in this case) to the profile fragment
            Bundle bundle = new Bundle();
            bundle.putString("email", auth.getCurrentUser().getEmail());
            Navigation.findNavController(view1).navigate(R.id.action_dashboard_to_profile, bundle);
        });

        // set onclick listener for new entry btn to launch create journal activity
        Button new_entry_btn = getView().findViewById(R.id.new_entry_btn);
        new_entry_btn.setOnClickListener(view12 -> startActivity(new Intent(getActivity(), CreateJournalEntry.class)));

        // load entries from Firestore
        getEntriesList();
    }

    public void getEntriesList() {
        // load entries from Firestore
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        // current user's entries are stored under journal_entries/<userId>/entries
        db.collection("journal_entries")
                .document(auth.getUid())
                .collection("entries")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        List<DocumentSnapshot> documents = task.getResult().getDocuments();
                        Collections.reverse(documents); // reversed so most recent is first

                        /*
                            we use a RecyclerView for the list of entries to improve performance.
                            the list of entries could be quite long so RecyclerView is idea in this
                            case as it reuses UI components after they've scrolled off screen.
                         */
                        recyclerView = getView().findViewById(R.id.recyclerView);
                        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

                        dataList = new ArrayList<>();
                        dataList.addAll(documents);

                        adapter = new ViewJournalEntryAdapter(getContext(), dataList);
                        recyclerView.setAdapter(adapter);
                    } else {
                        Log.w(TAG, "Error getting documents.", task.getException());
                    }
                });
    }

    @Override
    public void onResume() {
        super.onResume();
        getEntriesList();
    }
}