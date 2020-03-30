package app.bandemic.fragments;


import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import app.bandemic.R;
import app.bandemic.ui.NearbyDevicesAdapter;

public class NearbyDevicesFragment extends Fragment {

    private RecyclerView recyclerView;
    private NearbyDevicesAdapter mAdapter;
    private LinearLayout noInfectionInformation;
    private CardView cardView;


    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.nearby_devices_fragment, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        recyclerView = view.findViewById(R.id.environment_logger_list_recycler_view);
        noInfectionInformation = view.findViewById(R.id.layout_no_detections);
        recyclerView.setHasFixedSize(true);
        RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(getActivity());
        recyclerView.setLayoutManager(layoutManager);
        mAdapter = new NearbyDevicesAdapter();
        recyclerView.setAdapter(mAdapter);
        cardView = view.findViewById(R.id.environmentCard);
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        cardView.setCardBackgroundColor(getResources().getColor(R.color.colorNoDanger));
    }

    @Override
    public void onResume() {
        super.onResume();
        LocalBroadcastManager.getInstance(this.getContext()).registerReceiver(nearbyDevicesBroadcastReceiver,
                new IntentFilter("nearby-devices"));
    }

    private BroadcastReceiver nearbyDevicesBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            double[] distances = intent.getDoubleArrayExtra("distances");

            if(distances.length != 0) {
                mAdapter.setDistances(distances);
                noInfectionInformation.setVisibility(View.GONE);
                recyclerView.setVisibility(View.VISIBLE);
                cardView.setCardBackgroundColor(getResources().getColor(R.color.colorDanger));
            }
            else {
                noInfectionInformation.setVisibility(View.VISIBLE);
                recyclerView.setVisibility(View.GONE);
                cardView.setCardBackgroundColor(getResources().getColor(R.color.colorNoDanger));

            }
        }
    };

    @Override
    public void onPause() {
        LocalBroadcastManager.getInstance(this.getContext())
                .unregisterReceiver(nearbyDevicesBroadcastReceiver);
        super.onPause();
    }
}
