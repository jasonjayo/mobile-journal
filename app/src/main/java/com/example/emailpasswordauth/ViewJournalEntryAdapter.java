package com.example.emailpasswordauth;

import android.content.Context;
import android.graphics.Paint;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.DocumentSnapshot;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class ViewJournalEntryAdapter extends RecyclerView.Adapter<ViewJournalEntryAdapter.ViewHolder> {

    private final List<DocumentSnapshot> dataList;
    private final Context context;

    public ViewJournalEntryAdapter(Context context, List<DocumentSnapshot> dataList) {
        this.context = context;
        this.dataList = dataList;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.journal_entry_link, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        // get journal entry associated with this list item
        DocumentSnapshot doc = dataList.get(position);

        // name is date
        LocalDate date = LocalDate.parse(doc.getId(), DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        DateTimeFormatter long_format = DateTimeFormatter.ofPattern("MMMM d, yyyy");
        holder.entryName.setText(date.format(long_format));

        // underline
        holder.entryName.setPaintFlags(holder.entryName.getPaintFlags() | Paint.UNDERLINE_TEXT_FLAG);
        // switch to view journal entry fragment on click and pass entry data via bundle
        holder.entryName.setOnClickListener(view -> {
            Bundle bundle = new Bundle();
            bundle.putString("day", doc.getId());
            bundle.putString("content", doc.get("content").toString());
            bundle.putString("prompt_key", doc.get("prompt_key").toString());
            bundle.putString("prompt_value", doc.get("prompt_val").toString());
            bundle.putString("sentiment", doc.get("sentiment").toString());
            Navigation.findNavController(view).navigate(R.id.action_dashboard_to_viewJournalEntry, bundle);
        });
    }

    public int getItemCount() {
        return dataList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        /*
         this class is used to  represent items in the RecyclerView
         each item has an associated instance of this class
        */
        TextView entryName;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            // set the entryName text view for this list item
            entryName = itemView.findViewById(R.id.entryName);
        }
    }
}
