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
package com.example.pano.PanInterface;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.os.ParcelFileDescriptor;
import android.view.View;
import android.widget.Toast;

import com.example.pano.Globals;
import com.example.pano.Sshcom;
import com.example.pano.ui.notifications.NotificationsViewModel;
import com.example.pano.ui.option.MyOptionRecyclerViewAdapter;
import com.example.pano.ui.option.OptionFragment;

import org.jetbrains.annotations.NotNull;

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Helper class for providing sample content for user interfaces created by
 * Android template wizards.
 * <p>
 * TODO: Replace all uses of this class before publishing your app.
 */
public class PanCom {

    private static Context context = null;

    public void finalize(){
        context = null;
    }

    /**
     * A PanInterface item representing a piece of content.
     */
    public static class PanSequenceItem {
        public final String cmdChar;
        public final String description;

        public PanSequenceItem(String cmdChar, String description) {
            this.cmdChar = cmdChar;
            this.description = description;
        }

        @NotNull
        @Override
        public String toString() {
            return description;
        }
    }


    /**
     * An array of (PanInterface) items.
     */
    public static final List<PanSequenceItem> ITEMS = new ArrayList<>();

    /**
     * A map of (PanInterface) items, by ID.
     */
    public static final Map<String, PanSequenceItem> ITEM_MAP = new HashMap<>();

    public static void addItem(PanSequenceItem item) {
        ITEMS.add(item);
        ITEM_MAP.put(item.cmdChar, item);
    }

    // program start and prompt
//    private static final String programName = "python3 pan.py";
    private static final String programName = "go";
    private static final String panPrompt = "number:";
    public static final String logFilename = "pan_log";

    private static MyOptionRecyclerViewAdapter listViewAdapter = null;
    public static void setListViewAdapter(MyOptionRecyclerViewAdapter myOptionRecyclerViewAdapter) {
        listViewAdapter = myOptionRecyclerViewAdapter;
    }

    // The next three methods are for staring and stopping the pan.py program once a connection
    // has been established.
    // startPan() starts the pan.py pn the server assuming the current state is sitting at the OC cli
    //  prompt.  It uses case1 and case 2 of StartStopCallback.onComplete() for the callback.
    //  It first issues the command to execute pan.py.  THe it issues 1 <ret> which causes the
    //  sequence options to be written out.  case 2: of the callback parses the sequence options
    //  into a list for the option fragment
    // quitPan() utilizes case 3 and 4 of StartStopCallback.onComplete().  It issues a "q" command
    //  and waits for the OS cli prompt.

    // it shares with quitPan
    public static void startPan() {
        // start the command sequence
        Globals.sshcom.sendRunCommandMsg( Sshcom.RunCommandType.RUN_COMMAND,1,
                programName, panPrompt, 5, StartStopCallback);
    }
    public static void quitPan() {
        // Stop the pan.py
        StartStopCallback.onComplete(3, Sshcom.ReturnStatus.NULL, "");
    }

    // This runs the sequence for driving the
    private static final Sshcom.CommandResultCallback StartStopCallback = new Sshcom.CommandResultCallback() {
        @Override
        public void onComplete(int tag, Sshcom.ReturnStatus returnStatus, String result) {
            if (result != null) NotificationsViewModel.append(result);
            if (returnStatus == Sshcom.ReturnStatus.STILL_RUNNING) return;
            String cmdBuf = Sshcom.getCmdOutputBuffer();
            switch (tag) {
                // case 0 - 2 starts the program and populates the list with the program options
                case 1:  // issue a <return> to get the list of sequence options
                    Globals.sshcom.sendRunCommandMsg(Sshcom.RunCommandType.RUN_COMMAND,tag + 1,
                            "", panPrompt, 5, StartStopCallback);
                    break;
                case 2: // the sequence options are in the return message.  Parse and add it to the
                        // option list.  No new command is issued
                    String[] options = cmdBuf.split("\\n");
                    for (String option: options) {
                        String[] tmp = option.split(": ");
                        String cmdChar = tmp[0].substring(tmp[0].length()-1);
                        if (tmp.length == 2) {
                            PanCom.PanSequenceItem psi =
                                    new PanCom.PanSequenceItem(cmdChar, tmp[1]);
                            PanCom.addItem(psi);
                        }
                    }
                    if (listViewAdapter != null) listViewAdapter.updateList();
                    break;

                // case 3 and 4 handle quiting the pan.py program
                case 3:
                    if (result != null) NotificationsViewModel.append(result);
                    Globals.sshcom.sendRunCommandMsg(Sshcom.RunCommandType.RUN_COMMAND,tag + 1,
                            "q",
                            Globals.sshcom.getCliPrompt(Globals.sshcom.getCurrentHost()), 5, StartStopCallback);
                    break;
                case 4:
                    if (result != null) {
                        NotificationsViewModel.append(result);
                        // TODO add logic to show not running pan.py any more.
                        ITEM_MAP.clear();
                        ITEMS.clear();
                        if (listViewAdapter != null) listViewAdapter.updateList();
                    }
                    break;
                default:
                    if (result != null) NotificationsViewModel.append(result);
            }
        }
    };

    // The next 3 methods are for signing of the checklist.  When the pan.py program receives the 'c'
    // command it presents a check list one item at a time.  An <ret> advances it to the next item
    // until the list is complete and then returns to the main prompt.
    // This program handles that case as follows:
    // startChecklist() sends the "c" command to start the sequence specifying
    //      checklistCallback.onComplete().
    // checklistCallback.onComplete() receive the result and calls checklistSignOff() with the
    //      checklist item.
    // checklistSignOff puts the item into an alert dialog.  The OK button sends a <ret> cmd
    //      to get the next checklist item, also using checklistCallback.onComplete().
    // When checklistCallback.onComplete() sees the string contains the prompt for the pan
    //      controller it just returns, which ends the sequence.


    private static void checklistSignOff(String checklistItem) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setMessage(checklistItem);
        builder.setTitle("Checklist");
        builder.setPositiveButton("OK", (dialog, which) -> Globals.sshcom.sendRunCommandMsg( Sshcom.RunCommandType.RUN_COMMAND, 1,
                "", null, 1, checklistCallback));
        builder.show();
    }

    private static final Sshcom.CommandResultCallback checklistCallback = (tag, returnStatus, result) -> {
        if (result != null) {
            NotificationsViewModel.append(result);
        }
        if (returnStatus != Sshcom.ReturnStatus.TIMEOUT) return; // not ready yet
        String outBuf = Sshcom.getCmdOutputBuffer();
        if (outBuf != null) {
            if (outBuf.contains(panPrompt)) return; // its done so exit
            int cp = outBuf.indexOf('\n'); // remove the CR and the beginning of the string
            outBuf = outBuf.substring(cp+1);
            checklistSignOff(outBuf); // put up the alert dialog.
        }
    };

    public static void startChecklist(View view) {
        context = view.getContext();
        Globals.sshcom.sendRunCommandMsg( Sshcom.RunCommandType.RUN_COMMAND, 1,
                "c", null, 1, checklistCallback);
    }

    // handling all other commands is handled with the next 2 methods.  Since the command can run
    // for over 60 minutes, the time out is set to cover that.

    private static final Sshcom.CommandResultCallback sequenceCallback = (tag, returnStatus, result) -> {
        if (result != null) {
            NotificationsViewModel.append(result);
        }
        if (returnStatus != Sshcom.ReturnStatus.STILL_RUNNING){
            OptionFragment.setStatusRunning(false);
        }
    };

    public static void runSequence(String cmdChar) {
        Globals.sshcom.sendRunCommandMsg( Sshcom.RunCommandType.RUN_COMMAND, 1,
                cmdChar, panPrompt, 2000, sequenceCallback);
        OptionFragment.setStatusRunning(true);
    }

    // The following variables and 2 methods are used for copying the log file created in the last
    // run of the "go" script which creates a log file of the pan.py run.  It starts with a call to
    // copyLog() which writes the activity and ParcelFileDescriptor for the previous opened file
    // to local static variables.  It then issues to command to cat the log file.  copyLog() is
    // called from the HomeFragment
    // copyLogCallback() receives the response from the above command.  If the fileStream has not
    // been created yet it creates it and stores it in a class variable.  In this case, it does not
    // send most of the result to the spew panel.  It writes it to the file instead.

    private static ParcelFileDescriptor pfd = null;
    private static FileOutputStream fos = null;

    public static void copyLog(Activity activity, ParcelFileDescriptor pfd) {
        PanCom.context = activity;
        PanCom.pfd = pfd;
         Globals.sshcom.sendRunCommandMsg(Sshcom.RunCommandType.RUN_COMMAND, 1,
                "cat log.log", Globals.sshcom.getCliPrompt(Globals.sshcom.getCurrentHost()),
                 5, copyLogCallback);
    }

    private static final Sshcom.CommandResultCallback copyLogCallback = (step, returnStatus, result) -> {
        if (pfd == null) {  // pdf == null indicates some error happened in the past so stop
            NotificationsViewModel.append(result);
            return;
        }
        //if (result != null) NotificationsViewModel.append(result);
        if (result.contains("No such file or directory")) {
            NotificationsViewModel.append(result);
            Toast.makeText(context, "Copy log file ERROR, couldn't open source file",
                    Toast.LENGTH_LONG).show();
            try {
                fos.close();
                pfd.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            pfd = null;
            return;
        }
        if (fos == null) { // fOut == null indicates first pass and file needs to be opened
            fos = new FileOutputStream(pfd.getFileDescriptor());
        }
        // remove the cli prompt.
        if (returnStatus == Sshcom.ReturnStatus.WAITFOR_FOUND) { // remove the cli prompt.
            int crIdx = result.lastIndexOf('\n');
            result = result.substring(0, crIdx + 1);
            NotificationsViewModel.append(result.substring(crIdx));
        }
        // write out the result buffer
        final byte[] b = result.getBytes();
        try {
            fos.write(b);
         } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(context, "Copy log file ERROR, couldn't write to destination file",
                    Toast.LENGTH_LONG).show();
            try {
                fos.close();
                pfd.close();
            } catch (IOException ee) {
                ee.printStackTrace();
            }
            pfd = null;
            return;
        }
        // close the file
        if (returnStatus == Sshcom.ReturnStatus.WAITFOR_FOUND) {
            try {
                fos.close();
                pfd.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            Toast.makeText(context, "Copy log file complete",
                    Toast.LENGTH_LONG).show();
        }

    };
}