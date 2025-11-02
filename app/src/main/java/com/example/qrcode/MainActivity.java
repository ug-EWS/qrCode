package com.example.qrcode;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.UiModeManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.ImageDecoder;
import android.graphics.Picture;
import android.graphics.drawable.Icon;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.TextWatcher;
import android.transition.TransitionManager;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.PickVisualMediaRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.budiyev.android.codescanner.CodeScanner;
import com.budiyev.android.codescanner.CodeScannerView;
import com.caverock.androidsvg.SVG;
import com.caverock.androidsvg.SVGParseException;
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
import java.util.List;

import uk.org.okapibarcode.backend.HumanReadableLocation;
import uk.org.okapibarcode.backend.OkapiInputException;
import uk.org.okapibarcode.backend.Symbol;
import uk.org.okapibarcode.output.SvgRenderer;

public class MainActivity extends QrCodeActivity {
    private ImageView optionsButton;
    private LinearLayout bottomNavigation;
    private LinearLayout scanButton;
    private LinearLayout galleryButton;
    private LinearLayout generateButton;
    private LinearLayout scanScreen;
    private LinearLayout generateScreen;
    private CodeScannerView scannerView;
    private EditText generateEditText;
    private LinearLayout colorButton;
    private TextView colorText;
    private LinearLayout marginButton;
    private TextView marginText;
    private LinearLayout formatButton;
    private TextView formatText;
    private LinearLayout readableButton;
    private TextView readableText;
    private LinearLayout sizeButton;
    private TextView sizeText;
    private LinearLayout fileButton;
    private TextView fileText;
    private LinearLayout barLengthButton;
    private TextView barLengthText;
    private LinearLayout expandButton;
    private TextView expandText;
    private LinearLayout infoLayout;
    private LinearLayout warningLayout;
    private TextView warningText;
    private LinearLayout resultLayout;
    private ImageView resultImage;
    private TextView resultSize;
    private LinearLayout optionsLayout;
    private LinearLayout bottomLayout;
    private LinearLayout saveButton;
    private LinearLayout shareButton;

    private CodeScanner codeScanner;
    private ResultDialog resultDialog;
    private SaveDialog saveDialog;

    private QRCodeColor qrCodeColor;
    private int currentTab;
    private int margin;
    private int format;
    private int zoom;
    private int barLength;
    private int size;
    private int fileFormat;
    private boolean readable;
    private String bitmapSize;
    private SupportedBarcodeFormats supportedBarcodeFormats;
    private HistoryManager scanHistory;
    private HistoryEntry generatedEntry;

    private boolean saved;
    private boolean expanded;
    private int theme;

    private SharedPreferences sp;
    private SharedPreferences.Editor spe;

    private Bitmap generatedBitmap;
    private byte[] svgContent;

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
        margin = 5;
        format = 0;
        barLength = 40;
        size = 10;
        fileFormat = 0;
        readable = false;
        saved = false;
        expanded = false;

        theme = sp.getInt("theme", 0);

        saveDialog = new SaveDialog(this, this::saveGenerated);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 101) {
            if (grantResults[0] == PackageManager.PERMISSION_DENIED) {
                showMessage(getString(R.string.grant_permission));
                scannerView.setVisibility(View.GONE);
            } else {
                scannerView.setVisibility(View.VISIBLE);
                codeScanner.startPreview();
            }
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        Intent intent = getIntent();
        if (intent.getAction().equals("com.qrx.ACTION_REGENERATE"))
            regenerateResult(intent.getStringExtra("content"));

        if (checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_DENIED) {
            String[] permissions = {Manifest.permission.CAMERA};
            requestPermissions(permissions, 101);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (currentTab == 0) restartCameraPreview();

        scanHistory = new HistoryManager(sp.getString("scan", ""));
        resultDialog = new ResultDialog(this, true, dialog -> runOnUiThread(() -> codeScanner.startPreview()));
        saveDialog.refreshData();
    }

    @Override
    protected void onPause() {
        if (currentTab == 0) codeScanner.releaseResources();
        super.onPause();
    }

    @Override
    public void regenerateResult(String text) {
        super.regenerateResult(text);
        setCurrentTab(2);
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
        bottomNavigation = findViewById(R.id.bottomNavigation);
        scanButton = findViewById(R.id.myPlaylistsButton);
        galleryButton = findViewById(R.id.topStationsButton);
        generateButton = findViewById(R.id.countriesButton);
        scanScreen = findViewById(R.id.scanScreen);
        generateScreen = findViewById(R.id.generateScreen);
        scannerView = findViewById(R.id.scannerView);
        generateEditText = findViewById(R.id.generateEditText);
        colorButton = findViewById(R.id.colorButton);
        colorText = findViewById(R.id.colorText);
        marginButton = findViewById(R.id.marginButton);
        marginText = findViewById(R.id.marginText);
        formatButton = findViewById(R.id.formatButton);
        formatText = findViewById(R.id.formatText);
        fileButton = findViewById(R.id.fileTypeButton);
        fileText = findViewById(R.id.fileTypeText);
        sizeButton = findViewById(R.id.sizeButton);
        sizeText = findViewById(R.id.sizeText);
        readableButton = findViewById(R.id.readableButton);
        readableText = findViewById(R.id.readableText);
        barLengthButton = findViewById(R.id.barLengthButton);
        barLengthText = findViewById(R.id.barLengthText);
        expandButton = findViewById(R.id.expandButton);
        expandText = findViewById(R.id.expandText);
        infoLayout = findViewById(R.id.infoLayout);
        warningLayout = findViewById(R.id.warningLayout);
        warningText = findViewById(R.id.warningText);
        resultLayout = findViewById(R.id.resultLayout);
        resultImage = findViewById(R.id.resultImage);
        resultSize = findViewById(R.id.resultSize);
        optionsLayout = findViewById(R.id.optionsLayout);
        bottomLayout = findViewById(R.id.bottomLayout);
        saveButton = findViewById(R.id.saveButton);
        shareButton = findViewById(R.id.shareButton);
        optionsButton.setOnClickListener(v -> getOptionsPopupMenu().showMenu());
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
                generateCode();
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });

        colorButton.setOnClickListener(v -> showColorDialog());
        marginButton.setOnClickListener(v -> showMarginDialog());
        formatButton.setOnClickListener(v -> showFormatDialog());
        readableButton.setOnClickListener(v -> showReadableDialog());
        sizeButton.setOnClickListener(v -> showSizeDialog());
        fileButton.setOnClickListener(v -> showFileDialog());
        barLengthButton.setOnClickListener(v -> showBarLengthDialog());
        expandButton.setOnClickListener(v -> setExpanded(!expanded));
        saveButton.setOnClickListener(v -> saveDialog.show());
        shareButton.setOnClickListener(v -> shareGenerated());

        scanButton.setOnClickListener(v -> setCurrentTab(0));

        galleryButton.setOnClickListener(v ->
            uriResultLauncher.launch(new PickVisualMediaRequest.Builder()
                    .setMediaType(ActivityResultContracts.PickVisualMedia.ImageOnly.INSTANCE)
                    .build()));

        generateButton.setOnClickListener(v -> setCurrentTab(2));

        resultImage.setOnClickListener(v ->
                new PreviewDialog(this, generatedBitmap, bitmapSize).show());

        getOnBackPressedDispatcher().addCallback(new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (expanded) setExpanded(false);
                else finish();
            }
        });

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets insets1 = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            Insets insets2 = insets.getInsets(WindowInsetsCompat.Type.ime());
            Insets insets3 = insets.getInsets(WindowInsetsCompat.Type.displayCutout());
            boolean imeShowing = insets2.bottom > insets1.bottom;
            v.setPadding(Math.max(insets1.left, insets3.left), insets1.top, Math.max(insets1.right, insets3.right), Math.max(insets1.bottom, insets2.bottom));
            optionsLayout.setVisibility(imeShowing ? View.GONE : View.VISIBLE);
            bottomLayout.setVisibility(imeShowing ? View.GONE : View.VISIBLE);
            bottomNavigation.setVisibility(imeShowing ? View.GONE : View.VISIBLE);
            return insets;
        });
    }

    private void setAppTheme(int _theme) {
        theme = _theme;
        spe.putInt("theme", theme).commit();
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
                    if (uri != null) {
                        Bitmap bitmap;
                        try {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                                ImageDecoder.Source source = ImageDecoder.createSource(getContentResolver(), uri);
                                bitmap = ImageDecoder.decodeBitmap(source);
                            } else {
                                bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), uri);
                            }
                            Bitmap soft = null;
                            if (bitmap != null) soft = bitmap.copy(Bitmap.Config.ARGB_8888, true);
                            if (soft != null) scanImage(soft);
                            else showMessage("null");
                        } catch (IOException e) {
                            showMessage(R.string.could_not_get_file);
                            e.printStackTrace();
                        }
                    } else showMessage(R.string.could_not_get_file);
                } else showMessage(R.string.cancelled);
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
            showMessage(R.string.barcode_not_found);
        } catch (ChecksumException e) {
            showMessage(R.string.could_not_scan_checksum);
        } catch (FormatException e) {
            showMessage(R.string.could_not_scan_format);
        }
    }

    private void newScanEntry(String content) {
        HistoryEntry entry = new HistoryEntry(content, "", Calendar.getInstance().getTimeInMillis());
        scanHistory.addEntry(entry);
        resultDialog.showResult(entry, 0);
        spe.putString("scan", scanHistory.toJsonString()).commit();
    }

    private BottomSheetMenu getOptionsPopupMenu() {
        BottomSheetMenu menu = new BottomSheetMenu(this);
        menu.addMenuItem(R.drawable.baseline_qr_code_scanner_24, R.string.scan_history, () ->
                startActivity(new Intent(getApplicationContext(), ScanHistoryActivity.class)));
        menu.addMenuItem(R.drawable.baseline_auto_awesome_24, R.string.generate_history, () ->
                startActivity(new Intent(getApplicationContext(), GenerateHistoryActivity.class)));
        menu.addMenuItem(R.drawable.baseline_build_24, R.string.projects_center, () ->
                startActivity(new Intent(getApplicationContext(), ProjectsActivity.class)));
        menu.addMenuItem(R.drawable.baseline_color_lens_24, R.string.theme, () -> {
            BottomSheetMenu themeMenu = new BottomSheetMenu(this, theme == 0 ? 2 : theme - 1);
            themeMenu.addMenuItem(R.drawable.baseline_color_lens_24, R.string.light, () ->
                setAppTheme(1));
            themeMenu.addMenuItem(R.drawable.baseline_color_lens_24, R.string.dark, () ->
                setAppTheme(2));
            themeMenu.addMenuItem(R.drawable.baseline_color_lens_24, R.string.auto, () ->
                setAppTheme(0));
            themeMenu.showMenu();
        }, getString(List.of(R.string.auto, R.string.light, R.string.dark).get(theme)));
        return menu;
    }

    private void setCurrentTab(int tabIndex) {
        currentTab = tabIndex;
        for (int i = 0; i < 3; i++) {
            LinearLayout button = List.of(scanButton, galleryButton, generateButton).get(i);
            ImageView icon = findViewById(List.of(R.id.myPlaylistsIcon, R.id.topStationsIcon, R.id.countriesIcon).get(i));
            TextView text = findViewById(List.of(R.id.myPlaylistsText, R.id.topStationsText, R.id.countriesText).get(i));
            button.setScaleX(i == currentTab ? 1 : 0.9F);
            button.setScaleY(i == currentTab ? 1 : 0.9F);
            icon.setBackgroundResource(i == currentTab ? R.drawable.bottom_icon : R.drawable.ripple_bottom);
            icon.setImageIcon(Icon.createWithResource(this,
                            List.of(R.drawable.baseline_qr_code_scanner_24, R.drawable.baseline_photo_library_24, R.drawable.baseline_auto_awesome_24).get(i))
                    .setTint(getColor(i == currentTab ? R.color.purple_700 : R.color.grey9)));
            text.setTextColor(getColor(i == currentTab ? R.color.purple_700 : R.color.grey9));

            ((InputMethodManager) getSystemService(INPUT_METHOD_SERVICE))
                    .hideSoftInputFromWindow(generateEditText.getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
            scanScreen.setVisibility(currentTab == 0 ? View.VISIBLE : View.GONE);
            generateScreen.setVisibility(currentTab == 2 ? View.VISIBLE : View.GONE);
            if (currentTab == 0) restartCameraPreview();
            else codeScanner.releaseResources();
        }
    }

    private void showColorDialog() {
        new ColorDialog(this, qrCodeColor, _qrCodeColor -> {
            qrCodeColor.setColorMode(_qrCodeColor.getColorMode());
            qrCodeColor.setHue(_qrCodeColor.getHue());
            qrCodeColor.setShade(_qrCodeColor.getShade());
            colorText.setText(qrCodeColor.getColorMode() == QRCodeColor.COLOR_BLACK_WHITE ?
                    R.string.black_white :
                    qrCodeColor.getColorMode() == QRCodeColor.COLOR_FOREGROUND ?
                            R.string.colored_foreground : R.string.colored_background);
            if (!generateEditText.getText().toString().isEmpty()) generateCode();
        }).show();
    }

    private void showMarginDialog() {
        new SeekBarDialog(
                this,
                R.string.margin,
                0,
                R.string.margin_desc,
                margin,
                7,
                value -> value - 1,
                progress -> progress + 1,
                true,
                value -> {
                    margin = value;
                    marginText.setText(String.format("%d px", margin));
                    if (!generateEditText.getText().toString().isEmpty()) generateCode();
                }).show();
    }

    private void showFormatDialog() {
        new RadioButtonDialog(this, R.string.format, 0, "", supportedBarcodeFormats.getFormats(), format,
                selection -> {
                    format = selection;
                    formatText.setText(supportedBarcodeFormats.getFormatName(format));
                    if (!generateEditText.getText().toString().isEmpty()) generateCode();
                }).show();
    }

    private void showBarLengthDialog() {
        new SeekBarDialog(
                this,
                R.string.bar_length,
                0,
                R.string.bar_length_desc,
                barLength,
                9,
                value -> (value / 10) - 1,
                progress -> (progress + 1) * 10,
                false,
                value -> {
                    barLength = value;
                    barLengthText.setText(String.valueOf(barLength));
                    if (!generateEditText.getText().toString().isEmpty()) generateCode();
                }).show();
    }

    private void showSizeDialog() {
        new SeekBarDialog(
                this,
                R.string.magn_factor,
                0,
                R.string.magn_factor_desc,
                size,
                24,
                value -> value - 1,
                progress -> progress + 1,
                false,
                value -> {
                    size = value;
                    sizeText.setText(String.valueOf(size));
                    if (!generateEditText.getText().toString().isEmpty()) generateCode();
                }
        ).show();
    }

    private void showFileDialog() {
        ArrayList<String> options =
                new ArrayList<>(List.of(
                        getString(R.string.file_type_png),
                        getString(R.string.file_type_jpg),
                        getString(R.string.file_type_webp),
                        getString(R.string.file_type_svg)));
        new RadioButtonDialog(this, R.string.file_type, 0, "", options, fileFormat,
                selection -> {
                    fileFormat = selection;
                    fileText.setText(options.get(fileFormat));
                }).show();
    }

    private void showReadableDialog() {
        ArrayList<String> options =
                new ArrayList<>(List.of(
                   getString(R.string.show),
                   getString(R.string.hide)
                ));
        new RadioButtonDialog(this, R.string.readable_text, 0, "", options, readable ? 0 : 1,
                selection -> {
                    readable = selection == 0;
                    readableText.setText(options.get(selection));
                    if (!generateEditText.getText().toString().isEmpty()) generateCode();
                }).show();
    }

    private void setExpanded(boolean _expanded) {
        expanded = _expanded;
        TransitionManager.beginDelayedTransition(generateScreen);
        marginButton.setVisibility(expanded ? View.VISIBLE : View.GONE);
        readableButton.setVisibility(expanded ? View.VISIBLE : View.GONE);
        sizeButton.setVisibility(expanded ? View.VISIBLE : View.GONE);
        fileButton.setVisibility(expanded ? View.VISIBLE : View.GONE);
        barLengthButton.setVisibility(expanded ? View.VISIBLE : View.GONE);
        generateEditText.setVisibility(expanded ? View.INVISIBLE : View.VISIBLE);
        expandText.setText(expanded ? "Diğer seçenekleri gizle" : "Diğer seçenekleri göster");
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
        boolean hasReadable = supportedBarcodeFormats.isReadable(format);
        String name = supportedBarcodeFormats.getFormatName(format);

        if (name.equals("EAN-8") && text.length() == 8) text = text.substring(0, 7);
        if (name.equals("EAN-13") && text.length() == 13) text = text.substring(0, 12);

        symbol.setModuleWidth(1);
        symbol.setBarHeight(barLength);
        symbol.setQuietZoneHorizontal(readable && hasReadable ? margin + 8 : margin);
        symbol.setQuietZoneVertical(margin);
        symbol.setHumanReadableLocation(readable && hasReadable ? HumanReadableLocation.BOTTOM : HumanReadableLocation.NONE);
        symbol.setFontName("Monospace");
        symbol.setFontSize(11);
        if (symbol.supportsEci() && symbol.getEciMode() != 4) symbol.setEciMode(26);
        try {
            symbol.setContent(text);
            try {
                ByteArrayOutputStream stream = new ByteArrayOutputStream();
                SvgRenderer renderer = new SvgRenderer(stream, size, qrCodeColor.backgroundColor, qrCodeColor.foregroundColor, true);
                renderer.render(symbol);
                svgContent = stream.toByteArray();
                String content = new String(svgContent, StandardCharsets.UTF_8);
                SVG svg = SVG.getFromString(content);

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    generatedBitmap = Bitmap.createBitmap(svg.renderToPicture());
                } else {
                    Picture picture = svg.renderToPicture();
                    Bitmap bitmap = Bitmap.createBitmap(picture.getWidth(), picture.getHeight(), Bitmap.Config.ARGB_8888);
                    Canvas canvas = new Canvas(bitmap);
                    canvas.drawPicture(picture);
                    generatedBitmap = bitmap;
                }
                bitmapSize = String.format("%d x %d", generatedBitmap.getWidth(), generatedBitmap.getHeight());

                resultImage.setImageBitmap(generatedBitmap);
                resultSize.setText(bitmapSize);
                resultLayout.setVisibility(View.VISIBLE);

            } catch (IOException | SVGParseException e) {
                e.printStackTrace();
                warningText.setText(R.string.generate_warning);
                warningLayout.setVisibility(View.VISIBLE);
            }
        } catch (OkapiInputException e) {
            e.printStackTrace();
            int message = R.string.generate_warning_not_suitable;
            String eMessage = e.getMessage();
            if (eMessage != null) {
                if (eMessage.contains("too long"))
                    message = R.string.generate_warning_too_long;
                if (eMessage.contains("nvalid")) // "I"nvalid or "i"nvalid
                    message = R.string.generate_warning_inv_char;
            }
            warningText.setText(message);
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
        mediaPath = mediaPath.concat("/").concat(fileName).concat(List.of(".png", ".jpeg", ".webp", ".svg").get(fileFormat));
        File saveFile = new File(mediaPath);

        try {
            FileOutputStream fos = new FileOutputStream(saveFile);
            if (fileFormat == 3) {
                fos.write(svgContent);
                fos.close();
            } else {
                Bitmap.CompressFormat compressFormat =
                        List.of(Bitmap.CompressFormat.PNG, Bitmap.CompressFormat.JPEG, Bitmap.CompressFormat.WEBP).get(fileFormat);
                generatedBitmap.compress(compressFormat, 100, fos);
            }
            saved = true;
            showMessage(R.string.saved);
            generatedEntry = new HistoryEntry(generateEditText.getText().toString(), mediaPath, dateInMillis);
            saveDialog.save(generatedEntry);
        } catch (IOException e) {
            e.printStackTrace();
            showMessage(getString(R.string.could_not_save));
            saved = false;
        }
    }

    private void shareGenerated() {
        if (saved) shareResult(generatedEntry.content, generatedEntry.path);
        else saveDialog.show();
    }
}