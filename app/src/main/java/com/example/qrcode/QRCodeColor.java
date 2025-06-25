package com.example.qrcode;

import android.os.Build;

import uk.org.okapibarcode.graphics.Color;

public class QRCodeColor {
    private int hue;
    private int colorMode;
    public static final int COLOR_BLACK_WHITE = 0;
    public static final int COLOR_BACKGROUND = 1;
    public static final int COLOR_FOREGROUND = 2;
    public int backgroundColorInt;
    public int foregroundColorInt;
    public Color backgroundColor;
    public Color foregroundColor;

    QRCodeColor() {
        hue = 0;
        colorMode = COLOR_BLACK_WHITE;
        setBackgroundColor();
        setForegroundColor();
    }

    public void setHue(int _hue) {
        hue = _hue;
        setBackgroundColor();
        setForegroundColor();
    }

    public void setColorMode(int _colorMode) {
        colorMode = _colorMode;
        setBackgroundColor();
        setForegroundColor();
    }

    public int getHue() {
        return hue;
    }

    public int getColorMode() {
        return colorMode;
    }

    private void setBackgroundColor() {
        if (colorMode == COLOR_BACKGROUND) {
            float[] hsv = {hue, 0.3F, 0.8F};
            int color = android.graphics.Color.HSVToColor(hsv);
            int r = android.graphics.Color.red(color);
            int g = android.graphics.Color.green(color);
            int b = android.graphics.Color.blue(color);
            backgroundColor =  new Color(r, g, b);
            backgroundColorInt = android.graphics.Color.rgb(r, g, b);
        } else {
            backgroundColor = Color.WHITE;
            backgroundColorInt = android.graphics.Color.WHITE;
        }
    }

    private void setForegroundColor() {
        if (colorMode == COLOR_FOREGROUND) {
            float[] hsv = {hue, 0.8F, 0.4F};
            int color = android.graphics.Color.HSVToColor(hsv);
            int r = android.graphics.Color.red(color);
            int g = android.graphics.Color.green(color);
            int b = android.graphics.Color.blue(color);
            foregroundColor = new Color(r, g, b);
            foregroundColorInt = android.graphics.Color.rgb(r, g, b);
        } else {
            foregroundColor = Color.BLACK;
            foregroundColorInt = android.graphics.Color.BLACK;
        }
    }
}
