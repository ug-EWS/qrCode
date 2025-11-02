package com.example.qrcode;

import android.content.DialogInterface;
import android.graphics.drawable.PictureDrawable;
import android.net.Uri;
import android.os.Build;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;

import com.caverock.androidsvg.SVG;
import com.caverock.androidsvg.SVGParseException;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;

public class ResultDialog extends BottomSheetDialog {
    private QrCodeActivity activity;
    private TextView resultText;
    private TextView dateText;
    private TextView formatText;
    private ImageView resultImage;
    private LinearLayout copyButton;
    private LinearLayout shareButton;
    private LinearLayout regenerateButton;
    private LinearLayout deleteButton;
    private int indexInHistory;
    private String resultContent;
    private String resultPath;
    private boolean mode;

    ResultDialog(QrCodeActivity _activity, boolean _mode, DialogInterface.OnDismissListener onDismissListener) {
        super(_activity, R.style.BottomSheetDialogTheme);
        activity = _activity;
        mode = _mode;

        setOnDismissListener(onDismissListener);

        setContentView(R.layout.result_dialog_layout);
        resultText = findViewById(R.id.resultText);
        dateText = findViewById(R.id.date);
        formatText = findViewById(R.id.format);
        resultImage = findViewById(R.id.resultImage);
        copyButton = findViewById(R.id.copyButton);
        shareButton = findViewById(R.id.shareButton);
        regenerateButton = findViewById(R.id.regenerateButton);
        deleteButton = findViewById(R.id.deleteButton);

        resultImage.setVisibility(mode ? View.GONE : View.VISIBLE);
        copyButton.setVisibility(mode ? View.VISIBLE : View.GONE);
        regenerateButton.setVisibility(mode ? View.VISIBLE : View.GONE);
        formatText.setVisibility(mode ? View.GONE : View.VISIBLE);

        copyButton.setOnClickListener(v -> activity.copyResult(resultContent));
        shareButton.setOnClickListener(v -> activity.shareResult(indexInHistory));
        regenerateButton.setOnClickListener(v -> {
            dismiss();
            activity.regenerateResult(resultContent);
        });
        deleteButton.setOnClickListener(v -> {
            dismiss();
            activity.deleteResult(indexInHistory);
        });

        getBehavior().setState(BottomSheetBehavior.STATE_EXPANDED);
    }

    public void showResult(HistoryEntry entry, int _indexInHistory) {
        resultContent = entry.content;
        resultText.setText(resultContent);
        dateText.setText(entry.getVisibleDate());
        if (!mode) {
            resultPath = entry.path;
            formatText.setText(resultPath.endsWith(".png") ? "PNG"
                    : resultPath.endsWith(".jpeg") ? "JPEG"
                    : resultPath.endsWith(".webp") ? "WEBP"
                    : "SVG");
            if (resultPath.endsWith(".svg")) {
                try {
                    InputStream is = new FileInputStream(resultPath);
                    SVG svg = SVG.getFromInputStream(is);
                    resultImage.setImageDrawable(new PictureDrawable(svg.renderToPicture()));
                } catch (FileNotFoundException | SVGParseException e) {
                    e.printStackTrace();
                    resultImage.setImageDrawable(null);
                }
            }
            else resultImage.setImageURI(Uri.parse(resultPath));
        }
        indexInHistory = _indexInHistory;
        show();
    }
}