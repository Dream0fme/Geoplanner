package urum.geoplanner.adapter;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;

import androidx.appcompat.view.menu.MenuBuilder;
import androidx.appcompat.view.menu.MenuPopupHelper;
import androidx.appcompat.widget.PopupMenu;
import androidx.databinding.DataBindingUtil;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;
import androidx.transition.Fade;
import androidx.transition.Transition;
import androidx.transition.TransitionManager;
import androidx.transition.TransitionSet;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import urum.geoplanner.R;
import urum.geoplanner.databinding.RecyclerItemBinding;
import urum.geoplanner.db.entities.Place;
import urum.geoplanner.ui.PlaceActivity;
import urum.geoplanner.viewmodel.PlaceViewModel;


public class PlaceAdapter extends RecyclerView.Adapter<PlaceAdapter.PlaceItemViewHolder> {

    private static final String TAG = "mytag";
    public List<Place> mItemList;
    private List<Place> mItemListCopy;
    public List<Long> iDs;
    public boolean editingMod = false;
    RecyclerView recyclerView;
    public List<Long> placeIdlist;
    ItemTouchHelper mItemTouchHelper;
    Context context;


    ADAPTER_TYPE adapterType;

    private PlaceViewModel mPlaceViewModel;

    public PlaceAdapter(PlaceViewModel placeViewModel, RecyclerView recyclerView, Context context, ADAPTER_TYPE adapterType) {
        this.mPlaceViewModel = placeViewModel;
        this.recyclerView = recyclerView;
        this.context = context;
        this.adapterType = adapterType;
        mItemListCopy = new ArrayList<>();
        iDs = new ArrayList<>();
        this.placeIdlist = new ArrayList<>();
    }

    public void setPlaces(List<Place> mItemList) {
        this.mItemList = mItemList;
        this.mItemListCopy = new ArrayList<>();
        this.mItemListCopy.addAll(this.mItemList);
        notifyDataSetChanged();
    }

    public void editMod() {
        editingMod = !editingMod;
    }

    public void toggle(PlaceItemViewHolder holder) {
        Transition transition = new Fade();
        transition.setDuration(400);

        TransitionSet set = new TransitionSet();
        set.addTransition(transition);

        TransitionManager.beginDelayedTransition(recyclerView, transition);
        holder.binding.editMode.setVisibility(editingMod ? View.VISIBLE : View.INVISIBLE);
        holder.binding.txtOptionDigit.setVisibility(editingMod ? View.INVISIBLE : View.VISIBLE);
        holder.binding.switchBtn.setEnabled(!editingMod);
        holder.binding.view.setEnabled(!editingMod);
    }

    @Override
    public PlaceItemViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        RecyclerItemBinding binding = RecyclerItemBinding.inflate(inflater, parent, false);
        return new PlaceItemViewHolder(binding.getRoot());
    }


    @SuppressLint({"ClickableViewAccessibility", "RestrictedApi"})
    @Override
    public void onBindViewHolder(PlaceItemViewHolder holder, final int position) {
        Place place = mItemList.get(position);
        holder.binding.setPlace(place);
        holder.binding.executePendingBindings();

        if (editingMod) {
            if (holder.binding.editMode.getVisibility() == View.INVISIBLE) {
                toggle(holder);
                holder.binding.editMode.setOnTouchListener(new View.OnTouchListener() {
                    @Override
                    public boolean onTouch(View v, MotionEvent event) {
                        if (event.getActionMasked() == MotionEvent.ACTION_DOWN) {
                            onStartDrag(holder);
                        }
                        return false;
                    }
                });
            }
        } else {
            if (holder.binding.editMode.getVisibility() == View.VISIBLE) {
                toggle(holder);
            }
        }

        holder.binding.txtOptionDigit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                createPopMenu(v, position);
            }
        });

        holder.binding.switchBtn.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    place.setActivation(1);
                } else {
                    place.setActivation(0);
                }
                mPlaceViewModel.updateToPlaces(place);
            }
        });

        holder.binding.view.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(context, PlaceActivity.class);
                intent.putExtra("id", place.getId());
                context.startActivity(intent);
            }
        });
        if (adapterType == ADAPTER_TYPE.ARCHIVE) {
            holder.binding.view.setFocusable(false);
            holder.binding.view.setEnabled(false);
            holder.binding.switchBtn.setEnabled(false);
        }
    }

    @SuppressLint("RestrictedApi")
    public void createPopMenu(View v, int position) {
        PopupMenu popupMenu = new PopupMenu(context, v);
        MenuPopupHelper menuHelper = new MenuPopupHelper(context, (MenuBuilder) popupMenu.getMenu(), v);

        switch (adapterType) {
            case MAIN:
                popupMenu.inflate(R.menu.option_menu);
                menuHelper.setForceShowIcon(true);
                menuHelper.setGravity(Gravity.END);
                menuHelper.show();
                popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem item) {
                        switch (item.getItemId()) {
                            case R.id.mnu_item_save:
                                archiving(position, false);
                                break;
                            case R.id.mnu_item_delete:
                                onItemDismiss(position);
                                break;
                            default:
                                break;
                        }
                        return false;
                    }
                });
                break;
            case ARCHIVE:
                popupMenu.inflate(R.menu.option_menu_archive);
                menuHelper.setForceShowIcon(true);
                menuHelper.setGravity(Gravity.START);
                menuHelper.show();

                popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem item) {
                        switch (item.getItemId()) {
                            case R.id.unArchive:
                                unarchiving(position);
                                break;
                            case R.id.item_delete:
                                onItemDismiss(position);
                                break;
                            default:
                                break;
                        }
                        return false;
                    }
                });
                break;
        }
    }

    public void onStartDrag(RecyclerView.ViewHolder viewHolder) {
        mItemTouchHelper.startDrag(viewHolder);
    }

    public int getBasicItemCount() {
        return mItemList == null ? 0 : mItemList.size();
    }

    @Override
    public int getItemCount() {
        return getBasicItemCount();
    }

    public void unarchiving(int position) {
        Place place = mItemList.get(position);
        place.setArchiving(0);
        mPlaceViewModel.updateToPlaces(place);
        notifyItemRemoved(position);
    }

    public void archiving(int position, boolean swiped) {
        Place place = mItemList.get(position);
        place.setArchiving(1);
        mPlaceViewModel.updateToPlaces(place);

        if (swiped) {
            placeIdlist.add(place.getId());
        }
        notifyItemRemoved(position);
    }

    public void unArchiveAfterSwipe() {
        Collections.reverse(placeIdlist);
        Place place;
        for (int i = 0; i < placeIdlist.size(); i++) {
            place = mPlaceViewModel.getPlace(placeIdlist.get(i));
            place.setArchiving(0);
            mPlaceViewModel.updateToPlaces(place);
            mItemList.add(place);
            notifyItemInserted(mItemList.size());
        }
    }

    public void filter(String text) {
        mItemList.clear();
        if (text.isEmpty()) {
            mItemList.addAll(mItemListCopy);
        } else {
            text = text.trim();
            for (int i = 0; i < mItemListCopy.size(); i++) {
                if (containsIgnoreCase(mItemListCopy.get(i).getName().trim(), text) |
                        containsIgnoreCase(mItemListCopy.get(i).getAddress().trim(), text) |
                        containsIgnoreCase(Integer.toString(mItemListCopy.get(i).getCondition()).trim(), text) |
                        containsIgnoreCase(mItemListCopy.get(i).getNumber().trim(), text) |
                        containsIgnoreCase(mItemListCopy.get(i).getNumberExit().trim(), text) |
                        containsIgnoreCase(mItemListCopy.get(i).getSms().trim(), text) |
                        containsIgnoreCase(mItemListCopy.get(i).getSmsExit().trim(), text)) {
                    mItemList.add(mItemListCopy.get(i));
                }
            }
        }
        notifyDataSetChanged();
    }

    public static boolean containsIgnoreCase(String str, String searchStr) {
        if (str == null || searchStr == null) return false;

        final int length = searchStr.length();
        if (length == 0)
            return true;

        for (int i = str.length() - length; i >= 0; i--) {
            if (str.regionMatches(true, i, searchStr, 0, length))
                return true;
        }
        return false;
    }

    public void onItemDismiss(int position) {
        Place place = mItemList.get(position);
        mPlaceViewModel.deleteFromPlaces(place.getId());
        notifyItemRemoved(position);
    }


    public void onItemMove(int fromPosition, int toPosition) {
        if (fromPosition < toPosition) {
            for (int i = fromPosition; i < toPosition; i++) {
                Collections.swap(mItemList, i, i + 1);
            }
        } else {
            for (int i = fromPosition; i > toPosition; i--) {
                Collections.swap(mItemList, i, i - 1);
            }
        }
        notifyItemMoved(fromPosition, toPosition);
    }

    public void onItemClear() {
        //adapter.open();
        for (int i = 0; i < mItemList.size(); i++) {
            Place place = mItemList.get(i);
            place.setSort(i);
            mPlaceViewModel.updateToPlaces(place);
            //adapter.update(place);
        }
        notifyDataSetChanged();
        //adapter.close();
    }

    public void setTouchHelper(ItemTouchHelper mItemTouchHelper) {
        this.mItemTouchHelper = mItemTouchHelper;
    }

    public static class PlaceItemViewHolder extends RecyclerView.ViewHolder {
        public RecyclerItemBinding binding;

        public PlaceItemViewHolder(final View v) {
            super(v);
            binding = DataBindingUtil.bind(v);
        }
    }

    public enum ADAPTER_TYPE {
        ARCHIVE, MAIN;
    }
}

