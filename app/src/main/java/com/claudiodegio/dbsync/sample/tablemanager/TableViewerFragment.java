package com.claudiodegio.dbsync.sample.tablemanager;

import android.app.Fragment;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.claudiodegio.dbsync.sample.R;


public class TableViewerFragment extends Fragment {


    public static TableViewerFragment newInstance() {
        
        Bundle args = new Bundle();
        
        TableViewerFragment fragment = new TableViewerFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_table_view, container, false);
    }
}
