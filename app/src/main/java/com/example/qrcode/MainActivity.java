package com.example.qrcode;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.UiModeManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.ImageDecoder;
import android.hardware.Camera;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.TextWatcher;
import android.transition.TransitionManager;
import android.view.Menu;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowInsets;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.PickVisualMediaRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.cardview.widget.CardView;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.budiyev.android.codescanner.CodeScanner;
import com.budiyev.android.codescanner.CodeScannerView;
import com.caverock.androidsvg.SVG;
import com.caverock.androidsvg.SVGParseException;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.transition.platform.MaterialSharedAxis;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.ChecksumException;
import com.google.zxing.FormatException;
import com.google.zxing.LuminanceSource;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.NotFoundException;
import com.google.zxing.RGBLuminanceSource;
import com.google.zxing.Reader;
import com.google.zxing.Result;
import com.google.zxing.common.HybridBinarizer;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Timer;
import java.util.TimerTask;

import uk.org.okapibarcode.backend.HumanReadableLocation;
import uk.org.okapibarcode.backend.OkapiInputException;
import uk.org.okapibarcode.backend.Symbol;
import uk.org.okapibarcode.output.SvgRenderer;

public class MainActivity extends QrCodeActivity {
    private ImageView optionsButton;
    private TabLayout tabLayout;
    private LinearLayout scanScreen;
    private LinearLayout generateScreen;
    private CardView scanCard;
    private CodeScannerView scannerView;
    private EditText generateEditText;
    private LinearLayout colorButton;
    private TextView colorText;
    private LinearLayout marginButton;
    private TextView marginText;
    private LinearLayout formatButton;
    private TextView formatText;
    private LinearLayout infoLayout;
    private LinearLayout warningLayout;
    private TextView warningText;
    private LinearLayout resultLayout;
    private ImageView resultImage;
    private Button saveButton;
    private Button shareButton;
    private Button galleryButton;

    private CodeScanner codeScanner;
    private ResultDialog resultDialog;

    private QRCodeColor qrCodeColor;
    private int margin;
    private int format;
    private int formatSelection;
    private int zoom;
    private SupportedBarcodeFormats supportedBarcodeFormats;
    private HistoryManager scanHistory;
    private HistoryManager generateHistory;

    private boolean saved;
    private int theme;
    private Timer timer;

    private SharedPreferences sp;
    private SharedPreferences.Editor spe;

    private Bitmap generatedBitmap;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        initializeUi();

        sp = getSharedPreferences("QrCodeData", MODE_PRIVATE);
        spe = sp.edit();

        scannerView = findViewById(R.id.scannerView);
        codeScanner = new CodeScanner(this, scannerView, CodeScanner.CAMERA_BACK);
        codeScanner.setDecodeCallback(result -> runOnUiThread(() -> newScanEntry(result.getText())));
        codeScanner.setErrorCallback(thrown -> showMessage(getString(R.string.could_not_scan)));

        setCurrentTab(0);

        supportedBarcodeFormats = new SupportedBarcodeFormats();
        qrCodeColor = new QRCodeColor();
        margin = 32;
        format = 0;
        saved = false;

        theme = sp.getInt("theme", 0);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 101) {
            if (grantResults[0] == PackageManager.PERMISSION_DENIED) {
                showMessage(getString(R.string.grant_permission));
                scannerView.setVisibility(View.GONE);
            } else {
                codeScanner.startPreview();
            }
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        Intent intent = getIntent();
        if (intent.getAction().equals("com.qrx.ACTION_REGENERATE")) {
            regenerateResult(intent.getStringExtra("content"));
        }
        if (checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_DENIED) {
            String[] permissions = {Manifest.permission.CAMERA};
            requestPermissions(permissions, 101);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (tabLayout.getSelectedTabPosition() == 0) restartCameraPreview();

        scanHistory = new HistoryManager();
        scanHistory.fromJson(sp.getString("scan", ""));
        resultDialog = new ResultDialog(this, true, dialog -> runOnUiThread(() -> codeScanner.startPreview()));

        generateHistory = new HistoryManager();
        generateHistory.fromJson(sp.getString("generate", ""));
    }

    @Override
    protected void onPause() {
        if (tabLayout.getSelectedTabPosition() == 0) codeScanner.stopPreview();
        super.onPause();
    }

    @Override
    public void regenerateResult(String text) {
        super.regenerateResult(text);
        tabLayout.selectTab(tabLayout.getTabAt(1));
        generateEditText.setText(text);
        generateCode();
    }

    @Override
    public void deleteResult(int index) {
        super.deleteResult(index);
        scanHistory.removeEntryAt(index);
        showMessage(getString(R.string.deleted));
    }

    @Override
    public void deleteResult(ArrayList<Integer> indexes) {
        super.deleteResult(indexes);
        scanHistory.removeEntriesAt(indexes);
        showMessage(getString(R.string.deleted));
    }

    private void restartCameraPreview() {
        codeScanner.setCamera(CodeScanner.CAMERA_BACK);
        codeScanner.startPreview();
    }

    @SuppressLint("ClickableViewAccessibility")
    private void initializeUi() {
        optionsButton = findViewById(R.id.doneButton);
        tabLayout = findViewById(R.id.tabLayout);
        scanScreen = findViewById(R.id.scanScreen);
        generateScreen = findViewById(R.id.generateScreen);
        scanCard = findViewById(R.id.scanCard);
        scannerView = findViewById(R.id.scannerView);
        generateEditText = findViewById(R.id.generateEditText);
        colorButton = findViewById(R.id.colorButton);
        colorText = findViewById(R.id.colorText);
        marginButton = findViewById(R.id.marginButton);
        marginText = findViewById(R.id.marginText);
        formatButton = findViewById(R.id.formatButton);
        formatText = findViewById(R.id.formatText);
        infoLayout = findViewById(R.id.infoLayout);
        warningLayout = findViewById(R.id.warningLayout);
        warningText = findViewById(R.id.warningText);
        resultLayout = findViewById(R.id.resultLayout);
        resultImage = findViewById(R.id.resultImage);
        saveButton = findViewById(R.id.saveButton);
        shareButton = findViewById(R.id.shareButton);
        optionsButton.setOnClickListener(v -> getOptionsPopupMenu().show());
        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                int index = tab.getPosition();
                setCurrentTab(index);
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {

            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {

            }
        });
        ScaleGestureDetector scaleGestureDetector = new ScaleGestureDetector(this, new ScaleGestureDetector.OnScaleGestureListener() {
            @Override
            public boolean onScale(@NonNull ScaleGestureDetector detector) {
                int i = (int) (detector.getCurrentSpan() - detector.getPreviousSpan()) / 8;
                zoom = Math.max(zoom + i, 0);
                codeScanner.setZoom(zoom);
                return true;
            }

            @Override
            public boolean onScaleBegin(@NonNull ScaleGestureDetector detector) {
                return true;
            }

            @Override
            public void onScaleEnd(@NonNull ScaleGestureDetector detector) {
            }
        });
        scannerView.setOnTouchListener((v, event) -> {
            scaleGestureDetector.onTouchEvent(event);
            return true;
        });

        generateEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (timer != null) timer.cancel();
                timer = new Timer();
                timer.schedule(new TimerTask() {
                    @Override
                    public void run() {
                        runOnUiThread(() -> {
                            generateCode();
                            timer = null;
                        });
                    }
                    }, 500);
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });

        colorButton.setOnClickListener(v -> showColorDialog());
        marginButton.setOnClickListener(v -> showMarginDialog());
        formatButton.setOnClickListener(v -> showFormatDialog());
        saveButton.setOnClickListener(v -> saveGenerated());
        shareButton.setOnClickListener(v -> shareGenerated());

        galleryButton = findViewById(R.id.galleryButton);
        galleryButton.setOnClickListener(v ->
            uriResultLauncher.launch(new PickVisualMediaRequest.Builder()
                    .setMediaType(ActivityResultContracts.PickVisualMedia.ImageOnly.INSTANCE)
                    .build()));

        resultImage.setOnClickListener(v -> {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            ImageView imageView = new ImageView(this);
            imageView.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            imageView.setImageBitmap(generatedBitmap);
            builder.setTitle(R.string.preview);
            builder.setView(imageView);
            builder.setPositiveButton(R.string.dialog_ok, null);
            builder.create().show();
        });

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets insets1 = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(insets1.left, insets1.top, insets1.right, insets1.bottom);
            return insets;
        });
    }

    private void setAppTheme(int _theme) {
        theme = _theme;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
            ((UiModeManager) getSystemService(UI_MODE_SERVICE)).setApplicationNightMode(theme);
        else AppCompatDelegate.setDefaultNightMode(theme == 0 ? AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM : theme);
    }

    private final ActivityResultLauncher<Intent> bitmapResultLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            (result) -> {
                if (result.getResultCode() == RESULT_OK) {
                    Intent intent = result.getData();
                    Uri uri = intent != null ? intent.getData() : null;
                    Bitmap bitmap;
                    try {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                            ImageDecoder.Source source = ImageDecoder.createSource(getContentResolver(), uri);
                            bitmap = ImageDecoder.decodeBitmap(source);
                        }
                        else {
                            bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), uri);
                        }
                        Bitmap soft = null;
                        if (bitmap != null) soft = bitmap.copy(Bitmap.Config.ARGB_8888, false);
                        if (soft != null) scanImage(soft); else showMessage("null");
                    } catch (IOException e) {
                        showMessage("Dosyaya erişilemiyor");
                        e.printStackTrace();
                    }
                } else {
                    showMessage(getString(R.string.cancelled));
                }
            }
    );

    private final ActivityResultLauncher<PickVisualMediaRequest> uriResultLauncher = registerForActivityResult(
            new ActivityResultContracts.PickVisualMedia(),
            (result) -> {
                if (result != null) {
                    Intent intent = new Intent();
                    intent.setClass(getApplicationContext(), CropActivity.class);
                    intent.setData(result);
                    bitmapResultLauncher.launch(intent);
                }
            });

    private void scanImage(Bitmap bitmap) {
        try {
            int[] intArray = new int[bitmap.getWidth() * bitmap.getHeight()];
            bitmap.getPixels(intArray, 0, bitmap.getWidth(), 0, 0, bitmap.getWidth(), bitmap.getHeight());
            LuminanceSource source = new RGBLuminanceSource(bitmap.getWidth(), bitmap.getHeight(), intArray);
            BinaryBitmap binaryBitmap = new BinaryBitmap(new HybridBinarizer(source));
            Reader reader = new MultiFormatReader();
            Result result = reader.decode(binaryBitmap);
            newScanEntry(result.getText());
        } catch (NotFoundException e) {
            showMessage(getString(R.string.barcode_not_found));
        } catch (ChecksumException e) {
            showMessage("Checksum exception");
        } catch (FormatException e) {
            showMessage("Format exception");
        }
    }

    private void newScanEntry(String content) {
        HistoryEntry entry = new HistoryEntry(content, "", Calendar.getInstance().getTimeInMillis());
        scanHistory.addEntry(entry);
        resultDialog.showResult(entry, 0);
        spe.putString("scan", scanHistory.getJson()).commit();
    }

    private PopupMenu getOptionsPopupMenu() {
        PopupMenu popupMenu = new PopupMenu(this, optionsButton);
        popupMenu.inflate(R.menu.options);
        Menu menu = popupMenu.getMenu();
        if (theme == 0) menu.findItem(R.id.auto).setChecked(true);
        if (theme == 1) menu.findItem(R.id.light).setChecked(true);
        if (theme == 2) menu.findItem(R.id.dark).setChecked(true);
        popupMenu.setOnMenuItemClickListener(item -> {
            int id = item.getItemId();
            if (id == R.id.scanned) {
                Intent intent = new Intent();
                intent.setClass(getApplicationContext(), ScanHistoryActivity.class);
                startActivity(intent);
                return true;
            }
            if (id == R.id.generated) {
                Intent intent = new Intent();
                intent.setClass(getApplicationContext(), GenerateHistoryActivity.class);
                startActivity(intent);
                return true;
            }
            if (id == R.id.light) {
                setAppTheme(1);
                spe.putInt("theme", theme).commit();
                return true;
            }
            if (id == R.id.dark) {
                setAppTheme(2);
                spe.putInt("theme", theme).commit();
                return true;
            }
            if (id == R.id.auto) {
                setAppTheme(0);
                spe.putInt("theme", theme).commit();
                return true;
            }
            return false;
        });
        return popupMenu;
    }

    private void setCurrentTab(int index) {
        TransitionManager.beginDelayedTransition(findViewById(R.id.mainScreen), new MaterialSharedAxis(MaterialSharedAxis.X, index == 1));
        scanScreen.setVisibility(index == 0 ? View.VISIBLE : View.GONE);
        generateScreen.setVisibility(index == 1 ? View.VISIBLE : View.GONE);
        if (index == 0) restartCameraPreview(); else codeScanner.stopPreview();
    }

    private void showColorDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.Dialog);

        View dialogView = getLayoutInflater().inflate(R.layout.dialog_color, null);
        RadioGroup radioGroup = dialogView.findViewById(R.id.radioGroup);
        RadioButton blackWhite = dialogView.findViewById(R.id.blackWhite);
        RadioButton foreground = dialogView.findViewById(R.id.foreground);
        RadioButton background = dialogView.findViewById(R.id.background);
        LinearLayout hueLayout = dialogView.findViewById(R.id.hueLayout);
        TextView hue = dialogView.findViewById(R.id.hue);
        SeekBar seekBar = dialogView.findViewById(R.id.seekBar);
        ImageView preview = dialogView.findViewById(R.id.preview);

        QRCodeColor preCol = new QRCodeColor();

        radioGroup.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.blackWhite) {
                preCol.setColorMode(QRCodeColor.COLOR_BLACK_WHITE);
                preview.setBackgroundColor(Color.WHITE);
                preview.setColorFilter(Color.BLACK);
                hueLayout.setVisibility(View.GONE);
            }
            if (checkedId == R.id.foreground) {
                preCol.setColorMode(QRCodeColor.COLOR_FOREGROUND);
                preview.setBackgroundColor(Color.WHITE);
                preview.setColorFilter(preCol.foregroundColorInt);
                hueLayout.setVisibility(View.VISIBLE);
            }
            if (checkedId == R.id.background) {
                preCol.setColorMode(QRCodeColor.COLOR_BACKGROUND);
                preview.setColorFilter(Color.BLACK);
                preview.setBackgroundColor(preCol.backgroundColorInt);
                hueLayout.setVisibility(View.VISIBLE);
            }
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

        radioGroup.check(
                qrCodeColor.getColorMode() == qrCodeColor.COLOR_BLACK_WHITE ? R.id.blackWhite :
                        qrCodeColor.getColorMode() == qrCodeColor.COLOR_FOREGROUND ? R.id.foreground :
                                R.id.background);

        seekBar.setProgress(qrCodeColor.getHue());
        hueLayout.setVisibility(preCol.getColorMode() == QRCodeColor.COLOR_BLACK_WHITE ? View.GONE : View.VISIBLE);

        builder.setTitle(R.string.color);
        builder.setIcon(R.drawable.baseline_color_lens_24);
        builder.setView(dialogView);
        builder.setPositiveButton(R.string.dialog_ok, (dialog, which) -> {
            if (blackWhite.isChecked()) {
                qrCodeColor.setColorMode(QRCodeColor.COLOR_BLACK_WHITE);
                colorText.setText(R.string.black_white);
            }
            if (foreground.isChecked()) {
                qrCodeColor.setColorMode(QRCodeColor.COLOR_FOREGROUND);
                colorText.setText(R.string.colored_foreground);
            }
            if (background.isChecked()) {
                qrCodeColor.setColorMode(QRCodeColor.COLOR_BACKGROUND);
                colorText.setText(R.string.colored_background);
            }
            qrCodeColor.setHue(seekBar.getProgress());
            if (!generateEditText.getText().toString().isEmpty()) generateCode();
        });
        builder.setNegativeButton(R.string.dialog_cancel, (dialog, which) -> {

        });
        builder.create().show();
    }

    private void showMarginDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.Dialog);

        View dialogView = getLayoutInflater().inflate(R.layout.dialog_margin, null);
        TextView text = dialogView.findViewById(R.id.text);
        SeekBar seekBar = dialogView.findViewById(R.id.seekBar);

        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                text.setText(String.valueOf(progress * 8 + 16).concat(" px"));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        seekBar.setProgress((margin - 16) / 8);

        builder.setTitle(R.string.margin);
        builder.setIcon(R.drawable.baseline_margin_24);
        builder.setView(dialogView);
        builder.setPositiveButton(R.string.dialog_ok, (dialog, which) -> {
            margin = seekBar.getProgress() * 8 + 16;
            marginText.setText(String.valueOf(margin).concat(" px"));
            if (!generateEditText.getText().toString().isEmpty()) generateCode();
        });
        builder.setNegativeButton(R.string.dialog_cancel, (dialog, which) -> {

        });
        builder.create().show();
    }

    private void showFormatDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.Dialog);
        builder.setTitle(R.string.format);
        builder.setIcon(R.drawable.baseline_dataset_24);
        builder.setSingleChoiceItems(supportedBarcodeFormats.getFormats().toArray(new String[0]), format, (dialog, which) -> {formatSelection = which;});
        builder.setPositiveButton(R.string.dialog_ok, (dialog, which) -> {
            format = formatSelection;
            formatText.setText(supportedBarcodeFormats.getFormats().get(format));
            if (!generateEditText.getText().toString().isEmpty()) generateCode();
        });
        builder.create().show();
    }

    private void generateCode() {
        saved = false;
        infoLayout.setVisibility(View.GONE);
        warningLayout.setVisibility(View.GONE);
        resultLayout.setVisibility(View.GONE);
        String text = generateEditText.getText().toString();
        if (text.isEmpty()) {
            infoLayout.setVisibility(View.VISIBLE);
            return;
        }
        Symbol symbol = supportedBarcodeFormats.getSymbol(format);
        if (supportedBarcodeFormats.getFormats().get(format).contains("EAN")) {
            if (text.length() == 8) text = text.substring(0, 7);
            if (text.length() == 13) text = text.substring(0, 12);
        }
        symbol.setFontName("Monospaced");
        symbol.setFontSize(16);
        symbol.setModuleWidth(24);
        symbol.setBarHeight(512);
        symbol.setQuietZoneHorizontal(margin);
        symbol.setQuietZoneVertical(margin);
        symbol.setHumanReadableLocation(HumanReadableLocation.NONE);
        try {
            symbol.setContent(text);
            try {
                ByteArrayOutputStream stream = new ByteArrayOutputStream();
                SvgRenderer renderer = new SvgRenderer(stream, 1, qrCodeColor.backgroundColor, qrCodeColor.foregroundColor, true);
                renderer.render(symbol);
                String content = new String(stream.toByteArray(), StandardCharsets.UTF_8);
                SVG svg = SVG.getFromString(content);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    generatedBitmap = Bitmap.createBitmap(svg.renderToPicture());
                    resultImage.setBackgroundColor(qrCodeColor.backgroundColorInt);
                    resultImage.setImageBitmap(generatedBitmap);
                }
                resultLayout.setVisibility(View.VISIBLE);
            } catch (IOException | SVGParseException e) {
                e.printStackTrace();
                warningText.setText(R.string.generate_warning);
                warningLayout.setVisibility(View.VISIBLE);
            }
        } catch (OkapiInputException e) {
            e.printStackTrace();
            warningText.setText("Girdiğiniz yazı bu format\n için uygun değil");
            warningLayout.setVisibility(View.VISIBLE);
        }
    }

    private void saveGenerated() {
        Calendar calendar = Calendar.getInstance();
        String fileName = new SimpleDateFormat("ddMMyyyyHHmmss").format(calendar.getTime());
        long dateInMillis = calendar.getTimeInMillis();

        String mediaPath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).getAbsolutePath();
        mediaPath = mediaPath.concat("/Generated QR Codes");
        File saveFolder = new File(mediaPath);
        if (!saveFolder.exists()) saveFolder.mkdirs();
        mediaPath = mediaPath.concat("/").concat(fileName).concat(".png");
        File saveFile = new File(mediaPath);

        boolean success = false;

        try {
            generatedBitmap.compress(Bitmap.CompressFormat.PNG, 100, new FileOutputStream(saveFile));
            success = true;
        } catch (IOException e) {
            e.printStackTrace();
        }

        if (success) {
            generateHistory.addEntry(new HistoryEntry(generateEditText.getText().toString(), mediaPath, dateInMillis));
            spe.putString("generate", generateHistory.getJson()).commit();
            showMessage(getString(R.string.saved));
            saved = true;
        } else {
            showMessage(getString(R.string.could_not_save));
        }
    }

    private void shareGenerated() {
        if (!saved) saveGenerated();
        HistoryEntry entry = generateHistory.getEntryAt(0);
        shareResult(entry.content, entry.path);
    }
}