package com.example.volunteerapp.fragment;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.volunteerapp.Hellper.DALAppWriteConnection;
import com.example.volunteerapp.Hellper.VolunteerAppHelper;
import com.example.volunteerapp.R;
import com.example.volunteerapp.adapter.VolunteerApplicationAdapter;
import com.example.volunteerapp.model.Organization;
import com.example.volunteerapp.model.VolunteerApplication;
import com.example.volunteerapp.model.VolunteerOpportunity;

import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * واجهة عرض طلبات الانضمام الخاصة بالطالب.
 * <p>
 * تجلب طلبات التطوع المقدّمة من الطالب مع بيانات المؤسسات والفرص المرتبطة بها،
 * وتعرضها في قائمة مع حالة كل طلب (مقبول / مرفوض / قيد الانتظار).
 */
public class MyApplicationsFragment extends Fragment {

    private ExecutorService executor;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private VolunteerApplicationAdapter adapter;
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
        executor = Executors.newSingleThreadExecutor();
        View v = inflater.inflate(R.layout.fragment_my_applications, container, false);
        studentId = getArguments() != null ? getArguments().getString("studentId", "") : "";

        RecyclerView rv = v.findViewById(R.id.rv_applications);
        TextView tvEmpty = v.findViewById(R.id.tv_empty);
        rv.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new VolunteerApplicationAdapter();
        rv.setAdapter(adapter);

        loadData(tvEmpty);
        return v;
    }

    /**
     * يجلب طلبات الطالب وقوائم المؤسسات والفرص بشكل متوازٍ،
     * ثم يربطها معًا في المحوّل لعرض اسم المؤسسة والفرصة لكل طلب.
     */
    private void loadData(TextView tvEmpty) {
        executor.execute(() -> {
            VolunteerAppHelper helper = new VolunteerAppHelper(requireContext());
            DALAppWriteConnection.OperationResult<ArrayList<VolunteerApplication>> appResult =
                    helper.getStudentApplications(studentId);
            DALAppWriteConnection.OperationResult<ArrayList<Organization>> orgResult =
                    helper.getOrganizations();
            DALAppWriteConnection.OperationResult<ArrayList<VolunteerOpportunity>> oppResult =
                    helper.getOpportunities();
            mainHandler.post(() -> {
                if (!isAdded()) return;
                if (appResult.success && appResult.data != null) {
                    adapter.setList(appResult.data,
                            orgResult.success ? orgResult.data : null,
                            oppResult.success ? oppResult.data : null);
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
