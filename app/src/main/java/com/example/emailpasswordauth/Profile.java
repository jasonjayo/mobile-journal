package com.example.emailpasswordauth;

import static android.content.ContentValues.TAG;

import static com.example.emailpasswordauth.Prompts.possiblePrompts;
import static java.lang.Float.isNaN;
import static java.lang.Float.parseFloat;
import static java.lang.Integer.parseInt;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Paint;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import android.text.Html;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.RatingBar;
import android.widget.TextView;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import org.w3c.dom.Text;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link Profile#newInstance} factory method to
 * create an instance of this fragment.
 */
public class Profile extends Fragment {

    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    public Profile() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment Profile.
     */
    // TODO: Rename and change types and number of parameters
    public static Profile newInstance(String param1, String param2) {
        Profile fragment = new Profile();
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

        return inflater.inflate(R.layout.fragment_profile, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        Bundle args = getArguments();
        if (args != null) {
            String userEmail = args.getString("email");
            if (userEmail != null) {
                TextView userEmailTextView = view.findViewById(R.id.emailProfile);
                userEmailTextView.setText(userEmail);
            }
        } else {
            Log.e(TAG, "Arguments are null");
        }

        FirebaseAuth auth = FirebaseAuth.getInstance();
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("journal_entries").document(Objects.requireNonNull(auth.getUid())).collection("entries")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        List<DocumentSnapshot> documents = task.getResult().getDocuments();
                        // Rest of your code to populate UI with Firestore data
                    } else {
                        Log.w(TAG, "Error getting documents.", task.getException());
                    }
                });
        db.collection("journal_entries").document(auth.getUid()).collection("entries")
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            List<DocumentSnapshot> documents = task.getResult().getDocuments();
                            TextView userEntries = getView().findViewById(R.id.entryCounter);
                            LinearLayout layout = getView().findViewById(R.id.ratingsLayout);
                            LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
                                    LinearLayout.LayoutParams.MATCH_PARENT,
                                    LinearLayout.LayoutParams.WRAP_CONTENT
                            );
                            userEntries.setText(String.valueOf(documents.size() + 1) + " Journal Entries");
                            RatingBar avgRating = getView().findViewById(R.id.moodRating);
                            int totalRatings = 0, happyCount = 0, neutralCount = 0, sadCount = 0;
                            //Make indexes for prompts to later make ratingbars.
                            Map<String, Integer> intArrays = new HashMap<>();
                            int v = 0;
                            for (String key : possiblePrompts.keySet()) {
                                intArrays.put(key, v);
                                v++;
                            }
                            //create array for each prompt's totals and set a default value
                            int[][] totalRatingsCombined = new int[possiblePrompts.size()][2];
                            for (int i = 0;
                                 i < totalRatingsCombined.length; i++) {
                                totalRatingsCombined[i][0] = 0; //This will be for the total stars
                                totalRatingsCombined[i][1] = 0; //This will be the total times the prompt is used.
                            }
                            // Process the reversed list of documents
                            for (DocumentSnapshot document : documents) {
                                if (document.get("sentiment") != null) {
                                    switch (document.get("sentiment").toString()) {
                                        case ("HAPPY"):
                                            happyCount++;
                                            break;
                                        case ("NEUTRAL"):
                                            neutralCount++;
                                            break;
                                        default:
                                            sadCount++;
                                            break;
                                    }
                                } else neutralCount++;

                                if (document.get("prompt_val") != null) {
                                    totalRatings += parseInt(document.get("prompt_val").toString());
                                    if (document.get("prompt_key") != null) {
                                        String oldKey = document.get("prompt_key").toString();
                                        if (intArrays.containsKey(oldKey)) {
                                            int key = parseInt(String.valueOf(intArrays.get(oldKey)));
                                            totalRatingsCombined[key][0] += parseInt(document.get("prompt_val").toString());
                                            totalRatingsCombined[key][1]++;

                                        }


                                    }
                                }
                            }
                            DrawMoodAverages((float) happyCount, documents, (float) neutralCount, (float) sadCount);

                            float rating = (float) totalRatings / documents.size();
                            avgRating.setRating(rating);
                            v = 0; //reuse v for iteration
                            for (String i : possiblePrompts.values()) {

                                TextView caption = new TextView(getActivity().getApplicationContext());
                                float soloRating = (parseFloat(String.valueOf(totalRatingsCombined[v][0])) / parseFloat(String.valueOf(totalRatingsCombined[v][1])));
                                if (isNaN(soloRating)) {
                                    soloRating = 0;
                                }
                                caption.setText(Html.fromHtml(i + ":\t\t<br><b>" + Math.round(soloRating * 20) + "%</b><br>"));
                                layout.addView(caption, layoutParams);


                                v++;

                            }

                        } else {
                            Log.w(TAG, "Error getting documents.", task.getException());
                        }
                    }

                    @SuppressLint("SetTextI18n")
                    private void DrawMoodAverages(float happyCount, List<DocumentSnapshot> documents, float neutralCount, float sadCount) {
                        ImageButton sadButton = getView().findViewById(R.id.sentimentSad2);
                        ImageButton neutralButton = getView().findViewById(R.id.sentimentNeutral2);
                        ImageButton happyButton = getView().findViewById(R.id.sentimentHappy2);

                        TextView sadPercent = getView().findViewById(R.id.sadPercent);
                        TextView neutralPercent = getView().findViewById(R.id.neutralPercent);
                        TextView happyPercent = getView().findViewById(R.id.happyPercent);

                        float happy = happyCount / documents.size();
                        float neutral = neutralCount / documents.size();
                        float sad = sadCount / documents.size();

                        happyButton.setAlpha(happy);
                        neutralButton.setAlpha(neutral);
                        sadButton.setAlpha(sad);

                        sadPercent.setText(Math.round(sad * 100) + "%");
                        neutralPercent.setText(Math.round(neutral * 100) + "%");
                        happyPercent.setText(Math.round(happy * 100) + "%");
                    }
                });
    }

}