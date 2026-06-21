package com.example.volunteerapp.fragment;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.volunteerapp.Hellper.DALAppWriteConnection;
import com.example.volunteerapp.Hellper.VolunteerAppHelper;
import com.example.volunteerapp.R;
import com.example.volunteerapp.model.Organization;
import com.example.volunteerapp.model.Student;
import com.example.volunteerapp.model.VolunteerHour;
import com.example.volunteerapp.model.VolunteerOpportunity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * شاشة التقارير العامة، تعرض ملخص ساعات التطوع والإحصائيات.
 * تدعم وضعين: تقرير الطالب (ساعاته المكتملة مقسّمة حسب المؤسسة)
 * وتقرير المؤسسة (عدد المتطوعين والساعات والفرص المنشأة).
 */
public class ReportsFragment extends Fragment {

    private static final String ARG_USER_ID = "userId";
    private static final String ARG_IS_ORG = "isOrg";
    private ExecutorService executor;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    /**
     * إنشاء نسخة جديدة مع تحديد نوع المستخدم (طالب أو مؤسسة).
     *
     * @param userId معرّف المستخدم
     * @param isOrg  {@code true} إذا كان المستخدم مؤسسة، {@code false} إذا كان طالباً
     * @return نسخة مُهيّأة من ReportsFragment
     */
    public static ReportsFragment newInstance(String userId, boolean isOrg) {
        ReportsFragment f = new ReportsFragment();
        Bundle b = new Bundle();
        b.putString(ARG_USER_ID, userId);
        b.putBoolean(ARG_IS_ORG, isOrg);
        f.setArguments(b);
        return f;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        executor = Executors.newSingleThreadExecutor();
        View v = inflater.inflate(R.layout.fragment_reports, container, false);
        Bundle args = getArguments();
        if (args == null) return v;

        String userId = args.getString(ARG_USER_ID, "");
        boolean isOrg = args.getBoolean(ARG_IS_ORG, false);

        if (isOrg) {
            loadOrgReport(v, userId);
        } else {
            loadStudentReport(v, userId);
        }
        return v;
    }

    /**
     * تحميل وعرض تقرير الطالب: الساعات المكتملة، التقدّم، وتفصيل حسب المؤسسة.
     */
    private void loadStudentReport(View v, String studentId) {
        TextView tvTitle = v.findViewById(R.id.tv_report_title);
        TextView tvLine1 = v.findViewById(R.id.tv_summary_line1);
        TextView tvLine2 = v.findViewById(R.id.tv_summary_line2);
        ProgressBar progressHours = v.findViewById(R.id.progress_hours);
        TextView tvDetailsHeader = v.findViewById(R.id.tv_details_header);
        RecyclerView rv = v.findViewById(R.id.rv_report_items);
        ProgressBar pb = v.findViewById(R.id.progress_bar);

        tvTitle.setText(R.string.report_student_title);
        pb.setVisibility(View.VISIBLE);

        executor.execute(() -> {
            VolunteerAppHelper helper = new VolunteerAppHelper(requireContext());
            DALAppWriteConnection.OperationResult<Student> studentResult = helper.getStudentById(studentId);
            DALAppWriteConnection.OperationResult<ArrayList<VolunteerHour>> hoursResult = helper.getStudentHours(studentId);
            DALAppWriteConnection.OperationResult<ArrayList<Organization>> orgResult = helper.getOrganizations();

            mainHandler.post(() -> {
                if (!isAdded()) return;
                pb.setVisibility(View.GONE);

                int required = 0, completed = 0;
                if (studentResult.success && studentResult.data != null) {
                    required = studentResult.data.getRequiredHours();
                    completed = studentResult.data.getCompletedHours();
                }

                Map<String, String> orgNames = new HashMap<>();
                if (orgResult.success && orgResult.data != null) {
                    for (Organization o : orgResult.data) orgNames.put(o.getId(), o.getName());
                }

                Map<String, Integer> orgHoursAccepted = new HashMap<>();
                if (hoursResult.success && hoursResult.data != null) {
                    for (VolunteerHour h : hoursResult.data) {
                        if (VolunteerHour.STATUS_ACCEPTED.equals(h.getStatus())) {
                            String oid = h.getOrganizationId();
                            orgHoursAccepted.put(oid, orgHoursAccepted.getOrDefault(oid, 0) + h.getHours());
                        }
                    }
                }

                tvLine1.setText(String.format("الساعات المكتملة: %d / %d", completed, required));
                int totalAccepted = 0;
                for (int h : orgHoursAccepted.values()) totalAccepted += h;
                tvLine2.setText(String.format("إجمالي الساعات المقبولة: %d", totalAccepted));

                progressHours.setVisibility(View.VISIBLE);
                progressHours.setMax(Math.max(required, 1));
                progressHours.setProgress(completed);

                if (!orgHoursAccepted.isEmpty()) {
                    tvDetailsHeader.setVisibility(View.VISIBLE);
                    tvDetailsHeader.setText("تفصيل حسب المؤسسة");
                    ArrayList<String[]> rows = new ArrayList<>();
                    for (Map.Entry<String, Integer> e : orgHoursAccepted.entrySet()) {
                        String name = orgNames.containsKey(e.getKey()) ? orgNames.get(e.getKey()) : "مؤسسة";
                        rows.add(new String[]{name, e.getValue() + " ساعة"});
                    }
                    rv.setLayoutManager(new LinearLayoutManager(requireContext()));
                    rv.setAdapter(new SimpleReportAdapter(rows));
                }
            });
        });
    }

    /**
     * تحميل وعرض تقرير المؤسسة: عدد المتطوعين، الساعات المقبولة والمعلّقة، وقائمة الفرص.
     */
    private void loadOrgReport(View v, String orgId) {
        TextView tvTitle = v.findViewById(R.id.tv_report_title);
        TextView tvLine1 = v.findViewById(R.id.tv_summary_line1);
        TextView tvLine2 = v.findViewById(R.id.tv_summary_line2);
        TextView tvLine3 = v.findViewById(R.id.tv_summary_line3);
        TextView tvDetailsHeader = v.findViewById(R.id.tv_details_header);
        RecyclerView rv = v.findViewById(R.id.rv_report_items);
        ProgressBar pb = v.findViewById(R.id.progress_bar);

        tvTitle.setText(R.string.report_org_title);
        pb.setVisibility(View.VISIBLE);

        executor.execute(() -> {
            VolunteerAppHelper helper = new VolunteerAppHelper(requireContext());
            DALAppWriteConnection.OperationResult<ArrayList<VolunteerHour>> hoursResult = helper.getAllOrganizationHours(orgId);
            DALAppWriteConnection.OperationResult<ArrayList<VolunteerOpportunity>> oppResult = helper.getOrganizationOpportunities(orgId);

            mainHandler.post(() -> {
                if (!isAdded()) return;
                pb.setVisibility(View.GONE);

                int totalAccepted = 0, totalPending = 0;
                Set<String> uniqueStudents = new HashSet<>();
                Map<String, Integer> oppHours = new HashMap<>();

                if (hoursResult.success && hoursResult.data != null) {
                    for (VolunteerHour h : hoursResult.data) {
                        uniqueStudents.add(h.getStudentId());
                        if (VolunteerHour.STATUS_ACCEPTED.equals(h.getStatus())) {
                            totalAccepted += h.getHours();
                        } else if (VolunteerHour.STATUS_PENDING.equals(h.getStatus())) {
                            totalPending += h.getHours();
                        }
                    }
                }

                Map<String, String> oppNames = new HashMap<>();
                if (oppResult.success && oppResult.data != null) {
                    for (VolunteerOpportunity o : oppResult.data) {
                        oppNames.put(o.getId(), o.getTitle());
                    }
                }

                tvLine1.setText(String.format("عدد المتطوعين: %d", uniqueStudents.size()));
                tvLine2.setText(String.format("إجمالي الساعات المقبولة: %d", totalAccepted));
                tvLine3.setVisibility(View.VISIBLE);
                tvLine3.setText(String.format("ساعات قيد الانتظار: %d", totalPending));

                if (!oppNames.isEmpty()) {
                    tvDetailsHeader.setVisibility(View.VISIBLE);
                    tvDetailsHeader.setText("الفرص المنشأة");
                    ArrayList<String[]> rows = new ArrayList<>();
                    for (VolunteerOpportunity o : oppResult.data) {
                        String statusLabel = VolunteerOpportunity.STATUS_ACTIVE.equals(o.getStatus()) ? "نشطة" : "مغلقة";
                        rows.add(new String[]{o.getTitle(), statusLabel + " — " + o.getHours() + " ساعة"});
                    }
                    rv.setLayoutManager(new LinearLayoutManager(requireContext()));
                    rv.setAdapter(new SimpleReportAdapter(rows));
                }
            });
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        executor.shutdown();
    }

    /** محوّل بسيط لعرض صفوف التقرير (اسم + قيمة) في قائمة RecyclerView. */
    private static class SimpleReportAdapter extends RecyclerView.Adapter<SimpleReportAdapter.VH> {
        private final ArrayList<String[]> rows;
        SimpleReportAdapter(ArrayList<String[]> rows) { this.rows = rows; }

        @NonNull @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_report_row, parent, false);
            return new VH(v);
        }

        @Override public void onBindViewHolder(@NonNull VH h, int pos) {
            h.tvLabel.setText(rows.get(pos)[0]);
            h.tvValue.setText(rows.get(pos)[1]);
        }

        @Override public int getItemCount() { return rows.size(); }

        static class VH extends RecyclerView.ViewHolder {
            TextView tvLabel, tvValue;
            VH(View v) {
                super(v);
                tvLabel = v.findViewById(R.id.tv_label);
                tvValue = v.findViewById(R.id.tv_value);
            }
        }
    }
}
