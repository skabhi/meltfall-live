package com.anderson.singh.play.meltfalllive;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.view.Window;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

public class SettingsActivity extends Activity {
    private RainSettings.Values values;
    private TextView fpsValue;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);

        values = RainSettings.load(this);

        ScrollView scrollView = new ScrollView(this);
        scrollView.setFillViewport(true);
        scrollView.setBackgroundResource(R.drawable.bg_app);
        scrollView.setClipToPadding(false);
        SystemBarInsets.applyNavigationBarPadding(scrollView);

        LinearLayout page = new LinearLayout(this);
        page.setOrientation(LinearLayout.VERTICAL);
        page.setPadding(dp(20), dp(18), dp(20), dp(26));
        scrollView.addView(page);

        LinearLayout top = new LinearLayout(this);
        top.setGravity(Gravity.CENTER_VERTICAL);
        top.setOrientation(LinearLayout.HORIZONTAL);

        Button back = new Button(this);
        back.setText("<");
        back.setTextColor(Color.WHITE);
        back.setTextSize(26f);
        back.setBackgroundColor(Color.TRANSPARENT);
        back.setOnClickListener(v -> finish());
        top.addView(back, new LinearLayout.LayoutParams(dp(58), dp(58)));

        TextView title = new TextView(this);
        title.setText("App Settings");
        title.setTextColor(Color.WHITE);
        title.setTextSize(28f);
        title.setGravity(Gravity.CENTER_VERTICAL);
        top.addView(title, new LinearLayout.LayoutParams(0, dp(68), 1f));
        page.addView(top);

        TextView summary = mutedText("Settings here apply to the app and developer tools, not just one wallpaper.");
        summary.setTextSize(15f);
        page.addView(summary);

        if (RainSettings.canShowFpsCounter(this)) {
            addDeveloperSection(page);
        } else {
            TextView releaseNote = mutedText("Developer features are hidden in release builds.");
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );
            params.topMargin = dp(18);
            page.addView(releaseNote, params);
        }

        setContentView(scrollView);
    }

    private void addDeveloperSection(LinearLayout page) {
        TextView section = sectionTitle("Developer");
        LinearLayout.LayoutParams sectionParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        sectionParams.topMargin = dp(24);
        page.addView(section, sectionParams);

        addFpsToggle(page);
        fpsValue = addFpsControl(page);
        addCopyReleaseDefaultsControl(page);
        updateLabels();
    }

    private void addFpsToggle(LinearLayout page) {
        LinearLayout row = settingRow();
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);

        LinearLayout text = new LinearLayout(this);
        text.setOrientation(LinearLayout.VERTICAL);

        TextView title = titleText("Show FPS counter");
        text.addView(title);

        TextView subtitle = mutedText("Debug-only rendered frame counter for preview and live wallpaper.");
        text.addView(subtitle);

        row.addView(text, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        Switch toggle = new Switch(this);
        toggle.setChecked(values.showFps);
        toggle.setOnCheckedChangeListener((buttonView, checked) -> {
            values = new RainSettings.Values(
                    values.speedPercent,
                    values.emojiCount,
                    values.circleCount,
                    values.diamondCount,
                    values.sizePercent,
                    values.maxFps,
                    checked
            );
            RainSettings.save(this, values);
        });
        row.addView(toggle);

        addSettingRow(page, row);
    }

    private TextView addFpsControl(LinearLayout page) {
        LinearLayout row = settingRow();

        TextView valueText = titleText("");
        row.addView(valueText);

        TextView name = mutedText("Animation FPS limit");
        row.addView(name);

        SeekBar seekBar = new SeekBar(this);
        seekBar.setMax(3);
        seekBar.setProgress(fpsToProgress(values.maxFps));
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    values = new RainSettings.Values(
                            values.speedPercent,
                            values.emojiCount,
                            values.circleCount,
                            values.diamondCount,
                            values.sizePercent,
                            progressToFps(progress),
                            values.showFps
                    );
                    RainSettings.save(SettingsActivity.this, values);
                    updateLabels();
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });
        row.addView(seekBar);

        addSettingRow(page, row);
        return valueText;
    }

    private void addCopyReleaseDefaultsControl(LinearLayout page) {
        Button copy = new Button(this);
        copy.setText("Copy current values for defaults.properties");
        copy.setAllCaps(false);
        copy.setTextSize(16f);
        copy.setTextColor(Color.WHITE);
        copy.setBackgroundResource(R.drawable.bg_secondary_button);
        copy.setOnClickListener(v -> copyReleaseDefaults());

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(58)
        );
        params.topMargin = dp(14);
        page.addView(copy, params);

        TextView hint = mutedText("Paste into config/defaults.properties before building a release.");
        LinearLayout.LayoutParams hintParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        hintParams.topMargin = dp(8);
        page.addView(hint, hintParams);
    }

    private void copyReleaseDefaults() {
        String snippet =
                "drop_speed=" + values.speedPercent + "\\n" +
                "emoji_count=" + values.emojiCount + "\\n" +
                "circle_count=" + values.circleCount + "\\n" +
                "diamond_count=" + values.diamondCount + "\\n" +
                "emoji_size=" + values.sizePercent + "\\n" +
                "fps_limit=" + values.maxFps + "\\n" +
                "show_fps=" + values.showFps + "\\n";
        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        clipboard.setPrimaryClip(ClipData.newPlainText("Meltfall release defaults", snippet));
        Toast.makeText(this, "defaults.properties values copied", Toast.LENGTH_SHORT).show();
    }

    private void updateLabels() {
        if (fpsValue != null) {
            fpsValue.setText(values.maxFps == 0 ? "Unlimited" : values.maxFps + " FPS");
        }
    }

    private int fpsToProgress(int fps) {
        if (fps == 60) {
            return 1;
        }
        if (fps == 45) {
            return 2;
        }
        if (fps == 30) {
            return 3;
        }
        return 0;
    }

    private int progressToFps(int progress) {
        if (progress == 1) {
            return 60;
        }
        if (progress == 2) {
            return 45;
        }
        if (progress == 3) {
            return 30;
        }
        return 0;
    }

    private LinearLayout settingRow() {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.VERTICAL);
        row.setPadding(dp(16), dp(14), dp(16), dp(16));
        row.setBackgroundResource(R.drawable.bg_card);
        return row;
    }

    private void addSettingRow(LinearLayout page, LinearLayout row) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.topMargin = dp(14);
        page.addView(row, params);
    }

    private TextView sectionTitle(String text) {
        TextView view = new TextView(this);
        view.setText(text);
        view.setTextColor(Color.rgb(46, 230, 184));
        view.setTextSize(13f);
        return view;
    }

    private TextView titleText(String text) {
        TextView view = new TextView(this);
        view.setText(text);
        view.setTextColor(Color.WHITE);
        view.setTextSize(20f);
        return view;
    }

    private TextView mutedText(String text) {
        TextView view = new TextView(this);
        view.setText(text);
        view.setTextColor(Color.rgb(170, 178, 186));
        view.setTextSize(14f);
        view.setLineSpacing(dp(2), 1f);
        return view;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
