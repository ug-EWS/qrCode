package com.example.qrcode;

import android.content.Context;
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
import java.util.function.Consumer;

public class RadioButtonDialog extends BottomSheetDialog {
    private final ArrayList<String> options;
    private int selection;

    RadioButtonDialog(
            Context _context,
            int _title,
            int _icon,
            String _description,
            ArrayList<String> _options,
            int _initialSelection,
            Consumer<Integer> _listener) {
        super(_context, R.style.BottomSheetDialogTheme);

        setContentView(R.layout.dialog_radio_button);
        ImageView cancelButton = findViewById(R.id.cancelButton);
        TextView title = findViewById(R.id.title);
        LinearLayout applyButton = findViewById(R.id.addButton);
        TextView description = findViewById(R.id.description);
        RecyclerView recyclerView = findViewById(R.id.recyclerView);

        options = _options;
        selection = _initialSelection;

        cancelButton.setOnClickListener(v -> cancel());
        title.setText(_title);
        description.setText(_description);
        applyButton.setOnClickListener(v -> {
            dismiss();
            _listener.accept(selection);
        });

        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(new DialogAdapter());

        getBehavior().setState(BottomSheetBehavior.STATE_EXPANDED);
    }

    private class DialogAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
        @NonNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new RecyclerView.ViewHolder(getLayoutInflater().inflate(R.layout.menu_item, parent, false)) {};
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
            View view = holder.itemView;
            int pos = holder.getBindingAdapterPosition();
            ImageView icon = view.findViewById(R.id.icon);
            TextView title = view.findViewById(R.id.title);
            icon.setImageResource(selection == pos ? R.drawable.baseline_radio_button_checked_24 : R.drawable.baseline_radio_button_unchecked_24);
            title.setText(options.get(pos));
            view.setOnClickListener(v -> {
                if (selection != pos) {
                    int oldPos = selection;
                    selection = pos;
                    notifyItemChanged(oldPos);
                    notifyItemChanged(selection);
                }
            });
        }

        @Override
        public int getItemCount() {
            return options.size();
        }
    }
}
