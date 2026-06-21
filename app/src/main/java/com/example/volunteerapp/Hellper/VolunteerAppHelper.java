package com.example.volunteerapp.Hellper;

import android.content.Context;
import android.util.Log;

import com.example.volunteerapp.model.Organization;
import com.example.volunteerapp.model.ReportRequest;
import com.example.volunteerapp.model.Student;
import com.example.volunteerapp.model.VolunteerApplication;
import com.example.volunteerapp.model.VolunteerHour;
import com.example.volunteerapp.model.VolunteerOpportunity;

import java.util.ArrayList;
import java.util.UUID;

/**
 * طبقة منطق الأعمال (Business Logic Layer).
 * <p>
 * تغلّف جميع عمليات CRUD مع قاعدة بيانات Appwrite عبر {@link DALAppWriteConnection}.
 * كل عملية تُرجع {@link DALAppWriteConnection.OperationResult} يحتوي على نتيجة النجاح/الفشل والبيانات.
 * </p>
 * <h3>الجداول المُدارة:</h3>
 * <ul>
 *   <li>{@code students} — بيانات الطلاب</li>
 *   <li>{@code organizations} — بيانات المؤسسات</li>
 *   <li>{@code volunteer_applications} — طلبات الانضمام</li>
 *   <li>{@code volunteer_hours} — سجلات ساعات التطوع</li>
 *   <li>{@code volunteer_opportunities} — فرص التطوع</li>
 *   <li>{@code report_requests} — طلبات تقارير PDF</li>
 * </ul>
 */
public class VolunteerAppHelper {

    private static final String TAG = "VolunteerAppHelper";

    // أسماء المجموعات (Collections) في Appwrite
    public static final String TABLE_ORGANIZATIONS   = "organizations";
    public static final String TABLE_STUDENTS         = "students";
    public static final String TABLE_APPLICATIONS     = "volunteer_applications";
    public static final String TABLE_HOURS            = "volunteer_hours";
    public static final String TABLE_OPPORTUNITIES    = "volunteer_opportunities";
    public static final String TABLE_REPORT_REQUESTS  = "report_requests";

    private final DALAppWriteConnection dal;
    private final Context context;

    public VolunteerAppHelper(Context context) {
        this.context = context;
        this.dal = new DALAppWriteConnection(context);
        AuthSessionStore.applyToDal(context, dal);
    }

    /** الحصول على كائن DAL للعمليات المباشرة (مثل رفع الملفات) */
    public DALAppWriteConnection getDal() {
        return dal;
    }

    // ╔══════════════════════════════════════════════════════════════╗
    // ║                    تسجيل الدخول                              ║
    // ╚══════════════════════════════════════════════════════════════╝

    /** تسجيل دخول المؤسسة بالبريد الإلكتروني وكلمة السر */
    public DALAppWriteConnection.OperationResult<Organization> loginOrganization(String email, String password) {
        DALAppWriteConnection.OperationResult<ArrayList<Organization>> result =
                dal.getData(TABLE_ORGANIZATIONS, null, Organization.class);
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

    /** تسجيل دخول الطالب بالبريد الإلكتروني وكلمة السر */
    public DALAppWriteConnection.OperationResult<Student> loginStudent(String email, String password) {
        DALAppWriteConnection.OperationResult<ArrayList<Student>> result =
                dal.getData(TABLE_STUDENTS, null, Student.class);
        if (!result.success || result.data == null) {
            return new DALAppWriteConnection.OperationResult<>(false, "فشل جلب بيانات الطلاب");
        }
        for (Student s : result.data) {
            if (email.equals(s.getEmail()) && password.equals(s.getPassword())) {
                dal.setCurrentUserId(s.getId());
                dal.setCurrentUserEmail(email);
                return new DALAppWriteConnection.OperationResult<>(true, "تم تسجيل الدخول بنجاح", s);
            }
        }
        return new DALAppWriteConnection.OperationResult<>(false, "البريد الإلكتروني أو كلمة السر غير صحيحة");
    }

    // ╔══════════════════════════════════════════════════════════════╗
    // ║                    إنشاء الحسابات                            ║
    // ╚══════════════════════════════════════════════════════════════╝

    /** تسجيل مؤسسة جديدة */
    public DALAppWriteConnection.OperationResult<Organization> registerOrganization(Organization org, String password) {
        org.setId(UUID.randomUUID().toString());
        org.setPassword(password);
        DALAppWriteConnection.OperationResult<ArrayList<Organization>> saveResult =
                dal.saveData(org, TABLE_ORGANIZATIONS, null);
        if (saveResult.success && saveResult.data != null && !saveResult.data.isEmpty()) {
            return new DALAppWriteConnection.OperationResult<>(true, "تم إنشاء المؤسسة بنجاح", saveResult.data.get(0));
        }
        return new DALAppWriteConnection.OperationResult<>(false,
                saveResult.message != null ? saveResult.message : "فشل حفظ بيانات المؤسسة");
    }

    /** تسجيل طالب جديد (بيانات الحد الأدنى — يُكمل البروفايل لاحقاً) */
    public DALAppWriteConnection.OperationResult<Student> registerStudent(Student student) {
        Log.d(TAG, "registerStudent: " + student.getEmail());
        DALAppWriteConnection.OperationResult<ArrayList<Student>> result =
                dal.saveData(student, TABLE_STUDENTS, null);
        if (result.success && result.data != null && !result.data.isEmpty()) {
            return new DALAppWriteConnection.OperationResult<>(true, "تم إنشاء الحساب بنجاح", result.data.get(0));
        }
        return new DALAppWriteConnection.OperationResult<>(false,
                result.message != null ? result.message : "فشل التسجيل");
    }

    // ╔══════════════════════════════════════════════════════════════╗
    // ║                    إدارة المؤسسات                            ║
    // ╚══════════════════════════════════════════════════════════════╝

    /** جلب جميع المؤسسات */
    public DALAppWriteConnection.OperationResult<ArrayList<Organization>> getOrganizations() {
        return dal.getData(TABLE_ORGANIZATIONS, null, Organization.class);
    }

    /** جلب مؤسسة بمعرّفها */
    public DALAppWriteConnection.OperationResult<Organization> getOrganizationById(String id) {
        return dal.getDataById(TABLE_ORGANIZATIONS, id, null, Organization.class);
    }

    /** تحديث بيانات مؤسسة */
    public DALAppWriteConnection.OperationResult<Organization> updateOrganization(Organization org) {
        if (org == null || org.getId() == null || org.getId().isEmpty()) {
            return new DALAppWriteConnection.OperationResult<>(false, "معرف المؤسسة غير صالح");
        }
        DALAppWriteConnection.OperationResult<Organization> updateResult =
                dal.updateData(org, TABLE_ORGANIZATIONS, org.getId(), null);
        if (updateResult.success) {
            return new DALAppWriteConnection.OperationResult<>(true, "تم حفظ التعديلات", org);
        }
        return new DALAppWriteConnection.OperationResult<>(false,
                updateResult.message != null ? updateResult.message : "فشل حفظ البيانات");
    }

    // ╔══════════════════════════════════════════════════════════════╗
    // ║                    إدارة الطلاب                              ║
    // ╚══════════════════════════════════════════════════════════════╝

    /** جلب طالب بمعرّفه */
    public DALAppWriteConnection.OperationResult<Student> getStudentById(String id) {
        return dal.getDataById(TABLE_STUDENTS, id, null, Student.class);
    }

    /** تحديث بيانات طالب (البروفايل) */
    public DALAppWriteConnection.OperationResult<Student> updateStudent(Student student) {
        if (student == null || student.getId() == null || student.getId().isEmpty()) {
            return new DALAppWriteConnection.OperationResult<>(false, "معرف الطالب غير صالح");
        }
        DALAppWriteConnection.OperationResult<Student> updateResult =
                dal.updateData(student, TABLE_STUDENTS, student.getId(), null);
        if (updateResult.success) {
            return new DALAppWriteConnection.OperationResult<>(true, "تم حفظ التعديلات", student);
        }
        return new DALAppWriteConnection.OperationResult<>(false,
                updateResult.message != null ? updateResult.message : "فشل حفظ البيانات");
    }

    // ╔══════════════════════════════════════════════════════════════╗
    // ║                    فرص التطوع                                ║
    // ╚══════════════════════════════════════════════════════════════╝

    /** إنشاء فرصة تطوع جديدة */
    public DALAppWriteConnection.OperationResult<VolunteerOpportunity> createOpportunity(VolunteerOpportunity opp) {
        opp.setId(UUID.randomUUID().toString());
        DALAppWriteConnection.OperationResult<ArrayList<VolunteerOpportunity>> result =
                dal.saveData(opp, TABLE_OPPORTUNITIES, null);
        if (result.success && result.data != null && !result.data.isEmpty()) {
            return new DALAppWriteConnection.OperationResult<>(true, "تم إنشاء الفرصة بنجاح", result.data.get(0));
        }
        return new DALAppWriteConnection.OperationResult<>(false,
                result.message != null ? result.message : "فشل إنشاء الفرصة");
    }

    /** جلب جميع الفرص */
    public DALAppWriteConnection.OperationResult<ArrayList<VolunteerOpportunity>> getOpportunities() {
        return dal.getData(TABLE_OPPORTUNITIES, null, VolunteerOpportunity.class);
    }

    /** جلب الفرص النشطة فقط (للعرض للطلاب) */
    public DALAppWriteConnection.OperationResult<ArrayList<VolunteerOpportunity>> getActiveOpportunities() {
        DALAppWriteConnection.OperationResult<ArrayList<VolunteerOpportunity>> result =
                dal.getData(TABLE_OPPORTUNITIES, null, VolunteerOpportunity.class);
        if (!result.success || result.data == null) return result;
        ArrayList<VolunteerOpportunity> filtered = new ArrayList<>();
        for (VolunteerOpportunity o : result.data) {
            if (VolunteerOpportunity.STATUS_ACTIVE.equals(o.getStatus())) {
                filtered.add(o);
            }
        }
        return new DALAppWriteConnection.OperationResult<>(true, "تم جلب الفرص", filtered);
    }

    /** جلب فرص مؤسسة محددة */
    public DALAppWriteConnection.OperationResult<ArrayList<VolunteerOpportunity>> getOrganizationOpportunities(String orgId) {
        DALAppWriteConnection.OperationResult<ArrayList<VolunteerOpportunity>> result =
                dal.getData(TABLE_OPPORTUNITIES, null, VolunteerOpportunity.class);
        if (!result.success || result.data == null) return result;
        ArrayList<VolunteerOpportunity> filtered = new ArrayList<>();
        for (VolunteerOpportunity o : result.data) {
            if (orgId.equals(o.getOrganizationId())) {
                filtered.add(o);
            }
        }
        return new DALAppWriteConnection.OperationResult<>(true, "تم جلب الفرص", filtered);
    }

    /** جلب فرصة بمعرّفها */
    public DALAppWriteConnection.OperationResult<VolunteerOpportunity> getOpportunityById(String id) {
        return dal.getDataById(TABLE_OPPORTUNITIES, id, null, VolunteerOpportunity.class);
    }

    /** تحديث بيانات فرصة تطوع */
    public DALAppWriteConnection.OperationResult<VolunteerOpportunity> updateOpportunity(VolunteerOpportunity opp) {
        if (opp == null || opp.getId() == null || opp.getId().isEmpty()) {
            return new DALAppWriteConnection.OperationResult<>(false, "معرف الفرصة غير صالح");
        }
        DALAppWriteConnection.OperationResult<VolunteerOpportunity> updateResult =
                dal.updateData(opp, TABLE_OPPORTUNITIES, opp.getId(), null);
        if (updateResult.success) {
            return new DALAppWriteConnection.OperationResult<>(true, "تم التحديث", opp);
        }
        return new DALAppWriteConnection.OperationResult<>(false,
                updateResult.message != null ? updateResult.message : "فشل التحديث");
    }

    // ╔══════════════════════════════════════════════════════════════╗
    // ║                    طلبات الانضمام                            ║
    // ╚══════════════════════════════════════════════════════════════╝

    /**
     * تقديم طلب انضمام لفرصة تطوع محددة.
     *
     * @param studentId      معرّف الطالب المتقدم
     * @param organizationId معرّف المؤسسة
     * @param opportunityId  معرّف فرصة التطوع (يمكن أن يكون null للتقديم المباشر)
     */
    public DALAppWriteConnection.OperationResult<VolunteerApplication> applyToOpportunity(
            String studentId, String organizationId, String opportunityId) {
        VolunteerApplication app = new VolunteerApplication(studentId, organizationId, opportunityId);
        app.setId(UUID.randomUUID().toString());
        DALAppWriteConnection.OperationResult<ArrayList<VolunteerApplication>> result =
                dal.saveData(app, TABLE_APPLICATIONS, null);
        if (result.success && result.data != null && !result.data.isEmpty()) {
            return new DALAppWriteConnection.OperationResult<>(true, "تم إرسال الطلب بنجاح", result.data.get(0));
        }
        return new DALAppWriteConnection.OperationResult<>(false,
                result.message != null ? result.message : "فشل إرسال الطلب");
    }

    /** جلب طلبات طالب محدد (جميع الحالات) */
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

    /** جلب طلبات الانضمام المعلّقة لمؤسسة محددة */
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

    /**
     * تحديث حالة طلب انضمام (قبول/رفض).
     *
     * @param appId        معرّف الطلب
     * @param status       الحالة الجديدة ({@code accepted} أو {@code rejected})
     * @param rejectReason سبب الرفض (null عند القبول)
     */
    public DALAppWriteConnection.OperationResult<Void> updateApplicationStatus(
            String appId, String status, String rejectReason) {
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

    // ╔══════════════════════════════════════════════════════════════╗
    // ║                    ساعات التطوع                              ║
    // ╚══════════════════════════════════════════════════════════════╝

    /** تسجيل ساعات تطوع جديدة (بحالة «قيد الانتظار») */
    public DALAppWriteConnection.OperationResult<VolunteerHour> registerVolunteerHours(
            String studentId, String orgId, int hours, String description) {
        VolunteerHour vh = new VolunteerHour(studentId, orgId, hours, description);
        vh.setId(UUID.randomUUID().toString());
        DALAppWriteConnection.OperationResult<ArrayList<VolunteerHour>> result =
                dal.saveData(vh, TABLE_HOURS, null);
        if (result.success && result.data != null && !result.data.isEmpty()) {
            return new DALAppWriteConnection.OperationResult<>(true, "تم تسجيل الساعات بنجاح", result.data.get(0));
        }
        return new DALAppWriteConnection.OperationResult<>(false,
                result.message != null ? result.message : "فشل التسجيل");
    }

    /** جلب جميع ساعات طالب محدد */
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

    /** جلب ساعات التطوع المعلّقة لمؤسسة محددة (لمراجعتها) */
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

    /** جلب جميع ساعات مؤسسة (كل الحالات — للتقارير) */
    public DALAppWriteConnection.OperationResult<ArrayList<VolunteerHour>> getAllOrganizationHours(String orgId) {
        DALAppWriteConnection.OperationResult<ArrayList<VolunteerHour>> result =
                dal.getData(TABLE_HOURS, null, VolunteerHour.class);
        if (!result.success || result.data == null) return result;
        ArrayList<VolunteerHour> filtered = new ArrayList<>();
        for (VolunteerHour h : result.data) {
            if (orgId.equals(h.getOrganizationId())) filtered.add(h);
        }
        return new DALAppWriteConnection.OperationResult<>(true, "تم جلب الساعات", filtered);
    }

    /**
     * تحديث حالة ساعة تطوع (قبول/رفض).
     * عند القبول يُحدّث أيضاً عدد الساعات المكتملة للطالب.
     *
     * @param hourId       معرّف سجل الساعات
     * @param status       الحالة الجديدة
     * @param rejectReason سبب الرفض (null عند القبول)
     */
    public DALAppWriteConnection.OperationResult<Void> updateHourStatus(
            String hourId, String status, String rejectReason) {
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

        // عند القبول: تحديث مجموع ساعات الطالب المكتملة
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

    // ╔══════════════════════════════════════════════════════════════╗
    // ║                    رفع الملفات                               ║
    // ╚══════════════════════════════════════════════════════════════╝

    /** رفع صورة إلى Appwrite Storage */
    public DALAppWriteConnection.OperationResult<DALAppWriteConnection.FileInfo> uploadImage(
            byte[] imageData, String fileName) {
        return dal.uploadFile(imageData, fileName, "image/jpeg", null);
    }

    /** حذف ملف من Appwrite Storage */
    public DALAppWriteConnection.OperationResult<Void> deleteStorageFile(String fileId) {
        return dal.deleteFile(fileId, null);
    }

    // ╔══════════════════════════════════════════════════════════════╗
    // ║                    طلبات التقارير (PDF)                       ║
    // ╚══════════════════════════════════════════════════════════════╝

    /** إرسال طلب تقرير من طالب لمؤسسة محددة */
    public DALAppWriteConnection.OperationResult<ReportRequest> requestReport(String studentId, String orgId) {
        ReportRequest rr = new ReportRequest(studentId, orgId);
        rr.setId(UUID.randomUUID().toString());
        DALAppWriteConnection.OperationResult<ArrayList<ReportRequest>> result =
                dal.saveData(rr, TABLE_REPORT_REQUESTS, null);
        if (result.success && result.data != null && !result.data.isEmpty()) {
            return new DALAppWriteConnection.OperationResult<>(true, "تم إرسال طلب التقرير", result.data.get(0));
        }
        return new DALAppWriteConnection.OperationResult<>(false,
                result.message != null ? result.message : "فشل إرسال الطلب");
    }

    /** جلب طلبات التقارير المعلّقة لمؤسسة محددة */
    public DALAppWriteConnection.OperationResult<ArrayList<ReportRequest>> getOrgReportRequests(String orgId) {
        DALAppWriteConnection.OperationResult<ArrayList<ReportRequest>> result =
                dal.getData(TABLE_REPORT_REQUESTS, null, ReportRequest.class);
        if (!result.success || result.data == null) return result;
        ArrayList<ReportRequest> filtered = new ArrayList<>();
        for (ReportRequest rr : result.data) {
            if (orgId.equals(rr.getOrganizationId()) && ReportRequest.STATUS_PENDING.equals(rr.getStatus())) {
                filtered.add(rr);
            }
        }
        return new DALAppWriteConnection.OperationResult<>(true, "تم جلب الطلبات", filtered);
    }

    /** جلب جميع طلبات التقارير لطالب محدد (كل الحالات) */
    public DALAppWriteConnection.OperationResult<ArrayList<ReportRequest>> getStudentReportRequests(String studentId) {
        DALAppWriteConnection.OperationResult<ArrayList<ReportRequest>> result =
                dal.getData(TABLE_REPORT_REQUESTS, null, ReportRequest.class);
        if (!result.success || result.data == null) return result;
        ArrayList<ReportRequest> filtered = new ArrayList<>();
        for (ReportRequest rr : result.data) {
            if (studentId.equals(rr.getStudentId())) {
                filtered.add(rr);
            }
        }
        return new DALAppWriteConnection.OperationResult<>(true, "تم جلب الطلبات", filtered);
    }

    /** الموافقة على طلب تقرير (تغيير الحالة إلى approved) */
    public DALAppWriteConnection.OperationResult<Void> approveReportRequest(String requestId) {
        DALAppWriteConnection.OperationResult<ReportRequest> getResult =
                dal.getDataById(TABLE_REPORT_REQUESTS, requestId, null, ReportRequest.class);
        if (!getResult.success || getResult.data == null) {
            return new DALAppWriteConnection.OperationResult<>(false, "لم يتم العثور على الطلب");
        }
        ReportRequest rr = getResult.data;
        rr.setStatus(ReportRequest.STATUS_APPROVED);
        DALAppWriteConnection.OperationResult<ReportRequest> updateResult =
                dal.updateData(rr, TABLE_REPORT_REQUESTS, requestId, null);
        return new DALAppWriteConnection.OperationResult<>(updateResult.success, updateResult.message);
    }

    /** جلب الساعات المقبولة لطالب في مؤسسة محددة (لإنشاء تقرير PDF) */
    public DALAppWriteConnection.OperationResult<ArrayList<VolunteerHour>> getAcceptedHoursForStudentAtOrg(
            String studentId, String orgId) {
        DALAppWriteConnection.OperationResult<ArrayList<VolunteerHour>> result =
                dal.getData(TABLE_HOURS, null, VolunteerHour.class);
        if (!result.success || result.data == null) return result;
        ArrayList<VolunteerHour> filtered = new ArrayList<>();
        for (VolunteerHour h : result.data) {
            if (studentId.equals(h.getStudentId()) && orgId.equals(h.getOrganizationId())
                    && VolunteerHour.STATUS_ACCEPTED.equals(h.getStatus())) {
                filtered.add(h);
            }
        }
        return new DALAppWriteConnection.OperationResult<>(true, "تم جلب الساعات", filtered);
    }
}
