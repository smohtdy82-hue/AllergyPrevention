package com.example.mohtadyapp.model;

import com.google.gson.annotations.SerializedName;

/**
 * نموذج طلب التطوع - عندما يطلب الطالب الانضمام لمؤسسة
 * الحالة: pending, accepted, rejected
 */
public class VolunteerApplication {
    public static final String STATUS_PENDING = "pending";
    public static final String STATUS_ACCEPTED = "accepted";
    public static final String STATUS_REJECTED = "rejected";

    @SerializedName(value = "id", alternate = {"$id"})
    private String id;
    @SerializedName("studentId")
    private String studentId;
    @SerializedName("organizationId")
    private String organizationId;
    @SerializedName("status")
    private String status;
    @SerializedName("rejectReason")
    private String rejectReason;
    @SerializedName("createdAt")
    private String createdAt;

    public VolunteerApplication() {
    }

    public VolunteerApplication(String studentId, String organizationId) {
        this.studentId = studentId;
        this.organizationId = organizationId;
        this.status = STATUS_PENDING;
        this.createdAt = java.util.Calendar.getInstance().getTime().toString();
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getStudentId() { return studentId; }
    public void setStudentId(String studentId) { this.studentId = studentId; }
    public String getOrganizationId() { return organizationId; }
    public void setOrganizationId(String organizationId) { this.organizationId = organizationId; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getRejectReason() { return rejectReason; }
    public void setRejectReason(String rejectReason) { this.rejectReason = rejectReason; }
    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }

    @Override
    public String toString() {
        return "VolunteerApplication{id='" + id + "', studentId='" + studentId +
                "', organizationId='" + organizationId + "', status='" + status +
                "', rejectReason='" + rejectReason + "', createdAt='" + createdAt + "'}";
    }
}
