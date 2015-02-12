package com.repco.perfect.glassapp.base;

import android.app.Activity;
import android.os.Bundle;

import com.repco.perfect.glassapp.storage.Chapter;

/**
 * Created by chom on 2/11/15.
 */
public class ChapterActivity extends Activity {

    protected Chapter mChapter;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mChapter = (Chapter)getIntent().getSerializableExtra("chapter");
    }

}
