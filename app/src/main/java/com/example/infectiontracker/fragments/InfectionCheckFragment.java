package com.example.infectiontracker.fragments;

import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProviders;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.example.infectiontracker.R;
import com.example.infectiontracker.database.InfectedUUID;
import com.example.infectiontracker.ui.InfectedUUIDsAdapter;
import com.example.infectiontracker.viewmodel.InfectionCheckViewModel;

import java.util.List;

public class InfectionCheckFragment extends Fragment {

    private InfectionCheckViewModel mViewModel;

    private RecyclerView recyclerView;
    private InfectedUUIDsAdapter mAdapter;
    private RecyclerView.LayoutManager layoutManager;

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
        recyclerView.setHasFixedSize(true);
        layoutManager = new LinearLayoutManager(getActivity());
        recyclerView.setLayoutManager(layoutManager);
        mAdapter = new InfectedUUIDsAdapter();
        recyclerView.setAdapter(mAdapter);
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        mViewModel = ViewModelProviders.of(this).get(InfectionCheckViewModel.class);

        mViewModel.getPossiblyInfectedEncounters().observe(getViewLifecycleOwner(), new Observer<List<InfectedUUID>>() {
            @Override
            public void onChanged(List<InfectedUUID> infectedUUIDS) {
                mAdapter.setInfectedUUIDs(infectedUUIDS);
            }
        });

        mViewModel.refreshInfectedUUIDs();
    }
}
