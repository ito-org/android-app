package app.bandemic.fragments;

import androidx.lifecycle.ViewModelProviders;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import androidx.cardview.widget.CardView;

import app.bandemic.R;
import app.bandemic.ui.InfectedUUIDsAdapter;
import app.bandemic.viewmodel.InfectionCheckViewModel;
import app.bandemic.viewmodel.MainActivityViewModel;

public class InfectionCheckFragment extends Fragment {

    private InfectionCheckViewModel mViewModel;
    private MainActivityViewModel mainActivityViewModel;

    private RecyclerView recyclerView;
    private InfectedUUIDsAdapter mAdapter;
    private RecyclerView.LayoutManager layoutManager;
    private LinearLayout noInfectionInformation;
    private CardView cardView;

    public static InfectionCheckFragment newInstance() {
        return new InfectionCheckFragment();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.infection_check_fragment, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        recyclerView = view.findViewById(R.id.infection_check_list_recycler_view);
        noInfectionInformation = view.findViewById(R.id.layout_not_infected1);
        recyclerView.setHasFixedSize(true);
        layoutManager = new LinearLayoutManager(getActivity());
        recyclerView.setLayoutManager(layoutManager);
        mAdapter = new InfectedUUIDsAdapter();
        recyclerView.setAdapter(mAdapter);
        cardView = view.findViewById(R.id.infectionCheckFragment);
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        mViewModel = ViewModelProviders.of(this).get(InfectionCheckViewModel.class);
        mainActivityViewModel = ViewModelProviders.of(getActivity()).get(MainActivityViewModel.class);

        mainActivityViewModel.eventRefresh().observe(getViewLifecycleOwner(), refreshing -> {
            if(refreshing) {
                mViewModel.refreshInfectedUUIDs();
            }
        });

        mViewModel.getPossiblyInfectedEncounters().observe(getViewLifecycleOwner(), infectedUUIDS -> {
            mainActivityViewModel.finishRefresh();
            if(infectedUUIDS.size() != 0) {
                mAdapter.setInfectedUUIDs(infectedUUIDS);
                noInfectionInformation.setVisibility(View.GONE);
                recyclerView.setVisibility(View.VISIBLE);
                cardView.setBackgroundColor(getResources().getColor(R.color.colorDanger));
                getView().setOnClickListener(v -> {
                    //onInfectionClick(v);
                });
            }
            else {
                noInfectionInformation.setVisibility(View.VISIBLE);
                recyclerView.setVisibility(View.GONE);
                getView().setOnClickListener(null);
                cardView.setBackgroundColor(getResources().getColor(R.color.colorNoDanger));
            }
        });

        mViewModel.refreshInfectedUUIDs();
    }
}
