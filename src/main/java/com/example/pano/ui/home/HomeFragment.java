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
package com.example.pano.ui.home;

import android.content.Context;
import android.content.Intent;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.example.pano.Globals;
import com.example.pano.PanInterface.PanCom;
import com.example.pano.R;
import com.example.pano.Sshcom.Sshcom;
import com.example.pano.ui.notifications.NotificationsViewModel;

import java.io.IOException;
import java.net.InetAddress;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

public class HomeFragment extends Fragment {

    private Button testButton;
    private Button connectButton;
    private Button disconnectButton;
    private Button startPanButton;
    private Button copyLogButton;
    private Button ctrlCButton;
    private Button shutdownButton;
    private RadioGroup hostSelectButton;


    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        HomeViewModel homeViewModel = new ViewModelProvider(this).get(HomeViewModel.class);
        View root = inflater.inflate(R.layout.fragment_home, container, false);
        final TextView textView = root.findViewById(R.id.text_home);
        homeViewModel.getText().observe(getViewLifecycleOwner(), textView::setText);
        Globals.homeFragment = this;

        //set up all the buttons
        testButton = root.findViewById(R.id.test_button);
        testButton.setOnClickListener(v -> {
            patchIP();
            Globals.sshcom.test(Globals.getHostSelect());
        });
        ctrlCButton = root.findViewById(R.id.ctrl_c_button);
        ctrlCButton.setOnClickListener(v -> ctrlC());

        connectButton = root.findViewById(R.id.connect_button);
        connectButton.setOnClickListener(v -> connectAndStart());
        disconnectButton = root.findViewById(R.id.disconnect_button);
        disconnectButton.setOnClickListener(v -> disconnect());

        startPanButton = root.findViewById(R.id.start_pan_button);
        startPanButton.setOnClickListener(v -> PanCom.startPan());
        copyLogButton = root.findViewById(R.id.copy_log_button);
        copyLogButton.setOnClickListener(v -> openFileForWrite(PanCom.logFilename));

        Button hotspotButton = root.findViewById(R.id.hotspot_button);
        hotspotButton.setEnabled(true);
        hotspotButton.setOnClickListener(v -> dumpLocalIpAddresses());
        shutdownButton = root.findViewById(R.id.shutdown_button);
        shutdownButton.setOnClickListener(v -> shutdown());

        hostSelectButton = root.findViewById(R.id.host_radio_group);
        hostSelectButton.setOnCheckedChangeListener((group, checkedId) -> manageHostRadioButtons(checkedId));

        updateButtons();
        if ( Globals.powerManager.isPowerSaveMode()) {
            HomeViewModel.updateBanner("Battery Saver is on");
        }
        else {
            HomeViewModel.updateBanner("Battery Saver is off");
        }
        return root;
    }

    @Override
    protected void finalize() throws Throwable {
        Globals.homeFragment = null;
        super.finalize();
    }

    public void updateButtons() {
        int hostSelect = Globals.getHostSelect();
        if (hostSelect < 0){
            testButton.setEnabled(false);
            connectButton.setEnabled(false);
            disconnectButton.setEnabled(false);
            ctrlCButton.setEnabled(false);
            startPanButton.setEnabled(false);
            copyLogButton.setEnabled(false);
            shutdownButton.setEnabled(false);
            hostSelectButton.clearCheck();
        } else {
            testButton.setEnabled(!Globals.isConnected() && Globals.isNotConnecting());
            connectButton.setEnabled(!Globals.isConnected() && Globals.isNotConnecting());
            disconnectButton.setEnabled(Globals.isConnected());
            ctrlCButton.setEnabled(Globals.isConnected());
            startPanButton.setEnabled(Globals.isConnected() && !Globals.isPanRunning());
            copyLogButton.setEnabled(Globals.isConnected() && !Globals.isPanRunning());
            shutdownButton.setEnabled(Globals.isConnected() && !Globals.isPanRunning());
            if (Globals.getHostSelect() == 0) hostSelectButton.check(R.id.radio_host1);
            else if (Globals.getHostSelect() == 1) hostSelectButton.check(R.id.radio_host2);
        }
    }

    public void connectAndStart() {
        patchIP();
        // start the ssh thread
        Globals.sshcom.setCurrentHost(Globals.getHostSelect());
        Globals.setStatusConnecting();
        updateButtons();
        // start the login using onComplete at step 0
        sysCallback.onComplete(0, Sshcom.ReturnStatus.NULL, null);
    }

    private void patchIP() {
        String subIP = Globals.sshcom.getBaseIP(Globals.getHostSelect());
        if (subIP.contains("*")) {
            String[] tmp1s = subIP.split("\\.");
            String   tmp2 = getDeviceIpMobileData(tmp1s[0]+"."+tmp1s[1]);
            if (tmp2 == null) {
                Toast.makeText(getActivity(), "Local IP not matched" , Toast.LENGTH_SHORT).show();
                NotificationsViewModel.append("Local IP not matched" + "\n");
                return;
            }
            String[] tmp2s = tmp2.split("\\.");
            String tmp3 = tmp2s[0] + "." + tmp2s[1] + "." + tmp2s[2] + "." + tmp1s[3];
            Globals.sshcom.setIP(tmp3, Globals.getHostSelect());
            Toast.makeText(getActivity(), "Using host IP " + tmp3 , Toast.LENGTH_SHORT).show();
            NotificationsViewModel.append("Using host IP " + tmp3 + "\n");
        }
    }

    public void disconnect() {
        // start the disconnect at step 3
        sysCallback.onComplete(3, Sshcom.ReturnStatus.NULL, "");
    }
    public void shutdown() {
        // start the shutdown at step 5
        sysCallback.onComplete(5, Sshcom.ReturnStatus.NULL, "");
    }

    private static final int CREATE_FILE = 1;

    // handles the copy log function by launching a startActivity to open a file for write
    // the copy log function is started in the onActivityResult function below.
    public void openFileForWrite(String filename) {
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("text/plain");
        intent.putExtra(Intent.EXTRA_TITLE, filename);
        startActivityForResult(intent, CREATE_FILE);
    }

    // handles the hotspot button
    private void dumpLocalIpAddresses() {
        // implement hotspot stuff
        Thread tmp = new Thread(() -> {
            try {

                String iNet = getDeviceIpMobileData("192.168");
                if (iNet == null) iNet = getDeviceIpMobileData("10.0");
                if (iNet == null) iNet = getDeviceIpMobileData("171.16");
                if (iNet == null) {
                    NotificationsViewModel.append("No known IP address range.");
                }
                Context context = getContext();

                if (context != null) {

                    NetworkInfo activeNetwork = Globals.cm.getActiveNetworkInfo();
                    WifiManager wm = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
                    WifiInfo wifiInfo = wm.getConnectionInfo();

                    int ipAddress = wifiInfo.getIpAddress();
                    int b0 = (ipAddress >> 24) & 0xFF;
                    int b1 = (ipAddress >> 16) & 0xFF;
                    int b2 = (ipAddress >> 8) & 0xFF;
                    int b3 = (ipAddress     ) & 0xFF;
                    String ipString = b0 + "." + b1 + "." +
                            b2 + "." + b3;

                    System.out.println("activeNetwork: " + activeNetwork);
                    System.out.println("ipString: " + ipString);
                    NotificationsViewModel.append("activeNetwork: " + activeNetwork + "\n");
                    NotificationsViewModel.append("ipString: " + ipString + "\n");

                    String prefix = iNet != null ? iNet.substring(0, iNet.lastIndexOf('.')) : null;
                    System.out.println("prefix: " + prefix);
                    NotificationsViewModel.append("starting wifi scan with prefix:" + prefix + "\n");

                    for (int i = 0; i < 256; i++) {
                        String testIp = prefix + i;

                        InetAddress address = InetAddress.getByName(testIp);
                        boolean reachable = address.isReachable(100);
                        String hostName = address.getCanonicalHostName();

                        if (reachable) {
                            NotificationsViewModel.append("Host: " + hostName + "(" + testIp + ") is reachable!\n");
                            System.out.println("Host: " + hostName + "(" + testIp + ") is reachable!");
                        }
                    }
                    NotificationsViewModel.append("done");
                    System.out.println("done");
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
        tmp.start();
    }

    private void ctrlC() {
        // send unsolicited ctrlD
        Sshcom.sendUnsolicitedCommand("\u0003");
        // kill waiting for current responce
        Sshcom.endWaitForResponse();
        //then send a return and wait for the cli prompt
        sysCallback.onComplete(7, Sshcom.ReturnStatus.NULL, null);
    }

    private void manageHostRadioButtons(int checkedId) {
        if (checkedId == R.id.radio_host1) {
            Globals.setHostSelect(0);
            updateButtons();
        }
        else if (checkedId == R.id.radio_host2) {
            Globals.setHostSelect(1);
            updateButtons();
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        // accept the result from the startActivity in openFileForWrite() above
        if (requestCode == CREATE_FILE && data != null) {
            Toast.makeText(this.getActivity(), "Document successfully created", Toast.LENGTH_SHORT).show();
            try {
                //Uri documentUri = data.getData();
                ParcelFileDescriptor pfd = requireActivity().getContentResolver().
                        openFileDescriptor(data.getData(), "w");
                PanCom.copyLog(getActivity(), pfd);
             } catch (Exception e) {
                e.printStackTrace();
                Toast.makeText(getActivity(), "Document not written", Toast.LENGTH_SHORT).show();
            }
        }
    }

    // This callback handles the returns from host commands sent to Sshcom for several of the
    // buttons.  Includes connect (tag 0) disconnect (tag 3) and shutdown (tag 5)
    private final Sshcom.CommandResultCallback sysCallback = new Sshcom.CommandResultCallback() {
        @Override
        public void onComplete(int tag, Sshcom.ReturnStatus returnStatus, String result) {
            if (result != null) NotificationsViewModel.append(result);
            if (returnStatus == Sshcom.ReturnStatus.STILL_RUNNING ||
                    returnStatus == Sshcom.ReturnStatus.CHANNEL_OPENED)
                return;

            if (returnStatus == Sshcom.ReturnStatus.ERROR) {
                Globals.setStatusDisconnected();
                updateButtons();
                return;
            }
            switch (tag) {
                case 0:
                    Globals.sshcom.sendRunCommandMsg(Sshcom.RunCommandType.OPEN_CHANNEL, tag + 1,
                            "", Globals.sshcom.getCliPrompt(Globals.getHostSelect()), 5, sysCallback);
                    break;
                case 1:
                    Globals.sshcom.sendRunCommandMsg(Sshcom.RunCommandType.RUN_COMMAND, tag + 1,
                            "cd " + Globals.sshcom.getDirectory(Globals.getHostSelect()),
                            Globals.sshcom.getCliPrompt(Globals.getHostSelect()), 5, sysCallback);
                    break;
                case 2:
                    Globals.setStatusConnected();
                    updateButtons();
                    break;
                case 3:
                    Globals.sshcom.sendRunCommandMsg(Sshcom.RunCommandType.RUN_COMMAND, 4,
                            "exit", null, 5, sysCallback);
                    break;
                case 4:
                    Globals.sshcom.sendRunCommandMsg(Sshcom.RunCommandType.CLOSE_CHANNEL, 6,
                            null, null, null, sysCallback);
                    break;
                case 5:
                    Globals.sshcom.sendRunCommandMsg(Sshcom.RunCommandType.RUN_COMMAND, 4,
                            "sudo shutdown now", null, null, sysCallback);
                    break;
                case 6:
                    Globals.setStatusDisconnected();
                    updateButtons();
                    break;
                case 7:
                    Globals.sshcom.sendRunCommandMsg(Sshcom.RunCommandType.RUN_COMMAND, 8,
                            "", Globals.sshcom.getCliPrompt(Globals.getHostSelect()), 5, sysCallback);
                    Globals.setStatusPanNotRunning();
                    updateButtons();
                    break;
                case 8:
                    break;
                default:
                    if (result != null) NotificationsViewModel.append(result);
            }
        }
    };



    // this function gets the current IP address for the phones hotspot
   public String getDeviceIpMobileData(String localIP){
        String iNet = null;
        try {
            for (java.util.Enumeration<java.net.NetworkInterface> en = java.net.NetworkInterface.getNetworkInterfaces(); en.hasMoreElements();) {
                java.net.NetworkInterface networkinterface = en.nextElement();
                for (java.util.Enumeration<java.net.InetAddress> enumIpAddr = networkinterface.getInetAddresses(); enumIpAddr.hasMoreElements();) {
                    java.net.InetAddress inetAddress = enumIpAddr.nextElement();
                     String tmp = inetAddress.toString();
                    System.out.println(tmp);
                    if (tmp.contains(localIP)) {
                        iNet = tmp.replace("/", "");
                        NotificationsViewModel.append(iNet + "\n");
                        break;
                    }
                }
            }
        } catch (Exception ex) {
            System.out.println("Current IP" + ex.toString());
        }
        return iNet;
    }


}