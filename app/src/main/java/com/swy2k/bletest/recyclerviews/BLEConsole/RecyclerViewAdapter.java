package com.swy2k.bletest.recyclerviews.BLEConsole;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.swy2k.bletest.databinding.BleConsoleRecyclerviewItemBinding;

import java.util.ArrayList;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

public class RecyclerViewAdapter extends RecyclerView.Adapter<RecyclerViewAdapter.RecyclerViewHolder1> {

    private List<String> logList = new ArrayList<>();
    private final LayoutInflater layoutInflater;
    private final ViewGroup viewParent;

    public RecyclerViewAdapter(LayoutInflater layoutInflater, ViewGroup viewParent) {
        this.layoutInflater = layoutInflater;
        this.viewParent = viewParent;
    }

    @NonNull
    @Override
    public RecyclerViewHolder1 onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new RecyclerViewHolder1(BleConsoleRecyclerviewItemBinding.inflate(layoutInflater, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerViewHolder1 holder, int position) {
        if(logList.get(position).contains("<'>'>")) {
            holder.get().consoleLogText.setTextAlignment(View.TEXT_ALIGNMENT_TEXT_START);
            holder.get().consoleLogText.setText(logList.get(position).replace("<'>'>", ""));
        } else if(logList.get(position).contains("<'<'>")) {
            holder.get().consoleLogText.setTextAlignment(View.TEXT_ALIGNMENT_TEXT_END);
            holder.get().consoleLogText.setText(logList.get(position).replace("<'<'>", ""));
        }
    }

    @Override
    public int getItemCount() {
        return logList.size();
    }

    public void addItem(String item) {
        logList.add(item);
        viewParent.post(() -> this.notifyItemInserted(logList.size()));
    }

    public void addAllItem(List<String> items) {
        for (String item : items) {
            addItem(item);
        }
    }

    public void clearItem() {
        logList.clear();
        viewParent.post(this::notifyDataSetChanged);
    }

    public static class RecyclerViewHolder1 extends RecyclerView.ViewHolder {
        private BleConsoleRecyclerviewItemBinding binding;

        public RecyclerViewHolder1(@NonNull BleConsoleRecyclerviewItemBinding binding) {
            super(binding.getRoot());

            this.binding = binding;
        }

        public BleConsoleRecyclerviewItemBinding get() {
            return binding;
        }
    }
}
