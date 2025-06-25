package com.example.qrcode;

import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.provider.MediaStore;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;

import androidx.activity.EdgeToEdge;
import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.TooltipCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.canhub.cropper.CropImageView;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.net.URI;

public class CropActivity extends AppCompatActivity {
    private ImageView backButton;
    private ImageView flipButton;
    private ImageView leftButton;
    private ImageView rightButton;
    private ImageView doneButton;
    private CropImageView cropImageView;
    private String savePath;
    private AlertDialog dialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_crop);
        getOnBackPressedDispatcher().addCallback(new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                cancel();
            }
        });

        backButton = findViewById(R.id.backButton);
        backButton.setOnClickListener(v -> cancel());
        TooltipCompat.setTooltipText(backButton, getString(R.string.cancel));

        flipButton = findViewById(R.id.flipButton);
        flipButton.setOnClickListener(v -> cropImageView.flipImageHorizontally());
        TooltipCompat.setTooltipText(flipButton, getString(R.string.flip_horizontally));

        leftButton = findViewById(R.id.leftButton);
        leftButton.setOnClickListener(v -> cropImageView.rotateImage(-45));
        TooltipCompat.setTooltipText(leftButton, getString(R.string.rotate_ccw));

        rightButton = findViewById(R.id.rightButton);
        rightButton.setOnClickListener(v -> cropImageView.rotateImage(45));
        TooltipCompat.setTooltipText(rightButton, getString(R.string.rotate_cw));

        doneButton = findViewById(R.id.doneButton);
        doneButton.setOnClickListener(v -> {
            File file = new File(savePath);
            try {
                FileOutputStream fos = new FileOutputStream(savePath);
                resizeBitmap(cropImageView.getCroppedImage()).compress(Bitmap.CompressFormat.PNG,100, fos);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (NullPointerException e) {
                e.printStackTrace();
            }
            Intent intent = new Intent(null, Uri.fromFile(file));
            setResult(RESULT_OK, intent);
            finish();
        });
        TooltipCompat.setTooltipText(doneButton, getString(R.string.scan_this));

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets insets1 = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(insets1.left, insets1.top, insets1.right, insets1.bottom);
            return insets;
        });

        cropImageView = findViewById(R.id.cropImageView);
        savePath = getExternalFilesDir(null).getAbsolutePath().concat("/temp.png");
    }

    @Override
    protected void onStart() {
        super.onStart();
        Intent intent = getIntent();
        cropImageView.setImageUriAsync(intent.getData());
    }

    private void cancel() {
        setResult(RESULT_CANCELED);
        finish();
    }

    private Bitmap resizeBitmap(Bitmap _bitmap) {
        int width = _bitmap.getWidth();
        int height = _bitmap.getHeight();
        double ratio = (double) width / height;
        boolean wide = width > height;
        int newWidth, newHeight;
        if (wide) {
            newWidth = Math.min(256, width);
            newHeight = (int) (newWidth / ratio);
        } else {
            newHeight = Math.min(256, height);
            newWidth = (int) (newHeight * ratio);
        }
        return Bitmap.createScaledBitmap(_bitmap, newWidth, newHeight,false);
    }
}