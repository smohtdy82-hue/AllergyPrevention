package com.example.volunteerapp.fragment;

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
import com.example.volunteerapp.Hellper.PdfReportGenerator;
import com.example.volunteerapp.Hellper.VolunteerAppHelper;
import com.example.volunteerapp.R;
import com.example.volunteerapp.model.Organization;
import com.example.volunteerapp.model.ReportRequest;
import com.example.volunteerapp.model.Student;
import com.example.volunteerapp.model.VolunteerHour;
import com.google.android.material.button.MaterialButton;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * شاشة طلبات التقارير الموجهة للمؤسسة.
 * <p>
 * تعرض قائمة بالطلاب الذين طلبوا تقريراً رسمياً عن ساعاتهم التطوعية.
 * عند الموافقة يُولَّد ملف PDF يتضمن بيانات الطالب والمؤسسة والساعات المقبولة،
 * ثم يُشارَك عبر نية ({@link android.content.Intent}) نظام أندرويد.
 * </p>
 */
public class OrgReportRequestsFragment extends Fragment {

    private ExecutorService executor;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private VolunteerAppHelper helper;
    private String orgId;
    private RecyclerView rv;
    private TextView tvEmpty;

    public static OrgReportRequestsFragment newInstance(String orgId) {
        OrgReportRequestsFragment f = new OrgReportRequestsFragment();
        Bundle b = new Bundle();
        b.putString("orgId", orgId);
        f.setArguments(b);
        return f;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        executor = Executors.newSingleThreadExecutor();
        View v = inflater.inflate(R.layout.fragment_org_requests, container, false);
        orgId = getArguments() != null ? getArguments().getString("orgId", "") : "";
        helper = new VolunteerAppHelper(requireContext());

        rv = v.findViewById(R.id.rv_requests);
        tvEmpty = v.findViewById(R.id.tv_empty);
        rv.setLayoutManager(new LinearLayoutManager(requireContext()));

        loadData();
        return v;
    }

    /** يجلب طلبات التقارير مع بيانات الطلاب ويُحدّث القائمة. */
    private void loadData() {
        executor.execute(() -> {
            DALAppWriteConnection.OperationResult<ArrayList<ReportRequest>> reqResult = helper.getOrgReportRequests(orgId);
            Map<String, Student> studentMap = new HashMap<>();
            if (reqResult.success && reqResult.data != null) {
                for (ReportRequest rr : reqResult.data) {
                    if (!studentMap.containsKey(rr.getStudentId())) {
                        DALAppWriteConnection.OperationResult<Student> sResult = helper.getStudentById(rr.getStudentId());
                        if (sResult.success && sResult.data != null) studentMap.put(rr.getStudentId(), sResult.data);
                    }
                }
            }
            mainHandler.post(() -> {
                if (!isAdded()) return;
                if (reqResult.success && reqResult.data != null) {
                    rv.setAdapter(new ReportRequestAdapter(reqResult.data, studentMap));
                    tvEmpty.setVisibility(reqResult.data.isEmpty() ? View.VISIBLE : View.GONE);
                    if (reqResult.data.isEmpty()) tvEmpty.setText(R.string.no_report_requests);
                }
            });
        });
    }

    /**
     * يُولّد تقرير PDF للطالب يحتوي على ساعاته المقبولة في هذه المؤسسة،
     * يُوافق على الطلب في قاعدة البيانات، ثم يفتح نية المشاركة.
     */
    private void generateAndSharePdf(ReportRequest rr, Student student) {
        executor.execute(() -> {
            DALAppWriteConnection.OperationResult<Organization> orgResult = helper.getOrganizationById(orgId);
            DALAppWriteConnection.OperationResult<ArrayList<VolunteerHour>> hoursResult =
                    helper.getAcceptedHoursForStudentAtOrg(rr.getStudentId(), orgId);

            if (!orgResult.success || orgResult.data == null || !hoursResult.success || hoursResult.data == null) {
                mainHandler.post(() -> {
                    if (!isAdded()) return;
                    Toast.makeText(requireContext(), R.string.pdf_error, Toast.LENGTH_SHORT).show();
                });
                return;
            }

            helper.approveReportRequest(rr.getId());

            try {
                File pdf = PdfReportGenerator.generateReport(requireContext(), student, orgResult.data, hoursResult.data);
                mainHandler.post(() -> {
                    if (!isAdded()) return;
                    Toast.makeText(requireContext(), R.string.pdf_shared, Toast.LENGTH_SHORT).show();
                    startActivity(PdfReportGenerator.createShareIntent(requireContext(), pdf));
                    loadData();
                });
            } catch (Exception e) {
                mainHandler.post(() -> {
                    if (!isAdded()) return;
                    Toast.makeText(requireContext(), R.string.pdf_error, Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    /** محوّل داخلي لعرض عناصر طلبات التقارير في القائمة. */
    private class ReportRequestAdapter extends RecyclerView.Adapter<ReportRequestAdapter.VH> {
        private final ArrayList<ReportRequest> list;
        private final Map<String, Student> studentMap;

        ReportRequestAdapter(ArrayList<ReportRequest> list, Map<String, Student> studentMap) {
            this.list = list;
            this.studentMap = studentMap;
        }

        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_report_request, parent, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(@NonNull VH h, int position) {
            ReportRequest rr = list.get(position);
            Student student = studentMap.get(rr.getStudentId());
            h.tvName.setText(student != null ? student.getName() : "طالب");
            h.tvDate.setText(rr.getCreatedAt() != null ? rr.getCreatedAt() : "");
            h.btnGenerate.setOnClickListener(v -> {
                if (student != null) {
                    generateAndSharePdf(rr, student);
                }
            });
        }

        @Override
        public int getItemCount() { return list.size(); }

        class VH extends RecyclerView.ViewHolder {
            TextView tvName, tvDate;
            MaterialButton btnGenerate;
            VH(View v) {
                super(v);
                tvName = v.findViewById(R.id.tv_student_name);
                tvDate = v.findViewById(R.id.tv_date);
                btnGenerate = v.findViewById(R.id.btn_generate_pdf);
            }
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        executor.shutdown();
    }
}
