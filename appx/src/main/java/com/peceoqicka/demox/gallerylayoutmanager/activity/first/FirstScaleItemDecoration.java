package com.peceoqicka.demox.gallerylayoutmanager.activity.first;

import android.graphics.Canvas;
import android.graphics.Rect;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.peceoqicka.x.gallerylayoutmanager.GalleryLayoutManager;

public class FirstScaleItemDecoration extends RecyclerView.ItemDecoration {
    private int mStartOffset;
    private int mEndOffset;
    private int mSpace;

    public FirstScaleItemDecoration(int startOffset, int endOffset, int space) {
        this.mStartOffset = startOffset;
        this.mEndOffset = endOffset;
        this.mSpace = space;
    }

    @Override
    public void getItemOffsets(@NonNull Rect outRect, @NonNull View view, @NonNull RecyclerView parent, @NonNull RecyclerView.State state) {
        super.getItemOffsets(outRect, view, parent, state);
        RecyclerView.LayoutManager layoutManager = parent.getLayoutManager();
        if (layoutManager instanceof GalleryLayoutManager) {
            if (((GalleryLayoutManager) layoutManager).isInfinityMode()) {
                return;
            }
        }
        final int adapterPosition = parent.getChildAdapterPosition(view);
        if (adapterPosition == 0) {
            outRect.left = mStartOffset;
        } else {
            outRect.left = mSpace;
        }
        if (adapterPosition == state.getItemCount() - 1) {
            outRect.right = mEndOffset;
        }
    }

    @Override
    public void onDrawOver(@NonNull Canvas c, @NonNull RecyclerView parent, @NonNull RecyclerView.State state) {
        super.onDrawOver(c, parent, state);
    }
}
