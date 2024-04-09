package com.example.emailpasswordauth;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.storage.FileDownloadTask;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import org.w3c.dom.Text;

import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link ViewJournalEntry#newInstance} factory method to
 * create an instance of this fragment.
 */
public class ViewJournalEntry extends Fragment {

    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    private FirebaseAuth auth;

    public ViewJournalEntry() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment ViewJournalEntry.
     */
    // TODO: Rename and change types and number of parameters
    public static ViewJournalEntry newInstance(String param1, String param2) {
        ViewJournalEntry fragment = new ViewJournalEntry();
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

        auth = FirebaseAuth.getInstance();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_view_journal_entry, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        String day = getArguments().getString("day");
        TextView text = view.findViewById(R.id.day);
        LocalDate date = LocalDate.parse(day, DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        DateTimeFormatter long_format = DateTimeFormatter.ofPattern("MMMM d, yyyy");
        text.setText(date.format(long_format));

        String content = getArguments().getString("content");
        TextView contentTextView = view.findViewById(R.id.entry_content);
        contentTextView.setText(content);

        String sentiment = getArguments().getString("sentiment");
        TextView sentimentTextView = view.findViewById(R.id.entry_sentiment);
        sentimentTextView.setText(sentiment);

        String prompt_key = getArguments().getString("prompt_key");
        TextView promptKeyTextView = view.findViewById(R.id.prompt_key);
        promptKeyTextView.setText(Prompts.possiblePrompts.get(prompt_key));

        String prompt_value = getArguments().getString("prompt_value");
        TextView ratingTextView = view.findViewById(R.id.prompt_val);
        ratingTextView.setText(prompt_value + " / 5");


        // show image if present
        FirebaseStorage storage = FirebaseStorage.getInstance();
        StorageReference storageRef = storage.getReference();

        StorageReference imageRef = storageRef.child(auth.getUid() + "/" + day + ".jpg");

        File localFile;
        try {
            localFile = File.createTempFile("images", "jpg");

            File finalLocalFile = localFile;
            imageRef.getFile(localFile).addOnSuccessListener(taskSnapshot -> {
                // Local temp file has been created
                ImageView imgView = view.findViewById(R.id.viewEntryImage);
                Bitmap bmp = BitmapFactory.decodeFile(finalLocalFile.getAbsolutePath());
                ExifInterface exif;
                try {
                    exif = new ExifInterface(finalLocalFile);

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
                    Bitmap rotatedBitmap = Bitmap.createBitmap(bmp, 0, 0, bmp.getWidth(), bmp.getHeight(), matrix, true);
                    imgView.setImageBitmap(rotatedBitmap);

                } catch (IOException e) {
                    imgView.setImageBitmap(bmp);
                }
            }).addOnFailureListener(exception -> Toast.makeText(getContext(), "Unable to load image. " + exception, Toast.LENGTH_SHORT).show());
        } catch (IOException e) {
            Toast.makeText(getContext(), "Unable to load image. " + e, Toast.LENGTH_SHORT).show();
        }

    }
}