package com.example.qrcode;

import android.content.Context;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import android.widget.SeekBar;
import android.widget.TextView;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;

import java.util.function.Consumer;

public class ColorDialog extends BottomSheetDialog {
    ColorDialog(Context _context, QRCodeColor _qrCodeColor, Consumer<QRCodeColor> _listener) {
        super(_context, R.style.BottomSheetDialogTheme);
        setContentView(R.layout.dialog_color);

        ImageView cancelButton = findViewById(R.id.cancelButton);
        LinearLayout applyButton = findViewById(R.id.applyButton);
        RadioGroup radioGroup = findViewById(R.id.radioGroup);
        LinearLayout hueLayout = findViewById(R.id.hueLayout);
        TextView hue = findViewById(R.id.hue);
        SeekBar seekBar = findViewById(R.id.seekBar);
        LinearLayout shadeLayout = findViewById(R.id.shadeLayout);
        TextView shade = findViewById(R.id.shade);
        SeekBar shadeSeekBar = findViewById(R.id.shadeSeekBar);
        ImageView preview = findViewById(R.id.preview);

        QRCodeColor preCol = new QRCodeColor();
        preCol.setColorMode(_qrCodeColor.getColorMode());
        preCol.setHue(_qrCodeColor.getHue());
        preCol.setShade(_qrCodeColor.getShade());

        radioGroup.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.blackWhite) {
                preCol.setColorMode(QRCodeColor.COLOR_BLACK_WHITE);
                hueLayout.setVisibility(View.GONE);
                shadeLayout.setVisibility(View.GONE);
            }
            if (checkedId == R.id.foreground) {
                preCol.setColorMode(QRCodeColor.COLOR_FOREGROUND);
                hueLayout.setVisibility(View.VISIBLE);
                shadeLayout.setVisibility(View.VISIBLE);
            }
            if (checkedId == R.id.background) {
                preCol.setColorMode(QRCodeColor.COLOR_BACKGROUND);
                hueLayout.setVisibility(View.VISIBLE);
                shadeLayout.setVisibility(View.VISIBLE);
            }
            preview.setBackgroundColor(preCol.backgroundColorInt);
            preview.setColorFilter(preCol.foregroundColorInt);
        });

        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                preCol.setHue(progress);
                hue.setText(String.valueOf(progress));
                preview.setBackgroundColor(preCol.backgroundColorInt);
                preview.setColorFilter(preCol.foregroundColorInt);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        shadeSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                preCol.setShade(progress);
                shade.setText(String.valueOf(progress));
                preview.setBackgroundColor(preCol.backgroundColorInt);
                preview.setColorFilter(preCol.foregroundColorInt);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        radioGroup.check(
                preCol.getColorMode() == QRCodeColor.COLOR_BLACK_WHITE ? R.id.blackWhite :
                        preCol.getColorMode() == QRCodeColor.COLOR_FOREGROUND ? R.id.foreground :
                                R.id.background);

        seekBar.setProgress(preCol.getHue());
        hueLayout.setVisibility(preCol.getColorMode() == QRCodeColor.COLOR_BLACK_WHITE ? View.GONE : View.VISIBLE);
        shadeSeekBar.setProgress(preCol.getShade());
        shadeLayout.setVisibility(preCol.getColorMode() == QRCodeColor.COLOR_BLACK_WHITE ? View.GONE : View.VISIBLE);

        cancelButton.setOnClickListener(v -> cancel());
        applyButton.setOnClickListener(v -> {
            dismiss();
            _listener.accept(preCol);
        });

        getBehavior().setState(BottomSheetBehavior.STATE_EXPANDED);
    }
}
