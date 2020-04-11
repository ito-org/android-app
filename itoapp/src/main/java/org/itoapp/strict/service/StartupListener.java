package org.itoapp.strict.service;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/*
This BroadcastReceiver starts the tracing service when the system boots
 */
public class StartupListener extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        context.startService(new Intent(context, TracingService.class));
    }
}
