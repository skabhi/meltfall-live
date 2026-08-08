package com.example.meltingfacerain;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

public class MainActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(
                WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN
        );

        boolean portrait = getResources().getDisplayMetrics().heightPixels >= getResources().getDisplayMetrics().widthPixels;

        ScrollView scrollView = new ScrollView(this);
        scrollView.setFillViewport(true);
        scrollView.setBackgroundResource(R.drawable.bg_app);

        LinearLayout page = new LinearLayout(this);
        page.setOrientation(LinearLayout.VERTICAL);
        page.setPadding(dp(portrait ? 18 : 22), dp(portrait ? 18 : 22), dp(portrait ? 18 : 22), dp(28));
        scrollView.addView(page, new ScrollView.LayoutParams(
                ScrollView.LayoutParams.MATCH_PARENT,
                ScrollView.LayoutParams.WRAP_CONTENT
        ));

        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);
        page.addView(header, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        ));

        LinearLayout heading = new LinearLayout(this);
        heading.setOrientation(LinearLayout.VERTICAL);
        header.addView(heading, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        TextView eyebrow = new TextView(this);
        eyebrow.setText("LIVE WALLPAPER");
        eyebrow.setTextColor(Color.rgb(46, 230, 184));
        eyebrow.setTextSize(13f);
        heading.addView(eyebrow);

        TextView title = new TextView(this);
        title.setText("Meltfall Live");
        title.setTextColor(Color.WHITE);
        title.setTextSize(portrait ? 31f : 30f);
        title.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        titleParams.topMargin = dp(6);
        heading.addView(title, titleParams);

        ImageButton appSettings = new ImageButton(this);
        appSettings.setImageResource(android.R.drawable.ic_menu_manage);
        appSettings.setColorFilter(Color.WHITE);
        appSettings.setBackgroundResource(R.drawable.bg_secondary_button);
        appSettings.setPadding(dp(13), dp(13), dp(13), dp(13));
        appSettings.setContentDescription("App settings");
        appSettings.setOnClickListener(v -> startActivity(new Intent(this, SettingsActivity.class)));
        LinearLayout.LayoutParams gearParams = new LinearLayout.LayoutParams(dp(54), dp(54));
        gearParams.leftMargin = dp(12);
        header.addView(appSettings, gearParams);

        TextView subtitle = new TextView(this);
        subtitle.setText("A bright, reactive field of melting-face emojis for your home and lock screens.");
        subtitle.setTextColor(Color.rgb(176, 188, 196));
        subtitle.setTextSize(portrait ? 15f : 16f);
        subtitle.setLineSpacing(dp(2), 1f);
        LinearLayout.LayoutParams subtitleParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        subtitleParams.topMargin = dp(8);
        page.addView(subtitle, subtitleParams);

        LinearLayout card = new LinearLayout(this);
        card.setOrientation(portrait ? LinearLayout.VERTICAL : LinearLayout.HORIZONTAL);
        card.setGravity(Gravity.CENTER_VERTICAL);
        card.setPadding(dp(16), dp(16), dp(16), dp(16));
        card.setBackgroundResource(R.drawable.bg_card);
        card.setMinimumHeight(dp(portrait ? 0 : 230));
        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        cardParams.topMargin = dp(26);
        page.addView(card, cardParams);

        FrameLayout preview = new FrameLayout(this);
        preview.setBackgroundResource(R.drawable.bg_preview);
        preview.setClipToOutline(false);

        ImageView icon = new ImageView(this);
        icon.setImageResource(R.drawable.melting_face_cyan);
        icon.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
        FrameLayout.LayoutParams iconParams = new FrameLayout.LayoutParams(
                dp(portrait ? 154 : 138),
                dp(portrait ? 154 : 138),
                Gravity.CENTER
        );
        preview.addView(icon, iconParams);

        TextView chip = new TextView(this);
        chip.setText("Funky");
        chip.setTextColor(Color.rgb(224, 255, 245));
        chip.setTextSize(13f);
        chip.setGravity(Gravity.CENTER);
        chip.setBackgroundResource(R.drawable.bg_chip);
        FrameLayout.LayoutParams chipParams = new FrameLayout.LayoutParams(dp(92), dp(36), Gravity.TOP | Gravity.RIGHT);
        chipParams.setMargins(0, dp(14), dp(14), 0);
        preview.addView(chip, chipParams);

        LinearLayout.LayoutParams previewParams = new LinearLayout.LayoutParams(
                portrait ? LinearLayout.LayoutParams.MATCH_PARENT : dp(250),
                dp(portrait ? 190 : 198)
        );
        if (portrait) {
            previewParams.bottomMargin = dp(18);
        } else {
            previewParams.rightMargin = dp(18);
        }
        card.addView(preview, previewParams);

        LinearLayout details = new LinearLayout(this);
        details.setOrientation(LinearLayout.VERTICAL);

        TextView name = new TextView(this);
        name.setText("Meltfall Live");
        name.setTextColor(Color.WHITE);
        name.setTextSize(portrait ? 23f : 25f);
        details.addView(name);

        TextView path = new TextView(this);
        path.setText("live_wallpapers/Funky_Face_Rain");
        path.setTextColor(Color.rgb(146, 158, 168));
        path.setTextSize(portrait ? 14f : 15f);
        LinearLayout.LayoutParams pathParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        pathParams.topMargin = dp(4);
        details.addView(path, pathParams);

        TextView body = new TextView(this);
        body.setText("Tune speed, size, frame limit, and exact emoji count before applying.");
        body.setTextColor(Color.rgb(190, 201, 209));
        body.setTextSize(portrait ? 15f : 16f);
        body.setLineSpacing(dp(2), 1f);
        LinearLayout.LayoutParams bodyParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        bodyParams.topMargin = dp(14);
        details.addView(body, bodyParams);

        LinearLayout actions = new LinearLayout(this);
        actions.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams actionsParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        actionsParams.topMargin = dp(22);
        details.addView(actions, actionsParams);

        Button apply = new Button(this);
        apply.setText("View wallpaper");
        apply.setTextColor(Color.rgb(8, 12, 16));
        apply.setTextSize(17f);
        apply.setAllCaps(false);
        apply.setBackgroundResource(R.drawable.bg_primary_button);
        apply.setOnClickListener(v -> startActivity(new Intent(this, PreviewActivity.class)));
        actions.addView(apply, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(58),
                1f
        ));

        LinearLayout.LayoutParams detailsParams = new LinearLayout.LayoutParams(
                portrait ? LinearLayout.LayoutParams.MATCH_PARENT : 0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                portrait ? 0f : 1f
        );
        card.addView(details, detailsParams);

        TextView footer = new TextView(this);
        footer.setText("Tip: open the wallpaper view to tune this wallpaper before applying.");
        footer.setTextColor(Color.rgb(122, 136, 146));
        footer.setTextSize(14f);
        LinearLayout.LayoutParams footerParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        footerParams.topMargin = dp(18);
        page.addView(footer, footerParams);

        setContentView(scrollView);
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
