package com.example.qrcode;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextWatcher;
import android.text.style.ForegroundColorSpan;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.TooltipCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.ItemTouchHelper;
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

    private ScanHistoryAdapter scanHistoryAdapter;
    private ItemTouchHelper scanHistoryItemTouchHelper;

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

        scanHistory = new HistoryManager(sp.getString("scan", ""));

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
        deleteButton.setOnClickListener(v -> deleteResult(selectedItems));
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
                if (searchMode) {
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
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });

        resultInfo = findViewById(R.id.resultInfo);

        recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        scanHistoryAdapter = new ScanHistoryAdapter();
        recyclerView.setAdapter(scanHistoryAdapter);
        scanHistoryItemTouchHelper = new ItemTouchHelper(new ItemMoveCallback(scanHistoryAdapter));
        scanHistoryItemTouchHelper.attachToRecyclerView(recyclerView);

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
            Insets insets2 = insets.getInsets(WindowInsetsCompat.Type.ime());
            Insets insets3 = insets.getInsets(WindowInsetsCompat.Type.displayCutout());
            v.setPadding(Math.max(insets1.left, insets3.left), insets1.top, Math.max(insets1.right,insets3.right), Math.max(insets1.bottom, insets2.bottom));
            return insets;
        });

        imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
    }

    @Override
    protected void onPause() {
        spe.putString("scan", scanHistory.toJsonString()).commit();
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
    public void shareResult(int index) {
        shareResult(scanHistory.getEntryAt(index).content);
    }

    @Override
    public void deleteResult(int index) {
        super.deleteResult(index);
        new MiniDialog(this, scanHistory.getEntryAt(index).content, () -> {
            scanHistory.removeEntryAt(index);
            recyclerView.getAdapter().notifyItemRemoved(index);
            setNoItemsView();
        }, () -> recyclerView.getAdapter().notifyItemChanged(index)).show();
    }

    @Override
    public void deleteResult(ArrayList<Integer> indexes) {
        super.deleteResult(indexes);
        if (indexes.size() == 1) deleteResult(indexes.get(0));
        else new MiniDialog(this, scanHistory, indexes, () -> {
            scanHistory.removeEntriesAt(indexes);
            for (Integer i : indexes) recyclerView.getAdapter().notifyItemRemoved(i);
            selectedItems.clear();
            switchSelectionMode(false);
            setNoItemsView();
        }).show();
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
        searchMode = false;
        title.setVisibility(View.VISIBLE);
        searchButton.setVisibility(View.VISIBLE);
        searchEditText.setVisibility(View.GONE);
        searchEditText.setText("");
        setNoItemsView();
        imm.hideSoftInputFromWindow(searchEditText.getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
    }

    private void switchSelectionMode(boolean _selectionMode) {
        if (_selectionMode != selectionMode) {
            selectionMode = _selectionMode;
            recyclerView.getAdapter().notifyDataSetChanged();
            updateTitle();
            if (searchMode) title.setVisibility(selectionMode ? View.VISIBLE : View.GONE);
            deleteButton.setVisibility(selectionMode ? View.VISIBLE : View.GONE);
            selectAllButton.setVisibility(selectionMode ? View.VISIBLE : View.GONE);
            searchButton.setVisibility(selectionMode || searchMode ? View.GONE : View.VISIBLE);
            searchEditText.setVisibility(searchMode && !selectionMode ? View.VISIBLE : View.GONE);
        }
    }

    private void updateTitle() {
        int ite = selectedItems.size();
        title.setText(selectionMode ? String.valueOf(ite).concat(" ").concat(getString(R.string.items_selected)) : getString(R.string.scan_history));
    }

    private void setNoItemsView() {
        boolean noItems = scanHistory.getEntryCount() == 0;
        resultInfo.setText("Tarananlar listesi boş");
        resultInfo.setVisibility(noItems ? View.VISIBLE : View.GONE);
        recyclerView.setVisibility(noItems ? View.GONE : View.VISIBLE);
        searchButton.setVisibility(noItems ? View.GONE : View.VISIBLE);
    }

    private class ScanHistoryAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> implements ItemMoveCallback.ItemTouchHelperContract {
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
            TextView date = view.findViewById(R.id.date);
            CheckBox checkBox = view.findViewById(R.id.checkBox);
            int pos = holder.getBindingAdapterPosition();
            HistoryEntry entry = scanHistory.getVisibleEntryAt(pos);

            view.setOnClickListener(getItemClickListener(pos));
            view.setOnLongClickListener(getItemLongClickListener(pos));

            if (searchMode) {
                String searchQuery = searchEditText.getText().toString().toLowerCase();
                int foundAtStart = entry.content.toLowerCase().indexOf(searchQuery);
                int foundAtEnd = foundAtStart + searchQuery.length();
                SpannableString spannableString = new SpannableString(entry.content);
                spannableString.setSpan(new ForegroundColorSpan(getColor(R.color.yellow)),
                        foundAtStart,
                        foundAtEnd,
                        Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
                text.setText(spannableString, TextView.BufferType.SPANNABLE);
            } else text.setText(entry.content);
            date.setText(entry.getVisibleDate());
            checkBox.setVisibility(selectionMode ? View.VISIBLE : View.GONE);
            checkBox.setChecked(selectedItems.contains(pos));
        }

        @Override
        public int getItemCount() {
            return scanHistory.getVisibleEntriesCount();
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

        @Override
        public boolean isSwipeEnabled() {
            return !selectionMode;
        }

        @Override
        public void onSwipe(RecyclerView.ViewHolder myViewHolder, int i) {
            int position = myViewHolder.getBindingAdapterPosition();
            if (i == ItemTouchHelper.START) {
                shareResult(position);
                notifyItemChanged(position);
            }
            else deleteResult(position);
        }

        @Override
        public Context getContext() {
            return ScanHistoryActivity.this;
        }
    }
}