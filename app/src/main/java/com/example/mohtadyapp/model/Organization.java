package com.example.mohtadyapp.model;

import com.google.gson.annotations.SerializedName;

/**
 * نموذج المؤسسة - تحتوي على بيانات المؤسسة التطوعية
 * الدخول: البريد الإلكتروني وكلمة السر
 */
public class Organization {
    @SerializedName(value = "id", alternate = {"$id"})
    private String id;
    @SerializedName("name")
    private String name;
    @SerializedName("imageUrl")
    private String imageUrl;
    @SerializedName("address")
    private String address;
    @SerializedName("email")
    private String email;
    @SerializedName("password")
    private String password;
    @SerializedName("volunteerDays")
    private String volunteerDays;
    @SerializedName("volunteerHours")
    private String volunteerHours;
    @SerializedName("totalHours")
    private int totalHours;
    @SerializedName("contactPhone")
    private String contactPhone;
    @SerializedName("contactDetails")
    private String contactDetails;

    public Organization() {
    }

    public Organization(String name, String imageUrl, String address, String email, String password,
                        String volunteerDays, String volunteerHours, int totalHours,
                        String contactPhone, String contactDetails) {
        this.name = name;
        this.imageUrl = imageUrl;
        this.address = address;
        this.email = email;
        this.password = password;
        this.volunteerDays = volunteerDays;
        this.volunteerHours = volunteerHours;
        this.totalHours = totalHours;
        this.contactPhone = contactPhone;
        this.contactDetails = contactDetails;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public String getVolunteerDays() { return volunteerDays; }
    public void setVolunteerDays(String volunteerDays) { this.volunteerDays = volunteerDays; }
    public String getVolunteerHours() { return volunteerHours; }
    public void setVolunteerHours(String volunteerHours) { this.volunteerHours = volunteerHours; }
    public int getTotalHours() { return totalHours; }
    public void setTotalHours(int totalHours) { this.totalHours = totalHours; }
    public String getContactPhone() { return contactPhone; }
    public void setContactPhone(String contactPhone) { this.contactPhone = contactPhone; }
    public String getContactDetails() { return contactDetails; }
    public void setContactDetails(String contactDetails) { this.contactDetails = contactDetails; }

    @Override
    public String toString() {
        return "Organization{id='" + id + "', name='" + name + "', address='" + address +
                "', email='" + email + "', volunteerDays='" + volunteerDays +
                "', volunteerHours='" + volunteerHours + "', totalHours=" + totalHours + "}";
    }
}
