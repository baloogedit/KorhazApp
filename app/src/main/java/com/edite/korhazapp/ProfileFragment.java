package com.edite.korhazapp;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

public class ProfileFragment extends Fragment {

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    
    private TextView tvNotLoggedIn, tvProfileTitle, tvProfileName, tvProfileCNP, tvProfileEmail, tvProfileSection, tvAppointmentInfo;
    private LinearLayout llProfileContent, llUpcomingAppointments;
    private Button btnSwitchMode;
    private TextView tvMedicalRecordsLink, tvPastAppointmentsLink;
    
    private boolean isDoctorMode = false;
    private String userRole = "patient";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_profile, container, false);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // UI elemek inicializálása
        tvNotLoggedIn = view.findViewById(R.id.tvNotLoggedIn);
        llProfileContent = view.findViewById(R.id.llProfileContent);
        tvProfileTitle = view.findViewById(R.id.tvProfileTitle);
        tvProfileName = view.findViewById(R.id.tvProfileName);
        tvProfileCNP = view.findViewById(R.id.tvProfileCNP);
        tvProfileEmail = view.findViewById(R.id.tvProfileEmail);
        tvProfileSection = view.findViewById(R.id.tvProfileSection);
        tvAppointmentInfo = view.findViewById(R.id.tvAppointmentInfo);
        llUpcomingAppointments = view.findViewById(R.id.llUpcomingAppointments);
        btnSwitchMode = view.findViewById(R.id.btnSwitchMode);
        tvMedicalRecordsLink = view.findViewById(R.id.tvMedicalRecordsLink);
        tvPastAppointmentsLink = view.findViewById(R.id.tvPastAppointmentsLink);

        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            tvNotLoggedIn.setVisibility(View.VISIBLE);
            llProfileContent.setVisibility(View.GONE);
        } else {
            loadUserProfile(currentUser.getUid());
        }

        // Kijelentkezés
        view.findViewById(R.id.tvLogout).setOnClickListener(v -> {
            mAuth.signOut();
            startActivity(new Intent(getActivity(), LoginActivity.class));
            getActivity().finish();
        });

        // Mód váltás (Doktor <-> Beteg)
        btnSwitchMode.setOnClickListener(v -> {
            isDoctorMode = !isDoctorMode;
            updateUIForMode();
        });

        // Hyperlinkek kezelése - Új ablak (Fragment) megnyitása
        tvMedicalRecordsLink.setOnClickListener(v -> openRecords("doctor"));
        tvPastAppointmentsLink.setOnClickListener(v -> openRecords("patient"));

        return view;
    }

    private void openRecords(String mode) {
        MedicalRecordsFragment fragment = MedicalRecordsFragment.newInstance(mode);
        FragmentTransaction transaction = getParentFragmentManager().beginTransaction();
        transaction.replace(R.id.fragment_container, fragment);
        transaction.addToBackStack(null); // Visszagomb támogatása
        transaction.commit();
    }

    private void loadUserProfile(String userId) {
        db.collection("Users").document(userId).get().addOnSuccessListener(documentSnapshot -> {
            if (documentSnapshot.exists()) {
                userRole = documentSnapshot.getString("role");
                tvProfileName.setText("Név: " + documentSnapshot.getString("name"));
                tvProfileCNP.setText("CNP: " + documentSnapshot.getString("cnp"));
                tvProfileEmail.setText("Email: " + documentSnapshot.getString("email"));
                
                if ("doctor".equals(userRole)) {
                    isDoctorMode = true;
                    btnSwitchMode.setVisibility(View.VISIBLE);
                    tvProfileSection.setVisibility(View.VISIBLE);
                    tvProfileSection.setText("Szekció: " + documentSnapshot.getString("section"));
                } else {
                    isDoctorMode = false;
                    btnSwitchMode.setVisibility(View.GONE);
                    tvProfileSection.setVisibility(View.GONE);
                }
                
                updateUIForMode();
                loadAppointments(userId);
            }
        });
    }

    private void updateUIForMode() {
        if (isDoctorMode) {
            tvProfileTitle.setText(R.string.doctor_page_title);
            btnSwitchMode.setText(R.string.switch_to_patient);
            tvMedicalRecordsLink.setVisibility(View.VISIBLE);
            llUpcomingAppointments.setVisibility(View.GONE);
            tvProfileSection.setVisibility(View.VISIBLE);
        } else {
            tvProfileTitle.setText(R.string.patient_page_title);
            btnSwitchMode.setText(R.string.switch_to_doctor);
            tvMedicalRecordsLink.setVisibility(View.GONE);
            llUpcomingAppointments.setVisibility(View.VISIBLE);
            if ("doctor".equals(userRole)) {
                 tvProfileSection.setVisibility(View.VISIBLE);
            } else {
                 tvProfileSection.setVisibility(View.GONE);
            }
        }
    }

    private void loadAppointments(String userId) {
        db.collection("Appointments")
                .whereEqualTo("patient_id", userId)
                .whereEqualTo("status", "aktív")
                .orderBy("date", Query.Direction.ASCENDING)
                .limit(1)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null && !task.getResult().isEmpty()) {
                        QueryDocumentSnapshot doc = (QueryDocumentSnapshot) task.getResult().getDocuments().get(0);
                        String info = "Dátum: " + doc.getString("date") + ", " + doc.getString("time") + "\n" +
                                     "Szekció: " + doc.getString("section") + ", Orvos: " + doc.getString("doctor");
                        tvAppointmentInfo.setText(info);
                    } else {
                        tvAppointmentInfo.setText("Nincs közeledő időpont.");
                    }
                });
    }
}