package com.example.mohtadyapp;

import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import com.example.mohtadyapp.fragment.ChooseUserFragment;
import com.example.mohtadyapp.fragment.MyApplicationsFragment;
import com.example.mohtadyapp.fragment.MyHoursFragment;
import com.example.mohtadyapp.fragment.OrganizationsListFragment;
import com.example.mohtadyapp.fragment.OrgHoursReviewFragment;
import com.example.mohtadyapp.fragment.OrgLoginFragment;
import com.example.mohtadyapp.fragment.OrgRegisterFragment;
import com.example.mohtadyapp.fragment.OrgRequestsFragment;
import com.example.mohtadyapp.fragment.StudentLoginFragment;
import com.example.mohtadyapp.fragment.StudentProfileFragment;
import com.example.mohtadyapp.fragment.StudentRegisterFragment;
import com.example.mohtadyapp.model.Organization;
import com.example.mohtadyapp.model.Student;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationBarView;

public class MainActivity extends AppCompatActivity implements
        ChooseUserFragment.OnUserTypeSelected,
        StudentLoginFragment.OnLoginSuccess,
        StudentLoginFragment.OnGoRegister,
        StudentRegisterFragment.OnRegisterSuccess,
        OrgLoginFragment.OnLoginSuccess,
        OrgLoginFragment.OnGoRegister,
        OrgRegisterFragment.OnRegisterSuccess,
        StudentProfileFragment.OnLogout {

    private BottomNavigationView bottomNav;
    private String currentStudentId;
    private String currentOrgId;
    private boolean isStudentMode = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        bottomNav = findViewById(R.id.bottom_nav);
        bottomNav.setVisibility(View.GONE);

        showChooseUser();
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

    // === ChooseUserFragment callbacks ===
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

    // === Student login/register success ===
    @Override
    public void onStudentLoggedIn(Student student) {
        currentStudentId = student.getId();
        currentOrgId = null;
        isStudentMode = true;
        showStudentMain();
    }

    @Override
    public void onStudentRegistered(Student student) {
        currentStudentId = student.getId();
        currentOrgId = null;
        isStudentMode = true;
        getSupportFragmentManager().popBackStack(null, androidx.fragment.app.FragmentManager.POP_BACK_STACK_INCLUSIVE);
        showStudentMain();
    }

    @Override
    public void onGoStudentRegister() {
        StudentRegisterFragment f = new StudentRegisterFragment();
        f.setOnRegisterSuccess(this);
        replaceFragmentWithBack(f);
    }

    // === Org login/register success ===
    @Override
    public void onOrgLoggedIn(Organization org) {
        currentOrgId = org.getId();
        currentStudentId = null;
        isStudentMode = false;
        showOrgMain();
    }

    @Override
    public void onOrgRegistered(Organization org) {
        currentOrgId = org.getId();
        currentStudentId = null;
        isStudentMode = false;
        getSupportFragmentManager().popBackStack(null, androidx.fragment.app.FragmentManager.POP_BACK_STACK_INCLUSIVE);
        showOrgMain();
    }

    @Override
    public void onGoOrgRegister() {
        OrgRegisterFragment f = new OrgRegisterFragment();
        f.setOnRegisterSuccess(this);
        replaceFragmentWithBack(f);
    }

    // === Student main (bottom nav) ===
    private void showStudentMain() {
        bottomNav.setVisibility(View.VISIBLE);
        bottomNav.getMenu().clear();
        bottomNav.inflateMenu(R.menu.bottom_nav_menu);
        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_organizations) {
                replaceFragment(OrganizationsListFragment.newInstance(currentStudentId));
                return true;
            }
            if (id == R.id.nav_my_applications) {
                replaceFragment(MyApplicationsFragment.newInstance(currentStudentId));
                return true;
            }
            if (id == R.id.nav_my_hours) {
                replaceFragment(MyHoursFragment.newInstance(currentStudentId));
                return true;
            }
            if (id == R.id.nav_profile) {
                loadStudentAndShowProfile();
                return true;
            }
            return false;
        });
        replaceFragment(OrganizationsListFragment.newInstance(currentStudentId));
    }

    private void loadStudentAndShowProfile() {
        new Thread(() -> {
            com.example.mohtadyapp.Hellper.VolunteerAppHelper helper = new com.example.mohtadyapp.Hellper.VolunteerAppHelper(this);
            com.example.mohtadyapp.Hellper.DALAppWriteConnection.OperationResult<Student> result = helper.getStudentById(currentStudentId);
            runOnUiThread(() -> {
                if (result.success && result.data != null) {
                    StudentProfileFragment f = StudentProfileFragment.newInstance(result.data);
                    f.setOnLogout(this);
                    replaceFragment(f);
                }
            });
        }).start();
    }

    // === Org main (bottom nav) ===
    private void showOrgMain() {
        bottomNav.setVisibility(View.VISIBLE);
        bottomNav.getMenu().clear();
        bottomNav.inflateMenu(R.menu.bottom_nav_org);
        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_requests) {
                replaceFragment(OrgRequestsFragment.newInstance(currentOrgId));
                return true;
            }
            if (id == R.id.nav_org_profile) {
                Toast.makeText(this, "إدارة المؤسسة - قريباً", Toast.LENGTH_SHORT).show();
                return true;
            }
            if (id == R.id.nav_reports) {
                replaceFragment(OrgHoursReviewFragment.newInstance(currentOrgId));
                return true;
            }
            return false;
        });
        replaceFragment(OrgRequestsFragment.newInstance(currentOrgId));
    }

    // === Logout ===
    @Override
    public void onLogout() {
        currentStudentId = null;
        currentOrgId = null;
        bottomNav.setVisibility(View.GONE);
        com.example.mohtadyapp.Hellper.DALAppWriteConnection dal = new com.example.mohtadyapp.Hellper.DALAppWriteConnection(this);
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
