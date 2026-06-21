package com.example.volunteerapp.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.volunteerapp.R;
import com.example.volunteerapp.model.Student;
import com.example.volunteerapp.model.VolunteerApplication;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * محوّل لعرض طلبات الانضمام التطوعية الواردة للمؤسسة.
 * يعرض اسم الطالب ورقم هاتفه مع أزرار للقبول والرفض.
 * يربط كل طلب ببيانات الطالب عبر خريطة {@code studentMap}.
 */
public class OrgRequestAdapter extends RecyclerView.Adapter<OrgRequestAdapter.ViewHolder> {

    private List<VolunteerApplication> list = new ArrayList<>();
    private Map<String, Student> studentMap = new HashMap<>();
    private OnAcceptClickListener onAccept;
    private OnRejectClickListener onReject;

    /** واجهة استماع لحدث قبول طلب انضمام. */
    public interface OnAcceptClickListener {
        void onAccept(VolunteerApplication app);
    }

    /** واجهة استماع لحدث رفض طلب انضمام. */
    public interface OnRejectClickListener {
        void onReject(VolunteerApplication app);
    }

    public void setOnAcceptClickListener(OnAcceptClickListener listener) {
        this.onAccept = listener;
    }

    public void setOnRejectClickListener(OnRejectClickListener listener) {
        this.onReject = listener;
    }

    /**
     * يُحدّث قائمة الطلبات وبيانات الطلاب المرتبطة بها.
     * يبني خريطة بمعرّف الطالب كمفتاح للوصول السريع أثناء الربط.
     *
     * @param list     قائمة طلبات الانضمام
     * @param students قائمة الطلاب المتقدّمين
     */
    public void setList(List<VolunteerApplication> list, List<Student> students) {
        this.list = list != null ? list : new ArrayList<>();
        studentMap.clear();
        if (students != null) {
            for (Student s : students) studentMap.put(s.getId(), s);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_request, parent, false);
        return new ViewHolder(v);
    }

    /**
     * يربط بيانات طلب الانضمام بعناصر الواجهة، ويعرض اسم الطالب ورقم هاتفه
     * مع توصيل أزرار القبول والرفض بالمستمعين.
     */
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        VolunteerApplication app = list.get(position);
        Student student = studentMap.get(app.getStudentId());
        holder.tvStudentName.setText(student != null ? student.getName() : "طالب");
        holder.tvStudentPhone.setText(student != null ? student.getPhone() : "");

        holder.btnAccept.setOnClickListener(v -> {
            if (onAccept != null) onAccept.onAccept(app);
        });
        holder.btnReject.setOnClickListener(v -> {
            if (onReject != null) onReject.onReject(app);
        });
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvStudentName, tvStudentPhone;
        Button btnAccept, btnReject;

        ViewHolder(View itemView) {
            super(itemView);
            tvStudentName = itemView.findViewById(R.id.tv_student_name);
            tvStudentPhone = itemView.findViewById(R.id.tv_student_phone);
            btnAccept = itemView.findViewById(R.id.btn_accept);
            btnReject = itemView.findViewById(R.id.btn_reject);
        }
    }
}
