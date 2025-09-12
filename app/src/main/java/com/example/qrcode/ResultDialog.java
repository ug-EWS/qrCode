package com.example.qrcode;

import android.content.DialogInterface;
import android.graphics.drawable.PictureDrawable;
import android.net.Uri;
import android.os.Build;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;

import com.caverock.androidsvg.SVG;
import com.caverock.androidsvg.SVGParseException;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;

public class ResultDialog {
    private QrCodeActivity activity;
    private AlertDialog dialog;
    private AlertDialog.Builder builder;
    private View view;
    private TextView resultText;
    private TextView dateText;
    private TextView formatText;
    private ImageView resultImage;
    private ImageView copyButton;
    private ImageView shareButton;
    private ImageView regenerateButton;
    private ImageView deleteButton;
    private int indexInHistory;
    private String resultContent;
    private String resultPath;
    private boolean mode;

    ResultDialog(QrCodeActivity _activity, boolean _mode, DialogInterface.OnDismissListener onDismissListener) {
        activity = _activity;
        mode = _mode;
        builder = new AlertDialog.Builder(_activity, R.style.Dialog);
        builder.setOnDismissListener(onDismissListener);

        view = activity.getLayoutInflater().inflate(R.layout.result_dialog_layout, null);
        resultText = view.findViewById(R.id.resultText);
        dateText = view.findViewById(R.id.date);
        formatText = view.findViewById(R.id.format);
        resultImage = view.findViewById(R.id.resultImage);
        copyButton = view.findViewById(R.id.copyButton);
        shareButton = view.findViewById(R.id.shareButton);
        regenerateButton = view.findViewById(R.id.regenerateButton);
        deleteButton = view.findViewById(R.id.deleteButton);

        resultImage.setVisibility(mode ? View.GONE : View.VISIBLE);
        copyButton.setVisibility(mode ? View.VISIBLE : View.GONE);
        regenerateButton.setVisibility(mode ? View.VISIBLE : View.GONE);
        formatText.setVisibility(mode ? View.GONE : View.VISIBLE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            copyButton.setTooltipText(activity.getText(R.string.copy));
            shareButton.setTooltipText(activity.getText(R.string.share));
            regenerateButton.setTooltipText(activity.getText(R.string.regenerate));
            deleteButton.setTooltipText(activity.getText(R.string.delete));
        }

        copyButton.setOnClickListener(v -> activity.copyResult(resultContent));
        shareButton.setOnClickListener(v -> activity.shareResult(indexInHistory));
        regenerateButton.setOnClickListener(v -> {
            dialog.dismiss();
            activity.regenerateResult(resultContent);
        });
        deleteButton.setOnClickListener(v -> {
            dialog.dismiss();
            activity.deleteResult(indexInHistory);
        });
        builder.setView(view);
        dialog = builder.create();
    }

    public void showResult(HistoryEntry entry, int _indexInHistory) {
        resultContent = entry.content;
        resultText.setText(resultContent);
        dateText.setText(entry.getVisibleDate("d MMM yyyy, HH.mm"));
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
        dialog.show();
    }
}