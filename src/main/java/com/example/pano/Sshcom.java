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


import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.preference.PreferenceManager;

import com.example.pano.ui.notifications.NotificationsViewModel;
import com.jcraft.jsch.*;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.net.InetAddress;
import java.util.Properties;

import static java.lang.Thread.sleep;


public class Sshcom {
    // ssh Login and CLI information
    private static final int NUM_HOSTS = 2;
    private final String[] baseIP;
    private final String[] IP;
    private final String[] username;
    private final String[] password;
    private final String[] directory;
    private final String[] cliPrompt;
    private int currentHost = 0;
    // ssh storage of login and CLI info
    private final SharedPreferences settings;

    // shared variable for Jsch SSH interface
    final JSch jsch = new JSch();
    private Session session = null;
    private ChannelShell channel = null;
    private InputStream in = null;
    private PrintStream out = null;
    // handler to send info back to the UI thread
    private final Handler sshHandler;
    //thread for handling the ssh interface
    private HandlerThread runCommand = null;
    // handler for passing runCommand messages.
    private Handler runCommandHandler;

    private String errorMessage = null;

    // constructor gets activity for storing preferences and ui Handler.
    public Sshcom(Activity activity, Handler sshHandler) {
        settings = PreferenceManager.getDefaultSharedPreferences(activity);
        IP = new String[NUM_HOSTS];
        baseIP = new String[NUM_HOSTS];
        username = new String[NUM_HOSTS];
        password = new String[NUM_HOSTS];
        directory = new String[NUM_HOSTS];
        cliPrompt = new String[NUM_HOSTS];
        for (int i = 0; i < NUM_HOSTS; i++) {
            setBaseIP(settings.getString("ip_addr"+ i, null), i);
            setUsername(settings.getString("username"+ i, null), i);
            setPassword(settings.getString("password"+ i, null), i);
            setDirectory(settings.getString("directory"+ i, null), i);
            setCliPrompt(settings.getString("cliPrompt"+ i, null), i);
        }
        this.sshHandler = sshHandler;
    }

    // make sure to kill the HandlerThread.
    public void finalize(){
        getRunCommand().quit();
    }

    // setters and getters for host login information.
    public String getBaseIP(int select) {
        if (select < 0 || select >= NUM_HOSTS) return null;
        return baseIP[select];
    }
    public void setBaseIP(String IP, int select) {
        if (select < 0 || select >= NUM_HOSTS) return;
        setIP(IP,select);
        this.baseIP[select] = IP;
    }

    public String getIP(int select) {
        if (select < 0 || select >= NUM_HOSTS) return null;
        return IP[select];
    }
    public void setIP(String IP, int select) {
        if (select < 0 || select >= NUM_HOSTS) return;
        this.IP[select] = IP;
    }

    public String getUsername(int select) {
        if (select < 0 || select >= NUM_HOSTS) return null;
        return username[select];
    }
    public void setUsername(String username, int select) {
        if (select < 0 || select >= NUM_HOSTS) return;
        this.username[select] = username;
    }

    public String getPassword(int select) {
        if (select < 0 || select >= NUM_HOSTS) return null;
        return password[select];
    }
    public void setPassword(String password, int select) {
        if (select < 0 || select >= NUM_HOSTS) return;
        this.password[select] = password;
    }

    public String getDirectory(int select) {
        if (select < 0 || select >= NUM_HOSTS) return null;
        return directory[select];
    }
    public void setDirectory(String directory, int select) {
        if (select < 0 || select >= NUM_HOSTS) return;
        this.directory[select] = directory;
    }

    public String getCliPrompt(int select) {
        if (select < 0 || select >= NUM_HOSTS) return null;
        return cliPrompt[select];
    }
    public void setCliPrompt(String cliPrompt, int select) {
        if (select < 0 || select >= NUM_HOSTS) return;
        this.cliPrompt[select] = cliPrompt;
    }

    public boolean setCurrentHost(int currentHost) {
        if ( currentHost < 0 || currentHost >= NUM_HOSTS) return false;
        this.currentHost = currentHost;
        return true;
    }
    public int getCurrentHost() {
        return currentHost;
    }

    public HandlerThread getRunCommand(){ return runCommand; }

    public Handler getRunCommandHandler() { return runCommandHandler; }

    // save login and CLI info
    public void saveSettings(int select){
        SharedPreferences.Editor edit = settings.edit();
        edit.putString("ip_addr" + select, getBaseIP(select));
        edit.putString("username" + select, getUsername(select));
        edit.putString("password" + select, getPassword(select));
        edit.putString("directory" + select, getDirectory(select));
        edit.putString("cliPrompt" + select, getCliPrompt(select));
        edit.apply();
    }

    private Session getSession(){
        if(session == null || !session.isConnected()){
            session = connect(getIP(currentHost),getUsername(currentHost),getPassword(currentHost));
        }
        return session;
    }
    public void openChannel() {
        getChannel();
    }

    private void getChannel(){
        if(channel == null || !channel.isConnected()){
            try{
                channel = (ChannelShell)getSession().openChannel("shell");
                channel.connect();
                in = channel.getInputStream();
                out = new PrintStream(channel.getOutputStream());

            }catch(Exception e){
                errorMessage = "SSH Error: while opening channel: "+ e;
            }
        }
    }

    private Session connect(String hostname, String username, String password){
        try {
            session = jsch.getSession(username, hostname, 22);
            Properties config = new Properties();
            config.put("StrictHostKeyChecking", "no");
            session.setConfig(config);
            session.setPassword(password);

            //NotificationsViewModel.append("Connecting SSH to " + hostname + " - Please wait for few seconds... ");
            session.connect();
            //NotificationsViewModel.append("Connected!\n");

        }catch(Exception e){
            errorMessage = "SSH Error: occurred while connecting to "+hostname+": "+e;
        }
        return session;
    }

    private void sendCommand(String command){

        try{
            if (out == null) errorMessage = "SSH Error: Channel not set up correctly.\n";
            else {
                out.println(command);
                out.flush();
            }
        }catch(Exception e){
            errorMessage = "SSH Error: while sending commands: " + e;
        }
    }

    public void cmdAndWaitFor(RunCommandMsg rcm){
        if (out == null) return;
        try{
            out.println(rcm.command);
            out.flush();
        }catch(Exception e){
            return;
        }
        readChannelOutput(rcm);
    }

    private static int timeOut = 5;
    public static void endWaitForResponse(){
        timeOut = 1;
    }

    private void readChannelOutput(RunCommandMsg rcm){

        final int bufferSize = 16384;
        byte[] buffer = new byte[bufferSize];

        if (rcm.timeOut == null)
            timeOut = 5;
        else
            timeOut = rcm.timeOut;
        int j = 0;
        String line = null;
        int bytesRead = 0;

        if (channel == null || channel.isClosed()){
            line = "SSH Error: while reading channel output: Channel closed";
            returnFromRunCommand(rcm, ReturnStatus.ERROR, line);
            return;
        }
        if (in == null) {
            line = "SSH Error: while reading channel output: Channel not set up correctly";
            returnFromRunCommand(rcm, ReturnStatus.ERROR, line);
            return;
        }

        try {
            sleep(100);
        } catch (Exception ee){
            line = "SSH Error: while waiting for channel output: "+ ee;
            returnFromRunCommand(rcm, ReturnStatus.ERROR, line);
            return;
        }

        try{
            while (true ) {
                while (in.available() > 0) {
                    int i = in.read(buffer, bytesRead, bufferSize-bytesRead);
                    if (i < 0) {
                        break;
                    }
                    bytesRead += i;
                }
                if (bytesRead > 0) {
                    line = new String(buffer, 0, bytesRead);
                    if (rcm.waitFor != null && line.contains(rcm.waitFor)) {
                        returnFromRunCommand(rcm, ReturnStatus.WAITFOR_FOUND, line);
                        break;
                    }
                    if (j >= timeOut){
                        returnFromRunCommand(rcm, ReturnStatus.TIMEOUT, line);
                        break;
                    }
                    returnFromRunCommand(rcm, ReturnStatus.STILL_RUNNING, line);
                    bytesRead = 0; //previous lines read so clear buffer.
                    line = null;
                }

                if (j >= timeOut){
                    returnFromRunCommand(rcm, ReturnStatus.TIMEOUT, line);
                    break;
                }
                j++;

                try {
                    sleep(1000);
                } catch (Exception ee){
                    line = "SSH Error: while waiting for channel output: "+ ee;
                    returnFromRunCommand(rcm, ReturnStatus.ERROR, line);
                    break;
                }
            }
        }catch(Exception e){
            line = "SSH Error: while reading channel output: "+ e;
            returnFromRunCommand(rcm, ReturnStatus.ERROR, line);
        }
    }

    private static String removeEscSeq(String inStr) {
        String regexString = "\\e\\[[?;0-9]*[a-zA-Z]";
        String retStr = null;
        if (inStr != null) retStr = inStr.replaceAll(regexString, "");
        return retStr;
    }

    public void close(){
        if (channel != null && channel.isConnected()) channel.disconnect();
        if (session != null && session.isConnected()) session.disconnect();
        channel = null;
        session = null;
        //NotificationsViewModel.append("Disconnected channel and session\n");
    }

    // Solicited IO methods

    public enum ReturnStatus{
        NULL,           // no info
        ERROR,          // Error
        CHANNEL_OPENED, // Channel was opened
        CHANNEL_CLOSED, // Channel was closed
        STILL_RUNNING,  // Partial return  expecting more
        WAITFOR_FOUND,  // Final return waitFor text was found
        TIMEOUT         // Final return command timed out
    }

    public static class RunCommandType {
        public final static int OPEN_CHANNEL = 0;  // Open the channel
        public final static int RUN_COMMAND = 1;   // Send a command and wait for response
        public final static int CLOSE_CHANNEL = 2;  // close the channel
    }

    public interface CommandResultCallback {
        void onComplete(int tag, ReturnStatus returnStatus, String result);
    }

    public static class RunCommandMsg {
        final Integer tag;
        final String command;
        final String waitFor;
        final Integer timeOut;
        final CommandResultCallback callback;
        RunCommandMsg(Integer tag, String command, String waitFor, Integer timeOut,
                      CommandResultCallback callback){
            this.tag = tag;
            this.command = command;
            this.waitFor = waitFor;
            this.timeOut = timeOut;
            this.callback = callback;
        }
    }

    public Handler startRunCommandThread(int hostSelect){
        // if one is already running, stop it.
        if (runCommand != null && runCommand.isAlive()){
            runCommand.quit();
        }
        // make sure host select is in range.
        if ( hostSelect < 0 || hostSelect >= NUM_HOSTS) return null;
        currentHost = hostSelect;
        // start a new one.
        runCommand = new HandlerThread("runCommand");
        runCommand.start();
        Looper looper = runCommand.getLooper();
        runCommandHandler = new Handler(looper){
            @Override
            public void handleMessage(Message msg) {
                RunCommandMsg rcm = (RunCommandMsg)msg.obj;
                errorMessage = null;
                String result;
                switch (msg.what) {
                    case RunCommandType.OPEN_CHANNEL:
                        if (getIP(currentHost) == null || // check that there is login info
                                getUsername(currentHost) == null ||
                                getPassword(currentHost) == null) {
                            result = "Host # " + currentHost + " login info not set\n";
                            returnFromRunCommand(rcm, ReturnStatus.ERROR, result);
                            break;
                        }
                        try { // exit if host is not reachable
                            InetAddress address = InetAddress.getByName(getIP(currentHost));
                            boolean reachable = address.isReachable(1000);
                            if (!reachable) {
                                result = "Host: " + getIP(currentHost) + " is not reachable!\n";
                                returnFromRunCommand(rcm, ReturnStatus.ERROR, result);
                                break;
                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        openChannel();  // try opening the channel
                        if (channel != null && channel.isConnected()) {
                            result = "Channel Opened\n";
                            returnFromRunCommand(rcm, ReturnStatus.CHANNEL_OPENED, result);
                            if (rcm.waitFor != null || rcm.timeOut != null)  //get return text if requested
                                readChannelOutput(rcm);
                        } else {
                            result = "SSH Error: Channel did not open\n";
                            returnFromRunCommand(rcm, ReturnStatus.ERROR, result);
                        }
                        break;
                    case RunCommandType.RUN_COMMAND:
                        if (rcm != null) {
                            cmdAndWaitFor(rcm);
                        }
                        break;
                    case RunCommandType.CLOSE_CHANNEL:
                        close();
                        result = "Channel Closed\n";
                        returnFromRunCommand(rcm, ReturnStatus.CHANNEL_CLOSED, result);
                        break;
                    default:
                        result = "SSH Error: What?\n";
                        returnFromRunCommand(rcm, ReturnStatus.ERROR, result);
                        break;
                }
             }
        };
        return runCommandHandler;
    }

    private void returnFromRunCommand(RunCommandMsg rcm, final ReturnStatus rs, String result) {
        final String finalResult = removeEscSeq(result);
        final Integer tag = rcm.tag;
        if (rcm.callback != null) {
            sshHandler.post(() -> {
                if (finalResult != null) cmdOutputBuffer += finalResult;
                rcm.callback.onComplete(tag, rs, finalResult);
            });
        }

    }

    private static String cmdOutputBuffer;
    public static String getCmdOutputBuffer() {
        return cmdOutputBuffer;
    }

    public void sendRunCommandMsg(int type, int tag,
                           String command, String waitFor, Integer timeOut, CommandResultCallback callback){
        Sshcom.RunCommandMsg rmc = new RunCommandMsg(tag, command, waitFor, timeOut, callback);
        Message msg = new Message();
        msg.obj = rmc;
        msg.arg1 = tag;
        msg.what = type;
        runCommandHandler.sendMessage(msg);
        cmdOutputBuffer = "";
    }

    // Unsolicited IO
    public static void sendUnsolicitedCommand(final String cmd){
        Thread uc = new Thread(){
            @Override
            public void run() {
                super.run();
                if (Globals.sshcom != null) Globals.sshcom.sendCommand(cmd);
            }
        };
        uc.start();
    }


    // simple test case.  Here is the callback
    private final CommandResultCallback testCallback = new CommandResultCallback() {
        @Override
        public void onComplete(int tag, Sshcom.ReturnStatus returnStatus, String result) {
            if (result != null) NotificationsViewModel.append(result);
            if (returnStatus == Sshcom.ReturnStatus.STILL_RUNNING ||
                    returnStatus == Sshcom.ReturnStatus.ERROR ||
                    returnStatus == Sshcom.ReturnStatus.CHANNEL_OPENED)
                return;
            switch (tag) {  // if no more returns are expected, run the next command
                case 1:
                    Globals.sshcom.sendRunCommandMsg(RunCommandType.RUN_COMMAND,tag + 1,
                            "cd " + getDirectory(currentHost),
                            Globals.sshcom.cliPrompt[currentHost], 5, testCallback);
                    break;
                case 2:
                    Globals.sshcom.sendRunCommandMsg(RunCommandType.RUN_COMMAND,tag + 1,
                            "ls -l",
                            Globals.sshcom.cliPrompt[currentHost], 5, testCallback);
                    break;
                case 3:
                    Globals.sshcom.sendRunCommandMsg(RunCommandType.RUN_COMMAND,tag + 1,
                            "exit", null, 1, testCallback);
                    break;
                case 4:
                    Globals.sshcom.sendRunCommandMsg(RunCommandType.CLOSE_CHANNEL, tag + 1,
                            null, null, null, testCallback);
                    break;
                default:
                     //NotificationsViewModel.append("\nTest complete\n");
            }
        }
    };

    // this method starts the test.
    public void test(int hostSelect){
        final Handler handler = startRunCommandThread(hostSelect);
        // start the test at step 0
        Globals.sshcom.sendRunCommandMsg( RunCommandType.OPEN_CHANNEL, 1,
                "", cliPrompt[currentHost], 5, testCallback);
      }

}

