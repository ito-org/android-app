package com.example.infectiontracker.ui;

import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.example.infectiontracker.R;
import com.example.infectiontracker.database.InfectedUUID;

import java.util.List;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

public class InfectedUUIDsAdapter extends RecyclerView.Adapter<InfectedUUIDsAdapter.InfectedUUIDsViewHolder> {

    private List<InfectedUUID> infectedUUIDs;

    public static class InfectedUUIDsViewHolder extends RecyclerView.ViewHolder {
        public LinearLayout layout;
        public TextView textView;
        public InfectedUUIDsViewHolder(LinearLayout v) {
            super(v);
            layout = v;
            textView = v.findViewById(R.id.infected_list_text_view);
        }
    }

    @NonNull
    @Override
    public InfectedUUIDsViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LinearLayout l = (LinearLayout) LayoutInflater.from(parent.getContext())
                .inflate(R.layout.infected_encounter_view, parent, false);
        InfectedUUIDsViewHolder vh = new InfectedUUIDsViewHolder(l);
        return vh;
    }

    @Override
    public void onBindViewHolder(@NonNull InfectedUUIDsViewHolder holder, int position) {
        holder.textView.setText("Test " + infectedUUIDs.get(position).icdCode);
    }

    @Override
    public int getItemCount() {
        return infectedUUIDs.size();
    }

    public void setInfectedUUIDs(List<InfectedUUID> uuids) {
        this.infectedUUIDs = uuids;
        notifyDataSetChanged();
    }
}
