package com.example.qrcode;

import uk.org.okapibarcode.graphics.Color;

public class QRCodeColor {
    private int hue;
    private int shade;
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
        shade = 5;
        colorMode = COLOR_BLACK_WHITE;
        setBackgroundColor();
        setForegroundColor();
    }

    public void setHue(int _hue) {
        hue = _hue;
        setBackgroundColor();
        setForegroundColor();
    }

    public void setShade(int _shade) {
        shade = _shade;
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

    public int getShade() {
        return shade;
    }

    public int getColorMode() {
        return colorMode;
    }

    private void setBackgroundColor() {
        if (colorMode == COLOR_BACKGROUND) {
            float[] hsv = {hue, 0.3F, 0.8F + (shade * 0.02F)};
            int color = android.graphics.Color.HSVToColor(hsv);
            int r = android.graphics.Color.red(color);
            int g = android.graphics.Color.green(color);
            int b = android.graphics.Color.blue(color);
            backgroundColor =  new Color(r, g, b);
            backgroundColorInt = color;
        } else {
            backgroundColor = Color.WHITE;
            backgroundColorInt = android.graphics.Color.WHITE;
        }
    }

    private void setForegroundColor() {
        if (colorMode == COLOR_FOREGROUND) {
            float[] hsv = {hue, 0.8F, 0.25F + (shade * 0.02F)};
            int color = android.graphics.Color.HSVToColor(hsv);
            int r = android.graphics.Color.red(color);
            int g = android.graphics.Color.green(color);
            int b = android.graphics.Color.blue(color);
            foregroundColor = new Color(r, g, b);
            foregroundColorInt = color;
        } else {
            foregroundColor = Color.BLACK;
            foregroundColorInt = android.graphics.Color.BLACK;
        }
    }
}
