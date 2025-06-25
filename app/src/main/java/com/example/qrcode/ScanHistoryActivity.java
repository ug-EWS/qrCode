package com.example.qrcode;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.SearchView;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.TooltipCompat;
import androidx.core.content.res.ResourcesCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

public class ScanHistoryActivity extends QrCodeActivity {
    private ImageView backButton;
    private TextView title;
    private ImageView selectAllButton;
    private ImageView deleteButton;
    private ImageView searchButton;
    private EditText searchEditText;
    private TextView resultInfo;
    private RecyclerView recyclerView;

    private SharedPreferences sp;
    private SharedPreferences.Editor spe;

    private ResultDialog resultDialog;
    private HistoryManager scanHistory;

    private boolean selectionMode;
    private ArrayList<Integer> selectedItems;
    private boolean searchMode;

    private InputMethodManager imm;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_scan_history);


        sp = getSharedPreferences("QrCodeData", MODE_PRIVATE);
        spe = sp.edit();

        selectedItems = new ArrayList<>();

        scanHistory = new HistoryManager();
        scanHistory.fromJson(sp.getString("scan", ""));

        resultDialog = new ResultDialog(this, true, null);

        backButton = findViewById(R.id.backButton);
        backButton.setOnClickListener(v -> back());
        TooltipCompat.setTooltipText(backButton, getString(R.string.back));

        title = findViewById(R.id.title);

        selectAllButton = findViewById(R.id.selectAllButton);
        selectAllButton.setOnClickListener(v -> {
            selectedItems.clear();
            for (int i = 0; i < scanHistory.getVisibleEntriesCount(); i++) selectedItems.add(i);
            recyclerView.getAdapter().notifyDataSetChanged();
            updateTitle();
        });
        TooltipCompat.setTooltipText(selectAllButton, getString(R.string.select_all));

        deleteButton = findViewById(R.id.deleteButton);
        deleteButton.setOnClickListener(v -> {
            AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.Dialog);
            builder.setTitle(R.string.clear_history);
            builder.setMessage(String.valueOf(selectedItems.size()).concat(" ").concat(getString(R.string.number_delete_entries)));
            builder.setPositiveButton(R.string.dialog_delete, (dialog, which) -> deleteResult(selectedItems));
            builder.setNegativeButton(R.string.dialog_no, null);
            builder.create().show();
        });
        TooltipCompat.setTooltipText(deleteButton, getString(R.string.delete_selected));

        searchButton = findViewById(R.id.doneButton);
        searchButton.setOnClickListener(v -> expandSearchView());
        searchEditText = findViewById(R.id.searchEditText);
        searchEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                scanHistory.find(s.toString());
                if (s.toString().isEmpty()) {
                    resultInfo.setVisibility(View.VISIBLE);
                    resultInfo.setText(R.string.result_info);
                } else if (scanHistory.getVisibleEntriesCount() == 0) {
                    resultInfo.setVisibility(View.VISIBLE);
                    resultInfo.setText(R.string.result_not_found);
                } else {
                    resultInfo.setVisibility(View.GONE);
                }
                recyclerView.getAdapter().notifyDataSetChanged();
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });

        resultInfo = findViewById(R.id.resultInfo);

        recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(new RecyclerView.Adapter<RecyclerView.ViewHolder>() {
            @NonNull
            @Override
            public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                View view = getLayoutInflater().inflate(R.layout.scan_item, parent, false);
                return new RecyclerView.ViewHolder(view){};
            }

            @Override
            public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
                View view = holder.itemView;
                TextView text = view.findViewById(R.id.text);
                ImageView share = view.findViewById(R.id.share);
                CheckBox checkBox = view.findViewById(R.id.checkBox);
                int pos = holder.getAdapterPosition();
                HistoryEntry entry = scanHistory.getVisibleEntryAt(pos);

                view.setOnClickListener(getItemClickListener(pos));
                view.setOnLongClickListener(getItemLongClickListener(pos));
                share.setOnClickListener(getShareClickListener(pos));

                text.setText(entry.content);
                share.setVisibility(selectionMode ? View.GONE : View.VISIBLE);
                TooltipCompat.setTooltipText(share, getString(R.string.share));
                checkBox.setVisibility(selectionMode ? View.VISIBLE : View.GONE);
                checkBox.setChecked(selectedItems.contains(pos));
            }

            @Override
            public int getItemCount() {
                return scanHistory.getVisibleEntriesCount();
            }

            private View.OnClickListener getShareClickListener(int pos) {
                return v -> {
                    shareResult(scanHistory.getVisibleEntryAt(pos).content);
                };
            }


            private View.OnClickListener getItemClickListener(int pos) {
                return v -> {
                    if (selectionMode) {
                        if (selectedItems.contains(pos)) selectedItems.remove((Integer)pos); else selectedItems.add(pos);
                        if (selectedItems.isEmpty()) {
                            switchSelectionMode(false);
                        } else {
                            updateTitle();
                            recyclerView.getAdapter().notifyItemChanged(pos);
                        }
                    } else {
                        resultDialog.showResult(scanHistory.getVisibleEntryAt(pos), pos);
                    }
                };
            }

            private View.OnLongClickListener getItemLongClickListener(int pos) {
                return v -> {
                    selectedItems = new ArrayList<>();
                    selectedItems.add(pos);
                    switchSelectionMode(true);
                    return true;
                };
            }
        });

        OnBackPressedCallback callback = new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                back();
            }
        };

        getOnBackPressedDispatcher().addCallback(callback);
        setNoItemsView();

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets insets1 = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(insets1.left, insets1.top, insets1.right, insets1.bottom);
            return insets;
        });

        imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
    }

    @Override
    protected void onPause() {
        spe.putString("scan", scanHistory.getJson()).commit();
        super.onPause();
    }

    @Override
    public void regenerateResult(String text) {
        super.regenerateResult(text);
        Intent intent = new Intent("com.qrx.ACTION_REGENERATE");
        intent.setClass(getApplicationContext(), MainActivity.class);
        intent.putExtra("content", text);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
    }

    @Override
    public void deleteResult(int index) {
        super.deleteResult(index);
        scanHistory.removeVisibleEntryAt(index);
        recyclerView.getAdapter().notifyItemRemoved(index);
        setNoItemsView();
    }

    @Override
    public void deleteResult(ArrayList<Integer> indexes) {
        super.deleteResult(indexes);
        scanHistory.removeEntriesAt(indexes);
        for (Integer i : indexes) recyclerView.getAdapter().notifyItemRemoved(i);
        selectedItems.clear();
        switchSelectionMode(false);
        setNoItemsView();
    }

    private void back() {
        if (selectionMode) {
            selectedItems.clear();
            switchSelectionMode(false);
        }
        else if (searchMode) collapseSearchView();
        else finish();
    }

    private void expandSearchView() {
        title.setVisibility(View.GONE);
        searchButton.setVisibility(View.GONE);
        resultInfo.setVisibility(View.VISIBLE);
        resultInfo.setText(R.string.result_info);
        searchEditText.setVisibility(View.VISIBLE);
        searchEditText.requestFocus();
        imm.showSoftInput(searchEditText, InputMethodManager.SHOW_IMPLICIT);
        searchMode = true;
    }

    private void collapseSearchView() {
        scanHistory.cancelSearch();
        recyclerView.getAdapter().notifyDataSetChanged();
        title.setVisibility(View.VISIBLE);
        searchButton.setVisibility(View.VISIBLE);
        searchEditText.setVisibility(View.GONE);
        searchEditText.setText("");
        setNoItemsView();
        imm.hideSoftInputFromWindow(searchEditText.getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
        searchMode = false;
    }

    private void switchSelectionMode(boolean _selectionMode) {
        if (_selectionMode != selectionMode) {
            selectionMode = _selectionMode;
            recyclerView.getAdapter().notifyDataSetChanged();
            updateTitle();
            if (searchMode) title.setVisibility(selectionMode ? View.VISIBLE : View.GONE);
            deleteButton.setVisibility(selectionMode ? View.VISIBLE : View.GONE);
            selectAllButton.setVisibility(selectionMode ? View.VISIBLE : View.GONE);
            searchButton.setVisibility(selectionMode ? View.GONE : View.VISIBLE);
        }
    }

    private void updateTitle() {
        int ite = selectedItems.size();
        title.setText(ite == 0 ? getString(R.string.scan_history) : String.valueOf(ite).concat(" ").concat(getString(R.string.items_selected)));
    }

    private void setNoItemsView() {
        boolean noItems = scanHistory.getEntryCount() == 0;
        resultInfo.setText("Tarananlar listesi boş");
        resultInfo.setVisibility(noItems ? View.VISIBLE : View.GONE);
        recyclerView.setVisibility(noItems ? View.GONE : View.VISIBLE);
    }
}