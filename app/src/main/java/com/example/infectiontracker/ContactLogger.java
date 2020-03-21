package com.example.infectiontracker;

import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.widget.TextView;

import com.example.infectiontracker.database.Beacon;
import com.example.infectiontracker.viewmodel.ContactLoggerViewModel;

import java.util.List;
import java.util.function.Consumer;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;

public class ContactLogger extends AppCompatActivity {
    private static ContactLogger contactLogger;

    private ContactLoggerViewModel mContactLoggerViewModel;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mContactLoggerViewModel = new ViewModelProvider(this).get(ContactLoggerViewModel.class);

        setContentView(R.layout.contact_logger);
        ((TextView)findViewById(R.id.contactLoggerTextView)).setMovementMethod(new ScrollingMovementMethod());

        //Obviously a hack
        contactLogger = this;

        mContactLoggerViewModel.getAllBeacons().observe(this, new Observer<List<Beacon>>() {
            @Override
            public void onChanged(List<Beacon> beacons) {
                for(Beacon b : beacons) {
                    addContact(b.receivedHash.toString());
                }
            }
        });
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
