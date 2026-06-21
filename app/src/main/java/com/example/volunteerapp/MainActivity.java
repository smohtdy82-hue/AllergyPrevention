package com.example.volunteerapp;

import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.fragment.app.Fragment;

import com.example.volunteerapp.fragment.AddOpportunityFragment;
import com.example.volunteerapp.fragment.ChooseUserFragment;
import com.example.volunteerapp.fragment.OpportunitiesListFragment;
import com.example.volunteerapp.fragment.OpportunityDetailsFragment;
import com.example.volunteerapp.fragment.OrgManageFragment;
import com.example.volunteerapp.fragment.OrgLoginFragment;
import com.example.volunteerapp.fragment.OrgOpportunitiesFragment;
import com.example.volunteerapp.fragment.OrgRegisterFragment;
import com.example.volunteerapp.fragment.OrgCombinedRequestsFragment;
import com.example.volunteerapp.fragment.OrgRequestsFragment;
import com.example.volunteerapp.fragment.ReportsFragment;
import com.example.volunteerapp.fragment.StudentLoginFragment;
import com.example.volunteerapp.fragment.StudentMyOrgsFragment;
import com.example.volunteerapp.fragment.StudentProfileFragment;
import com.example.volunteerapp.fragment.StudentRegisterFragment;
import com.example.volunteerapp.fragment.StudentReportsFragment;
import com.example.volunteerapp.Hellper.AuthSessionStore;
import com.example.volunteerapp.model.Organization;
import com.example.volunteerapp.model.Student;
import com.example.volunteerapp.model.VolunteerOpportunity;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.bottomnavigation.BottomNavigationView;

/**
 * النشاط الرئيسي للتطبيق، يدير التنقل بين شاشات الطالب والمؤسسة
 * عبر شريط التنقل السفلي، ويتعامل مع عمليات تسجيل الدخول والتسجيل
 * وتسجيل الخروج واستعادة الجلسة المحفوظة.
 */
public class MainActivity extends AppCompatActivity implements
        ChooseUserFragment.OnUserTypeSelected,
        StudentLoginFragment.OnLoginSuccess,
        StudentLoginFragment.OnGoRegister,
        StudentRegisterFragment.OnRegisterSuccess,
        OrgLoginFragment.OnLoginSuccess,
        OrgLoginFragment.OnGoRegister,
        OrgRegisterFragment.OnRegisterSuccess,
        StudentProfileFragment.OnLogout,
        OpportunitiesListFragment.OnOpportunitySelected,
        OrgOpportunitiesFragment.OnAddOpportunity,
        AddOpportunityFragment.OnOpportunitySaved {

    private BottomNavigationView bottomNav;
    private String currentStudentId;
    private String currentOrgId;
    private boolean isStudentMode = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        setContentView(R.layout.activity_main);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        WindowInsetsControllerCompat wic = new WindowInsetsControllerCompat(getWindow(), getWindow().getDecorView());
        wic.setAppearanceLightStatusBars(false);

        ViewCompat.setOnApplyWindowInsetsListener(toolbar, (v, insets) -> {
            Insets top = insets.getInsets(WindowInsetsCompat.Type.statusBars() | WindowInsetsCompat.Type.displayCutout());
            v.setPadding(v.getPaddingLeft(), top.top, v.getPaddingRight(), v.getPaddingBottom());
            return insets;
        });

        bottomNav = findViewById(R.id.bottom_nav);
        ViewCompat.setOnApplyWindowInsetsListener(bottomNav, (v, insets) -> {
            Insets nav = insets.getInsets(WindowInsetsCompat.Type.navigationBars());
            v.setPadding(v.getPaddingLeft(), v.getPaddingTop(), v.getPaddingRight(), nav.bottom);
            return insets;
        });
        bottomNav.setVisibility(View.GONE);

        tryRestoreSessionOrShowChooser();
    }

    /**
     * محاولة استعادة جلسة المستخدم المحفوظة؛ إذا لم توجد يُعرض شاشة اختيار نوع المستخدم.
     */
    private void tryRestoreSessionOrShowChooser() {
        AuthSessionStore.Snapshot snap = AuthSessionStore.read(this);
        if (!snap.valid) {
            showChooseUser();
            return;
        }
        if (snap.student) {
            currentStudentId = snap.userId;
            currentOrgId = null;
            isStudentMode = true;
            showStudentMain();
        } else {
            currentOrgId = snap.userId;
            currentStudentId = null;
            isStudentMode = false;
            showOrgMain();
        }
    }

    private void showChooseUser() {
        ChooseUserFragment f = new ChooseUserFragment();
        f.setOnUserTypeSelected(this);
        replaceFragment(f);
    }

    private void replaceFragment(Fragment f) {
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.nav_host_fragment, f)
                .commit();
    }

    private void replaceFragmentWithBack(Fragment f) {
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.nav_host_fragment, f)
                .addToBackStack(null)
                .commit();
    }

    // ==================== ChooseUserFragment callbacks ====================

    @Override
    public void onStudentLogin() {
        StudentLoginFragment f = new StudentLoginFragment();
        f.setOnLoginSuccess(this);
        f.setOnGoRegister(this);
        replaceFragmentWithBack(f);
    }

    @Override
    public void onStudentRegister() {
        StudentRegisterFragment f = new StudentRegisterFragment();
        f.setOnRegisterSuccess(this);
        replaceFragmentWithBack(f);
    }

    @Override
    public void onOrgLogin() {
        OrgLoginFragment f = new OrgLoginFragment();
        f.setOnLoginSuccess(this);
        f.setOnGoRegister(this);
        replaceFragmentWithBack(f);
    }

    @Override
    public void onOrgRegister() {
        OrgRegisterFragment f = new OrgRegisterFragment();
        f.setOnRegisterSuccess(this);
        replaceFragmentWithBack(f);
    }

    // ==================== Student login/register ====================

    @Override
    public void onStudentLoggedIn(Student student) {
        currentStudentId = student.getId();
        currentOrgId = null;
        isStudentMode = true;
        AuthSessionStore.saveStudentSession(this, student.getId(), student.getEmail());
        showStudentMain();
    }

    @Override
    public void onStudentRegistered(Student student) {
        currentStudentId = student.getId();
        currentOrgId = null;
        isStudentMode = true;
        AuthSessionStore.saveStudentSession(this, student.getId(), student.getEmail());
        getSupportFragmentManager().popBackStack(null, androidx.fragment.app.FragmentManager.POP_BACK_STACK_INCLUSIVE);
        showStudentMain();
    }

    @Override
    public void onGoStudentRegister() {
        StudentRegisterFragment f = new StudentRegisterFragment();
        f.setOnRegisterSuccess(this);
        replaceFragmentWithBack(f);
    }

    // ==================== Org login/register ====================

    @Override
    public void onOrgLoggedIn(Organization org) {
        currentOrgId = org.getId();
        currentStudentId = null;
        isStudentMode = false;
        AuthSessionStore.saveOrgSession(this, org.getId(), org.getEmail());
        showOrgMain();
    }

    @Override
    public void onOrgRegistered(Organization org) {
        currentOrgId = org.getId();
        currentStudentId = null;
        isStudentMode = false;
        AuthSessionStore.saveOrgSession(this, org.getId(), org.getEmail());
        getSupportFragmentManager().popBackStack(null, androidx.fragment.app.FragmentManager.POP_BACK_STACK_INCLUSIVE);
        showOrgMain();
    }

    @Override
    public void onGoOrgRegister() {
        OrgRegisterFragment f = new OrgRegisterFragment();
        f.setOnRegisterSuccess(this);
        replaceFragmentWithBack(f);
    }

    // ==================== Student main (bottom nav) ====================

    /** إعداد وعرض واجهة الطالب الرئيسية مع شريط التنقل السفلي الخاص بالطالب. */
    private void showStudentMain() {
        bottomNav.setVisibility(View.VISIBLE);
        bottomNav.getMenu().clear();
        bottomNav.inflateMenu(R.menu.bottom_nav_menu);
        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_opportunities) {
                OpportunitiesListFragment f = OpportunitiesListFragment.newInstance(currentStudentId);
                f.setOnOpportunitySelected(this);
                replaceFragment(f);
                return true;
            }
            if (id == R.id.nav_my_orgs) {
                replaceFragment(StudentMyOrgsFragment.newInstance(currentStudentId));
                return true;
            }
            if (id == R.id.nav_student_reports) {
                replaceFragment(StudentReportsFragment.newInstance(currentStudentId));
                return true;
            }
            if (id == R.id.nav_profile) {
                StudentProfileFragment f = StudentProfileFragment.newInstance(currentStudentId);
                f.setOnLogout(this);
                replaceFragment(f);
                return true;
            }
            return false;
        });
        OpportunitiesListFragment f = OpportunitiesListFragment.newInstance(currentStudentId);
        f.setOnOpportunitySelected(this);
        replaceFragment(f);
    }

    @Override
    public void onOpportunitySelected(VolunteerOpportunity opportunity, String studentId) {
        OpportunityDetailsFragment f = OpportunityDetailsFragment.newInstance(
                opportunity.getId(), opportunity.getOrganizationId(), studentId);
        replaceFragmentWithBack(f);
    }

    // ==================== Org main (bottom nav) ====================

    /** إعداد وعرض واجهة المؤسسة الرئيسية مع شريط التنقل السفلي الخاص بالمؤسسة. */
    private void showOrgMain() {
        bottomNav.setVisibility(View.VISIBLE);
        bottomNav.getMenu().clear();
        bottomNav.inflateMenu(R.menu.bottom_nav_org);
        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_opportunities) {
                OrgOpportunitiesFragment f = OrgOpportunitiesFragment.newInstance(currentOrgId);
                f.setOnAddOpportunity(this);
                replaceFragment(f);
                return true;
            }
            if (id == R.id.nav_requests) {
                replaceFragment(OrgCombinedRequestsFragment.newInstance(currentOrgId));
                return true;
            }
            if (id == R.id.nav_reports) {
                replaceFragment(ReportsFragment.newInstance(currentOrgId, true));
                return true;
            }
            if (id == R.id.nav_org_profile) {
                OrgManageFragment f = OrgManageFragment.newInstance(currentOrgId);
                f.setOnLogout(this);
                replaceFragment(f);
                return true;
            }
            return false;
        });
        OrgOpportunitiesFragment f = OrgOpportunitiesFragment.newInstance(currentOrgId);
        f.setOnAddOpportunity(this);
        replaceFragment(f);
    }

    @Override
    public void onAddOpportunity(String orgId) {
        AddOpportunityFragment f = AddOpportunityFragment.newInstance(orgId);
        f.setOnOpportunitySaved(this);
        replaceFragmentWithBack(f);
    }

    @Override
    public void onOpportunitySaved() {
        getSupportFragmentManager().popBackStack();
    }

    // ==================== Logout ====================

    /** تسجيل الخروج: مسح الجلسة المحفوظة وتسجيل الخروج من Appwrite والعودة لشاشة الاختيار. */
    @Override
    public void onLogout() {
        currentStudentId = null;
        currentOrgId = null;
        bottomNav.setVisibility(View.GONE);
        AuthSessionStore.clear(this);
        com.example.volunteerapp.Hellper.DALAppWriteConnection dal = new com.example.volunteerapp.Hellper.DALAppWriteConnection(this);
        dal.logoutUser();
        showChooseUser();
    }

    @Override
    @SuppressWarnings("deprecation")
    public void onBackPressed() {
        if (getSupportFragmentManager().getBackStackEntryCount() > 0) {
            getSupportFragmentManager().popBackStack();
        } else if (currentStudentId != null || currentOrgId != null) {
            finish();
        } else {
            super.onBackPressed();
        }
    }
}
