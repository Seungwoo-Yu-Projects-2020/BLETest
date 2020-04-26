package com.swy2k.bletest.recyclerviews;

import android.content.Context;
import android.os.Build;
import android.util.AttributeSet;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

public class LinearLayoutManagerWrapper extends LinearLayoutManager {
    public LinearLayoutManagerWrapper(Context context) {
        super(context);
    }

    public LinearLayoutManagerWrapper(Context context, int orientation, boolean reverseLayout) {
        super(context, orientation, reverseLayout);
    }

    public LinearLayoutManagerWrapper(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    /**
     * Samsung devices aren't compatible with Animations from Recyclerview at this moment.
     * So it should be turned off for the incompatible animations,
     * otherwise it crashes whenever one of the notify functions is called below
     *
     * {@link RecyclerView.Adapter#notifyItemChanged(int)}
     * {@link RecyclerView.Adapter#notifyItemChanged(int, Object)}
     * {@link RecyclerView.Adapter#notifyItemInserted(int)}
     * {@link RecyclerView.Adapter#notifyItemRangeChanged(int, int)}
     * {@link RecyclerView.Adapter#notifyItemRangeChanged(int, int, Object)}
     * {@link RecyclerView.Adapter#notifyItemRangeInserted(int, int)}
     * {@link RecyclerView.Adapter#notifyItemRangeRemoved(int, int)}
     * {@link RecyclerView.Adapter#notifyItemRemoved(int)}
     *
     * except {@link RecyclerView.Adapter#notifyDataSetChanged()}
     */
    @Override
    public boolean supportsPredictiveItemAnimations() {
        if(Build.MANUFACTURER.toLowerCase().equals("samsung")) {
            return false;
        }
        return super.supportsPredictiveItemAnimations();
    }
}
