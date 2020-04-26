package com.swy2k.bletest.recyclerviews.BLEList;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import com.swy2k.bletest.R;
import com.swy2k.bletest.databinding.BleListRecyclerviewItemBinding;

import java.util.ArrayList;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

public abstract class RecyclerViewAdapter extends RecyclerView.Adapter<RecyclerViewAdapter.RecyclerViewHolder1> {

    private final List<RecyclerViewItem> itemList = new ArrayList<>();
    private final LayoutInflater layoutInflater;
    private final ViewGroup viewParent;

    public RecyclerViewAdapter(LayoutInflater layoutInflater, ViewGroup viewParent) {
        this.layoutInflater = layoutInflater;
        this.viewParent = viewParent;
    }

    @NonNull
    @Override
    public RecyclerViewHolder1 onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new RecyclerViewHolder1(BleListRecyclerviewItemBinding.inflate(layoutInflater, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerViewHolder1 holder, int position) {
        if(itemList.get(position).getName() == null || itemList.get(position).getName().equals("")) {
            holder.get().bleName.setText(R.string.no_name);
        } else {
            holder.get().bleName.setText(itemList.get(position).getName());
        }
        holder.get().bleMacAddress.setText(itemList.get(position).getMacAddress());

        switch (itemList.get(position).getStatus()) {
            case RecyclerViewItem.STATUS_CONNECTIBLE:
                holder.get().bleStatus.setText(R.string.connectible);
                break;
            case RecyclerViewItem.STATUS_CONNECTING:
                holder.get().bleStatus.setText(R.string.connecting);
                break;
            case RecyclerViewItem.STATUS_CONNECTED:
                holder.get().bleStatus.setText(R.string.connected);
                break;
            case RecyclerViewItem.STATUS_ERROR:
                holder.get().bleStatus.setText(R.string.error);
                break;
        }

        holder.get().recyclerviewItemParent.setOnClickListener(v -> onClick(itemList.get(position)));
    }

    @Override
    public int getItemCount() {
        return itemList.size();
    }

    public RecyclerViewItem getItem(int position) {
        return itemList.get(position);
    }

    public List<RecyclerViewItem> getAllItem() {
        return itemList;
    }

    public void addItem(RecyclerViewItem item) {
        final int index = indexOf(item);
        if(index > -1) {
            replaceItem(item, index);
        } else {
            itemList.add(item);
            viewParent.post(() -> this.notifyItemInserted(itemList.size()));
        }
    }

    public void addAllItem(List<RecyclerViewItem> items) {
        for (RecyclerViewItem item : items) {
            addItem(item);
        }
    }

    public void clearItem() {
        itemList.clear();
        viewParent.post(this::notifyDataSetChanged);
    }

    public void replaceItem(RecyclerViewItem item, int position) {
        itemList.set(position, item);
        viewParent.post(() -> this.notifyItemChanged(position));
    }

    public int indexOf(RecyclerViewItem item) {
        for(int i = 0; i < itemList.size(); i++) {
            if(itemList.get(i).getMacAddress().equals(item.getMacAddress())) {
                return i;
            }
        }

        return -1;
    }

    public abstract void onClick(RecyclerViewItem item);

    static class RecyclerViewHolder1 extends RecyclerView.ViewHolder {

        private BleListRecyclerviewItemBinding binding;

        private RecyclerViewHolder1(@NonNull BleListRecyclerviewItemBinding binding) {
            super(binding.getRoot());

            this.binding = binding;
        }

        private BleListRecyclerviewItemBinding get() {
            return binding;
        }
    }
}
