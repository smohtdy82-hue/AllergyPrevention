package com.example.volunteerapp.fragment;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
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
 * واجهة عرض طلبات التقارير الخاصة بالطالب.
 * <p>
 * تعرض قائمة بالتقارير المطلوبة مع حالتها (معتمد / قيد الانتظار)،
 * وتتيح تحميل التقرير بصيغة PDF ومشاركته عند الموافقة عليه.
 */
public class StudentReportsFragment extends Fragment {

    private VolunteerAppHelper helper;
    private String studentId;
    private ExecutorService executor;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private RecyclerView rv;
    private ProgressBar progressBar;
    private TextView tvEmpty;

    public static StudentReportsFragment newInstance(String studentId) {
        StudentReportsFragment f = new StudentReportsFragment();
        Bundle b = new Bundle();
        b.putString("studentId", studentId);
        f.setArguments(b);
        return f;
    }

    // ============================ دورة حياة الواجهة ============================

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        executor = Executors.newSingleThreadExecutor();
        View v = inflater.inflate(R.layout.fragment_student_reports, container, false);
        studentId = getArguments() != null ? getArguments().getString("studentId", "") : "";
        helper = new VolunteerAppHelper(requireContext());

        rv = v.findViewById(R.id.rv_reports);
        progressBar = v.findViewById(R.id.progress_bar);
        tvEmpty = v.findViewById(R.id.tv_empty);
        rv.setLayoutManager(new LinearLayoutManager(requireContext()));

        loadData();
        return v;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        executor.shutdown();
    }

    // ============================ تحميل البيانات ============================

    /** يجلب طلبات التقارير وأسماء المؤسسات ثم يُحدّث القائمة. */
    private void loadData() {
        progressBar.setVisibility(View.VISIBLE);
        tvEmpty.setVisibility(View.GONE);

        executor.execute(() -> {
            DALAppWriteConnection.OperationResult<ArrayList<ReportRequest>> reqResult =
                    helper.getStudentReportRequests(studentId);
            DALAppWriteConnection.OperationResult<ArrayList<Organization>> orgResult =
                    helper.getOrganizations();

            Map<String, Organization> orgMap = new HashMap<>();
            if (orgResult.success && orgResult.data != null) {
                for (Organization o : orgResult.data) orgMap.put(o.getId(), o);
            }

            mainHandler.post(() -> {
                if (!isAdded()) return;
                progressBar.setVisibility(View.GONE);

                if (!reqResult.success || reqResult.data == null || reqResult.data.isEmpty()) {
                    tvEmpty.setVisibility(View.VISIBLE);
                    return;
                }

                rv.setAdapter(new ReportRequestAdapter(reqResult.data, orgMap));
            });
        });
    }

    // ============================ إنشاء ومشاركة PDF ============================

    /** ينشئ ملف PDF بالتقرير التطوعي ويفتح نافذة المشاركة. */
    private void generateAndSharePdf(ReportRequest request, Organization org) {
        progressBar.setVisibility(View.VISIBLE);
        executor.execute(() -> {
            DALAppWriteConnection.OperationResult<Student> studentResult = helper.getStudentById(studentId);
            DALAppWriteConnection.OperationResult<ArrayList<VolunteerHour>> hoursResult =
                    helper.getAcceptedHoursForStudentAtOrg(studentId, org.getId());

            mainHandler.post(() -> {
                if (!isAdded()) return;
                progressBar.setVisibility(View.GONE);

                if (!studentResult.success || studentResult.data == null) {
                    Toast.makeText(requireContext(), R.string.pdf_error, Toast.LENGTH_SHORT).show();
                    return;
                }

                try {
                    File pdf = PdfReportGenerator.generateReport(
                            requireContext(), studentResult.data, org,
                            hoursResult.success && hoursResult.data != null ? hoursResult.data : new ArrayList<>());
                    startActivity(PdfReportGenerator.createShareIntent(requireContext(), pdf));
                } catch (Exception e) {
                    Toast.makeText(requireContext(), R.string.pdf_error, Toast.LENGTH_SHORT).show();
                }
            });
        });
    }

    // ============================ محوّل قائمة طلبات التقارير ============================

    /**
     * محوّل RecyclerView لعرض كل طلب تقرير مع اسم المؤسسة وحالته وزر التحميل.
     */
    private class ReportRequestAdapter extends RecyclerView.Adapter<ReportRequestAdapter.VH> {
        private final ArrayList<ReportRequest> items;
        private final Map<String, Organization> orgMap;

        ReportRequestAdapter(ArrayList<ReportRequest> items, Map<String, Organization> orgMap) {
            this.items = items;
            this.orgMap = orgMap;
        }

        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_student_report_request, parent, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(@NonNull VH h, int pos) {
            ReportRequest rr = items.get(pos);
            Organization org = orgMap.get(rr.getOrganizationId());
            h.tvOrgName.setText(org != null ? org.getName() : rr.getOrganizationId());

            String date = rr.getCreatedAt();
            if (date != null && date.length() > 20) date = date.substring(0, 20);
            h.tvDate.setText(date != null ? date : "");

            boolean approved = ReportRequest.STATUS_APPROVED.equals(rr.getStatus());
            h.tvStatus.setText(approved
                    ? getString(R.string.report_status_approved)
                    : getString(R.string.report_status_pending));
            h.tvStatus.setTextColor(approved
                    ? 0xFF2E7D32
                    : 0xFFFF8F00);

            if (approved && org != null) {
                h.btnDownload.setVisibility(View.VISIBLE);
                h.btnDownload.setOnClickListener(v -> generateAndSharePdf(rr, org));
            } else {
                h.btnDownload.setVisibility(View.GONE);
            }
        }

        @Override
        public int getItemCount() { return items.size(); }

        class VH extends RecyclerView.ViewHolder {
            TextView tvOrgName, tvStatus, tvDate;
            MaterialButton btnDownload;
            VH(View v) {
                super(v);
                tvOrgName = v.findViewById(R.id.tv_org_name);
                tvStatus = v.findViewById(R.id.tv_status);
                tvDate = v.findViewById(R.id.tv_date);
                btnDownload = v.findViewById(R.id.btn_download_pdf);
            }
        }
    }
}
