package app.bandemic.ui;

import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import app.bandemic.R;

public class NearbyDevicesAdapter extends RecyclerView.Adapter<NearbyDevicesAdapter.NearbyDevicesViewHolder> {

    private double[] distances = new double[0];

    public static class NearbyDevicesViewHolder extends RecyclerView.ViewHolder {
        public LinearLayout layout;
        TextView textViewDate;
        TextView textViewDistance;

        NearbyDevicesViewHolder(LinearLayout v) {
            super(v);
            layout = v;
            textViewDate = v.findViewById(R.id.nearby_devices_list_text_view_date);
            textViewDistance = v.findViewById(R.id.nearby_devices_list_text_view_distance);
        }
    }

    @NonNull
    @Override
    public NearbyDevicesViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LinearLayout l = (LinearLayout) LayoutInflater.from(parent.getContext())
                .inflate(R.layout.nearby_devices_view, parent, false);
        NearbyDevicesViewHolder vh = new NearbyDevicesViewHolder(l);
        return vh;
    }

    @Override
    public void onBindViewHolder(@NonNull NearbyDevicesViewHolder holder, int position) {
        double distance = distances[position];

        holder.textViewDate.setText("Device");
        holder.textViewDistance.setText(String.format("%.1f m", distance));
    }

    @Override
    public int getItemCount() {
        return distances.length;
    }

    public void setDistances(double[] distances) {
        this.distances = distances;
        notifyDataSetChanged();
    }
}
