package com.example.qrcode;

import android.content.Context;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;

import java.util.function.Consumer;
import java.util.function.Function;

public class SeekBarDialog extends BottomSheetDialog {
    SeekBarDialog(
            Context _context,
            int _title,
            int _icon,
            int _description,
            int _initialValue,
            int _maxSeekBarValue,
            Function<Integer, Integer> _toProgressConverter,
            Function<Integer, Integer> _toValueConverter,
            boolean _showUnit,
            Consumer<Integer> _listener) {
        super(_context, R.style.BottomSheetDialogTheme);
        setContentView(R.layout.dialog_seek_bar);
        ImageView cancelButton = findViewById(R.id.cancelButton);
        TextView title = findViewById(R.id.title);
        LinearLayout applyButton = findViewById(R.id.addButton);
        TextView description = findViewById(R.id.description);
        TextView displayValue = findViewById(R.id.text);
        SeekBar seekBar = findViewById(R.id.seekBar);

        cancelButton.setOnClickListener(v -> cancel());
        title.setText(_title);
        description.setText(_description);
        seekBar.setMax(_maxSeekBarValue);
        seekBar.setProgress(_toProgressConverter.apply(_initialValue));
        displayValue.setText(String.format(_showUnit ? "%d px" : "%d", _initialValue));
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                displayValue.setText(String.format(_showUnit ? "%d px" : "%d", _toValueConverter.apply(progress)));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
        applyButton.setOnClickListener(v -> {
            dismiss();
            _listener.accept(_toValueConverter.apply(seekBar.getProgress()));
        });

        getBehavior().setState(BottomSheetBehavior.STATE_EXPANDED);
    }
}
