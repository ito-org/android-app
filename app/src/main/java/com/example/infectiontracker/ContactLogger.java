package com.example.infectiontracker;

import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class ContactLogger extends AppCompatActivity {
    private static ContactLogger contactLogger;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.contact_logger);
        ((TextView)findViewById(R.id.contactLoggerTextView)).setMovementMethod(new ScrollingMovementMethod());

        //Obviously a hack
        contactLogger = this;
    }

    public static void addContact(final String contactName) {
        if(contactLogger==null) {
            return;
        }

        contactLogger.runOnUiThread( () -> {
            contactLogger.addNewContact(contactName);
        });
    }

    private void addNewContact(String contactName) {
        TextView tv = (TextView)findViewById(R.id.contactLoggerTextView);
        tv.append("\n"+contactName);
    }
}
