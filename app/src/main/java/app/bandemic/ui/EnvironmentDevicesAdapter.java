package app.bandemic.ui;

import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import app.bandemic.R;
import app.bandemic.strict.database.Beacon;

import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

public class EnvironmentDevicesAdapter extends RecyclerView.Adapter<EnvironmentDevicesAdapter.EnvironmentDevicesViewHolder> {

    private List<Beacon> beacons = Collections.emptyList();

    public static class EnvironmentDevicesViewHolder extends RecyclerView.ViewHolder {
        public LinearLayout layout;
        public TextView textViewDate;
        public TextView textViewDistance;

        public EnvironmentDevicesViewHolder(LinearLayout v) {
            super(v);
            layout = v;
            textViewDate = v.findViewById(R.id.nearby_devices_list_text_view_date);
            textViewDistance = v.findViewById(R.id.nearby_devices_list_text_view_distance);
        }
    }

    @NonNull
    @Override
    public EnvironmentDevicesViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LinearLayout l = (LinearLayout) LayoutInflater.from(parent.getContext())
                .inflate(R.layout.nearby_devices_view, parent, false);
        EnvironmentDevicesViewHolder vh = new EnvironmentDevicesViewHolder(l);
        return vh;
    }

    @Override
    public void onBindViewHolder(@NonNull EnvironmentDevicesViewHolder holder, int position) {
        Beacon data = beacons.get(position);

        holder.textViewDate.setText(SimpleDateFormat.getDateTimeInstance().format(data.timestamp));
        holder.textViewDistance.setText(String.format("%.1f m", data.distance));
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
