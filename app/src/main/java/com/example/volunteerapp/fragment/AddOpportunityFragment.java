package com.example.volunteerapp.fragment;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.volunteerapp.Hellper.DALAppWriteConnection;
import com.example.volunteerapp.Hellper.VolunteerAppHelper;
import com.example.volunteerapp.R;
import com.example.volunteerapp.model.VolunteerOpportunity;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * شاشة إضافة فرصة تطوع جديدة.
 * <p>
 * تسمح للمؤسسة بإدخال عنوان الفرصة ووصفها وموقعها وعدد الساعات المطلوبة،
 * ثم تحفظها في قاعدة البيانات عبر {@link VolunteerAppHelper#createOpportunity}.
 * عند النجاح يتم إبلاغ المستمع عبر {@link OnOpportunitySaved}.
 * </p>
 */
public class AddOpportunityFragment extends Fragment {

    private static final String ARG_ORG_ID = "orgId";
    private ExecutorService executor;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private String orgId;

    /** يُستدعى عند حفظ الفرصة بنجاح للعودة إلى قائمة الفرص. */
    public interface OnOpportunitySaved {
        void onOpportunitySaved();
    }

    private OnOpportunitySaved onSaved;

    public void setOnOpportunitySaved(OnOpportunitySaved l) { this.onSaved = l; }

    public static AddOpportunityFragment newInstance(String orgId) {
        AddOpportunityFragment f = new AddOpportunityFragment();
        Bundle b = new Bundle();
        b.putString(ARG_ORG_ID, orgId);
        f.setArguments(b);
        return f;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) orgId = getArguments().getString(ARG_ORG_ID);
    }

    /**
     * يبني نموذج الإدخال ويُعدّ زر الحفظ الذي يتحقق من الحقول
     * ثم يُنشئ الفرصة في خيط خلفي.
     */
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        executor = Executors.newSingleThreadExecutor();
        View v = inflater.inflate(R.layout.fragment_add_opportunity, container, false);

        TextInputEditText etTitle = v.findViewById(R.id.et_title);
        TextInputEditText etDesc = v.findViewById(R.id.et_description);
        TextInputEditText etLocation = v.findViewById(R.id.et_location);
        TextInputEditText etHours = v.findViewById(R.id.et_hours);
        MaterialButton btnSave = v.findViewById(R.id.btn_save);

        btnSave.setOnClickListener(x -> {
            String title = text(etTitle);
            String desc = text(etDesc);
            String location = text(etLocation);
            String hoursStr = text(etHours);

            if (title.isEmpty()) { etTitle.setError("مطلوب"); return; }
            if (desc.isEmpty()) { etDesc.setError("مطلوب"); return; }
            if (location.isEmpty()) { etLocation.setError("مطلوب"); return; }
            int hours;
            try { hours = Integer.parseInt(hoursStr); } catch (NumberFormatException e) {
                etHours.setError("أدخل رقماً صحيحاً"); return;
            }

            btnSave.setEnabled(false);
            VolunteerOpportunity opp = new VolunteerOpportunity(title, desc, location, hours, orgId);

            executor.execute(() -> {
                VolunteerAppHelper helper = new VolunteerAppHelper(requireContext());
                DALAppWriteConnection.OperationResult<VolunteerOpportunity> result = helper.createOpportunity(opp);
                mainHandler.post(() -> {
                    if (!isAdded()) return;
                    btnSave.setEnabled(true);
                    if (result.success) {
                        Toast.makeText(requireContext(), "تم إنشاء الفرصة بنجاح", Toast.LENGTH_SHORT).show();
                        if (onSaved != null) onSaved.onOpportunitySaved();
                    } else {
                        Toast.makeText(requireContext(),
                                result.message != null ? result.message : "فشل الحفظ",
                                Toast.LENGTH_LONG).show();
                    }
                });
            });
        });

        return v;
    }

    private static String text(TextInputEditText et) {
        return et.getText() != null ? et.getText().toString().trim() : "";
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        executor.shutdown();
    }
}
