package com.example.volunteerapp.fragment;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.example.volunteerapp.Hellper.DALAppWriteConnection;
import com.example.volunteerapp.Hellper.VolunteerAppHelper;
import com.example.volunteerapp.R;
import com.example.volunteerapp.model.Organization;
import com.example.volunteerapp.model.VolunteerApplication;
import com.example.volunteerapp.model.VolunteerOpportunity;
import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * شاشة عرض تفاصيل فرصة تطوعية محددة، تشمل معلومات الفرصة وبيانات المؤسسة
 * وصورها، مع إمكانية التقديم عليها إذا لم يكن الطالب قد تقدّم مسبقاً.
 */
public class OpportunityDetailsFragment extends Fragment {

    private static final String ARG_OPP_ID = "oppId";
    private static final String ARG_ORG_ID = "orgId";
    private static final String ARG_STUDENT_ID = "studentId";
    private ExecutorService executor;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    /**
     * إنشاء نسخة جديدة مع معرّفات الفرصة والمؤسسة والطالب.
     *
     * @param oppId     معرّف الفرصة التطوعية
     * @param orgId     معرّف المؤسسة المالكة
     * @param studentId معرّف الطالب المتقدّم
     * @return نسخة مُهيّأة من OpportunityDetailsFragment
     */
    public static OpportunityDetailsFragment newInstance(String oppId, String orgId, String studentId) {
        OpportunityDetailsFragment f = new OpportunityDetailsFragment();
        Bundle b = new Bundle();
        b.putString(ARG_OPP_ID, oppId);
        b.putString(ARG_ORG_ID, orgId);
        b.putString(ARG_STUDENT_ID, studentId);
        f.setArguments(b);
        return f;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        executor = Executors.newSingleThreadExecutor();
        View v = inflater.inflate(R.layout.fragment_opportunity_details, container, false);
        Bundle args = getArguments();
        if (args == null) return v;

        String oppId = args.getString(ARG_OPP_ID, "");
        String orgId = args.getString(ARG_ORG_ID, "");
        String studentId = args.getString(ARG_STUDENT_ID, "");

        TextView tvTitle = v.findViewById(R.id.tv_title);
        TextView tvOrgName = v.findViewById(R.id.tv_org_name);
        TextView tvDesc = v.findViewById(R.id.tv_description);
        TextView tvLocation = v.findViewById(R.id.tv_location);
        TextView tvHours = v.findViewById(R.id.tv_hours);
        MaterialButton btnApply = v.findViewById(R.id.btn_apply);
        ProgressBar pb = v.findViewById(R.id.progress_bar);

        View scrollOrgPhotos = v.findViewById(R.id.scroll_org_photos);
        ImageView ivPhoto1 = v.findViewById(R.id.iv_org_photo1);
        ImageView ivPhoto2 = v.findViewById(R.id.iv_org_photo2);
        ImageView ivPhoto3 = v.findViewById(R.id.iv_org_photo3);
        LinearLayout layoutOrgInfo = v.findViewById(R.id.layout_org_info);
        TextView tvOrgAddress = v.findViewById(R.id.tv_org_address);
        TextView tvOrgPhone = v.findViewById(R.id.tv_org_phone);
        TextView tvOrgDays = v.findViewById(R.id.tv_org_days);
        TextView tvOrgHoursSchedule = v.findViewById(R.id.tv_org_hours_schedule);

        pb.setVisibility(View.VISIBLE);
        btnApply.setVisibility(View.GONE);

        executor.execute(() -> {
            VolunteerAppHelper helper = new VolunteerAppHelper(requireContext());
            DALAppWriteConnection.OperationResult<VolunteerOpportunity> oppResult = helper.getOpportunityById(oppId);
            DALAppWriteConnection.OperationResult<Organization> orgResult = helper.getOrganizationById(orgId);

            boolean alreadyApplied = false;
            DALAppWriteConnection.OperationResult<ArrayList<VolunteerApplication>> appsResult =
                    helper.getStudentApplications(studentId);
            if (appsResult.success && appsResult.data != null) {
                for (VolunteerApplication a : appsResult.data) {
                    if (oppId.equals(a.getOpportunityId())) {
                        alreadyApplied = true;
                        break;
                    }
                }
            }

            boolean finalAlreadyApplied = alreadyApplied;
            mainHandler.post(() -> {
                if (!isAdded()) return;
                pb.setVisibility(View.GONE);
                if (oppResult.success && oppResult.data != null) {
                    VolunteerOpportunity opp = oppResult.data;
                    tvTitle.setText(opp.getTitle());
                    tvDesc.setText(opp.getDescription());
                    tvLocation.setText(opp.getLocation());
                    tvHours.setText(opp.getHours() + " ساعة");

                    if (orgResult.success && orgResult.data != null) {
                        Organization org = orgResult.data;
                        tvOrgName.setText(org.getName());
                        bindOrgInfo(org, scrollOrgPhotos, ivPhoto1, ivPhoto2, ivPhoto3,
                                layoutOrgInfo, tvOrgAddress, tvOrgPhone, tvOrgDays, tvOrgHoursSchedule);
                    }

                    btnApply.setVisibility(View.VISIBLE);
                    if (finalAlreadyApplied) {
                        btnApply.setText(R.string.already_applied);
                        btnApply.setEnabled(false);
                    } else {
                        btnApply.setOnClickListener(x -> {
                            btnApply.setEnabled(false);
                            executor.execute(() -> {
                                DALAppWriteConnection.OperationResult<VolunteerApplication> applyResult =
                                        helper.applyToOpportunity(studentId, opp.getOrganizationId(), oppId);
                                mainHandler.post(() -> {
                                    if (!isAdded()) return;
                                    if (applyResult.success) {
                                        btnApply.setText(R.string.already_applied);
                                        Toast.makeText(requireContext(), "تم إرسال الطلب بنجاح", Toast.LENGTH_SHORT).show();
                                    } else {
                                        btnApply.setEnabled(true);
                                        Toast.makeText(requireContext(),
                                                applyResult.message != null ? applyResult.message : "فشل",
                                                Toast.LENGTH_SHORT).show();
                                    }
                                });
                            });
                        });
                    }
                }
            });
        });

        return v;
    }

    /**
     * ربط بيانات المؤسسة (الصور، العنوان، الهاتف، أيام وساعات التطوع) بعناصر الواجهة.
     */
    private void bindOrgInfo(Organization org, View scrollPhotos,
                             ImageView iv1, ImageView iv2, ImageView iv3,
                             LinearLayout layoutInfo, TextView tvAddr,
                             TextView tvPhone, TextView tvDays, TextView tvSchedule) {

        String url1 = org.getImageUrl();
        String url2 = org.getImageUrl2();
        String url3 = org.getImageUrl3();
        boolean hasPhotos = false;

        if (url1 != null && !url1.isEmpty()) {
            iv1.setVisibility(View.VISIBLE);
            Glide.with(this).load(url1).centerCrop().into(iv1);
            hasPhotos = true;
        }
        if (url2 != null && !url2.isEmpty()) {
            iv2.setVisibility(View.VISIBLE);
            Glide.with(this).load(url2).centerCrop().into(iv2);
            hasPhotos = true;
        }
        if (url3 != null && !url3.isEmpty()) {
            iv3.setVisibility(View.VISIBLE);
            Glide.with(this).load(url3).centerCrop().into(iv3);
            hasPhotos = true;
        }
        scrollPhotos.setVisibility(hasPhotos ? View.VISIBLE : View.GONE);

        layoutInfo.setVisibility(View.VISIBLE);

        String addr = org.getAddress();
        tvAddr.setText(addr != null && !addr.isEmpty() ? addr : "—");

        String phone = org.getContactPhone();
        tvPhone.setText(phone != null && !phone.isEmpty() ? phone : "—");

        String days = org.getVolunteerDays();
        tvDays.setText(days != null && !days.isEmpty() ? days : "—");

        String hours = org.getVolunteerHours();
        tvSchedule.setText(hours != null && !hours.isEmpty() ? hours : "—");
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        executor.shutdown();
    }
}
