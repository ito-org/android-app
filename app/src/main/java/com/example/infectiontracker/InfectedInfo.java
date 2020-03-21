package com.example.infectiontracker;

import android.content.Intent;
import android.os.Bundle;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class InfectedInfo extends AppCompatActivity {
    public static final String INTENT_CONTACT_TIME = "contact_time";
    public static final String INTENT_CONTACT_DURATION = "contact_duration";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.infected_info);
        TextView tv = (TextView)findViewById(R.id.infectedInfoLink);
        tv.setText(Html.fromHtml(getString(R.string.infected_info_link)));
        tv.setMovementMethod(LinkMovementMethod.getInstance());

        Intent intent = getIntent();

        ((TextView)findViewById(R.id.approxContactTime)).setText(intent.getStringExtra(INTENT_CONTACT_TIME) );
        ((TextView)findViewById(R.id.approxContactDuration)).setText(intent.getStringExtra(INTENT_CONTACT_DURATION) );
    }
}
