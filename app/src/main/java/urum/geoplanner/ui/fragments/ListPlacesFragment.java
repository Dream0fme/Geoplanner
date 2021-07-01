package urum.geoplanner.ui.fragments;

import android.annotation.SuppressLint;
import android.graphics.Color;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.SearchView;

import androidx.annotation.Nullable;
import androidx.appcompat.view.menu.MenuBuilder;
import androidx.databinding.DataBindingUtil;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.android.material.snackbar.Snackbar;

import java.util.List;

import urum.geoplanner.R;
import urum.geoplanner.adapter.PlaceAdapter;
import urum.geoplanner.adapter.SimpleItemTouchHelperCallback;
import urum.geoplanner.databinding.FragmentListPlacesBinding;
import urum.geoplanner.db.entities.Place;
import urum.geoplanner.ui.MainActivity;
import urum.geoplanner.viewmodel.ModelFactory;
import urum.geoplanner.viewmodel.PlaceViewModel;

import static urum.geoplanner.utils.Utils.findNavController;


public class ListPlacesFragment extends Fragment {


    FragmentListPlacesBinding binding;
    private LinearLayoutManager layoutManager;
    private PlaceViewModel mViewModel;
    private PlaceAdapter mAdapter;

    private Menu menu;
    SearchView searchView;
    SimpleItemTouchHelperCallback callback;
    ItemTouchHelper mItemTouchHelper;
    Snackbar snackbarArchiving;

    boolean activeEditingMod;

    MainActivity mainActivity;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = DataBindingUtil.inflate(
                inflater, R.layout.fragment_list_places, container, false);

        binding.setLifecycleOwner(this);

        mViewModel = new ViewModelProvider(this,
                new ModelFactory(getActivity().getApplication())).get(PlaceViewModel.class);

        binding.setViewmodel(mViewModel);

        layoutManager = new LinearLayoutManager(requireActivity());
        binding.recyclerView.setLayoutManager(layoutManager);

        mAdapter = new PlaceAdapter(mViewModel, binding.recyclerView, requireActivity(), PlaceAdapter.ADAPTER_TYPE.MAIN);
        binding.recyclerView.setAdapter(mAdapter);

        callback = new SimpleItemTouchHelperCallback(mAdapter);
        mItemTouchHelper = new ItemTouchHelper(callback);
        mItemTouchHelper.attachToRecyclerView(binding.recyclerView);
        mAdapter.setTouchHelper(mItemTouchHelper);

        mViewModel.getPlacesFromPlaces().observe(requireActivity(), new Observer<List<Place>>() {
            @Override
            public void onChanged(@Nullable List<Place> places) {
                //Log.d("mytag", places.toString());
                mAdapter.setPlaces(places);
            }
        });

        setHasOptionsMenu(true);
        mainActivity = (MainActivity) getActivity();

        return binding.getRoot();
    }

    @SuppressLint("RestrictedApi")
    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu, menu);
        super.onCreateOptionsMenu(menu, inflater);
        if (menu instanceof MenuBuilder) {
            ((MenuBuilder) menu).setOptionalIconsVisible(true);
        }
        this.menu = menu;
        MenuItem buttonComp = menu.findItem(R.id.buttonComplete);
        buttonComp.setVisible(false);
        MenuItem sItem = menu.findItem(R.id.menuSearch);
        searchView = (SearchView) sItem.getActionView();
        sItem.expandActionView();
        searchView.setQueryHint(getString(R.string.search_by_place));

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                mAdapter.filter(query);
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                mAdapter.filter(newText);
                return true;
            }
        });
        for (int i = 0; i < menu.size(); i++) {
            MenuItem item = menu.getItem(i);
            if (item.getItemId() != R.id.buttonComplete) {
                SpannableString spanString = new SpannableString(menu.getItem(i).getTitle().toString());
                spanString.setSpan(new ForegroundColorSpan(Color.BLACK), 0, spanString.length(), 0); //fix the color to white
                item.setTitle(spanString);
            } else {
                item.setShowAsAction(MenuItem.SHOW_AS_ACTION_WITH_TEXT | MenuItem.SHOW_AS_ACTION_ALWAYS);
                item.setIcon(R.drawable.ic_check_24px);
                item.setTitle(getString(R.string.complete));
            }
        }
    }

    public void snackBarShowArchive() {
        if (!mAdapter.placeIdlist.isEmpty()) {
            int countPlace = mAdapter.placeIdlist.size();
            int lastInt = countPlace % 10;
            String strPlace;

            if (lastInt > 1 & lastInt < 5) {
                strPlace = countPlace + " места";
            } else if (lastInt == 1) {
                strPlace = countPlace + " место";
            } else {
                strPlace = countPlace + " мест";
            }

            snackbarArchiving = Snackbar.make(requireActivity().findViewById(R.id.container),
                    strPlace + " перемещено в архив.",
                    Snackbar.LENGTH_SHORT).setAction(R.string.cancel,
                    new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            mAdapter.unArchiveAfterSwipe();
                            mAdapter.placeIdlist.clear();
                        }
                    });
            snackbarArchiving.setAnchorView(mainActivity.binding.coordinatorFAB);
            snackbarArchiving.addCallback(new Snackbar.Callback() {
                @Override
                public void onShown(Snackbar snackbar) {
                    super.onShown(snackbar);
                }

                @Override
                public void onDismissed(Snackbar snackbar, int event) {
                    super.onDismissed(snackbar, event);
                    mAdapter.placeIdlist.clear();
                }
            });
            snackbarArchiving.show();
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        super.onOptionsItemSelected(item);

        switch (item.getItemId()) {
            case R.id.menuEditing:
                activeEditingMod = true;
                callback.editing();
                mAdapter.editMod();
                mAdapter.notifyDataSetChanged();
                mainActivity.binding.fabButton.hide();
                menu.findItem(R.id.menuArchive).setVisible(false);
                menu.findItem(R.id.menuEditing).setVisible(false);
                menu.findItem(R.id.menuSearch).setVisible(false);
                menu.findItem(R.id.buttonComplete).setVisible(true);
                break;

            case R.id.menuArchive:
                findNavController(this).navigate(R.id.action_list_places_to_archive);
                break;

            case R.id.buttonComplete:
                activeEditingMod = false;
                callback.editing();
                mAdapter.editMod();
                //mAdapter.notifyDataSetChanged();
                mAdapter.setSorting();
                mainActivity.binding.fabButton.show();
                snackBarShowArchive();
                menu.findItem(R.id.buttonComplete).setVisible(false);
                menu.findItem(R.id.menuArchive).setVisible(true);
                menu.findItem(R.id.menuEditing).setVisible(true);
                menu.findItem(R.id.menuSearch).setVisible(true);
                break;
        }
        return true;
    }
}