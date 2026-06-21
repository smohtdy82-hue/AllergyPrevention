package com.example.volunteerapp.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.CircleCrop;
import com.bumptech.glide.request.RequestOptions;
import com.example.volunteerapp.R;
import com.example.volunteerapp.model.Organization;

import java.util.ArrayList;
import java.util.List;

/**
 * محوّل لعرض قائمة المؤسسات التطوعية المتاحة للطالب.
 * يعرض اسم المؤسسة وعنوانها وأوقات التطوع وصورتها الرئيسية
 * مع زر لتقديم طلب الانضمام.
 */
public class OrganizationAdapter extends RecyclerView.Adapter<OrganizationAdapter.ViewHolder> {

    private List<Organization> list = new ArrayList<>();
    private OnApplyClickListener listener;

    /** واجهة استماع لحدث تقديم طلب الانضمام لمؤسسة. */
    public interface OnApplyClickListener {
        void onApply(Organization org);
    }

    public void setOnApplyClickListener(OnApplyClickListener listener) {
        this.listener = listener;
    }

    public void setList(List<Organization> list) {
        this.list = list != null ? list : new ArrayList<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_organization, parent, false);
        return new ViewHolder(v);
    }

    /**
     * يربط بيانات المؤسسة بعناصر الواجهة: الاسم، العنوان، أوقات التطوع.
     * يحمّل الصورة الرئيسية بشكل دائري عبر Glide أو يعرض صورة افتراضية.
     */
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Organization org = list.get(position);
        holder.tvName.setText(org.getName());
        holder.tvAddress.setText(org.getAddress());
        holder.tvHours.setText(org.getVolunteerDays() + " - " + org.getVolunteerHours() + " (" + org.getTotalHours() + " ساعة)");

        String primary = org.getPrimaryImageUrl();
        if (primary != null && !primary.isEmpty()) {
            Glide.with(holder.itemView.getContext())
                    .load(primary)
                    .apply(RequestOptions.bitmapTransform(new CircleCrop()))
                    .placeholder(android.R.drawable.ic_menu_gallery)
                    .into(holder.ivImage);
        } else {
            holder.ivImage.setImageResource(android.R.drawable.ic_menu_gallery);
        }

        holder.btnApply.setOnClickListener(v -> {
            if (listener != null) listener.onApply(org);
        });
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView ivImage;
        TextView tvName, tvAddress, tvHours;
        Button btnApply;

        ViewHolder(View itemView) {
            super(itemView);
            ivImage = itemView.findViewById(R.id.iv_org_image);
            tvName = itemView.findViewById(R.id.tv_org_name);
            tvAddress = itemView.findViewById(R.id.tv_org_address);
            tvHours = itemView.findViewById(R.id.tv_org_hours);
            btnApply = itemView.findViewById(R.id.btn_apply);
        }
    }
}
