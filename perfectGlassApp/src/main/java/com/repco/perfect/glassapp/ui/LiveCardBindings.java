package com.repco.perfect.glassapp.ui;

import android.text.Html;
import android.text.Spanned;
import android.text.format.DateUtils;
import android.widget.RemoteViews;

import com.repco.perfect.glassapp.R;
import com.repco.perfect.glassapp.storage.Chapter;

import java.util.Date;

/**
 * Created by chom on 2/3/15.
 */
public class LiveCardBindings {
    private static void setHTML(RemoteViews v, int id, Spanned val) {
		v.setTextViewText(id, val);
	}

    static final String dashHtmlTemplate =

            "<span>Your last chapter started <font color='blue'>%s</font></span><br/>" +
            "<span>You have <font color='blue'>%d</font> clips</span><br/>";
    static final String noClipsDashHtmlTemplate = "No chapter in progress";
    public static void buildDashView(RemoteViews view,Chapter activeChapter){
        String html = null;
        if (activeChapter.clips.size() == 0){
            html = noClipsDashHtmlTemplate;
        }else {
            long chapterStart = activeChapter.clips.get(0).ts.getTime();
            long now = new Date().getTime();
            String diffString = DateUtils.getRelativeTimeSpanString(chapterStart,now,DateUtils.DAY_IN_MILLIS).toString();
            html = String.format(dashHtmlTemplate,
                    diffString,
                    activeChapter.clips.size()
                    );
        }

        setHTML(view,R.id.body_text, Html.fromHtml(html));
    }
}
