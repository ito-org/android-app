package app.bandemic.ui;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.PreferenceManager;
import app.bandemic.R;

public class DataProtectionInfo extends AppCompatActivity {
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.data_protection_info);
        TextView tv = findViewById(R.id.dataProtectionContent);
        tv.setText(Html.fromHtml(getString(R.string.dataprotection_content)));
        tv.setMovementMethod(LinkMovementMethod.getInstance());
    }

    public void onOk(View v) {
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putBoolean(MainActivity.PREFERENCE_DATA_OK, true);
        editor.apply();
        finish();
    }
}
