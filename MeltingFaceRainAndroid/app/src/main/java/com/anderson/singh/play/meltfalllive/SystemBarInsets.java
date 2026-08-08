package com.anderson.singh.play.meltfalllive;

import android.view.View;
import android.view.WindowInsets;

public final class SystemBarInsets {
    private SystemBarInsets() {
    }

    public static void applyNavigationBarPadding(View view) {
        int left = view.getPaddingLeft();
        int top = view.getPaddingTop();
        int right = view.getPaddingRight();
        int bottom = view.getPaddingBottom();

        view.setOnApplyWindowInsetsListener((target, insets) -> {
            target.setPadding(
                    left + insets.getSystemWindowInsetLeft(),
                    top,
                    right + insets.getSystemWindowInsetRight(),
                    bottom + insets.getSystemWindowInsetBottom()
            );
            return insets;
        });
        view.post(view::requestApplyInsets);
    }
}
