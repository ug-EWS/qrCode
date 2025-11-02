package com.example.qrcode;

import android.content.Context;
import android.graphics.Bitmap;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;

public class PreviewDialog extends BottomSheetDialog {
    PreviewDialog(Context _context, Bitmap _bitmap, String _description) {
        super(_context, R.style.BottomSheetDialogTheme);
        setContentView(R.layout.dialog_preview);
        ImageView cancelButton = findViewById(R.id.cancelButton);
        ImageView preview = findViewById(R.id.image);
        TextView description = findViewById(R.id.text);
        cancelButton.setOnClickListener(v -> cancel());
        preview.setImageBitmap(_bitmap);
        description.setText(_description);

        getBehavior().setState(BottomSheetBehavior.STATE_EXPANDED);
    }
}
