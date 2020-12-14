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

import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.pano.PanInterface.PanCom;
import com.example.pano.R;
import com.example.pano.PanInterface.PanCom.PanSequenceItem;

import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * {@link RecyclerView.Adapter} that can display a {@link PanSequenceItem}.
 */
public class MyOptionRecyclerViewAdapter extends RecyclerView.Adapter<MyOptionRecyclerViewAdapter.ViewHolder> {

    private final List<PanSequenceItem> mValues;

    public MyOptionRecyclerViewAdapter(List<PanSequenceItem> items) {
        mValues = items;
    }

    @NotNull
    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.fragment_option, parent, false);
        ViewHolder vh = new ViewHolder(view, new ViewHolder.IMyViewHolderClicks() {
            @Override
            public void onViewClick(View caller, int position) {
                 handleCLick(position);
            }
            @Override
            public void onImageViewClick(ImageView callerImage, int position) {
                handleCLick(position);
            }

            private void handleCLick(int position) {
                String cmdChar = mValues.get(position).cmdChar;
                Toast.makeText(view.getContext(), "Option " + cmdChar + " selected", Toast.LENGTH_SHORT).show();
                switch (cmdChar) {
                    case "q":
                        PanCom.quitPan();
                        break;
                    case "c":
                        PanCom.startChecklist(view);
                        break;
                    default:
                        PanCom.runSequence(cmdChar);
                }
            }
        });
        PanCom.setListViewAdapter(this);
        return vh;
    }

    public void updateList(){
        notifyDataSetChanged();
    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, int position) {
        holder.mItem = mValues.get(position);
        holder.mIdView.setText(mValues.get(position).cmdChar);
        holder.mContentView.setText(mValues.get(position).description);
    }

    @Override
    public int getItemCount() {
        return mValues.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        public final View mView;
        public final TextView mIdView;
        public final TextView mContentView;
        public PanSequenceItem mItem;
        public final IMyViewHolderClicks mListener;

        public ViewHolder(View itemLayoutView, IMyViewHolderClicks listener) {
            super(itemLayoutView);
            mListener = listener;
            mView = itemLayoutView;
            mIdView = (TextView) itemLayoutView.findViewById(R.id.item_number);
            mContentView = (TextView) itemLayoutView.findViewById(R.id.content);
            mIdView.setOnClickListener(this);
            itemLayoutView.setOnClickListener(this);
        }

        @NotNull
        @Override
        public String toString() {
            return super.toString() + " '" + mContentView.getText() + "'";
        }

        @Override
        public void onClick(View v) {
            {
                int position = getAdapterPosition();
                if (v instanceof ImageView) {
                    mListener.onImageViewClick((ImageView) v, position);
                } else {
                    mListener.onViewClick(v, position);
                }
            }
        }

        public interface IMyViewHolderClicks {
            void onViewClick(View caller, int position);
            void onImageViewClick(ImageView callerImage, int position);
        }
    }
}