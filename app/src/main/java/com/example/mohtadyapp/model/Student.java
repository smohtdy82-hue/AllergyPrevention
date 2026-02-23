package com.example.mohtadyapp.model;

import com.google.gson.annotations.SerializedName;

/**
 * نموذج الطالب - يحتوي على بيانات الطالب المتطوع
 * الدخول: رقم الهاتف وكلمة السر
 */
public class Student {
    @SerializedName(value = "id", alternate = {"$id"})
    private String id;
    @SerializedName("name")
    private String name;
    @SerializedName("imageUrl")
    private String imageUrl;
    @SerializedName("idNumber")
    private String idNumber;
    @SerializedName("schoolAddress")
    private String schoolAddress;
    @SerializedName("phone")
    private String phone;
    @SerializedName("password")
    private String password;
    @SerializedName("birthDate")
    private String birthDate;
    @SerializedName("requiredHours")
    private int requiredHours;
    @SerializedName("completedHours")
    private int completedHours;

    public Student() {
    }

    public Student(String name, String imageUrl, String idNumber, String schoolAddress,
                   String phone, String password, String birthDate, int requiredHours) {
        this.name = name;
        this.imageUrl = imageUrl;
        this.idNumber = idNumber;
        this.schoolAddress = schoolAddress;
        this.phone = phone;
        this.password = password;
        this.birthDate = birthDate;
        this.requiredHours = requiredHours;
        this.completedHours = 0;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getImageUrl() { return imageUrl; }
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
        return "Student{id='" + id + "', name='" + name + "', idNumber='" + idNumber +
                "', phone='" + phone + "', schoolAddress='" + schoolAddress +
                "', birthDate='" + birthDate + "', requiredHours=" + requiredHours +
                ", completedHours=" + completedHours + "}";
    }
}
