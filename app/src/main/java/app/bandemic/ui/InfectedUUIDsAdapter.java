package app.bandemic.ui;

import android.content.Context;
import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import app.bandemic.R;

import org.itoapp.strict.database.Infection;

import java.util.Collections;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

public class InfectedUUIDsAdapter extends RecyclerView.Adapter<InfectedUUIDsAdapter.InfectedUUIDsViewHolder> {

    private List<Infection> infectedUUIDs = Collections.emptyList();

    public static class InfectedUUIDsViewHolder extends RecyclerView.ViewHolder {
        public LinearLayout layout;
        public TextView textViewDate;
        public TextView textViewDisease;
        public TextView textViewDistance;
        public TextView textViewDanger;

        public InfectedUUIDsViewHolder(LinearLayout v) {
            super(v);
            layout = v;
            textViewDate = v.findViewById(R.id.infected_list_text_view_date);
            textViewDisease = v.findViewById(R.id.infected_list_text_view_disease);
            textViewDistance = v.findViewById(R.id.infected_list_text_view_distance);
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
        Infection infection = infectedUUIDs.get(position);
        Context c = holder.layout.getContext();

        java.text.DateFormat df = DateFormat.getDateFormat(c);
        holder.textViewDate.setText(df.format(infection.encounterDate));
        holder.textViewDisease.setText(infection.icdCode);
        holder.textViewDistance.setText(String.format("%.1f m", infection.distance));
        holder.textViewDanger.setText(infection.distrustLevel == 0 ?
                c.getResources().getString(R.string.verified) :
                c.getResources().getString(R.string.unverified));
    }

    @Override
    public int getItemCount() {
        return infectedUUIDs.size();
    }

    public void setInfectedUUIDs(List<Infection> uuids) {
        this.infectedUUIDs = uuids;
        notifyDataSetChanged();
    }

    public Infection getLastInfectedUUUID() {
        if(!infectedUUIDs.isEmpty()) {
            return infectedUUIDs.get(infectedUUIDs.size()-1);
        }
        return null;
    }
}
