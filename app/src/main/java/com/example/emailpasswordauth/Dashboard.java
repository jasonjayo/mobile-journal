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
 * Use the {@link Dashboard#newInstance} factory method to
 * create an instance of this fragment.
 */
public class Dashboard extends Fragment {

    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    private FirebaseAuth auth;

    ViewJournalEntryAdapter adapter;

    private RecyclerView recyclerView;

    private List<DocumentSnapshot> dataList;

    public Dashboard() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment Dashboard.
     */
    // TODO: Rename and change types and number of parameters
    public static Dashboard newInstance(String param1, String param2) {
        Dashboard fragment = new Dashboard();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_PARAM1);
            mParam2 = getArguments().getString(ARG_PARAM2);
        }
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

        TextView userEmail = getView().findViewById(R.id.userEmail);
        auth = FirebaseAuth.getInstance();
        userEmail.setText(auth.getCurrentUser().getEmail());
        userEmail.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Bundle bundle = new Bundle();
                bundle.putString("email", auth.getCurrentUser().getEmail());
                Navigation.findNavController(view).navigate(R.id.action_dashboard_to_profile, bundle);
            }
        });
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        Button new_entry_btn = getView().findViewById(R.id.new_entry_btn);
        new_entry_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(getActivity(), CreateJournalEntry.class));
            }
        });

        db.collection("journal_entries").document(auth.getUid()).collection("entries")
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            List<DocumentSnapshot> documents = task.getResult().getDocuments();
                            Collections.reverse(documents); // reversed so most recent first

                            recyclerView = getView().findViewById(R.id.recyclerView);
                            recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

                            dataList = new ArrayList<>();
                            dataList.addAll(documents);

                            adapter = new ViewJournalEntryAdapter(getContext(), dataList);
                            recyclerView.setAdapter(adapter);
                        } else {
                            Log.w(TAG, "Error getting documents.", task.getException());
                        }
                    }
                });

        // send test notification
//        Button notif_btn = getView().findViewById(R.id.notif_btn);
//        notif_btn.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                Intent intent = new Intent(getContext(), Dashboard.class);
//                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
//                PendingIntent pendingIntent = PendingIntent.getActivity(getContext(), 0, intent, PendingIntent.FLAG_IMMUTABLE);
//
//                // intent here does nothing
//
//                NotificationCompat.Builder builder = new NotificationCompat.Builder(getContext(), "MY_CHANNEL")
//                        .setSmallIcon(R.drawable.baseline_sentiment_satisfied_alt_24)
//                        .setContentTitle("Test Notification")
//                        .setContentText("Time to rate")
//                        .setPriority(NotificationCompat.PRIORITY_DEFAULT)
//                        // Set the intent that fires when the user taps the notification.
//                        .setContentIntent(pendingIntent)
//                        .setAutoCancel(true);
//
//                NotificationManagerCompat notificationManager = NotificationManagerCompat.from(getContext());
//                if (ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
//                    return;
//                }
//
//                // notificationId is a unique int for each notification that you must define.
//                notificationManager.notify(123, builder.build());
//
//            }
//        });

    }

    @Override
    public void onResume() {
        super.onResume();
    }
}