package com.example.mohtadyapp.Hellper;

import android.content.Context;

import com.example.mohtadyapp.model.Organization;
import com.example.mohtadyapp.model.Student;
import com.example.mohtadyapp.model.VolunteerApplication;
import com.example.mohtadyapp.model.VolunteerHour;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * مساعد تطبيق التطوع - يوفر دوال مخصصة للتعامل مع بيانات التطوع
 * يستخدم DALAppWriteConnection للاتصال بـ Appwrite
 */
public class VolunteerAppHelper {
    public static final String TABLE_ORGANIZATIONS = "organizations";
    public static final String TABLE_STUDENTS = "students";
    public static final String TABLE_APPLICATIONS = "volunteer_applications";
    public static final String TABLE_HOURS = "volunteer_hours";

    private final DALAppWriteConnection dal;
    private final Context context;

    public VolunteerAppHelper(Context context) {
        this.context = context;
        this.dal = new DALAppWriteConnection(context);
    }

    public DALAppWriteConnection getDal() {
        return dal;
    }

    // === تسجيل دخول المؤسسة (بالبريد وكلمة السر - من جدول المؤسسات بدون مصادقة Appwrite) ===
    public DALAppWriteConnection.OperationResult<Organization> loginOrganization(String email, String password) {
        DALAppWriteConnection.OperationResult<ArrayList<Organization>> result = dal.getData(TABLE_ORGANIZATIONS, null, Organization.class);
        if (!result.success || result.data == null) {
            return new DALAppWriteConnection.OperationResult<>(false, "فشل جلب بيانات المؤسسات");
        }
        for (Organization org : result.data) {
            if (email.equals(org.getEmail()) && password.equals(org.getPassword())) {
                dal.setCurrentUserId(org.getId());
                dal.setCurrentUserEmail(org.getEmail());
                return new DALAppWriteConnection.OperationResult<>(true, "تم تسجيل الدخول بنجاح", org);
            }
        }
        return new DALAppWriteConnection.OperationResult<>(false, "البريد الإلكتروني أو كلمة السر غير صحيحة");
    }

    // === تسجيل دخول الطالب (برقم الهاتف - مخصص) ===
    public DALAppWriteConnection.OperationResult<Student> loginStudent(String phone, String password) {
        DALAppWriteConnection.OperationResult<ArrayList<Student>> result = dal.getData(TABLE_STUDENTS, null, Student.class);
        if (!result.success || result.data == null) {
            return new DALAppWriteConnection.OperationResult<>(false, "فشل جلب بيانات الطلاب");
        }
        for (Student s : result.data) {
            if (phone.equals(s.getPhone()) && password.equals(s.getPassword())) {
                dal.setCurrentUserId(s.getId());
                dal.setCurrentUserEmail(phone);
                return new DALAppWriteConnection.OperationResult<>(true, "تم تسجيل الدخول بنجاح", s);
            }
        }
        return new DALAppWriteConnection.OperationResult<>(false, "رقم الهاتف أو كلمة السر غير صحيحة");
    }

    // === إنشاء مؤسسة (حفظ البيانات فقط بدون مصادقة Appwrite) ===
    public DALAppWriteConnection.OperationResult<Organization> registerOrganization(Organization org, String password) {
        org.setId(UUID.randomUUID().toString());
        org.setPassword(password);
        DALAppWriteConnection.OperationResult<ArrayList<Organization>> saveResult =
                dal.saveData(org, TABLE_ORGANIZATIONS, null);
        if (saveResult.success && saveResult.data != null && !saveResult.data.isEmpty()) {
            return new DALAppWriteConnection.OperationResult<>(true, "تم إنشاء المؤسسة بنجاح", saveResult.data.get(0));
        }
        return new DALAppWriteConnection.OperationResult<>(false, saveResult.message != null ? saveResult.message : "فشل حفظ بيانات المؤسسة");
    }

    // === إنشاء طالب ===
    public DALAppWriteConnection.OperationResult<Student> registerStudent(Student student) {
        DALAppWriteConnection.OperationResult<ArrayList<Student>> result = dal.saveData(student, TABLE_STUDENTS, null);
        if (result.success && result.data != null && !result.data.isEmpty()) {
            return new DALAppWriteConnection.OperationResult<>(true, "تم إنشاء الحساب بنجاح", result.data.get(0));
        }
        return new DALAppWriteConnection.OperationResult<>(false, result.message != null ? result.message : "فشل التسجيل");
    }

    // === جلب جميع المؤسسات ===
    public DALAppWriteConnection.OperationResult<ArrayList<Organization>> getOrganizations() {
        return dal.getData(TABLE_ORGANIZATIONS, null, Organization.class);
    }

    // === جلب مؤسسة بالمعرف ===
    public DALAppWriteConnection.OperationResult<Organization> getOrganizationById(String id) {
        return dal.getDataById(TABLE_ORGANIZATIONS, id, null, Organization.class);
    }

    // === جلب طالب بالمعرف ===
    public DALAppWriteConnection.OperationResult<Student> getStudentById(String id) {
        return dal.getDataById(TABLE_STUDENTS, id, null, Student.class);
    }

    // === تقديم طلب انضمام ===
    public DALAppWriteConnection.OperationResult<VolunteerApplication> applyToOrganization(String studentId, String organizationId) {
        VolunteerApplication app = new VolunteerApplication(studentId, organizationId);
        DALAppWriteConnection.OperationResult<ArrayList<VolunteerApplication>> result =
                dal.saveData(app, TABLE_APPLICATIONS, null);
        if (result.success && result.data != null && !result.data.isEmpty()) {
            return new DALAppWriteConnection.OperationResult<>(true, "تم إرسال الطلب بنجاح", result.data.get(0));
        }
        return new DALAppWriteConnection.OperationResult<>(false, result.message != null ? result.message : "فشل إرسال الطلب");
    }

    // === جلب طلبات الطالب ===
    public DALAppWriteConnection.OperationResult<ArrayList<VolunteerApplication>> getStudentApplications(String studentId) {
        DALAppWriteConnection.OperationResult<ArrayList<VolunteerApplication>> result =
                dal.getData(TABLE_APPLICATIONS, null, VolunteerApplication.class);
        if (!result.success || result.data == null) return result;
        ArrayList<VolunteerApplication> filtered = new ArrayList<>();
        for (VolunteerApplication a : result.data) {
            if (studentId.equals(a.getStudentId())) filtered.add(a);
        }
        return new DALAppWriteConnection.OperationResult<>(true, "تم جلب الطلبات", filtered);
    }

    // === جلب طلبات المؤسسة (pending) ===
    public DALAppWriteConnection.OperationResult<ArrayList<VolunteerApplication>> getOrganizationApplications(String orgId) {
        DALAppWriteConnection.OperationResult<ArrayList<VolunteerApplication>> result =
                dal.getData(TABLE_APPLICATIONS, null, VolunteerApplication.class);
        if (!result.success || result.data == null) return result;
        ArrayList<VolunteerApplication> filtered = new ArrayList<>();
        for (VolunteerApplication a : result.data) {
            if (orgId.equals(a.getOrganizationId()) && VolunteerApplication.STATUS_PENDING.equals(a.getStatus())) {
                filtered.add(a);
            }
        }
        return new DALAppWriteConnection.OperationResult<>(true, "تم جلب الطلبات", filtered);
    }

    // === قبول/رفض طلب ===
    public DALAppWriteConnection.OperationResult<Void> updateApplicationStatus(String appId, String status, String rejectReason) {
        DALAppWriteConnection.OperationResult<VolunteerApplication> getResult =
                dal.getDataById(TABLE_APPLICATIONS, appId, null, VolunteerApplication.class);
        if (!getResult.success || getResult.data == null) {
            return new DALAppWriteConnection.OperationResult<>(false, "لم يتم العثور على الطلب");
        }
        VolunteerApplication app = getResult.data;
        app.setStatus(status);
        app.setRejectReason(rejectReason);
        DALAppWriteConnection.OperationResult<VolunteerApplication> updateResult =
                dal.updateData(app, TABLE_APPLICATIONS, appId, null);
        return new DALAppWriteConnection.OperationResult<>(updateResult.success, updateResult.message);
    }

    // === تسجيل ساعات تطوع ===
    public DALAppWriteConnection.OperationResult<VolunteerHour> registerVolunteerHours(String studentId, String orgId, int hours, String description) {
        VolunteerHour vh = new VolunteerHour(studentId, orgId, hours, description);
        DALAppWriteConnection.OperationResult<ArrayList<VolunteerHour>> result =
                dal.saveData(vh, TABLE_HOURS, null);
        if (result.success && result.data != null && !result.data.isEmpty()) {
            return new DALAppWriteConnection.OperationResult<>(true, "تم تسجيل الساعات بنجاح", result.data.get(0));
        }
        return new DALAppWriteConnection.OperationResult<>(false, result.message != null ? result.message : "فشل التسجيل");
    }

    // === جلب ساعات الطالب ===
    public DALAppWriteConnection.OperationResult<ArrayList<VolunteerHour>> getStudentHours(String studentId) {
        DALAppWriteConnection.OperationResult<ArrayList<VolunteerHour>> result =
                dal.getData(TABLE_HOURS, null, VolunteerHour.class);
        if (!result.success || result.data == null) return result;
        ArrayList<VolunteerHour> filtered = new ArrayList<>();
        for (VolunteerHour h : result.data) {
            if (studentId.equals(h.getStudentId())) filtered.add(h);
        }
        return new DALAppWriteConnection.OperationResult<>(true, "تم جلب الساعات", filtered);
    }

    // === جلب ساعات المؤسسة (للمراجعة) ===
    public DALAppWriteConnection.OperationResult<ArrayList<VolunteerHour>> getOrganizationHours(String orgId) {
        DALAppWriteConnection.OperationResult<ArrayList<VolunteerHour>> result =
                dal.getData(TABLE_HOURS, null, VolunteerHour.class);
        if (!result.success || result.data == null) return result;
        ArrayList<VolunteerHour> filtered = new ArrayList<>();
        for (VolunteerHour h : result.data) {
            if (orgId.equals(h.getOrganizationId()) && VolunteerHour.STATUS_PENDING.equals(h.getStatus())) {
                filtered.add(h);
            }
        }
        return new DALAppWriteConnection.OperationResult<>(true, "تم جلب الطلبات", filtered);
    }

    // === قبول/رفض ساعات ===
    public DALAppWriteConnection.OperationResult<Void> updateHourStatus(String hourId, String status, String rejectReason) {
        DALAppWriteConnection.OperationResult<VolunteerHour> getResult =
                dal.getDataById(TABLE_HOURS, hourId, null, VolunteerHour.class);
        if (!getResult.success || getResult.data == null) {
            return new DALAppWriteConnection.OperationResult<>(false, "لم يتم العثور على الساعات");
        }
        VolunteerHour h = getResult.data;
        h.setStatus(status);
        h.setRejectReason(rejectReason);
        DALAppWriteConnection.OperationResult<VolunteerHour> updateResult =
                dal.updateData(h, TABLE_HOURS, hourId, null);
        if (updateResult.success && VolunteerHour.STATUS_ACCEPTED.equals(status)) {
            DALAppWriteConnection.OperationResult<Student> studentResult = getStudentById(h.getStudentId());
            if (studentResult.success && studentResult.data != null) {
                Student st = studentResult.data;
                st.setCompletedHours(st.getCompletedHours() + h.getHours());
                dal.updateData(st, TABLE_STUDENTS, st.getId(), null);
            }
        }
        return new DALAppWriteConnection.OperationResult<>(updateResult.success, updateResult.message);
    }

    // === رفع صورة ===
    public DALAppWriteConnection.OperationResult<DALAppWriteConnection.FileInfo> uploadImage(byte[] imageData, String fileName) {
        return dal.uploadFile(imageData, fileName, "image/jpeg", null);
    }
}
