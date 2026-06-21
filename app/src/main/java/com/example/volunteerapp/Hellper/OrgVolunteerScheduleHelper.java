package com.example.volunteerapp.Hellper;

import android.content.Context;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;

import androidx.annotation.Nullable;

import com.example.volunteerapp.R;
import com.google.android.material.checkbox.MaterialCheckBox;

import java.util.ArrayList;

/**
 * واجهة موحدة لأيام وساعات التطوع (تسجيل مؤسسة وإدارتها).
 */
public final class OrgVolunteerScheduleHelper {

    private static final String HOURS_24 = "24 ساعة";
    private static final int[] DAY_LAYOUT_IDS = {
            R.id.day_sat, R.id.day_sun, R.id.day_mon, R.id.day_tue, R.id.day_wed, R.id.day_thu, R.id.day_fri
    };
    private static final int[] DAY_CB_IDS = {
            R.id.cb_sat, R.id.cb_sun, R.id.cb_mon, R.id.cb_tue, R.id.cb_wed, R.id.cb_thu, R.id.cb_fri
    };
    private static final String[] DAY_LABELS = {
            "السبت", "الأحد", "الإثنين", "الثلاثاء", "الأربعاء", "الخميس", "الجمعة"
    };

    private OrgVolunteerScheduleHelper() {
    }

    public static void setupScheduleUi(Context context, View root, @Nullable Runnable onScheduleChanged) {
        setupDayClickListeners(root, onScheduleChanged);
        setupTimeSpinners(context, root, onScheduleChanged);

        MaterialCheckBox cb24 = root.findViewById(R.id.cb_24_hours);
        View layoutTimeRange = root.findViewById(R.id.layout_time_range);
        if (cb24 != null && layoutTimeRange != null) {
            cb24.setOnCheckedChangeListener((buttonView, isChecked) -> {
                layoutTimeRange.setVisibility(isChecked ? View.GONE : View.VISIBLE);
                if (onScheduleChanged != null) onScheduleChanged.run();
            });
            layoutTimeRange.setVisibility(cb24.isChecked() ? View.GONE : View.VISIBLE);
        }
    }

    private static void setupDayClickListeners(View root, @Nullable Runnable onScheduleChanged) {
        for (int i = 0; i < DAY_LAYOUT_IDS.length; i++) {
            View dayLayout = root.findViewById(DAY_LAYOUT_IDS[i]);
            MaterialCheckBox cb = root.findViewById(DAY_CB_IDS[i]);
            if (dayLayout == null || cb == null) continue;
            final MaterialCheckBox checkbox = cb;
            dayLayout.setOnClickListener(v -> checkbox.setChecked(!checkbox.isChecked()));
            cb.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (onScheduleChanged != null) onScheduleChanged.run();
            });
        }
    }

    private static void setupTimeSpinners(Context context, View root, @Nullable Runnable onScheduleChanged) {
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(context,
                R.array.hours_list, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        Spinner spinnerFrom = root.findViewById(R.id.spinner_from_time);
        Spinner spinnerTo = root.findViewById(R.id.spinner_to_time);
        if (spinnerFrom == null || spinnerTo == null) return;
        spinnerFrom.setAdapter(adapter);
        spinnerTo.setAdapter(adapter);
        spinnerFrom.setSelection(8);
        spinnerTo.setSelection(17);

        AdapterView.OnItemSelectedListener timeListener = new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (onScheduleChanged != null) onScheduleChanged.run();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        };
        spinnerFrom.setOnItemSelectedListener(timeListener);
        spinnerTo.setOnItemSelectedListener(timeListener);
    }

    /** يملأ المربعات والدوّارات من النصوص المخزّنة في الخادم */
    public static void bindStoredSchedule(View root, @Nullable String volunteerDays, @Nullable String volunteerHours) {
        for (int id : DAY_CB_IDS) {
            MaterialCheckBox cb = root.findViewById(id);
            if (cb != null) cb.setChecked(false);
        }
        if (volunteerDays != null && !volunteerDays.trim().isEmpty()) {
            String[] parts = volunteerDays.split("[,،]+");
            for (String raw : parts) {
                String token = raw.trim();
                for (int i = 0; i < DAY_LABELS.length; i++) {
                    if (DAY_LABELS[i].equals(token)) {
                        MaterialCheckBox cb = root.findViewById(DAY_CB_IDS[i]);
                        if (cb != null) cb.setChecked(true);
                        break;
                    }
                }
            }
        }

        MaterialCheckBox cb24 = root.findViewById(R.id.cb_24_hours);
        Spinner spinnerFrom = root.findViewById(R.id.spinner_from_time);
        Spinner spinnerTo = root.findViewById(R.id.spinner_to_time);
        View layoutTimeRange = root.findViewById(R.id.layout_time_range);
        if (cb24 == null || spinnerFrom == null || spinnerTo == null || layoutTimeRange == null) return;

        String vh = volunteerHours != null ? volunteerHours.trim() : "";
        boolean is24 = HOURS_24.equals(vh)
                || (vh.contains("24") && (vh.contains("ساعة") || vh.contains("ساع")));
        if (is24) {
            cb24.setChecked(true);
            layoutTimeRange.setVisibility(View.GONE);
        } else {
            cb24.setChecked(false);
            layoutTimeRange.setVisibility(View.VISIBLE);
            if (!vh.isEmpty()) {
                String[] rangeParts = vh.split("\\s*-\\s*");
                if (rangeParts.length == 2) {
                    setSpinnerToLabel(spinnerFrom, rangeParts[0].trim());
                    setSpinnerToLabel(spinnerTo, rangeParts[1].trim());
                }
            } else {
                spinnerFrom.setSelection(8);
                spinnerTo.setSelection(17);
            }
        }
    }

    private static void setSpinnerToLabel(Spinner spinner, String label) {
        android.widget.Adapter adapter = spinner.getAdapter();
        if (adapter == null) return;
        for (int i = 0; i < adapter.getCount(); i++) {
            Object item = adapter.getItem(i);
            if (item != null && label.equals(item.toString())) {
                spinner.setSelection(i);
                return;
            }
        }
    }

    public static String collectVolunteerDays(View root) {
        ArrayList<String> daysList = new ArrayList<>();
        for (int i = 0; i < DAY_CB_IDS.length; i++) {
            MaterialCheckBox cb = root.findViewById(DAY_CB_IDS[i]);
            if (cb != null && cb.isChecked()) {
                daysList.add(DAY_LABELS[i]);
            }
        }
        return String.join("، ", daysList);
    }

    public static String collectVolunteerHours(View root) {
        MaterialCheckBox cb24 = root.findViewById(R.id.cb_24_hours);
        if (cb24 != null && cb24.isChecked()) {
            return HOURS_24;
        }
        Spinner spinnerFrom = root.findViewById(R.id.spinner_from_time);
        Spinner spinnerTo = root.findViewById(R.id.spinner_to_time);
        if (spinnerFrom == null || spinnerTo == null) return "";
        CharSequence from = "";
        CharSequence to = "";
        if (spinnerFrom.getSelectedItem() != null) from = spinnerFrom.getSelectedItem().toString();
        if (spinnerTo.getSelectedItem() != null) to = spinnerTo.getSelectedItem().toString();
        return from + " - " + to;
    }

    /** null = صالح؛ وإلا رسالة خطأ */
    @Nullable
    public static String validateSchedule(View root) {
        boolean hasDay = false;
        for (int id : DAY_CB_IDS) {
            MaterialCheckBox cb = root.findViewById(id);
            if (cb != null && cb.isChecked()) {
                hasDay = true;
                break;
            }
        }
        MaterialCheckBox cb24 = root.findViewById(R.id.cb_24_hours);
        boolean is24 = cb24 != null && cb24.isChecked();
        if (!hasDay) {
            return "اختر يوم واحد على الأقل";
        }
        if (!is24) {
            Spinner spinnerFrom = root.findViewById(R.id.spinner_from_time);
            Spinner spinnerTo = root.findViewById(R.id.spinner_to_time);
            if (spinnerFrom == null || spinnerTo == null) {
                return "أدخل أوقات التطوع";
            }
            int fromPos = spinnerFrom.getSelectedItemPosition();
            int toPos = spinnerTo.getSelectedItemPosition();
            if (fromPos >= toPos) {
                return "ساعة البداية يجب أن تكون قبل ساعة النهاية";
            }
        }
        return null;
    }
}
