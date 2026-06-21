package com.example.volunteerapp.fragment;

import android.app.AlertDialog;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
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

import com.example.volunteerapp.Hellper.DALAppWriteConnection;
import com.example.volunteerapp.Hellper.VolunteerAppHelper;
import com.example.volunteerapp.R;
import com.example.volunteerapp.adapter.VolunteerHourAdapter;
import com.example.volunteerapp.model.Organization;
import com.example.volunteerapp.model.ReportRequest;
import com.example.volunteerapp.model.Student;
import com.example.volunteerapp.model.VolunteerApplication;
import com.example.volunteerapp.model.VolunteerHour;
import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * واجهة عرض الساعات التطوعية المسجّلة للطالب.
 * <p>
 * تعرض قائمة بالساعات مع حالتها (مقبولة / مرفوضة / قيد المراجعة)،
 * وملخصًا بإجمالي الساعات المنتهية مقارنةً بالمطلوبة.
 * تتيح أيضًا تسجيل ساعات جديدة لدى المؤسسات المقبولة وطلب تقرير رسمي.
 */
public class MyHoursFragment extends Fragment {

    private VolunteerHourAdapter adapter;
    private VolunteerAppHelper helper;
    private String studentId;
    private TextView tvSummary;
    private TextView tvEmpty;
    private ExecutorService executor;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

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
        executor = Executors.newSingleThreadExecutor();
        View v = inflater.inflate(R.layout.fragment_my_hours, container, false);
        studentId = getArguments() != null ? getArguments().getString("studentId", "") : "";
        helper = new VolunteerAppHelper(requireContext());
        tvSummary = v.findViewById(R.id.tv_summary);
        tvEmpty = v.findViewById(R.id.tv_empty);

        RecyclerView rv = v.findViewById(R.id.rv_hours);
        rv.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new VolunteerHourAdapter();
        rv.setAdapter(adapter);

        v.findViewById(R.id.fab_action).setOnClickListener(v1 -> {
            String[] options = {"تسجيل ساعات", getString(R.string.request_report)};
            new AlertDialog.Builder(requireContext())
                    .setItems(options, (d, which) -> {
                        if (which == 0) showRegisterHoursDialog();
                        else showRequestReportDialog();
                    })
                    .show();
        });

        loadData();
        return v;
    }

    /**
     * يعرض حوارًا متعدد المراحل لتسجيل ساعات تطوعية:
     * أولًا يختار الطالب مؤسسة من المؤسسات المقبول فيها،
     * ثم يُدخل عدد الساعات ووصف النشاط ويُرسل الطلب للمراجعة.
     */
    private void showRegisterHoursDialog() {
        executor.execute(() -> {
            DALAppWriteConnection.OperationResult<ArrayList<VolunteerApplication>> appResult = helper.getStudentApplications(studentId);
            DALAppWriteConnection.OperationResult<ArrayList<Organization>> orgResult = helper.getOrganizations();
            mainHandler.post(() -> {
                if (!isAdded()) return;
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
                                        executor.execute(() -> {
                                            DALAppWriteConnection.OperationResult<VolunteerHour> result = helper.registerVolunteerHours(studentId, selected.getId(), hoursToUse, descVal);
                                            mainHandler.post(() -> {
                                                if (!isAdded()) return;
                                                Toast.makeText(requireContext(), result.success ? "تم التسجيل" : result.message, Toast.LENGTH_SHORT).show();
                                                if (result.success) loadData();
                                            });
                                        });
                                    })
                                    .setNegativeButton("إلغاء", null)
                                    .show();
                        })
                        .setNegativeButton("إلغاء", null)
                        .show();
            });
        });
    }

    /**
     * يعرض قائمة بالمؤسسات المقبول فيها الطالب لاختيار واحدة
     * وإرسال طلب تقرير رسمي بساعاته التطوعية لديها.
     */
    private void showRequestReportDialog() {
        executor.execute(() -> {
            DALAppWriteConnection.OperationResult<ArrayList<VolunteerApplication>> appResult = helper.getStudentApplications(studentId);
            DALAppWriteConnection.OperationResult<ArrayList<Organization>> orgResult = helper.getOrganizations();
            mainHandler.post(() -> {
                if (!isAdded()) return;
                if (!appResult.success || appResult.data == null || orgResult.data == null) return;
                java.util.List<Organization> acceptedOrgs = new ArrayList<>();
                java.util.Map<String, Organization> orgMap = new java.util.HashMap<>();
                java.util.Set<String> addedOrgIds = new java.util.HashSet<>();
                for (Organization o : orgResult.data) orgMap.put(o.getId(), o);
                for (VolunteerApplication a : appResult.data) {
                    if (VolunteerApplication.STATUS_ACCEPTED.equals(a.getStatus())) {
                        Organization o = orgMap.get(a.getOrganizationId());
                        if (o != null && !addedOrgIds.contains(o.getId())) {
                            acceptedOrgs.add(o);
                            addedOrgIds.add(o.getId());
                        }
                    }
                }
                if (acceptedOrgs.isEmpty()) {
                    Toast.makeText(requireContext(), R.string.no_accepted_orgs, Toast.LENGTH_SHORT).show();
                    return;
                }
                String[] names = new String[acceptedOrgs.size()];
                for (int i = 0; i < acceptedOrgs.size(); i++) names[i] = acceptedOrgs.get(i).getName();
                new AlertDialog.Builder(requireContext())
                        .setTitle(R.string.choose_org_for_report)
                        .setItems(names, (d, which) -> {
                            Organization selected = acceptedOrgs.get(which);
                            executor.execute(() -> {
                                DALAppWriteConnection.OperationResult<ReportRequest> result =
                                        helper.requestReport(studentId, selected.getId());
                                mainHandler.post(() -> {
                                    if (!isAdded()) return;
                                    Toast.makeText(requireContext(),
                                            result.success ? getString(R.string.report_request_sent) : result.message,
                                            Toast.LENGTH_SHORT).show();
                                });
                            });
                        })
                        .setNegativeButton("إلغاء", null)
                        .show();
            });
        });
    }

    /**
     * يجلب ساعات الطالب وبيانات المؤسسات والطالب نفسه،
     * ثم يحسب إجمالي الساعات المقبولة ويُحدّث الملخص والقائمة.
     */
    private void loadData() {
        executor.execute(() -> {
            DALAppWriteConnection.OperationResult<ArrayList<VolunteerHour>> hoursResult = helper.getStudentHours(studentId);
            DALAppWriteConnection.OperationResult<ArrayList<Organization>> orgResult = helper.getOrganizations();
            DALAppWriteConnection.OperationResult<Student> studentResult = helper.getStudentById(studentId);
            mainHandler.post(() -> {
                if (!isAdded()) return;
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
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        executor.shutdown();
    }
}
