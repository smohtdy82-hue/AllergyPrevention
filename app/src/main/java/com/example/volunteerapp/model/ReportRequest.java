package com.example.volunteerapp.model;

import com.google.gson.annotations.SerializedName;

/**
 * نموذج طلب تقرير ساعات التطوع.
 * <p>
 * يُرسله الطالب لمؤسسة محددة. عند موافقة المؤسسة يتحول إلى {@link #STATUS_APPROVED}
 * ويستطيع الطالب تحميل/مشاركة ملف PDF بالساعات المقبولة.
 * يُخزّن في مجموعة {@code report_requests}.
 * </p>
 */
public class ReportRequest {

    public static final String STATUS_PENDING  = "pending";
    public static final String STATUS_APPROVED = "approved";

    @SerializedName(value = "id", alternate = {"$id"})
    private String id;

    /** معرّف مكرّر يتطلبه مخطط Appwrite */
    @SerializedName("reportrequestId")
    private String reportrequestId = "";

    @SerializedName("studentId")
    private String studentId = "";

    @SerializedName("organizationId")
    private String organizationId = "";

    @SerializedName("status")
    private String status = "";

    @SerializedName("createdAt")
    private String createdAt = "";

    // ========================== المُنشئات ==========================

    public ReportRequest() {
    }

    /**
     * إنشاء طلب تقرير جديد بحالة «قيد الانتظار».
     *
     * @param studentId      معرّف الطالب الطالب
     * @param organizationId معرّف المؤسسة المطلوب منها التقرير
     */
    public ReportRequest(String studentId, String organizationId) {
        this.studentId = studentId;
        this.organizationId = organizationId;
        this.status = STATUS_PENDING;
        this.createdAt = java.util.Calendar.getInstance().getTime().toString();
    }

    // ========================== Getters & Setters ==========================

    public String getId() { return id; }
    public void setId(String id) {
        this.id = id;
        this.reportrequestId = id;
    }

    public String getStudentId() { return studentId; }
    public void setStudentId(String studentId) { this.studentId = studentId; }

    public String getOrganizationId() { return organizationId; }
    public void setOrganizationId(String organizationId) { this.organizationId = organizationId; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
}
