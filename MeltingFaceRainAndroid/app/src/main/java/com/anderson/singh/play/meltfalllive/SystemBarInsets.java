package com.anderson.singh.play.meltfalllive;

import android.os.Build;
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
            int[] systemBars = systemBarInsets(insets);
            target.setPadding(
                    left + systemBars[0],
                    top,
                    right + systemBars[2],
                    bottom + systemBars[3]
            );
            return insets;
        });
        view.post(view::requestApplyInsets);
    }

    private static int[] systemBarInsets(WindowInsets insets) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            android.graphics.Insets systemBars = insets.getInsets(WindowInsets.Type.systemBars());
            return new int[] {
                    systemBars.left,
                    systemBars.top,
                    systemBars.right,
                    systemBars.bottom
            };
        }
        return systemBarInsetsLegacy(insets);
    }

    @SuppressWarnings("deprecation")
    private static int[] systemBarInsetsLegacy(WindowInsets insets) {
        return new int[] {
                insets.getSystemWindowInsetLeft(),
                insets.getSystemWindowInsetTop(),
                insets.getSystemWindowInsetRight(),
                insets.getSystemWindowInsetBottom()
        };
    }
}
