package com.example.volunteerapp.model;

import android.util.Base64;

import com.example.volunteerapp.Hellper.DALAppWriteConnection;
import com.google.gson.annotations.SerializedName;

import org.json.JSONArray;
import org.json.JSONException;

import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * نموذج المؤسسة. صور المؤسسة الثلاث تُحفظ في {@code imageUrl} بصيغة مضغوطة:
 * {@code معرفملف} أو {@code معرف|معرف|معرف} (معرفات Appwrite UUID) لتناسب حد 255 حرفاً في Appwrite.
 * عند العرض يُبنى رابط التحميل من المعرف عبر {@link DALAppWriteConnection#publicStorageDownloadUrl(String)}.
 */
public class Organization {

    private static final String ORG_IMG_MARKER = "#org3=";
    private static final Pattern UUID_TOKEN = Pattern.compile(
            "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$");
    /** استخراج المعرف من مسار Appwrite: .../files/{uuid}/... */
    private static final Pattern FILES_SEGMENT = Pattern.compile(
            "/files/([0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12})(?:/|\\?|$)");

    @SerializedName(value = "id", alternate = {"$id"})
    private String id;
    @SerializedName("organizationId")
    private String organizationId = "";
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

    private static boolean isUuid(String s) {
        return s != null && UUID_TOKEN.matcher(s.trim()).matches();
    }

    /**
     * يستخرج معرف ملف التخزين من معرف UUID أو من رابط Appwrite.
     * يُستخدم عند حذف الملف من التخزين أو عند الضغط للحفظ في الحقل.
     */
    public static String extractStorageFileId(String s) {
        if (s == null || (s = s.trim()).isEmpty()) {
            return "";
        }
        if (isUuid(s)) {
            return s;
        }
        Matcher m = FILES_SEGMENT.matcher(s);
        if (m.find()) {
            return m.group(1);
        }
        return "";
    }

    private static String[] unpackLegacyFragmentOrJson(String t) {
        int marker = t.indexOf(ORG_IMG_MARKER);
        if (marker >= 0) {
            String b64 = t.substring(marker + ORG_IMG_MARKER.length());
            try {
                byte[] dec = Base64.decode(b64, Base64.URL_SAFE | Base64.NO_WRAP);
                JSONArray a = new JSONArray(new String(dec, StandardCharsets.UTF_8));
                return new String[]{
                        a.optString(0, ""),
                        a.optString(1, ""),
                        a.optString(2, "")
                };
            } catch (Exception ignored) {
                String before = marker > 0 ? t.substring(0, marker) : "";
                return new String[]{before, "", ""};
            }
        }
        if (t.startsWith("[")) {
            try {
                JSONArray a = new JSONArray(t);
                String[] out = new String[3];
                for (int i = 0; i < 3; i++) {
                    out[i] = a.optString(i, "");
                }
                return out;
            } catch (JSONException ignored) {
                return new String[]{t, "", ""};
            }
        }
        return null;
    }

    /** قراءة الحقل الخام: ثلاث قيم (معرف أو رابط قديم) لكل خانة */
    private static String[] unpackTokens(String raw) {
        if (raw == null || raw.trim().isEmpty()) {
            return new String[]{"", "", ""};
        }
        String t = raw.trim();
        String[] legacy = unpackLegacyFragmentOrJson(t);
        if (legacy != null) {
            return legacy;
        }
        if (t.contains("|")) {
            String[] parts = t.split("\\|", -1);
            String[] out = new String[]{"", "", ""};
            for (int i = 0; i < Math.min(3, parts.length); i++) {
                out[i] = parts[i].trim();
            }
            return out;
        }
        return new String[]{t, "", ""};
    }

    /** تخزين في الحقل: معرفات فقط، طول ≤ 255 */
    private static String packStoredIds(String raw0, String raw1, String raw2) {
        String a = extractStorageFileId(raw0);
        String b = extractStorageFileId(raw1);
        String c = extractStorageFileId(raw2);
        if (b.isEmpty() && c.isEmpty()) {
            return a;
        }
        return a + "|" + b + "|" + c;
    }

    private void setSlot(int index, String value) {
        String[] p = unpackTokens(imageUrl);
        p[index] = value == null ? "" : value.trim();
        imageUrl = packStoredIds(p[0], p[1], p[2]);
    }

    private static String resolveToDisplayUrl(String token) {
        if (token == null || token.isEmpty()) {
            return "";
        }
        String t = token.trim();
        if (isUuid(t)) {
            String url = DALAppWriteConnection.publicStorageDownloadUrl(t);
            return url != null ? url : "";
        }
        if (t.startsWith("http://") || t.startsWith("https://")) {
            return t;
        }
        return t;
    }

    private String getSlotDisplay(int index) {
        return resolveToDisplayUrl(unpackTokens(imageUrl)[index]);
    }

    public String getId() { return id; }
    public void setId(String id) {
        this.id = id;
        this.organizationId = id;
    }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    /** رابط عرض للصورة 1 (Glide) */
    public String getImageUrl() { return getSlotDisplay(0); }
    public void setImageUrl(String imageUrl) { setSlot(0, imageUrl); }

    public String getImageUrl2() { return getSlotDisplay(1); }
    public void setImageUrl2(String imageUrl2) { setSlot(1, imageUrl2); }

    public String getImageUrl3() { return getSlotDisplay(2); }
    public void setImageUrl3(String imageUrl3) { setSlot(2, imageUrl3); }

    /** أول رابط صورة غير فارغ — للعرض في القوائم */
    public String getPrimaryImageUrl() {
        for (String tok : unpackTokens(imageUrl)) {
            if (tok != null && !tok.isEmpty()) {
                String url = resolveToDisplayUrl(tok);
                if (url != null && !url.isEmpty()) {
                    return url;
                }
            }
        }
        return null;
    }

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
