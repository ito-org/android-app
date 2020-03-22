package com.example.infectiontracker.ui;

import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.example.infectiontracker.R;
import com.example.infectiontracker.database.Beacon;

import java.util.Collections;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

public class EnvironmentDevicesAdapter extends RecyclerView.Adapter<EnvironmentDevicesAdapter.EnvironmentDevicesViewHolder> {

    private List<Beacon> beacons = Collections.emptyList();

    public static class EnvironmentDevicesViewHolder extends RecyclerView.ViewHolder {
        public LinearLayout layout;
        public TextView textViewDate;
        public TextView textViewDisease;
        public TextView textViewDanger;

        public EnvironmentDevicesViewHolder(LinearLayout v) {
            super(v);
            layout = v;
            textViewDate = v.findViewById(R.id.infected_list_text_view_date);
            textViewDisease = v.findViewById(R.id.infected_list_text_view_disease);
            textViewDanger = v.findViewById(R.id.infected_list_text_view_danger);
        }
    }

    @NonNull
    @Override
    public EnvironmentDevicesViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LinearLayout l = (LinearLayout) LayoutInflater.from(parent.getContext())
                .inflate(R.layout.infected_encounter_view, parent, false);
        EnvironmentDevicesViewHolder vh = new EnvironmentDevicesViewHolder(l);
        return vh;
    }

    @Override
    public void onBindViewHolder(@NonNull EnvironmentDevicesViewHolder holder, int position) {
        Beacon data = beacons.get(position);

        java.text.DateFormat df = DateFormat.getDateFormat(holder.layout.getContext());
        // TODO: change to actual date from Beacons Table
        holder.textViewDate.setText(df.format(data.timestamp));
        holder.textViewDisease.setText(data.distance);
        // TODO: this isn't actually the danger yet
        // TODO: add nearest distance
    }

    @Override
    public int getItemCount() {
        return beacons.size();
    }

    public void setBeacons(List<Beacon> beacons) {
        this.beacons = beacons;
        notifyDataSetChanged();
    }
}
