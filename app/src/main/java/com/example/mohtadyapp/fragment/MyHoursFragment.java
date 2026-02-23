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
import com.example.mohtadyapp.adapter.VolunteerHourAdapter;
import com.example.mohtadyapp.model.Organization;
import com.example.mohtadyapp.model.Student;
import com.example.mohtadyapp.model.VolunteerApplication;
import com.example.mohtadyapp.model.VolunteerHour;
import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;

public class MyHoursFragment extends Fragment {

    private VolunteerHourAdapter adapter;
    private VolunteerAppHelper helper;
    private String studentId;
    private TextView tvSummary;
    private TextView tvEmpty;

    public static MyHoursFragment newInstance(String studentId) {
        MyHoursFragment f = new MyHoursFragment();
        Bundle b = new Bundle();
        b.putString("studentId", studentId);
        f.setArguments(b);
        return f;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_my_hours, container, false);
        studentId = getArguments() != null ? getArguments().getString("studentId", "") : "";
        helper = new VolunteerAppHelper(requireContext());
        tvSummary = v.findViewById(R.id.tv_summary);
        tvEmpty = v.findViewById(R.id.tv_empty);

        RecyclerView rv = v.findViewById(R.id.rv_hours);
        rv.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new VolunteerHourAdapter();
        rv.setAdapter(adapter);

        v.findViewById(R.id.fab_register_hours).setOnClickListener(v1 -> showRegisterHoursDialog());

        loadData();
        return v;
    }

    private void showRegisterHoursDialog() {
        new Thread(() -> {
            DALAppWriteConnection.OperationResult<ArrayList<VolunteerApplication>> appResult = helper.getStudentApplications(studentId);
            DALAppWriteConnection.OperationResult<ArrayList<Organization>> orgResult = helper.getOrganizations();
            requireActivity().runOnUiThread(() -> {
                if (!appResult.success || appResult.data == null || orgResult.data == null) return;
                java.util.List<Organization> acceptedOrgs = new ArrayList<>();
                java.util.Map<String, Organization> orgMap = new java.util.HashMap<>();
                for (Organization o : orgResult.data) orgMap.put(o.getId(), o);
                for (VolunteerApplication a : appResult.data) {
                    if (VolunteerApplication.STATUS_ACCEPTED.equals(a.getStatus())) {
                        Organization o = orgMap.get(a.getOrganizationId());
                        if (o != null) acceptedOrgs.add(o);
                    }
                }
                if (acceptedOrgs.isEmpty()) {
                    Toast.makeText(requireContext(), "لا توجد مؤسسات مقبولة لتسجيل الساعات", Toast.LENGTH_SHORT).show();
                    return;
                }
                String[] names = new String[acceptedOrgs.size()];
                for (int i = 0; i < acceptedOrgs.size(); i++) names[i] = acceptedOrgs.get(i).getName();
                new AlertDialog.Builder(requireContext())
                        .setTitle("اختر المؤسسة")
                        .setItems(names, (d, which) -> {
                            Organization selected = acceptedOrgs.get(which);
                            View dialogView = getLayoutInflater().inflate(R.layout.dialog_register_hours, null);
                            TextInputEditText etHours = dialogView.findViewById(R.id.et_hours);
                            TextInputEditText etDesc = dialogView.findViewById(R.id.et_description);
                            new AlertDialog.Builder(requireContext())
                                    .setView(dialogView)
                                    .setTitle("تسجيل ساعات - " + selected.getName())
                                    .setPositiveButton("تسجيل", (d2, w2) -> {
                                        int hoursVal = 0;
                                        try { hoursVal = Integer.parseInt(etHours.getText() != null ? etHours.getText().toString() : "0"); } catch (NumberFormatException ignored) {}
                                        final int hoursToUse = hoursVal;
                                        final String descVal = etDesc.getText() != null ? etDesc.getText().toString() : "";
                                        if (hoursToUse <= 0) {
                                            Toast.makeText(requireContext(), "أدخل عدد الساعات", Toast.LENGTH_SHORT).show();
                                            return;
                                        }
                                        new Thread(() -> {
                                            DALAppWriteConnection.OperationResult<VolunteerHour> result = helper.registerVolunteerHours(studentId, selected.getId(), hoursToUse, descVal);
                                            requireActivity().runOnUiThread(() -> {
                                                Toast.makeText(requireContext(), result.success ? "تم التسجيل" : result.message, Toast.LENGTH_SHORT).show();
                                                if (result.success) loadData();
                                            });
                                        }).start();
                                    })
                                    .setNegativeButton("إلغاء", null)
                                    .show();
                        })
                        .setNegativeButton("إلغاء", null)
                        .show();
            });
        }).start();
    }

    private void loadData() {
        new Thread(() -> {
            DALAppWriteConnection.OperationResult<ArrayList<VolunteerHour>> hoursResult = helper.getStudentHours(studentId);
            DALAppWriteConnection.OperationResult<ArrayList<Organization>> orgResult = helper.getOrganizations();
            DALAppWriteConnection.OperationResult<Student> studentResult = helper.getStudentById(studentId);
            requireActivity().runOnUiThread(() -> {
                if (hoursResult.success && hoursResult.data != null) {
                    adapter.setList(hoursResult.data, orgResult.success ? orgResult.data : null);
                    int total = 0;
                    for (VolunteerHour h : hoursResult.data) {
                        if (VolunteerHour.STATUS_ACCEPTED.equals(h.getStatus())) total += h.getHours();
                    }
                    int required = studentResult.success && studentResult.data != null ? studentResult.data.getRequiredHours() : 0;
                    tvSummary.setText("الساعات المنتهية: " + total + " / " + required + " المطلوبة");
                    tvEmpty.setVisibility(hoursResult.data.isEmpty() ? View.VISIBLE : View.GONE);
                }
            });
        }).start();
    }
}
