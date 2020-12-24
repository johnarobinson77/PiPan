/*
 * Copyright 2020 John Robinson.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.example.pano.Sshcom;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;

import com.example.pano.MainActivity;
import com.example.pano.R;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

public class SshcomForegroundService extends Service {

    private static final String CHANNEL_ID = "SshcomServiceChannel";
    private static final int ID = 1;
    private NotificationManagerCompat nmc;
    Notification notification;
    public SshcomForegroundService() {
        super();
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.i("SshcomForegroundService", "Created");
        Sshcom.sshcom.startRunCommandThread();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String input = intent.getStringExtra("inputExtra");
        createNotificationChannel();
        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this,
                0, notificationIntent, 0);
        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle(getString(R.string.notification_ssh_running))
                .setContentText(input)
                .setSmallIcon(R.drawable.ic_stat_name)
                .setContentIntent(pendingIntent)
                .build();
        startForeground(ID, notification);
        nmc = NotificationManagerCompat.from(this);
        //stopSelf();
        return START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {
        nmc.cancel(ID);
        Sshcom.sshcom.getRunCommand().quit();
        super.onDestroy();
        Log.i("SshcomForegroundService", "work Complete");
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void createNotificationChannel() {
        NotificationChannel serviceChannel = new NotificationChannel(
                CHANNEL_ID,
                "Sshcom Foreground Service Channel",
                NotificationManager.IMPORTANCE_DEFAULT
        );
        NotificationManager manager = getSystemService(NotificationManager.class);
        manager.createNotificationChannel(serviceChannel);
    }

    public void updateNotification(String content) {
        //nmc.getNotificationChannel(CHANNEL_ID).
    }
}
