package com.example.volunteerapp.model;

import com.google.gson.annotations.SerializedName;

/**
 * نموذج ساعة التطوع.
 * <p>
 * يُسجّل الطالب ساعات تطوعه في مؤسسة معيّنة، وتراجعها المؤسسة
 * (قبول أو رفض مع سبب). يُخزّن في مجموعة {@code volunteer_hours}.
 * </p>
 * <p>
 * ملاحظة: حقل {@code rejectReason} غير موجود حالياً في مخطط Appwrite،
 * لذا يُزال تلقائياً عند الإرسال بواسطة {@code stripUnknownAttributes}.
 * </p>
 */
public class VolunteerHour {

    public static final String STATUS_PENDING  = "pending";
    public static final String STATUS_ACCEPTED = "accepted";
    public static final String STATUS_REJECTED = "rejected";

    @SerializedName(value = "id", alternate = {"$id"})
    private String id;

    /** معرّف مكرّر يتطلبه مخطط Appwrite */
    @SerializedName("volunteerhourId")
    private String volunteerhourId = "";

    @SerializedName("studentId")
    private String studentId;

    @SerializedName("organizationId")
    private String organizationId;

    @SerializedName("hours")
    private int hours;

    @SerializedName("description")
    private String description;

    @SerializedName("status")
    private String status;

    /** سبب الرفض — يُملأ فقط عند الرفض من المؤسسة */
    @SerializedName("rejectReason")
    private String rejectReason;

    @SerializedName("date")
    private String date;

    @SerializedName("createdAt")
    private String createdAt;

    // ========================== المُنشئات ==========================

    public VolunteerHour() {
    }

    /**
     * إنشاء سجل ساعات جديد بحالة «قيد الانتظار».
     *
     * @param studentId      معرّف الطالب
     * @param organizationId معرّف المؤسسة
     * @param hours          عدد الساعات
     * @param description    وصف العمل التطوعي
     */
    public VolunteerHour(String studentId, String organizationId, int hours, String description) {
        this.studentId = studentId;
        this.organizationId = organizationId;
        this.hours = hours;
        this.description = description;
        this.status = STATUS_PENDING;
        this.date = java.util.Calendar.getInstance().getTime().toString();
        this.createdAt = this.date;
    }

    // ========================== Getters & Setters ==========================

    public String getId() { return id; }
    public void setId(String id) {
        this.id = id;
        this.volunteerhourId = id;
    }

    public String getStudentId() { return studentId; }
    public void setStudentId(String studentId) { this.studentId = studentId; }

    public String getOrganizationId() { return organizationId; }
    public void setOrganizationId(String organizationId) { this.organizationId = organizationId; }

    public int getHours() { return hours; }
    public void setHours(int hours) { this.hours = hours; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getRejectReason() { return rejectReason; }
    public void setRejectReason(String rejectReason) { this.rejectReason = rejectReason; }

    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }

    @Override
    public String toString() {
        return "VolunteerHour{id='" + id + "', hours=" + hours + ", status='" + status + "'}";
    }
}
