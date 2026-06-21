package com.example.volunteerapp.model;

import com.google.gson.annotations.SerializedName;

/**
 * نموذج طلب الانضمام / التقديم على فرصة تطوع.
 * <p>
 * يربط بين الطالب والمؤسسة (و/أو الفرصة). يُخزّن في مجموعة {@code volunteer_applications}.
 * الحالات: {@link #STATUS_PENDING} → {@link #STATUS_ACCEPTED} أو {@link #STATUS_REJECTED}.
 * </p>
 */
public class VolunteerApplication {

    public static final String STATUS_PENDING  = "pending";
    public static final String STATUS_ACCEPTED = "accepted";
    public static final String STATUS_REJECTED = "rejected";

    @SerializedName(value = "id", alternate = {"$id"})
    private String id;

    /** معرّف مكرّر يتطلبه مخطط Appwrite */
    @SerializedName("volunteerapplicationId")
    private String volunteerapplicationId = "";

    @SerializedName("studentId")
    private String studentId;

    @SerializedName("organizationId")
    private String organizationId;

    @SerializedName("opportunityId")
    private String opportunityId;

    @SerializedName("status")
    private String status;

    /** سبب الرفض — يُملأ فقط عند الرفض */
    @SerializedName("rejectReason")
    private String rejectReason;

    @SerializedName("createdAt")
    private String createdAt;

    // ========================== المُنشئات ==========================

    public VolunteerApplication() {
    }

    /** تقديم مباشر على مؤسسة (بدون فرصة محددة) */
    public VolunteerApplication(String studentId, String organizationId) {
        this.studentId = studentId;
        this.organizationId = organizationId;
        this.status = STATUS_PENDING;
        this.createdAt = java.util.Calendar.getInstance().getTime().toString();
    }

    /** تقديم على فرصة تطوع محددة */
    public VolunteerApplication(String studentId, String organizationId, String opportunityId) {
        this.studentId = studentId;
        this.organizationId = organizationId;
        this.opportunityId = opportunityId;
        this.status = STATUS_PENDING;
        this.createdAt = java.util.Calendar.getInstance().getTime().toString();
    }

    // ========================== Getters & Setters ==========================

    public String getId() { return id; }
    public void setId(String id) {
        this.id = id;
        this.volunteerapplicationId = id;
    }

    public String getStudentId() { return studentId; }
    public void setStudentId(String studentId) { this.studentId = studentId; }

    public String getOrganizationId() { return organizationId; }
    public void setOrganizationId(String organizationId) { this.organizationId = organizationId; }

    public String getOpportunityId() { return opportunityId; }
    public void setOpportunityId(String opportunityId) { this.opportunityId = opportunityId; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getRejectReason() { return rejectReason; }
    public void setRejectReason(String rejectReason) { this.rejectReason = rejectReason; }

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }

    @Override
    public String toString() {
        return "VolunteerApplication{id='" + id + "', studentId='" + studentId +
                "', orgId='" + organizationId + "', status='" + status + "'}";
    }
}
