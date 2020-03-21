package com.example.infectiontracker;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
/*
This BoardcastReceiver starts the tracing service when the system boots
 */
public class StartupListener extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if(Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction()))
        {
            Intent serviceIntent = new Intent(context, TracingService.class);
            context.startService(serviceIntent);
        }
    }
}
