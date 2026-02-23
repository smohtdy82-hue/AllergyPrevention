package com.example.mohtadyapp.fragment;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.mohtadyapp.Hellper.DALAppWriteConnection;
import com.example.mohtadyapp.Hellper.VolunteerAppHelper;
import com.example.mohtadyapp.R;
import com.example.mohtadyapp.adapter.OrgRequestAdapter;
import com.example.mohtadyapp.model.Student;
import com.example.mohtadyapp.model.VolunteerApplication;
import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;

public class OrgRequestsFragment extends Fragment {

    private OrgRequestAdapter adapter;
    private VolunteerAppHelper helper;
    private String orgId;

    public static OrgRequestsFragment newInstance(String orgId) {
        OrgRequestsFragment f = new OrgRequestsFragment();
        Bundle b = new Bundle();
        b.putString("orgId", orgId);
        f.setArguments(b);
        return f;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_org_requests, container, false);
        orgId = getArguments() != null ? getArguments().getString("orgId", "") : "";
        helper = new VolunteerAppHelper(requireContext());

        RecyclerView rv = v.findViewById(R.id.rv_requests);
        TextView tvEmpty = v.findViewById(R.id.tv_empty);
        rv.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new OrgRequestAdapter();
        adapter.setOnAcceptClickListener(app -> {
            new Thread(() -> {
                DALAppWriteConnection.OperationResult<Void> result = helper.updateApplicationStatus(app.getId(), VolunteerApplication.STATUS_ACCEPTED, null);
                requireActivity().runOnUiThread(() -> {
                    Toast.makeText(requireContext(), result.success ? "تم القبول" : result.message, Toast.LENGTH_SHORT).show();
                    if (result.success) loadData(tvEmpty);
                });
            }).start();
        });
        adapter.setOnRejectClickListener(app -> {
            View dialogView = getLayoutInflater().inflate(R.layout.dialog_reject_reason, null);
            TextInputEditText etReason = dialogView.findViewById(R.id.et_reject_reason);
            new AlertDialog.Builder(requireContext())
                    .setView(dialogView)
                    .setTitle("سبب الرفض")
                    .setPositiveButton("رفض", (d, w) -> {
                        String reason = etReason.getText() != null ? etReason.getText().toString() : "";
                        new Thread(() -> {
                            DALAppWriteConnection.OperationResult<Void> result = helper.updateApplicationStatus(app.getId(), VolunteerApplication.STATUS_REJECTED, reason);
                            requireActivity().runOnUiThread(() -> {
                                Toast.makeText(requireContext(), result.success ? "تم الرفض" : result.message, Toast.LENGTH_SHORT).show();
                                if (result.success) loadData(tvEmpty);
                            });
                        }).start();
                    })
                    .setNegativeButton("إلغاء", null)
                    .show();
        });
        rv.setAdapter(adapter);

        loadData(tvEmpty);
        return v;
    }

    private void loadData(TextView tvEmpty) {
        new Thread(() -> {
            DALAppWriteConnection.OperationResult<ArrayList<VolunteerApplication>> appResult = helper.getOrganizationApplications(orgId);
            ArrayList<Student> students = new ArrayList<>();
            if (appResult.success && appResult.data != null) {
                for (VolunteerApplication a : appResult.data) {
                    DALAppWriteConnection.OperationResult<Student> sResult = helper.getStudentById(a.getStudentId());
                    if (sResult.success && sResult.data != null) students.add(sResult.data);
                }
            }
            requireActivity().runOnUiThread(() -> {
                if (appResult.success && appResult.data != null) {
                    adapter.setList(appResult.data, students);
                    tvEmpty.setVisibility(appResult.data.isEmpty() ? View.VISIBLE : View.GONE);
                }
            });
        }).start();
    }
}
