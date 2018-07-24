package org.odk.share.fragments;

import android.database.Cursor;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.odk.share.R;
import org.odk.share.adapters.TransferInstanceAdapter;
import org.odk.share.dao.InstancesDao;
import org.odk.share.dao.TransferDao;
import org.odk.share.dto.Instance;
import org.odk.share.dto.TransferInstance;
import org.odk.share.provider.InstanceProviderAPI;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;

import butterknife.BindView;
import butterknife.ButterKnife;

import static org.odk.share.activities.MainActivity.FORM_DISPLAY_NAME;
import static org.odk.share.activities.MainActivity.FORM_ID;
import static org.odk.share.activities.MainActivity.FORM_VERSION;

/**
 * Created by laksh on 6/27/2018.
 */

public class ReceivedInstancesFragment extends AppListFragment {

    @BindView(R.id.recyclerview)
    RecyclerView recyclerView;
    @BindView(R.id.buttonholder)
    LinearLayout buttonLayout;
    @BindView(R.id.empty_view)
    TextView emptyView;

    HashMap<Long, Instance> instanceMap;

    TransferInstanceAdapter transferInstanceAdapter;
    List<TransferInstance> transferInstanceList;
    List<TransferInstance> transferInstanceFilteredList;

    boolean showCheckBox = false;

    public ReceivedInstancesFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        setHasOptionsMenu(true);
        View view = inflater.inflate(R.layout.fragment_instances, container, false);
        ButterKnife.bind(this, view);

        instanceMap = new HashMap<>();
        transferInstanceList = new ArrayList<>();
        transferInstanceFilteredList = new ArrayList<>();
        selectedInstances = new LinkedHashSet<>();

        buttonLayout.setVisibility(View.GONE);

        LinearLayoutManager llm = new LinearLayoutManager(getActivity());
        llm.setOrientation(LinearLayoutManager.VERTICAL);
        recyclerView.setLayoutManager(llm);

        setupAdapter();
        getInstanceFromDB();
        updateAdapter();

        return view;
    }

    private void getInstanceFromDB() {
        String formVersion = getActivity().getIntent().getStringExtra(FORM_VERSION);
        String formId = getActivity().getIntent().getStringExtra(FORM_ID);
        String []selectionArgs;
        String selection;

        if (formVersion == null) {
            selectionArgs = new String[]{formId};
            selection = InstanceProviderAPI.InstanceColumns.JR_FORM_ID + "=? AND "
                    + InstanceProviderAPI.InstanceColumns.JR_VERSION + " IS NULL";
        } else {
            selectionArgs = new String[]{formId, formVersion};
            selection = InstanceProviderAPI.InstanceColumns.JR_FORM_ID + "=? AND "
                    + InstanceProviderAPI.InstanceColumns.JR_VERSION + "=?";
        }

        Cursor cursor = new InstancesDao().getInstancesCursor(selection, selectionArgs);
        instanceMap = new InstancesDao().getMapFromCursor(cursor);

        Cursor transferCursor = new TransferDao().getReceiveInstancesCursor();
        List<TransferInstance> transferInstances = new TransferDao().getInstancesFromCursor(transferCursor);
        for (TransferInstance instance : transferInstances) {
            if (instanceMap.containsKey(instance.getInstanceId())) {
                instance.setInstance(instanceMap.get(instance.getInstanceId()));
                transferInstanceList.add(instance);
            }
        }
    }

    private void setupAdapter() {
        transferInstanceAdapter = new TransferInstanceAdapter(getActivity(), transferInstanceFilteredList,
                this::onItemClick, selectedInstances, showCheckBox);
        recyclerView.setAdapter(transferInstanceAdapter);
    }

    private void setEmptyViewVisibility(String text) {
        if (transferInstanceFilteredList.size() > 0) {
            recyclerView.setVisibility(View.VISIBLE);
            emptyView.setVisibility(View.GONE);
        } else {
            recyclerView.setVisibility(View.GONE);
            emptyView.setVisibility(View.VISIBLE);
            emptyView.setText(text);
        }
    }

    private void onItemClick(View view, int position) {
    }

    @Override
    protected void updateAdapter() {
        transferInstanceFilteredList.clear();
        for (TransferInstance instance: transferInstanceList) {
            if (instance.getInstance().getDisplayName().toLowerCase(Locale.getDefault())
                    .contains(getFilterText().toString().toLowerCase(Locale.getDefault()))) {
                transferInstanceFilteredList.add(instance);
            }
        }

        setEmptyViewVisibility(getString(R.string.no_forms_received,
                getActivity().getIntent().getStringExtra(FORM_DISPLAY_NAME)));
        transferInstanceAdapter.notifyDataSetChanged();
    }
}
