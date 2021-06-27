package urum.geoplanner.ui.fragments;

import android.annotation.SuppressLint;
import android.graphics.Color;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.SearchView;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.Nullable;
import androidx.appcompat.view.menu.MenuBuilder;
import androidx.databinding.DataBindingUtil;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;

import java.util.List;

import urum.geoplanner.R;
import urum.geoplanner.adapter.PlaceAdapter;
import urum.geoplanner.adapter.SimpleItemTouchHelperCallback;
import urum.geoplanner.databinding.FragmentArchivePlacesBinding;
import urum.geoplanner.db.entities.Place;
import urum.geoplanner.ui.MainActivity;
import urum.geoplanner.viewmodel.ModelFactory;
import urum.geoplanner.viewmodel.PlaceViewModel;


public class ArchivePlacesFragment extends Fragment {


    FragmentArchivePlacesBinding binding;
    private LinearLayoutManager layoutManager;
    private PlaceViewModel mViewModel;
    private PlaceAdapter mAdapter;

    private Menu menu;
    SearchView searchView;
    SimpleItemTouchHelperCallback callback;
    ItemTouchHelper mItemTouchHelper;

    MainActivity mainActivity;

    public ArchivePlacesFragment() {
    }


    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @SuppressLint("NewApi")
    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        //setRetainInstance(true);
        // Костыль одним словом
        mainActivity = (MainActivity) getActivity();
        mainActivity.changeToolbarForArchive();
        mainActivity.changeLayoutForArchive(null);

        OnBackPressedCallback callback = new OnBackPressedCallback(true /* enabled by default */) {
            @Override
            public void handleOnBackPressed() {
                if (mainActivity.getSupportActionBar() != null) {
                    mainActivity.getSupportActionBar().setDisplayHomeAsUpEnabled(false);
                    mainActivity.getSupportActionBar().setDisplayShowTitleEnabled(false);
                    mainActivity.getSupportActionBar().setDisplayShowHomeEnabled(false);
                }
                mainActivity.binding.fabButton.show();

                if (mainActivity.binding.navView.getVisibility() == View.VISIBLE) {
                    mainActivity.slideUp();
                } else {
                    mainActivity.binding.navView.setVisibility(View.VISIBLE);
                }
                mainActivity.navController.navigate(R.id.list_places);
            }
        };
        requireActivity().getOnBackPressedDispatcher().addCallback(getViewLifecycleOwner(), callback);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        binding = DataBindingUtil.inflate(
                inflater, R.layout.fragment_archive_places, container, false);

        binding.setLifecycleOwner(this);

        mViewModel = new ViewModelProvider(this,
                new ModelFactory(getActivity().getApplication())).get(PlaceViewModel.class);

        binding.setViewmodel(mViewModel);

        layoutManager = new LinearLayoutManager(requireActivity());
        binding.recyclerView.setLayoutManager(layoutManager);

        mAdapter = new PlaceAdapter(mViewModel, binding.recyclerView, requireActivity(), PlaceAdapter.ADAPTER_TYPE.ARCHIVE);
        binding.recyclerView.setAdapter(mAdapter);
        callback = new SimpleItemTouchHelperCallback(mAdapter);
        mItemTouchHelper = new ItemTouchHelper(callback);
        mItemTouchHelper.attachToRecyclerView(binding.recyclerView);
        mAdapter.setTouchHelper(mItemTouchHelper);

        //mViewModel.deleteFromPlacesAll();

        mViewModel.getPlacesFromArchive().observe(requireActivity(), new Observer<List<Place>>() {
            @Override
            public void onChanged(@Nullable List<Place> places) {
                Log.d("mytag", places.toString());
                mAdapter.setPlaces(places);
            }
        });

        setHasOptionsMenu(true);
        return binding.getRoot();
    }

    @SuppressLint("RestrictedApi")
    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu_archive, menu);
        super.onCreateOptionsMenu(menu, inflater);
        if (menu instanceof MenuBuilder) {
            ((MenuBuilder) menu).setOptionalIconsVisible(true);
        }
        this.menu = menu;
        MenuItem buttonComp = menu.findItem(R.id.btnComplete);
        buttonComp.setVisible(false);
        MenuItem sItem = menu.findItem(R.id.menuSearchbtn);
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
            if (item.getItemId() != R.id.btnComplete) {
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

    @SuppressLint("NonConstantResourceId")
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        super.onOptionsItemSelected(item);
        switch (item.getItemId()) {
            case R.id.menuEditingItem:
                callback.editing();
                mAdapter.editMod();
                mAdapter.notifyDataSetChanged();
                menu.findItem(R.id.menuEditingItem).setVisible(false);
                menu.findItem(R.id.menuSearchbtn).setVisible(false);
                menu.findItem(R.id.btnComplete).setVisible(true);
                break;
            case R.id.btnComplete:
                callback.editing();
                mAdapter.editMod();
                mAdapter.notifyDataSetChanged();
                menu.findItem(R.id.btnComplete).setVisible(false);
                menu.findItem(R.id.menuEditingItem).setVisible(true);
                menu.findItem(R.id.menuSearchbtn).setVisible(true);
                break;
        }
        return true;
    }


}