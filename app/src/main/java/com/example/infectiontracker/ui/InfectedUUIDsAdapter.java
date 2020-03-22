package com.example.infectiontracker.ui;

import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.example.infectiontracker.R;
import com.example.infectiontracker.database.InfectedUUID;

import java.util.Collections;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

public class InfectedUUIDsAdapter extends RecyclerView.Adapter<InfectedUUIDsAdapter.InfectedUUIDsViewHolder> {

    private List<InfectedUUID> infectedUUIDs = Collections.emptyList();

    public static class InfectedUUIDsViewHolder extends RecyclerView.ViewHolder {
        public LinearLayout layout;
        public TextView textViewDate;
        public TextView textViewDisease;
        public TextView textViewDanger;

        public InfectedUUIDsViewHolder(LinearLayout v) {
            super(v);
            layout = v;
            textViewDate = v.findViewById(R.id.infected_list_text_view_date);
            textViewDisease = v.findViewById(R.id.infected_list_text_view_disease);
            textViewDanger = v.findViewById(R.id.infected_list_text_view_danger);
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
        InfectedUUID data = infectedUUIDs.get(position);

        java.text.DateFormat df = DateFormat.getDateFormat(holder.layout.getContext());
        // TODO: change to actual date from Beacons Table
        holder.textViewDate.setText(df.format(data.createdOn));
        holder.textViewDisease.setText(data.icdCode);
        // TODO: this isn't actually the danger yet
        holder.textViewDanger.setText(Integer.toString(data.distrustLevel));
        // TODO: add nearest distance
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
