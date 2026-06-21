package com.example.volunteerapp.Hellper;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.concurrent.TimeUnit;

/**
 * حفظ جلسة تسجيل الدخول محلياً (SharedPreferences) مع انتهاء صلاحية بعد فترة محددة.
 * المطلوب: بقاء الجلسة يوماً على الأقل — المدة الافتراضية يوم واحد (قابلة للتمديد).
 */
public final class AuthSessionStore {

    private static final String PREF = "volunteer_auth_session";
    private static final String K_MODE = "mode";
    private static final String K_USER_ID = "user_id";
    private static final String K_IDENTIFIER = "identifier";
    private static final String K_SAVED_AT = "saved_at_ms";

    private static final String MODE_STUDENT = "student";
    private static final String MODE_ORG = "org";

    /** مدة صلاحية الجلسة — يوم واحد على الأقل؛ يمكن زيادة المدة بتغيير هذا الثابت */
    public static final long SESSION_DURATION_MS = TimeUnit.DAYS.toMillis(1);

    private AuthSessionStore() {
    }

    public static void saveStudentSession(Context context, String studentId, String phone) {
        Context app = context.getApplicationContext();
        app.getSharedPreferences(PREF, Context.MODE_PRIVATE).edit()
                .putString(K_MODE, MODE_STUDENT)
                .putString(K_USER_ID, studentId)
                .putString(K_IDENTIFIER, phone != null ? phone : "")
                .putLong(K_SAVED_AT, System.currentTimeMillis())
                .apply();
    }

    public static void saveOrgSession(Context context, String orgId, String email) {
        Context app = context.getApplicationContext();
        app.getSharedPreferences(PREF, Context.MODE_PRIVATE).edit()
                .putString(K_MODE, MODE_ORG)
                .putString(K_USER_ID, orgId)
                .putString(K_IDENTIFIER, email != null ? email : "")
                .putLong(K_SAVED_AT, System.currentTimeMillis())
                .apply();
    }

    public static void clear(Context context) {
        context.getApplicationContext().getSharedPreferences(PREF, Context.MODE_PRIVATE).edit().clear().apply();
    }

    public static final class Snapshot {
        public final boolean valid;
        public final boolean student;
        public final String userId;
        public final String identifier;

        private Snapshot(boolean valid, boolean student, String userId, String identifier) {
            this.valid = valid;
            this.student = student;
            this.userId = userId;
            this.identifier = identifier;
        }

        static Snapshot invalid() {
            return new Snapshot(false, true, null, null);
        }
    }

    public static Snapshot read(Context context) {
        SharedPreferences p = context.getApplicationContext().getSharedPreferences(PREF, Context.MODE_PRIVATE);
        String mode = p.getString(K_MODE, null);
        String userId = p.getString(K_USER_ID, null);
        long savedAt = p.getLong(K_SAVED_AT, 0L);
        if (mode == null || userId == null || userId.isEmpty() || savedAt <= 0L) {
            return Snapshot.invalid();
        }
        long elapsed = System.currentTimeMillis() - savedAt;
        if (elapsed > SESSION_DURATION_MS) {
            clear(context);
            return Snapshot.invalid();
        }
        String identifier = p.getString(K_IDENTIFIER, "");
        if (MODE_STUDENT.equals(mode)) {
            return new Snapshot(true, true, userId, identifier);
        }
        if (MODE_ORG.equals(mode)) {
            return new Snapshot(true, false, userId, identifier);
        }
        return Snapshot.invalid();
    }

    /** مزامنة معرف الجلسة في DAL عند إنشاء اتصال جديد */
    public static void applyToDal(Context context, DALAppWriteConnection dal) {
        if (dal == null) return;
        Snapshot s = read(context);
        if (!s.valid) return;
        dal.setCurrentUserId(s.userId);
        dal.setCurrentUserEmail(s.identifier != null ? s.identifier : "");
    }
}
