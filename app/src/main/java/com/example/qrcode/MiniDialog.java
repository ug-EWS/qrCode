package com.example.qrcode;

import android.content.Context;
import android.graphics.Color;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;

import java.util.ArrayList;
import java.util.Comparator;

public class MiniDialog extends BottomSheetDialog {
    private HistoryManager historyManager;
    private ArrayList<Integer> selectedItems;
    private ImageView cancelButton;
    private TextView title;
    private RecyclerView recyclerView;
    private TextView content;
    private LinearLayout projectLayout;
    private TextView promptText;
    private LinearLayout button;
    private TextView buttonText;

    MiniDialog(Context _context, String _content, Runnable _listener, Runnable _onCancel) {
        super(_context, R.style.BottomSheetDialogTheme);
        prepare();

        title.setText(R.string.delete);
        promptText.setText(getContext().getString(R.string.delete_prompt));

        content.setVisibility(View.VISIBLE);
        content.setText(_content);

        buttonText.setTextColor(getContext().getColor(R.color.red));

        button.setOnClickListener(v -> {
            dismiss();
            _listener.run();
        });

        setOnCancelListener(dialog -> _onCancel.run());
    }

    MiniDialog(Context _context, HistoryManager _historyManager, ArrayList<Integer> _selectedItems, Runnable _listener) {
        super(_context, R.style.BottomSheetDialogTheme);
        prepare();

        historyManager = _historyManager;
        selectedItems = _selectedItems;
        selectedItems.sort(Comparator.naturalOrder());

        title.setText(getContext().getString(R.string.clear_history));
        promptText.setText(String.format(getContext().getString(R.string.number_delete_entries), selectedItems.size()));

        recyclerView.setVisibility(View.VISIBLE);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(new ItemAdapter());

        buttonText.setTextColor(getContext().getColor(R.color.red));

        button.setOnClickListener(v -> {
            dismiss();
            _listener.run();
        });
    }

    MiniDialog(Context _context, ProjectManager.Project _project, String _title, String _message, String _buttonText, int _buttonIcon, int _dangerLevel, Runnable _listener) {
        super(_context, R.style.BottomSheetDialogTheme);
        prepare();
        ImageView buttonImage = findViewById(R.id.buttonIcon);

        title.setText(_title);
        promptText.setText(_message);

        projectLayout.setVisibility(View.VISIBLE);
        ImageView icon = findViewById(R.id.playlistIcon);
        TextView name = findViewById(R.id.playlistTitle);
        TextView size = findViewById(R.id.playlistSize);

        icon.setBackgroundResource(_project.isComplete() ? R.drawable.project_icon_complete
                : _project.isProgress() ? R.drawable.project_icon_progress
                : R.drawable.project_icon_canceled);
        name.setText(_project.name);
        size.setText(_project.isCanceled() ? "İptal edildi" : String.format("%d dosya", _project.getFileCount()));

        buttonImage.setImageResource(_buttonIcon);
        buttonText.setText(_buttonText);

        if (_dangerLevel != 0) {
            int color = getContext().getColor(_dangerLevel > 0 ? R.color.red : R.color.green);
            buttonImage.setColorFilter(color);
            buttonText.setTextColor(color);
        }

        button.setOnClickListener(v -> {
            dismiss();
            _listener.run();
        });
    }

    private void prepare() {
        setContentView(R.layout.mini_dialog);
        cancelButton = findViewById(R.id.cancelButton);
        title = findViewById(R.id.title);
        recyclerView = findViewById(R.id.recycler);
        promptText = findViewById(R.id.promptText);
        button = findViewById(R.id.button);
        buttonText = findViewById(R.id.buttonText);
        content = findViewById(R.id.stationTitle);
        projectLayout = findViewById(R.id.projectLayout);

        recyclerView.setVisibility(View.GONE);
        content.setVisibility(View.GONE);
        projectLayout.setVisibility(View.GONE);

        cancelButton.setOnClickListener(v -> cancel());

        getBehavior().setState(BottomSheetBehavior.STATE_EXPANDED);
    }

    private class ItemAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
        @NonNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new RecyclerView.ViewHolder(
                    getLayoutInflater().inflate(R.layout.result_item, parent, false)) {};
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
            View view = holder.itemView;
            int pos = holder.getBindingAdapterPosition();
            HistoryEntry entry = historyManager.getVisibleEntryAt(selectedItems.get(pos));

            TextView content = view.findViewById(R.id.videoTitle);
            content.setText(entry.content);
        }

        @Override
        public int getItemCount() {
            return selectedItems.size();
        }
    }
}