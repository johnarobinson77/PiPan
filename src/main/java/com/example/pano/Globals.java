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

import android.net.ConnectivityManager;

public class Globals {
    public static Sshcom sshcom = null;
    public static boolean isNetworkConnected;

    private static int connectedStatus = 0;
    public static void setStatusConnected(){
        Globals.connectedStatus = 2;
    }
    public static void setStatusConnecting(){
        Globals.connectedStatus = 1;
    }
    public static void setStatusDisconnected(){
        Globals.connectedStatus = 0;
    }
    public static boolean isConnected(){
        return connectedStatus == 2;
    }
    public static boolean isNotConnecting(){ return connectedStatus != 1; }

    private static int runStatus = 0;
    public static void setStatusRunning(){
        Globals.runStatus = 2;
    }
    public static void setStatusNotRunning(){
        Globals.runStatus = 0;
    }
    public static boolean isRunning(){
        return runStatus == 2;
    }

    public static ConnectivityManager cm = null;

    private static int hostSelect = -1;
    public static int getHostSelect() {
        return hostSelect;
    }
    public static void setHostSelect(int hostSelect) {
        Globals.hostSelect = hostSelect;
    }


}
