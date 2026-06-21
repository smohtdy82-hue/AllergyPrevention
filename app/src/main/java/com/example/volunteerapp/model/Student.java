package com.example.volunteerapp.model;

import com.google.gson.annotations.SerializedName;

/**
 * نموذج بيانات الطالب.
 * <p>
 * يمثّل طالباً في منظومة التطوع. يُخزّن في مجموعة {@code students} في Appwrite.
 * الصورة الشخصية تُخزّن كمعرّف ملف (fileId) ويُبنى رابط التحميل عند الطلب.
 * </p>
 *
 * @see com.example.volunteerapp.Hellper.VolunteerAppHelper
 */
public class Student {

    @SerializedName(value = "id", alternate = {"$id"})
    private String id;

    @SerializedName("name")
    private String name = "";

    @SerializedName("email")
    private String email = "";

    /** معرّف ملف الصورة في Appwrite Storage أو رابط URL كامل */
    @SerializedName("imageUrl")
    private String imageUrl = "";

    @SerializedName("idNumber")
    private String idNumber = "";

    @SerializedName("schoolAddress")
    private String schoolAddress = "";

    @SerializedName("phone")
    private String phone = "";

    @SerializedName("password")
    private String password = "";

    @SerializedName("birthDate")
    private String birthDate = "";

    @SerializedName("requiredHours")
    private int requiredHours;

    @SerializedName("completedHours")
    private int completedHours;

    // ========================== المُنشئات ==========================

    public Student() {
    }

    /**
     * تسجيل سريع: يُنشئ طالباً ببيانات الحد الأدنى.
     * باقي الحقول تُكمَل لاحقاً من صفحة البروفايل.
     */
    public Student(String name, String email, String password) {
        this.name = name;
        this.email = email;
        this.password = password;
    }

    // ========================== منطق الصورة ==========================

    /** يتحقق إن كانت السلسلة بصيغة UUID (معرّف ملف) وليست رابط HTTP */
    private static boolean isUuid(String s) {
        return s != null && s.length() >= 32 && s.length() <= 40
                && !s.startsWith("http://") && !s.startsWith("https://");
    }

    /**
     * رابط عرض الصورة — يحوّل fileId إلى URL كامل قابل للتحميل.
     *
     * @return رابط الصورة أو سلسلة فارغة إن لم تتوفر
     */
    public String getImageUrl() {
        if (imageUrl == null || imageUrl.isEmpty()) return "";
        if (isUuid(imageUrl)) {
            String url = com.example.volunteerapp.Hellper.DALAppWriteConnection.publicStorageDownloadUrl(imageUrl);
            return url != null ? url : "";
        }
        return imageUrl;
    }

    /** يتحقق هل توجد صورة محفوظة (fileId أو URL) */
    public boolean hasImage() {
        return imageUrl != null && !imageUrl.isEmpty();
    }

    // ========================== Getters & Setters ==========================

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public String getIdNumber() { return idNumber; }
    public void setIdNumber(String idNumber) { this.idNumber = idNumber; }

    public String getSchoolAddress() { return schoolAddress; }
    public void setSchoolAddress(String schoolAddress) { this.schoolAddress = schoolAddress; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getBirthDate() { return birthDate; }
    public void setBirthDate(String birthDate) { this.birthDate = birthDate; }

    public int getRequiredHours() { return requiredHours; }
    public void setRequiredHours(int requiredHours) { this.requiredHours = requiredHours; }

    public int getCompletedHours() { return completedHours; }
    public void setCompletedHours(int completedHours) { this.completedHours = completedHours; }

    @Override
    public String toString() {
        return "Student{id='" + id + "', name='" + name + "', email='" + email + "'}";
    }
}
