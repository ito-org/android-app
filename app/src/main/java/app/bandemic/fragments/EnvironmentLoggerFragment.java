package app.bandemic.fragments;


import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProviders;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.cardview.widget.CardView;
import android.widget.LinearLayout;

import app.bandemic.R;
import org.itoapp.strict.database.Beacon;
import app.bandemic.ui.EnvironmentDevicesAdapter;
import app.bandemic.viewmodel.EnvironmentLoggerViewModel;

import java.util.List;

public class EnvironmentLoggerFragment extends Fragment {

    private EnvironmentLoggerViewModel mViewModel;

    private RecyclerView recyclerView;
    private EnvironmentDevicesAdapter mAdapter;
    private RecyclerView.LayoutManager layoutManager;
    private LinearLayout noInfectionInformation;
    private CardView cardView;


    public static EnvironmentLoggerFragment newInstance() {
        return new EnvironmentLoggerFragment();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.environment_logger_fragment, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        recyclerView = view.findViewById(R.id.environment_logger_list_recycler_view);
        noInfectionInformation = view.findViewById(R.id.layout_no_detections);
        recyclerView.setHasFixedSize(true);
        layoutManager = new LinearLayoutManager(getActivity());
        recyclerView.setLayoutManager(layoutManager);
        mAdapter = new EnvironmentDevicesAdapter();
        recyclerView.setAdapter(mAdapter);
        cardView = view.findViewById(R.id.environmentCard);
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        //View cardLayout = cardView.findViewById(R.id.environmentCard);
        mViewModel = ViewModelProviders.of(this).get(EnvironmentLoggerViewModel.class);
        cardView.setCardBackgroundColor(getResources().getColor(R.color.colorNoDanger));

        mViewModel.getDistinctBeacons().observe(getViewLifecycleOwner(), new Observer<List<Beacon>>() {
            @Override
            public void onChanged(List<Beacon> beacons) {
                if(beacons.size() != 0) {
                    mAdapter.setBeacons(beacons);
                    noInfectionInformation.setVisibility(View.GONE);
                    recyclerView.setVisibility(View.VISIBLE);
                    if (beacons.size() >= 1) { //todo change to 5 (1 only for testing)
                        cardView.setCardBackgroundColor(getResources().getColor(R.color.colorDanger));
                    } else if (beacons.size() > 10) {
                        cardView.setCardBackgroundColor(getResources().getColor(R.color.colorRealDanger));
                    }
                }
                else {
                    noInfectionInformation.setVisibility(View.VISIBLE);
                    recyclerView.setVisibility(View.GONE);
                    cardView.setCardBackgroundColor(getResources().getColor(R.color.colorNoDanger));

                }
            }
        });
    }
}
