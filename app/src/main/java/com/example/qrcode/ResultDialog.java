package com.example.qrcode;

import android.content.DialogInterface;
import android.net.Uri;
import android.os.Build;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;

public class ResultDialog {
    private QrCodeActivity activity;
    private AlertDialog dialog;
    private AlertDialog.Builder builder;
    private View view;
    private TextView resultText;
    private ImageView resultImage;
    private Button copyButton;
    private Button shareButton;
    private Button regenerateButton;
    private Button deleteButton;
    private int indexInHistory;
    private String resultContent;
    private String resultPath;
    private boolean mode;

    ResultDialog(QrCodeActivity _activity, boolean _mode, DialogInterface.OnDismissListener onDismissListener) {
        activity = _activity;
        mode = _mode;
        builder = new AlertDialog.Builder(_activity, R.style.Dialog);
        builder.setOnDismissListener(onDismissListener);
        if (mode) builder.setTitle("Sonuç");

        view = activity.getLayoutInflater().inflate(R.layout.result_dialog_layout, null);
        resultText = view.findViewById(R.id.resultText);
        resultImage = view.findViewById(R.id.resultImage);
        copyButton = view.findViewById(R.id.copyButton);
        shareButton = view.findViewById(R.id.shareButton);
        regenerateButton = view.findViewById(R.id.regenerateButton);
        deleteButton = view.findViewById(R.id.deleteButton);

        resultImage.setVisibility(mode ? View.GONE : View.VISIBLE);
        copyButton.setVisibility(mode ? View.VISIBLE : View.GONE);
        regenerateButton.setVisibility(mode ? View.VISIBLE : View.GONE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            copyButton.setTooltipText(activity.getText(R.string.copy));
            shareButton.setTooltipText(activity.getText(R.string.share));
            regenerateButton.setTooltipText(activity.getText(R.string.regenerate));
            deleteButton.setTooltipText(activity.getText(R.string.delete));
        }

        copyButton.setOnClickListener(v -> activity.copyResult(resultContent));
        shareButton.setOnClickListener(mode ? v -> activity.shareResult(resultContent) : v -> activity.shareResult(resultContent, resultPath));
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
        if (!mode) {
            resultPath = entry.path;
            resultImage.setImageURI(Uri.parse(resultPath));
        }
        indexInHistory = _indexInHistory;
        dialog.show();
    }
}