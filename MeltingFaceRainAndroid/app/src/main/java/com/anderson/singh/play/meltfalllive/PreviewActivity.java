package com.anderson.singh.play.meltfalllive;

import android.animation.ValueAnimator;
import android.app.Activity;
import android.app.WallpaperManager;
import android.content.ComponentName;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.view.animation.DecelerateInterpolator;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.TextView;

public class PreviewActivity extends Activity {
    private static final int SHEET_ALPHA = 185;

    private LinearLayout topBar;
    private LinearLayout bottomSheet;
    private CheckBox previewToggle;
    private FrameLayout root;
    private FrameLayout.LayoutParams bottomSheetParams;
    private RainSettings.Values values;
    private TextView speedValue;
    private TextView sizeValue;
    private int collapsedHeight;
    private int expandedHeight;
    private boolean sheetExpanded;
    private float dragStartY;
    private int dragStartHeight;
    private ValueAnimator sheetAnimator;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        hideSystemBars();

        values = RainSettings.load(this);
        collapsedHeight = dp(218);
        expandedHeight = dp(620);

        root = new FrameLayout(this);
        root.setOnClickListener(v -> {
            if (previewToggle != null && previewToggle.isChecked()) {
                previewToggle.setChecked(false);
            }
        });
        root.addView(new RainView(this), new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
        ));

        topBar = buildTopBar();
        root.addView(topBar, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                dp(92),
                Gravity.TOP
        ));

        bottomSheet = buildBottomSheet();
        bottomSheetParams = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                collapsedHeight,
                Gravity.BOTTOM
        );
        root.addView(bottomSheet, bottomSheetParams);

        root.post(() -> {
            int height = root.getHeight();
            collapsedHeight = Math.min(dp(218), Math.max(dp(176), height / 3));
            expandedHeight = Math.max(collapsedHeight, height - dp(72));
            setSheetHeight(sheetExpanded ? expandedHeight : collapsedHeight);
        });

        setContentView(root);
    }

    @Override
    protected void onResume() {
        super.onResume();
        hideSystemBars();
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            hideSystemBars();
        }
    }

    private void hideSystemBars() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            WindowInsetsController controller = getWindow().getInsetsController();
            if (controller != null) {
                controller.hide(WindowInsets.Type.systemBars());
                controller.setSystemBarsBehavior(
                        WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                );
            }
        } else {
            hideSystemBarsLegacy();
        }
    }

    @SuppressWarnings("deprecation")
    private void hideSystemBarsLegacy() {
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
        );
    }

    private LinearLayout buildTopBar() {
        LinearLayout bar = new LinearLayout(this);
        bar.setOrientation(LinearLayout.HORIZONTAL);
        bar.setGravity(Gravity.CENTER_VERTICAL);
        bar.setPadding(dp(18), dp(18), dp(18), dp(12));

        Button back = new Button(this);
        back.setText("<");
        back.setTextSize(28f);
        back.setTextColor(Color.WHITE);
        back.setBackgroundColor(Color.TRANSPARENT);
        back.setOnClickListener(v -> finish());
        bar.addView(back, new LinearLayout.LayoutParams(dp(68), dp(58)));

        View space = new View(this);
        bar.addView(space, new LinearLayout.LayoutParams(0, 1, 1f));

        previewToggle = new CheckBox(this);
        previewToggle.setText("Preview");
        previewToggle.setTextColor(Color.WHITE);
        previewToggle.setTextSize(20f);
        previewToggle.setButtonTintList(ColorStateList.valueOf(Color.WHITE));
        previewToggle.setOnCheckedChangeListener(this::onPreviewChanged);
        bar.addView(previewToggle);

        return bar;
    }

    private LinearLayout buildBottomSheet() {
        LinearLayout sheet = new LinearLayout(this);
        sheet.setOrientation(LinearLayout.VERTICAL);
        sheet.setPadding(dp(20), dp(10), dp(20), dp(20));
        sheet.setBackgroundColor(Color.argb(SHEET_ALPHA, 0, 0, 0));

        LinearLayout dragZone = new LinearLayout(this);
        dragZone.setOrientation(LinearLayout.VERTICAL);
        dragZone.setGravity(Gravity.CENTER_HORIZONTAL);
        dragZone.setPadding(0, 0, 0, dp(6));
        dragZone.setOnTouchListener(this::onSheetDrag);

        View handle = new View(this);
        handle.setBackgroundResource(R.drawable.bg_sheet_handle);
        LinearLayout.LayoutParams handleParams = new LinearLayout.LayoutParams(dp(54), dp(5));
        handleParams.bottomMargin = dp(12);
        dragZone.addView(handle, handleParams);

        TextView title = new TextView(this);
        title.setText("Meltfall Live");
        title.setTextColor(Color.WHITE);
        title.setTextSize(28f);
        title.setGravity(Gravity.CENTER);
        dragZone.addView(title, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        ));

        Button setWallpaper = new Button(this);
        setWallpaper.setText("Use this wallpaper");
        setWallpaper.setAllCaps(false);
        setWallpaper.setTextSize(18f);
        setWallpaper.setTextColor(Color.rgb(12, 16, 24));
        setWallpaper.setBackgroundResource(R.drawable.bg_primary_button);
        setWallpaper.setOnClickListener(v -> openSystemWallpaperPreview());
        LinearLayout.LayoutParams buttonParams = new LinearLayout.LayoutParams(dp(250), dp(62));
        buttonParams.gravity = Gravity.CENTER_HORIZONTAL;
        buttonParams.topMargin = dp(18);
        dragZone.addView(setWallpaper, buttonParams);

        sheet.addView(dragZone, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        ));

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(false);
        scroll.setOverScrollMode(View.OVER_SCROLL_IF_CONTENT_SCROLLS);

        LinearLayout settings = new LinearLayout(this);
        settings.setOrientation(LinearLayout.VERTICAL);
        settings.setPadding(0, dp(14), 0, dp(28));
        scroll.addView(settings);

        TextView hint = new TextView(this);
        hint.setText("Drag this panel up for live settings");
        hint.setTextColor(Color.rgb(166, 176, 185));
        hint.setTextSize(14f);
        hint.setGravity(Gravity.CENTER);
        settings.addView(hint, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        ));

        speedValue = addPercentControl(settings, "Drop speed", 0, 200, values.speedPercent, value -> {
            values = new RainSettings.Values(value, values.emojiCount, values.sizePercent, values.maxFps, values.showFps);
            RainSettings.save(this, values);
            updateLabels();
        });

        addEmojiCountControl(settings);

        sizeValue = addPercentControl(settings, "Emoji size", 60, 170, values.sizePercent, value -> {
            values = new RainSettings.Values(values.speedPercent, values.emojiCount, value, values.maxFps, values.showFps);
            RainSettings.save(this, values);
            updateLabels();
        });

        addResetControl(settings);

        sheet.addView(scroll, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                1f
        ));

        updateLabels();
        return sheet;
    }

    private boolean onSheetDrag(View view, MotionEvent event) {
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                cancelSheetAnimation();
                dragStartY = event.getRawY();
                dragStartHeight = bottomSheetParams.height;
                return true;
            case MotionEvent.ACTION_MOVE:
                int target = dragStartHeight + Math.round(dragStartY - event.getRawY());
                setSheetHeight(clamp(target, collapsedHeight, expandedHeight));
                return true;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                int midpoint = collapsedHeight + ((expandedHeight - collapsedHeight) / 2);
                animateSheetTo(bottomSheetParams.height >= midpoint ? expandedHeight : collapsedHeight);
                view.performClick();
                return true;
            default:
                return false;
        }
    }

    private TextView addPercentControl(LinearLayout page, String label, int min, int max, int current, IntChange onChange) {
        LinearLayout row = settingRow();

        TextView valueText = new TextView(this);
        valueText.setTextColor(Color.WHITE);
        valueText.setTextSize(20f);
        row.addView(valueText);

        TextView name = mutedText(label, 15f);
        row.addView(name);

        SeekBar seekBar = new SeekBar(this);
        seekBar.setMax(max - min);
        seekBar.setProgress(current - min);
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    onChange.changed(min + progress);
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

    private void addEmojiCountControl(LinearLayout page) {
        LinearLayout row = settingRow();

        TextView title = titleText("Number of emojis", 20f);
        row.addView(title);

        TextView subtitle = mutedText("Enter any whole number", 15f);
        row.addView(subtitle);

        EditText input = new EditText(this);
        input.setText(String.valueOf(values.emojiCount));
        input.setSelectAllOnFocus(true);
        input.setSingleLine(true);
        input.setInputType(InputType.TYPE_CLASS_NUMBER);
        input.setTextColor(Color.WHITE);
        input.setTextSize(20f);
        input.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable editable) {
                String raw = editable.toString().trim();
                if (!raw.isEmpty()) {
                    saveEmojiCount(raw);
                }
            }
        });

        LinearLayout.LayoutParams inputParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(58)
        );
        inputParams.topMargin = dp(10);
        row.addView(input, inputParams);

        addSettingRow(page, row);
    }

    private void saveEmojiCount(String raw) {
        values = new RainSettings.Values(
                values.speedPercent,
                parseEmojiCount(raw),
                values.sizePercent,
                values.maxFps,
                values.showFps
        );
        RainSettings.save(this, values);
    }

    private int parseEmojiCount(String raw) {
        try {
            return Integer.parseInt(raw);
        } catch (NumberFormatException ignored) {
            return Integer.MAX_VALUE;
        }
    }

    private void addResetControl(LinearLayout page) {
        Button reset = new Button(this);
        reset.setText("Reset defaults");
        reset.setAllCaps(false);
        reset.setTextSize(17f);
        reset.setTextColor(Color.WHITE);
        reset.setBackgroundResource(R.drawable.bg_secondary_button);
        reset.setOnClickListener(v -> {
            RainSettings.save(this, new RainSettings.Values(
                    RainSettings.DEFAULT_SPEED,
                    RainSettings.DEFAULT_EMOJI_COUNT,
                    RainSettings.DEFAULT_SIZE,
                    values.maxFps,
                    values.showFps
            ));
            values = RainSettings.load(this);
            recreate();
        });
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(58)
        );
        params.topMargin = dp(14);
        page.addView(reset, params);
    }

    private LinearLayout settingRow() {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.VERTICAL);
        row.setPadding(dp(16), dp(14), dp(16), dp(14));
        row.setBackgroundColor(Color.argb(205, 32, 38, 44));
        return row;
    }

    private void addSettingRow(LinearLayout page, View row) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.topMargin = dp(14);
        page.addView(row, params);
    }

    private TextView titleText(String text, float size) {
        TextView view = new TextView(this);
        view.setText(text);
        view.setTextColor(Color.WHITE);
        view.setTextSize(size);
        return view;
    }

    private TextView mutedText(String text, float size) {
        TextView view = new TextView(this);
        view.setText(text);
        view.setTextColor(Color.rgb(170, 178, 186));
        view.setTextSize(size);
        return view;
    }

    private void updateLabels() {
        speedValue.setText(formatSpeed(values.speedPercent));
        sizeValue.setText(values.sizePercent + "%");
    }

    private String formatSpeed(int speedValue) {
        return String.format(java.util.Locale.US, "%.2fx", speedValue / 100f);
    }

    private void onPreviewChanged(CompoundButton button, boolean checked) {
        topBar.setVisibility(checked ? View.GONE : View.VISIBLE);
        bottomSheet.setVisibility(checked ? View.GONE : View.VISIBLE);
    }

    private void openSystemWallpaperPreview() {
        ComponentName component = new ComponentName(this, FunkyFaceWallpaperService.class);
        Intent intent = new Intent(WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER);
        intent.putExtra(WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT, component);
        startActivity(intent);
    }

    private void animateSheetTo(int targetHeight) {
        cancelSheetAnimation();
        int startHeight = bottomSheetParams.height;
        sheetAnimator = ValueAnimator.ofInt(startHeight, targetHeight);
        sheetAnimator.setDuration(240);
        sheetAnimator.setInterpolator(new DecelerateInterpolator());
        sheetAnimator.addUpdateListener(animation -> setSheetHeight((int) animation.getAnimatedValue()));
        sheetAnimator.start();
        sheetExpanded = targetHeight == expandedHeight;
    }

    private void setSheetHeight(int height) {
        bottomSheetParams.height = clamp(height, collapsedHeight, expandedHeight);
        bottomSheet.setLayoutParams(bottomSheetParams);
    }

    private void cancelSheetAnimation() {
        if (sheetAnimator != null) {
            sheetAnimator.cancel();
            sheetAnimator = null;
        }
    }

    private int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private interface IntChange {
        void changed(int value);
    }
}
