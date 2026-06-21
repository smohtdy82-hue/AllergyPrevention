package com.example.volunteerapp.model;

import com.google.gson.annotations.SerializedName;

/**
 * نموذج فرصة التطوع.
 * <p>
 * تُنشئها المؤسسة وتظهر للطلاب في قائمة الفرص المتاحة.
 * يُخزّن في مجموعة {@code volunteer_opportunities}.
 * الحالات: {@link #STATUS_ACTIVE} (نشطة) أو {@link #STATUS_CLOSED} (مغلقة).
 * </p>
 */
public class VolunteerOpportunity {

    public static final String STATUS_ACTIVE = "active";
    public static final String STATUS_CLOSED = "closed";

    @SerializedName(value = "id", alternate = {"$id"})
    private String id;

    /** معرّف مكرّر يتطلبه مخطط Appwrite */
    @SerializedName("volunteeropportunityId")
    private String volunteeropportunityId = "";

    @SerializedName("title")
    private String title;

    @SerializedName("description")
    private String description;

    @SerializedName("location")
    private String location;

    @SerializedName("hours")
    private int hours;

    @SerializedName("organizationId")
    private String organizationId;

    @SerializedName("status")
    private String status;

    @SerializedName("createdAt")
    private String createdAt;

    // ========================== المُنشئات ==========================

    public VolunteerOpportunity() {
    }

    /**
     * إنشاء فرصة تطوع جديدة بحالة «نشطة».
     *
     * @param title          عنوان الفرصة
     * @param description    وصف مختصر
     * @param location       الموقع الجغرافي
     * @param hours          عدد الساعات المطلوبة
     * @param organizationId معرّف المؤسسة المنشئة
     */
    public VolunteerOpportunity(String title, String description, String location,
                                int hours, String organizationId) {
        this.title = title;
        this.description = description;
        this.location = location;
        this.hours = hours;
        this.organizationId = organizationId;
        this.status = STATUS_ACTIVE;
        this.createdAt = java.util.Calendar.getInstance().getTime().toString();
    }

    // ========================== Getters & Setters ==========================

    public String getId() { return id; }
    public void setId(String id) {
        this.id = id;
        this.volunteeropportunityId = id;
    }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    public int getHours() { return hours; }
    public void setHours(int hours) { this.hours = hours; }

    public String getOrganizationId() { return organizationId; }
    public void setOrganizationId(String organizationId) { this.organizationId = organizationId; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }

    @Override
    public String toString() {
        return "VolunteerOpportunity{id='" + id + "', title='" + title + "', status='" + status + "'}";
    }
}
