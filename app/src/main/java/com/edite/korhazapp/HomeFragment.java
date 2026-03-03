package com.edite.korhazapp;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class HomeFragment extends Fragment {

    private FirebaseFirestore db;
    private Spinner spinnerSection;
    private TextView tvDescription, tvAddress, tvProgram, tvDoctors;
    private List<String> sectionNames = new ArrayList<>();
    private ArrayAdapter<String> adapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        db = FirebaseFirestore.getInstance();
        spinnerSection = view.findViewById(R.id.spinnerSection);
        tvDescription = view.findViewById(R.id.tvDescription);
        tvAddress = view.findViewById(R.id.tvAddress);
        tvProgram = view.findViewById(R.id.tvProgram);
        tvDoctors = view.findViewById(R.id.tvDoctors);

        setupSpinner();
        loadSectionsFromFirestore();

        return view;
    }

    private void setupSpinner() {
        adapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_item, sectionNames);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerSection.setAdapter(adapter);

        spinnerSection.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selectedSection = sectionNames.get(position);
                loadSectionDetails(selectedSection);
                loadDoctorsForSection(selectedSection);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private void loadSectionsFromFirestore() {
        db.collection("Sections").get().addOnCompleteListener(task -> {
            if (task.isSuccessful() && task.getResult() != null) {
                sectionNames.clear();
                if (task.getResult().isEmpty()) {
                    seedInitialData();
                } else {
                    for (QueryDocumentSnapshot document : task.getResult()) {
                        sectionNames.add(document.getId());
                    }
                    adapter.notifyDataSetChanged();
                }
            } else {
                Toast.makeText(getContext(), "Hiba az adatok letöltésekor", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadSectionDetails(String sectionName) {
        db.collection("Sections").document(sectionName).get().addOnSuccessListener(documentSnapshot -> {
            if (documentSnapshot.exists()) {
                tvDescription.setText(documentSnapshot.getString("description"));
                tvAddress.setText(documentSnapshot.getString("address"));
                tvProgram.setText(documentSnapshot.getString("program"));
            }
        });
    }

    private void loadDoctorsForSection(String sectionName) {
        // Mostantól a "Users" kollekcióból kérjük le az orvosokat
        db.collection("Users")
                .whereEqualTo("role", "doctor")
                .whereEqualTo("section", sectionName)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        StringBuilder doctorsList = new StringBuilder();
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            if (doctorsList.length() > 0) doctorsList.append(", ");
                            doctorsList.append(document.getString("name"));
                        }
                        
                        if (doctorsList.length() == 0) {
                            tvDoctors.setText("Nincsenek orvosok ebben a szekcióban.");
                        } else {
                            tvDoctors.setText(doctorsList.toString());
                        }
                    }
                });
    }

    private void seedInitialData() {
        addSectionToFirestore("Kardiológia", 
            "Szív- és érrendszeri betegségek diagnosztizálása és kezelése.", 
            "Str. Gheorghe Marinescu, nr. 50, Târgu Mureș", 
            "Hétfő - Péntek: 08:00 - 15:00");

        addSectionToFirestore("Neurológia", 
            "Idegrendszeri megbetegedések szakellátása.", 
            "Str. Gheorghe Marinescu, nr. 1, Târgu Mureș", 
            "Non-stop sürgősségi ellátás");

        addSectionToFirestore("Sebészet", 
            "Általános sebészeti beavatkozások és konzultációk.", 
            "Str. Gheorghe Marinescu, nr. 50", 
            "Hétfő - Péntek: 07:00 - 14:00");
    }

    private void addSectionToFirestore(String name, String desc, String addr, String prog) {
        java.util.Map<String, Object> section = new java.util.HashMap<>();
        section.put("description", desc);
        section.put("address", addr);
        section.put("program", prog);

        db.collection("Sections").document(name).set(section).addOnSuccessListener(aVoid -> {
            loadSectionsFromFirestore();
        });
    }
}