package com.repco.perfect.glassapp.ui;

import android.content.Context;
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
            "<article>\n" +
                    "  <figure>\n" +
                    "    <ul class=\"mosaic mosaic3\">\n" +
                    "      <li style=\"background-image: url(%s)\"></li>\n" +
                    "    </ul>\n" +
                    "  </figure>\n" +
                    "  <section>\n" +
                    "<span class=\"text-small\">Your current chapter has <span style= \"color:#99cc33;\">%d</span> videos and started <span style=\"color:#ddbb11\">%s</span></span>\n" +
                    "  </section>\n" +
                    "</article>";
    static final String noClipsDashHtmlTemplate = "Get started on a new chapter <font color='#99cc33'>today</font>?";
    public static void buildDashView(Context c,RemoteViews view,Chapter activeChapter){
        String html = null;
        if (activeChapter.clips.size() == 0){
            html = noClipsDashHtmlTemplate;
        }else {
            long chapterStart = activeChapter.clips.get(0).ts.getTime();
            long now = new Date().getTime();
            String diffString = DateUtils.getRelativeDateTimeString(c,chapterStart,DateUtils.MINUTE_IN_MILLIS,DateUtils.WEEK_IN_MILLIS,0).toString().split(",")[0];
            String imgString = activeChapter.clips.get(0).previewPath;
            int clipCount = activeChapter.clips.size();

            html = String.format(dashHtmlTemplate,imgString, clipCount, diffString);
        }

        setHTML(view,R.id.body_text, Html.fromHtml(html));
    }
}
