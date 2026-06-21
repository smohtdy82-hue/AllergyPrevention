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
import com.example.volunteerapp.adapter.OrgRequestAdapter;
import com.example.volunteerapp.model.Student;
import com.example.volunteerapp.model.VolunteerApplication;
import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * شاشة طلبات انضمام المتطوعين للمؤسسة.
 * <p>
 * تعرض قائمة بطلبات الانضمام المعلّقة مع إمكانية قبول أو رفض كل طلب.
 * عند الرفض يُعرض حوار لإدخال سبب الرفض. يتم جلب بيانات الطالب
 * لكل طلب لعرض اسمه في القائمة.
 * </p>
 */
public class OrgRequestsFragment extends Fragment {

    private OrgRequestAdapter adapter;
    private VolunteerAppHelper helper;
    private String orgId;
    private ExecutorService executor;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private TextView tvEmpty;

    public static OrgRequestsFragment newInstance(String orgId) {
        OrgRequestsFragment f = new OrgRequestsFragment();
        Bundle b = new Bundle();
        b.putString("orgId", orgId);
        f.setArguments(b);
        return f;
    }

    /**
     * يبني الواجهة ويُعدّ المحوّل مع مستمعات القبول والرفض.
     * عند القبول يُحدّث الحالة مباشرةً، وعند الرفض يفتح حوار إدخال السبب.
     */
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        executor = Executors.newSingleThreadExecutor();
        View v = inflater.inflate(R.layout.fragment_org_requests, container, false);
        orgId = getArguments() != null ? getArguments().getString("orgId", "") : "";
        helper = new VolunteerAppHelper(requireContext());

        RecyclerView rv = v.findViewById(R.id.rv_requests);
        tvEmpty = v.findViewById(R.id.tv_empty);
        rv.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new OrgRequestAdapter();
        adapter.setOnAcceptClickListener(app -> {
            executor.execute(() -> {
                DALAppWriteConnection.OperationResult<Void> result = helper.updateApplicationStatus(app.getId(), VolunteerApplication.STATUS_ACCEPTED, null);
                mainHandler.post(() -> {
                    if (!isAdded()) return;
                    Toast.makeText(requireContext(), result.success ? "تم القبول" : result.message, Toast.LENGTH_SHORT).show();
                    if (result.success) loadData();
                });
            });
        });
        adapter.setOnRejectClickListener(app -> {
            View dialogView = getLayoutInflater().inflate(R.layout.dialog_reject_reason, null);
            TextInputEditText etReason = dialogView.findViewById(R.id.et_reject_reason);
            new AlertDialog.Builder(requireContext())
                    .setView(dialogView)
                    .setTitle("سبب الرفض")
                    .setPositiveButton("رفض", (d, w) -> {
                        String reason = etReason.getText() != null ? etReason.getText().toString() : "";
                        executor.execute(() -> {
                            DALAppWriteConnection.OperationResult<Void> result = helper.updateApplicationStatus(app.getId(), VolunteerApplication.STATUS_REJECTED, reason);
                            mainHandler.post(() -> {
                                if (!isAdded()) return;
                                Toast.makeText(requireContext(), result.success ? "تم الرفض" : result.message, Toast.LENGTH_SHORT).show();
                                if (result.success) loadData();
                            });
                        });
                    })
                    .setNegativeButton("إلغاء", null)
                    .show();
        });
        rv.setAdapter(adapter);

        loadData();
        return v;
    }

    /** يجلب طلبات الانضمام مع بيانات الطلاب المرتبطة ويُحدّث القائمة. */
    private void loadData() {
        executor.execute(() -> {
            DALAppWriteConnection.OperationResult<ArrayList<VolunteerApplication>> appResult = helper.getOrganizationApplications(orgId);
            ArrayList<Student> students = new ArrayList<>();
            if (appResult.success && appResult.data != null) {
                for (VolunteerApplication a : appResult.data) {
                    DALAppWriteConnection.OperationResult<Student> sResult = helper.getStudentById(a.getStudentId());
                    if (sResult.success && sResult.data != null) students.add(sResult.data);
                }
            }
            mainHandler.post(() -> {
                if (!isAdded()) return;
                if (appResult.success && appResult.data != null) {
                    adapter.setList(appResult.data, students);
                    tvEmpty.setVisibility(appResult.data.isEmpty() ? View.VISIBLE : View.GONE);
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
