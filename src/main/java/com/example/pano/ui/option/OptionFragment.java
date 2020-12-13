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
package com.example.pano.ui.option;

import android.content.Context;
import android.os.Bundle;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import com.example.pano.Globals;
import com.example.pano.R;
import com.example.pano.PanInterface.PanCom;
import com.example.pano.Sshcom;

/**
 * A fragment representing a list of Items.
 */
public class OptionFragment extends Fragment {

    // TODO: Customize parameter argument names
    private static final String ARG_COLUMN_COUNT = "column-count";
    // TODO: Customize parameters
    private int mColumnCount = 1;
    private static OptionFragment optionFragment;

    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public OptionFragment() {
        optionFragment = this;
    }

    // TODO: Customize parameter initialization
    @SuppressWarnings("unused")
    public static OptionFragment newInstance(int columnCount) {
        OptionFragment fragment = new OptionFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_COLUMN_COUNT, columnCount);
        fragment.setArguments(args);
        return fragment;
    }

    Button endSeqButton;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getArguments() != null) {
            mColumnCount = getArguments().getInt(ARG_COLUMN_COUNT);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_option_list, container, false);

        // Set the adapter
        RecyclerView recyclerView = (RecyclerView) view.findViewById(R.id.option_list);;
        Context context = recyclerView.getContext();
        if (mColumnCount <= 1) {
            recyclerView.setLayoutManager(new LinearLayoutManager(context));
        } else {
            recyclerView.setLayoutManager(new GridLayoutManager(context, mColumnCount));
        }
        recyclerView.setAdapter(new MyOptionRecyclerViewAdapter(PanCom.ITEMS));
        endSeqButton = (Button) view.findViewById(R.id.end_sequence_button);
        endSeqButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Sshcom.sendUnsolicitedCommand("e");
            }
        });
        endSeqButton.setEnabled(Globals.isRunning());

        return view;
    }

    public static void setStatusRunning(boolean en) {
        if (en) Globals.setStatusRunning();
        else Globals.setStatusNotRunning();
        optionFragment.endSeqButton.setEnabled(Globals.isRunning());
    }
}