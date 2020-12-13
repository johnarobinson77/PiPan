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

import android.app.AlertDialog;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

public class MainActivity extends AppCompatActivity {
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        BottomNavigationView navView = findViewById(R.id.nav_view);
        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        AppBarConfiguration appBarConfiguration = new AppBarConfiguration.Builder(
                R.id.navigation_home, R.id.option_list, R.id.navigation_notifications)
                .build();
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment);
        NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration);
        NavigationUI.setupWithNavController(navView, navController);
        Looper mainLooper = getMainLooper();
        Handler sshHandler = new Handler(mainLooper);
        Globals.sshcom = new Sshcom(this, sshHandler);
        registerNetworkCallback();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.start_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection

        if (item.getItemId() == R.id.settings1) {
            settingsDialog(0);
            return true;
        }
        if (item.getItemId() == R.id.settings2) {
            settingsDialog(1);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
    private void settingsDialog(int select){
        final AlertDialog dialogBuilder = new AlertDialog.Builder(this).create();
        LayoutInflater inflater = this.getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.settings_dialog_layout, null);
        TextView tv =  dialogView.findViewById(R.id.settings_dialog_title);
        tv.setText("Host " + (select+1));

        final EditText ipEditText = dialogView.findViewById(R.id.ip_address);
        final EditText unEditText = dialogView.findViewById(R.id.user_name);
        final EditText pwEditText = dialogView.findViewById(R.id.password);
        final EditText dirEditText = dialogView.findViewById(R.id.working_dir);
        final EditText pgEditText = dialogView.findViewById(R.id.program_name);
        Button button1 = dialogView.findViewById(R.id.dialog_cancel);
        Button button2 = dialogView.findViewById(R.id.dialog_save);
        if (Globals.sshcom.getIP(select) != null) {
            ipEditText.setText(Globals.sshcom.getBaseIP(select));
        }
        if (Globals.sshcom.getUsername(select) != null) {
            unEditText.setText(Globals.sshcom.getUsername(select));
        }
        if (Globals.sshcom.getPassword(select) != null) {
            pwEditText.setText(Globals.sshcom.getPassword(select));
        }
        if (Globals.sshcom.getDirectory(select) != null) {
            dirEditText.setText(Globals.sshcom.getDirectory(select));
        }
        if (Globals.sshcom.getCliPrompt(select) != null) {
            pgEditText.setText(Globals.sshcom.getCliPrompt(select));
        }

        button1.setOnClickListener(view -> dialogBuilder.dismiss());
        button2.setOnClickListener(view -> {
            Globals.sshcom.setBaseIP(String.valueOf(ipEditText.getText()),select);
            Globals.sshcom.setIP(Globals.sshcom.getBaseIP(select),select);
            Globals.sshcom.setUsername(String.valueOf(unEditText.getText()),select);
            Globals.sshcom.setPassword(String.valueOf(pwEditText.getText()),select);
            Globals.sshcom.setDirectory(String.valueOf(dirEditText.getText()),select);
            Globals.sshcom.setCliPrompt(String.valueOf(pgEditText.getText()),select);
            dialogBuilder.dismiss();
            Globals.sshcom.saveSettings(select);
        });

        dialogBuilder.setView(dialogView);
        dialogBuilder.show();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (Globals.sshcom.getRunCommand() != null)
            Globals.sshcom.getRunCommand().getLooper().quit();
    }

    // Network Check
    public void registerNetworkCallback()
    {
        try {
            Globals.cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);

            Globals.cm.registerDefaultNetworkCallback(new ConnectivityManager.NetworkCallback(){
                   @Override
                   public void onAvailable(Network network) {
                       Globals.isNetworkConnected = true; // Global Static Variable
                       Toast.makeText(MainActivity.this, "Network Available", Toast.LENGTH_SHORT).show();
                   }
                   @Override
                   public void onLost(Network network) {
                       Globals.isNetworkConnected = false; // Global Static Variable
                       Toast.makeText(MainActivity.this, "Network Not Available", Toast.LENGTH_SHORT).show();
                   }
               }
            );
            Globals.isNetworkConnected = false;
        }catch (Exception e){
            Globals.isNetworkConnected = false;
        }
    }

}