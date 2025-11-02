package com.example.qrcode;

import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.drawable.PictureDrawable;
import android.net.Uri;
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
import androidx.appcompat.widget.TooltipCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.caverock.androidsvg.SVG;
import com.caverock.androidsvg.SVGParseException;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.ArrayList;

public class ProjectsActivity extends QrCodeActivity {
    private ImageView backButton, optionsButton;
    private TextView title, infoText;
    private RecyclerView projectManagerRecycler, projectRecycler;

    private ProjectManagerAdapter projectManagerAdapter;
    private ProjectAdapter projectAdapter;
    private ItemTouchHelper projectAdapterItemTouchHelper;

    private SharedPreferences sp;
    private SharedPreferences.Editor spe;

    private HistoryManager historyManager;
    private ProjectManager projectManager;
    private ResultDialog resultDialog;

    private int currentProjectIndex;
    private ProjectManager.Project currentProject;

    private boolean projectOpen = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_projects);

        sp = getSharedPreferences("QrCodeData", MODE_PRIVATE);
        spe = sp.edit();

        historyManager = new HistoryManager(sp.getString("generate", ""));
        projectManager = new ProjectManager(sp.getString("projects", ""));

        resultDialog = new ResultDialog(this, false, null);

        backButton = findViewById(R.id.backButton);
        backButton.setOnClickListener(v -> back());
        TooltipCompat.setTooltipText(backButton, getString(R.string.back));

        title = findViewById(R.id.title);
        infoText = findViewById(R.id.infoText);

        optionsButton = findViewById(R.id.optionsButton);
        optionsButton.setOnClickListener(v -> getProjectMenu(currentProjectIndex).showMenu());

        projectManagerRecycler = findViewById(R.id.projectManagerRecycler);
        projectManagerRecycler.setLayoutManager(new LinearLayoutManager(this));
        projectManagerAdapter = new ProjectManagerAdapter();
        projectManagerRecycler.setAdapter(projectManagerAdapter);

        projectRecycler = findViewById(R.id.projectRecycler);
        projectRecycler.setLayoutManager(new LinearLayoutManager(this));

        OnBackPressedCallback callback = new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                back();
            }
        };

        getOnBackPressedDispatcher().addCallback(callback);

        setViewMode(false);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets insets1 = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            Insets insets2 = insets.getInsets(WindowInsetsCompat.Type.ime());
            Insets insets3 = insets.getInsets(WindowInsetsCompat.Type.displayCutout());
            v.setPadding(Math.max(insets1.left, insets3.left), insets1.top, Math.max(insets1.right, insets3.right), Math.max(insets1.bottom, insets2.bottom));
            return insets;
        });
    }

    @Override
    protected void onResume() {
        setNoItemsView();
        super.onResume();
    }

    @Override
    protected void onPause() {
        spe.putString("projects", projectManager.toJsonString()).commit();
        super.onPause();
    }

    @Override
    public void shareResult(int index) {
        HistoryEntry result = currentProject.files.get(index);
        shareResult(result.content, result.path);
    }

    @Override
    public void deleteResult(int index) {
        super.deleteResult(index);
        new MiniDialog(this, currentProject.getFileAt(index).content, () -> {
            currentProject.removeFileAt(index);
            projectAdapter.notifyItemRemoved(index);
            projectManagerAdapter.notifyItemChanged(currentProjectIndex);
            setNoItemsView();
        }, () -> projectAdapter.notifyItemChanged(index)).show();
    }

    private BottomSheetMenu getProjectMenu(int index) {
        ProjectManager.Project forProject = projectManager.getProjectAt(index);
        BottomSheetMenu menu = new BottomSheetMenu(this, forProject);
        if (forProject.isProgress())
            menu.addMenuItem(R.drawable.baseline_done_24, R.string.complete_project, () ->
                    new MiniDialog(this,
                    forProject,
                    getString(R.string.complete_project),
                    "Proje tamamlansın mı?",
                    "Tamamla",
                    R.drawable.baseline_done_24,
                    -1,
                    () -> {
                        forProject.completeProject();
                        projectManager.sort();
                        if (projectOpen) currentProjectIndex = projectManager.getIndexOf(currentProject);
                        projectManagerAdapter.notifyDataSetChanged();
                    }).show(), -1);
        if (forProject.isProgress())
            menu.addMenuItem(R.drawable.baseline_edit_24, R.string.edit_project, () ->
                new ProjectDialog(this, forProject.name, newName -> {
                    forProject.name = newName;
                    if (projectOpen) title.setText(newName);
                    projectManagerAdapter.notifyItemChanged(index);
                }).show());
        if (forProject.isComplete())
            menu.addMenuItem(R.drawable.baseline_swap_horiz_24, R.string.continue_project, () ->
                new MiniDialog(this,
                        forProject,
                        getString(R.string.continue_project),
                        "Projeye devam edilsin mi?",
                        "Devam et",
                            R.drawable.baseline_swap_horiz_24,
                            0,
                        () -> {
                            forProject.continueProject();
                            projectManager.sort();
                            if (projectOpen) currentProjectIndex = projectManager.getIndexOf(currentProject);
                            projectManagerAdapter.notifyDataSetChanged();
                        }).show());
        if (!forProject.isComplete())
            menu.addMenuItem(R.drawable.baseline_restart_alt_24, R.string.restart_project, () ->
                new MiniDialog(this,
                        forProject,
                        getString(R.string.restart_project),
                        "Proje yeniden başlatılsın mı?",
                        "Yeniden başlat",
                        R.drawable.baseline_restart_alt_24,
                        0,
                        () -> {
                            forProject.restartProject();
                            projectManager.sort();
                            if (projectOpen) currentProjectIndex = projectManager.getIndexOf(currentProject);
                            projectManagerAdapter.notifyDataSetChanged();
                        }).show());
        if (forProject.isProgress())
            menu.addMenuItem(R.drawable.baseline_close_24, R.string.cancel_project, () ->
                new MiniDialog(this,
                        forProject,
                        getString(R.string.cancel_project),
                        "Proje iptal edilsin mi? Projedeki dosyalar Oluşturulanlar bölümüne taşınacak.",
                        "Projeyi iptal et",
                        R.drawable.baseline_close_24,
                        1,
                        () -> {
                            ArrayList<HistoryEntry> entries = forProject.cancelProject();
                            for (HistoryEntry i : entries) historyManager.addEntry(i);
                            historyManager.sort();
                            forProject.removeAllFiles();
                            spe.putString("generate", historyManager.toJsonString()).apply();
                            projectManager.sort();
                            if (projectOpen) {
                                setNoItemsView();
                                currentProjectIndex = projectManager.getIndexOf(currentProject);
                            }
                            projectManagerAdapter.notifyDataSetChanged();
                        }).show(), 1);
        if (!forProject.isProgress())
            menu.addMenuItem(R.drawable.baseline_delete_forever_red_24, R.string.delete_project, () ->
                new MiniDialog(this,
                        forProject,
                        getString(R.string.delete_project),
                        "Proje silinsin mi?",
                        "Sil",
                        R.drawable.baseline_delete_forever_red_24,
                        1,
                        () -> {
                            projectManager.removeProject(index);
                            projectManagerAdapter.notifyItemRemoved(index);
                            if (projectOpen) setViewMode(false);
                        }).show(),1);
        return menu;
    }

    private void back() {
        if (projectOpen) {
            currentProjectIndex = -1;
            setViewMode(false);
        }
        else finish();
    }

    private void openProject(int index) {
        currentProjectIndex = index;
        currentProject = projectManager.getProjectAt(index);
        setViewMode(true);
    }

    private void setViewMode(boolean _projectOpen) {
        projectOpen = _projectOpen;
        title.setText(projectOpen ? currentProject.name : getString(R.string.projects_center));
        optionsButton.setVisibility(projectOpen ? View.VISIBLE : View.GONE);
        setNoItemsView();
        if (projectOpen) {
            projectAdapter = new ProjectAdapter();
            projectRecycler.setAdapter(projectAdapter);
            projectAdapterItemTouchHelper = new ItemTouchHelper(new ItemMoveCallback(projectAdapter));
            projectAdapterItemTouchHelper.attachToRecyclerView(projectRecycler);
        }
    }

    private void setNoItemsView() {
        boolean noItems = projectOpen ? currentProject.getFileCount() == 0 : projectManager.getProjectCount() == 0;
        infoText.setText(projectOpen ? currentProject.isCanceled() ? "Proje iptal edilmiş." : "Proje boş." : "Proje oluşturulmamış.");
        View view = noItems ? infoText : projectOpen ? projectRecycler : projectManagerRecycler;
        infoText.setVisibility(View.GONE);
        projectManagerRecycler.setVisibility(View.GONE);
        projectRecycler.setVisibility(View.GONE);
        view.setVisibility(View.VISIBLE);
    }

    private class ProjectManagerAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
        @NonNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new RecyclerView.ViewHolder(getLayoutInflater().inflate(R.layout.project_item, parent, false)) {};
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
            View view = holder.itemView;
            int pos = holder.getBindingAdapterPosition();
            ProjectManager.Project project = projectManager.getProjectAt(pos);
            ImageView icon = view.findViewById(R.id.playlistIcon);
            TextView name = view.findViewById(R.id.playlistTitle);
            TextView size = view.findViewById(R.id.playlistSize);
            ImageView options = view.findViewById(R.id.playlistOptions);

            icon.setBackgroundResource(project.isComplete() ? R.drawable.project_icon_complete
                    : project.isProgress() ? R.drawable.project_icon_progress
                    : R.drawable.project_icon_canceled);

            name.setText(project.name);
            size.setText(project.isCanceled() ? "İptal edildi" : String.format("%d dosya", project.getFileCount()));

            view.setOnClickListener(v -> openProject(pos));
            options.setOnClickListener(v -> getProjectMenu(pos).showMenu());
        }

        @Override
        public int getItemCount() {
            return projectManager.getProjectCount();
        }
    }

    private class ProjectAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> implements ItemMoveCallback.ItemTouchHelperContract {
        @NonNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = getLayoutInflater().inflate(R.layout.generate_item, parent, false);
            return new RecyclerView.ViewHolder(view){};
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
            View view = holder.itemView;
            ImageView img = view.findViewById(R.id.img);
            TextView text = view.findViewById(R.id.text);
            TextView date = view.findViewById(R.id.date);
            TextView format = view.findViewById(R.id.format);
            int pos = holder.getBindingAdapterPosition();
            HistoryEntry entry = currentProject.getFileAt(pos);
            String path = entry.path;

            view.setOnClickListener(v -> resultDialog.showResult(entry, pos));
            if (path.endsWith(".svg")) {
                try {
                    InputStream is = new FileInputStream(path);
                    SVG svg = SVG.getFromInputStream(is);
                    img.setImageDrawable(new PictureDrawable(svg.renderToPicture()));
                } catch (FileNotFoundException | SVGParseException e) {
                    e.printStackTrace();
                    img.setImageDrawable(null);
                }
            }
            else img.setImageURI(Uri.parse(path));

            text.setText(entry.content);
            date.setText(entry.getVisibleDate());
            format.setText(path.endsWith(".png") ? "PNG"
                    : path.endsWith(".jpeg") ? "JPEG"
                    : path.endsWith(".webp") ? "WEBP"
                    : "SVG");
        }

        @Override
        public int getItemCount() {
            return currentProject.getFileCount();
        }

        @Override
        public boolean isSwipeEnabled() {
            return true;
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
            return ProjectsActivity.this;
        }
    }
}