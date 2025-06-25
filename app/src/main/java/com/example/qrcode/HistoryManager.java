package com.example.qrcode;

import java.io.File;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Locale;

public class HistoryManager {
    private ArrayList<HistoryEntry> entries;
    private ArrayList<Integer> visibleEntries;

    HistoryManager() {
    }

    public void addEntry(HistoryEntry entry) {
        if (entries.isEmpty()) entries.add(entry);
        else entries.add(0, entry);
    }

    public HistoryEntry getEntryAt(int index) {
        return entries.get(index);
    }

    public HistoryEntry getVisibleEntryAt(int index) {
        return visibleEntries == null ? getEntryAt(index) : entries.get(visibleEntries.get(index));
    }

    public int getEntryCount() {
        return entries.size();
    }

    public int getVisibleEntriesCount() {
        return visibleEntries == null ? getEntryCount() : visibleEntries.size();
    }

    public void removeEntryAt(int index) {
        entries.remove(index);
    }

    public void removeVisibleEntryAt(int index) {
        if (visibleEntries == null) removeEntryAt(index);
        else {
            entries.remove((int)visibleEntries.get(index));
            visibleEntries.remove(index);
        }
    }

    public void removeEntriesAt(ArrayList<Integer> indexes) {
        indexes.sort((Comparator.reverseOrder()));
        for (Integer i : indexes) removeVisibleEntryAt(i);
    }

    public void find(String query) {
        visibleEntries = new ArrayList<>();
        if (!query.isEmpty()) {
            query = query.toLowerCase();
            for (int i = 0; i < entries.size(); i++) {
                String content = entries.get(i).content.toLowerCase();
                if (content.contains(query)) visibleEntries.add(i);
            }
        }
    }

    public void cancelSearch() {
        visibleEntries = null;
    }

    public void removeIfPathNotExist() {
        for (int i = 0; i < entries.size(); i++) {
            if (!new File(entries.get(i).path).exists()) removeEntryAt(i);
        }
    }

    public String getJson() {
        ArrayList<String> list = new ArrayList<>();
        for (HistoryEntry i : entries) list.add(i.getJson());
        return Json.valueOf(list);
    }

    public void fromJson(String json) {
        entries = new ArrayList<>();
        if (!json.isEmpty()) {
            ArrayList<String> list = Json.toList(json);
            for (String i : list) entries.add(new HistoryEntry(i));
        }
    }
}