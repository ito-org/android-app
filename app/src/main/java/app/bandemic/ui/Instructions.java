package app.bandemic.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import app.bandemic.R;

public class Instructions extends AppCompatActivity {
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.instructions);
    }

    public void onNextClick(View v) {
        startActivity(new Intent(this, DataProtectionInfo.class));
        finish();
    }
}
