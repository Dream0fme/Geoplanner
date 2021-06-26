package urum.geoplanner.adapter;

import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;

public class SimpleItemTouchHelperCallback extends ItemTouchHelper.Callback {

    private PlaceAdapter mAdapter;

    private boolean touch = false;
    private PlaceAdapter.ADAPTER_TYPE adapterType;

    public SimpleItemTouchHelperCallback(PlaceAdapter adapter) {
        mAdapter = adapter;
        adapterType = mAdapter.adapterType;
    }


    public void editing() {
        touch = !touch;
    }

    @Override
    public boolean isLongPressDragEnabled() {
        return touch;
    }

    @Override
    public boolean isItemViewSwipeEnabled() {
        return touch;
    }

    @Override
    public int getMovementFlags(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder) {
        final int dragFlags = ItemTouchHelper.UP | ItemTouchHelper.DOWN;
        final int swipeFlags = ItemTouchHelper.START | ItemTouchHelper.END;
        return makeMovementFlags(dragFlags, swipeFlags);
    }

    @Override
    public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder source, RecyclerView.ViewHolder target) {
        mAdapter.onItemMove(source.getBindingAdapterPosition(), target.getBindingAdapterPosition());
        return true;
    }

    @Override
    public void onSwiped(RecyclerView.ViewHolder viewHolder, int i) {
        switch (adapterType) {
            case MAIN:
                mAdapter.archiving(viewHolder.getBindingAdapterPosition(), true);
                break;
            case ARCHIVE:
                mAdapter.onItemDismiss(viewHolder.getBindingAdapterPosition());
                break;
        }
    }
}
