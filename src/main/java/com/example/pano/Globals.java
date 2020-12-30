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
package com.example.pano;

import android.content.Context;
import android.net.ConnectivityManager;
import android.os.PowerManager;

import com.example.pano.Sshcom.Sshcom;
import com.example.pano.ui.home.HomeFragment;

public class Globals {
    public static boolean isNetworkConnected = false;
    public static Context context = null;
    public static Sshcom sshcom = null;
    public static HomeFragment homeFragment = null;
    public static PowerManager powerManager = null;

    private static int connectedStatus = 0;
    public static void setStatusConnected(){
        connectedStatus = 2;
    }
    public static void setStatusConnecting(){
        connectedStatus = 1;
    }
    public static void setStatusDisconnected(){
        connectedStatus = 0;
    }
    public static boolean isConnected(){
        return connectedStatus == 2;
    }
    public static boolean isNotConnecting(){ return connectedStatus != 1; }

    private static int panRunStatus = 0;
    public static void setStatusPanRunning(){
        panRunStatus = 2;
    }
    public static void setStatusPanNotRunning(){
        panRunStatus = 0;
    }
    public static boolean isPanRunning(){
        return panRunStatus == 2;
    }

    private static int seqRunStatus = 0;
    public static void setStatusSeqRunning(){
        seqRunStatus = 2;
    }
    public static void setStatusSeqNotRunning(){
        seqRunStatus = 0;
    }
    public static boolean isSeqRunning(){
        return seqRunStatus == 2;
    }

    public static ConnectivityManager cm = null;

    private static int hostSelect = -1;
    public static int getHostSelect() {
        return hostSelect;
    }
    public static void setHostSelect(int hostSelect) {
        Globals.hostSelect = hostSelect;
    }

    public static void reset() {
        sshcom = null;
        context = null;
        homeFragment = null;
        powerManager = null;
        connectedStatus = 0;
        panRunStatus = 0;
        seqRunStatus = 0;
        cm = null;
        hostSelect = -1;
    }
}
