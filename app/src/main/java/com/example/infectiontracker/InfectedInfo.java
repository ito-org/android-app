package com.example.infectiontracker;

import android.content.Intent;
import android.os.Bundle;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class InfectedInfo extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.infected_info);
        TextView tv = (TextView)findViewById(R.id.infectedInfoLink);
        tv.setText(Html.fromHtml(getString(R.string.infected_info_link)));
        tv.setMovementMethod(LinkMovementMethod.getInstance());
    }
}
