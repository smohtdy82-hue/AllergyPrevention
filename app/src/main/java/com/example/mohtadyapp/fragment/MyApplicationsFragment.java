package com.example.mohtadyapp.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.mohtadyapp.Hellper.DALAppWriteConnection;
import com.example.mohtadyapp.Hellper.VolunteerAppHelper;
import com.example.mohtadyapp.R;
import com.example.mohtadyapp.adapter.VolunteerApplicationAdapter;
import com.example.mohtadyapp.model.Organization;
import com.example.mohtadyapp.model.VolunteerApplication;

import java.util.ArrayList;

public class MyApplicationsFragment extends Fragment {

    private VolunteerApplicationAdapter adapter;
    private VolunteerAppHelper helper;
    private String studentId;

    public static MyApplicationsFragment newInstance(String studentId) {
        MyApplicationsFragment f = new MyApplicationsFragment();
        Bundle b = new Bundle();
        b.putString("studentId", studentId);
        f.setArguments(b);
        return f;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_my_applications, container, false);
        studentId = getArguments() != null ? getArguments().getString("studentId", "") : "";
        helper = new VolunteerAppHelper(requireContext());

        RecyclerView rv = v.findViewById(R.id.rv_applications);
        TextView tvEmpty = v.findViewById(R.id.tv_empty);
        rv.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new VolunteerApplicationAdapter();
        rv.setAdapter(adapter);

        loadData(tvEmpty);
        return v;
    }

    private void loadData(TextView tvEmpty) {
        new Thread(() -> {
            DALAppWriteConnection.OperationResult<ArrayList<VolunteerApplication>> appResult =
                    helper.getStudentApplications(studentId);
            DALAppWriteConnection.OperationResult<ArrayList<Organization>> orgResult =
                    helper.getOrganizations();
            requireActivity().runOnUiThread(() -> {
                if (appResult.success && appResult.data != null) {
                    adapter.setList(appResult.data, orgResult.success ? orgResult.data : null);
                    tvEmpty.setVisibility(appResult.data.isEmpty() ? View.VISIBLE : View.GONE);
                }
            });
        }).start();
    }
}
