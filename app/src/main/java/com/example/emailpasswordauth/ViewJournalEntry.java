package com.example.emailpasswordauth;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.os.AsyncTask;
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
 */
public class ViewJournalEntry extends Fragment {

    private FirebaseAuth auth;

    public ViewJournalEntry() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
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

        // get info passed as bundle from Dashboard
        // display title as date of this journal
        String day = getArguments().getString("day");
        TextView title = view.findViewById(R.id.day);
        LocalDate date = LocalDate.parse(day, DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        DateTimeFormatter long_format = DateTimeFormatter.ofPattern("MMMM d, yyyy");
        title.setText(date.format(long_format));

        // display content
        String content = getArguments().getString("content");
        TextView contentTextView = view.findViewById(R.id.entry_content);
        contentTextView.setText(content);

        String sentiment = getArguments().getString("sentiment");

        // show only the relevant sentiment, hide others
        final float inactiveMoodBtnOpacity = 0f;

        ImageView NeutralImage = view.findViewById(R.id.Neutral);
        ImageView HappyImage = view.findViewById(R.id.Happy);
        ImageView SadImage = view.findViewById(R.id.Sad);

        if ("HAPPY".equals(sentiment)) {
            HappyImage.setAlpha(1f);
            NeutralImage.setAlpha(inactiveMoodBtnOpacity);
            SadImage.setAlpha(inactiveMoodBtnOpacity);
        }
        if ("NEUTRAL".equals(sentiment)) {
            HappyImage.setAlpha(inactiveMoodBtnOpacity);
            NeutralImage.setAlpha(1f);
            SadImage.setAlpha(inactiveMoodBtnOpacity);
        }
        if ("SAD".equals(sentiment)) {
            HappyImage.setAlpha(inactiveMoodBtnOpacity);
            NeutralImage.setAlpha(inactiveMoodBtnOpacity);
            SadImage.setAlpha(1f);
        }

        // show the prompt key (question) and value (answer)
        String prompt_key = getArguments().getString("prompt_key");
        TextView promptKeyTextView = view.findViewById(R.id.prompt_key);
        promptKeyTextView.setText(Prompts.possiblePrompts.get(prompt_key));

        String prompt_value = getArguments().getString("prompt_value");
        TextView ratingTextView = view.findViewById(R.id.prompt_val);
        ratingTextView.setText(prompt_value + " / 5");

        // show image if present
        FirebaseStorage storage = FirebaseStorage.getInstance();
        StorageReference storageRef = storage.getReference();
        // get reference to the image
        StorageReference imageRef = storageRef.child(auth.getUid() + "/" + day + ".jpg");
        File localFile;
        try {
            // create temp local file to hold image when we download
            localFile = File.createTempFile("images", "jpg");

            File finalLocalFile = localFile;
            // load image - if this entry has no image, the catch block deals with this
            imageRef.getFile(localFile).addOnSuccessListener(taskSnapshot -> {
                ImageView imgView = view.findViewById(R.id.viewEntryImage);
                /*
                to display image, we need to decode the downloaded file into a bitmap, rotate it &
                then resize it. this is computationally expensive so could temporarily freeze
                the app's UI while it happens. to prevent this, we're using threading/async task
                to offload these processes to another thread. once all is done, the result will
                be shown in the imgView ImageView, all without freezing the app's UI
                */
                new ProcessImage(imgView, finalLocalFile).execute();
            }).addOnFailureListener(e -> {
                // no image exists for this entry - this is ok
            });
        } catch (IOException e) {
            // unable to create temp file for image
            Toast.makeText(getContext(), "Unable to display image. " + e, Toast.LENGTH_SHORT).show();
        }
    }

    private class ProcessImage extends AsyncTask<String, Void, Bitmap> {
        // String is the params input to doInBackground, not used in our case
        // Void is any output produced during doInBackground, nothing in our case
        // Bitmap is the final output from doInBackground

        private ImageView imgView;
        private File localFile;

        public ProcessImage(ImageView imgView, File localFile) {
            this.imgView = imgView;
            this.localFile = localFile;
        }

        // this method does the heavy lifting - i.e., all the image processing
        protected Bitmap doInBackground(String... params) {
            // Local temp file has been created, decode image from Firebase storage into it
            Bitmap bmp = BitmapFactory.decodeFile(localFile.getAbsolutePath());
            bmp = Bitmap.createScaledBitmap(bmp, bmp.getWidth() / 2, bmp.getHeight() / 2, false);
            ExifInterface exif;
            try {
                /*
                 sometimes images doesn't appear in correct orientation so we get EXIF
                 orientation info and then manually rotate
                 */
                // get exif orientation info
                exif = new ExifInterface(localFile);
                int orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);

                // rotate manually
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
                return Bitmap.createBitmap(bmp, 0, 0, bmp.getWidth(), bmp.getHeight(), matrix, true);

            } catch (IOException e) {
                // if rotation fails for some reason, just show image as is
                return bmp;
            }
        }

        public void onPostExecute(Bitmap bmp) {
            imgView.setImageBitmap(bmp);
        }
    }
}