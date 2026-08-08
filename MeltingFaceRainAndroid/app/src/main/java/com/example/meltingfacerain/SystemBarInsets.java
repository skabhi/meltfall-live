package com.example.meltingfacerain;

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
                    left,
                    top,
                    right,
                    bottom + insets.getSystemWindowInsetBottom()
            );
            return insets;
        });
        view.requestApplyInsets();
    }
}
