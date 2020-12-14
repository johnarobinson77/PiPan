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
package com.example.pano.ui.notifications;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class NotificationsViewModel extends ViewModel {

    private static final MutableLiveData<String> mText = new MutableLiveData<>();

    private static String savedLog = new String();
    private static Integer lineCnt = 0;

    public NotificationsViewModel() {
        //mText = new MutableLiveData<>();
        //mText.setValue("This is notifications fragment0");
        mText.setValue(savedLog);
        //if (lineCnt == 0) testAsync();
     }

    public static void append(String logString){
        savedLog += logString;
        lineCnt++;
        mText.postValue(logString);
    }

    public LiveData<String> getText() {
        return mText;
    }
}