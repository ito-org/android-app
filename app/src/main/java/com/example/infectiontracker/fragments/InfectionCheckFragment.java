package com.example.infectiontracker.fragments;

import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProviders;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.example.infectiontracker.R;
import com.example.infectiontracker.database.InfectedUUID;
import com.example.infectiontracker.viewmodel.InfectionCheckViewModel;

import java.util.List;

public class InfectionCheckFragment extends Fragment {

    private InfectionCheckViewModel mViewModel;

    public static InfectionCheckFragment newInstance() {
        return new InfectionCheckFragment();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.infection_check_fragment, container, false);
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mViewModel = ViewModelProviders.of(this).get(InfectionCheckViewModel.class);

        mViewModel.getPossiblyInfectedEncounters().observe(getViewLifecycleOwner(), new Observer<List<InfectedUUID>>() {
            @Override
            public void onChanged(List<InfectedUUID> infectedUUIDS) {
                for(InfectedUUID infectedUUID : infectedUUIDS) {
                    Log.wtf("Test", infectedUUID.toString());
                }
            }
        });

        mViewModel.refreshInfectedUUIDs();
    }

}
