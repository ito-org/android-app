package com.example.infectiontracker;

import android.os.Bundle;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class DataProtectionInfo extends AppCompatActivity {
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.data_protection_info);
        TextView tv = (TextView)findViewById(R.id.dataProtectionContent);
        tv.setText(Html.fromHtml(getString(R.string.dataprotection_content)));
        tv.setMovementMethod(LinkMovementMethod.getInstance());
    }

    public void onOk(View v) {
        //TODO: Save in db that data protection info was shown and okayed
        finish();
    }
}
