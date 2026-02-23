package com.example.mohtadyapp.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.mohtadyapp.Hellper.DALAppWriteConnection;
import com.example.mohtadyapp.Hellper.VolunteerAppHelper;
import com.example.mohtadyapp.R;
import com.example.mohtadyapp.model.VolunteerApplication;
import com.example.mohtadyapp.adapter.OrganizationAdapter;
import com.example.mohtadyapp.model.Organization;

import java.util.ArrayList;

public class OrganizationsListFragment extends Fragment {

    private OrganizationAdapter adapter;
    private VolunteerAppHelper helper;
    private String studentId;

    public static OrganizationsListFragment newInstance(String studentId) {
        OrganizationsListFragment f = new OrganizationsListFragment();
        Bundle b = new Bundle();
        b.putString("studentId", studentId);
        f.setArguments(b);
        return f;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_organizations_list, container, false);
        studentId = getArguments() != null ? getArguments().getString("studentId", "") : "";
        helper = new VolunteerAppHelper(requireContext());

        RecyclerView rv = v.findViewById(R.id.rv_organizations);
        ProgressBar progress = v.findViewById(R.id.progress_bar);
        rv.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new OrganizationAdapter();
        adapter.setOnApplyClickListener(org -> {
            DALAppWriteConnection.OperationResult<VolunteerApplication> result = helper.applyToOrganization(studentId, org.getId());
            if (result.success) {
                Toast.makeText(requireContext(), "تم إرسال الطلب بنجاح", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(requireContext(), result.message != null ? result.message : "فشل إرسال الطلب", Toast.LENGTH_SHORT).show();
            }
        });
        rv.setAdapter(adapter);

        progress.setVisibility(View.VISIBLE);
        new Thread(() -> {
            DALAppWriteConnection.OperationResult<ArrayList<Organization>> result = helper.getOrganizations();
            requireActivity().runOnUiThread(() -> {
                progress.setVisibility(View.GONE);
                if (result.success && result.data != null) {
                    adapter.setList(result.data);
                }
            });
        }).start();

        return v;
    }
}
