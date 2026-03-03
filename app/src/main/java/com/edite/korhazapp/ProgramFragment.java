package com.edite.korhazapp;

import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class ProgramFragment extends Fragment {

    private FirebaseFirestore db;
    private Spinner spinnerSection, spinnerDoctor, spinnerMonth, spinnerTime;
    private GridView calendarGrid;
    private List<String> sectionNames = new ArrayList<>();
    private List<String> doctorNames = new ArrayList<>();
    private List<DayStatus> days = new ArrayList<>();
    private CalendarAdapter calendarAdapter;
    private ArrayAdapter<String> sectionAdapter;
    private ArrayAdapter<String> doctorAdapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_program, container, false);

        db = FirebaseFirestore.getInstance();
        spinnerSection = view.findViewById(R.id.spinnerProgSection);
        spinnerDoctor = view.findViewById(R.id.spinnerDoctor);
        spinnerMonth = view.findViewById(R.id.spinnerMonth);
        spinnerTime = view.findViewById(R.id.spinnerTime);
        calendarGrid = view.findViewById(R.id.calendarGrid);

        setupSpinners();
        loadSections();
        setupCalendar();

        view.findViewById(R.id.btnBook).setOnClickListener(v -> makeAppointment());
        view.findViewById(R.id.btnBack).setOnClickListener(v -> {
             if (getActivity() != null) {
                 ((MainActivity) getActivity()).getSupportFragmentManager().beginTransaction()
                         .replace(R.id.fragment_container, new HomeFragment())
                         .commit();
             }
        });

        return view;
    }

    private void setupSpinners() {
        // Section Spinner
        sectionAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_item, sectionNames);
        sectionAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerSection.setAdapter(sectionAdapter);

        spinnerSection.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selectedSection = sectionNames.get(position);
                loadDoctorsForSection(selectedSection);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        // Doctor Spinner
        doctorAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_item, doctorNames);
        doctorAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerDoctor.setAdapter(doctorAdapter);

        // Hónapok
        String[] monthList = {"Január", "Február", "Március", "Április", "Május", "Június", "Július", "Augusztus", "Szeptember", "Október", "November", "December"};
        ArrayAdapter<String> monthAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_item, monthList);
        monthAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerMonth.setAdapter(monthAdapter);
        spinnerMonth.setSelection(Calendar.getInstance().get(Calendar.MONTH));

        // Időpontok
        String[] timeList = {"08:00", "08:30", "09:00", "09:30", "10:00", "11:00", "13:00", "14:30"};
        ArrayAdapter<String> timeAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_item, timeList);
        timeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerTime.setAdapter(timeAdapter);
    }

    private void loadSections() {
        db.collection("Sections").get().addOnCompleteListener(task -> {
            if (task.isSuccessful() && task.getResult() != null) {
                sectionNames.clear();
                for (QueryDocumentSnapshot document : task.getResult()) {
                    sectionNames.add(document.getId());
                }
                sectionAdapter.notifyDataSetChanged();
                
                if (!sectionNames.isEmpty()) {
                    loadDoctorsForSection(sectionNames.get(0));
                }
            }
        });
    }

    private void loadDoctorsForSection(String sectionName) {
        // Mostantól a "Users" kollekcióból kérjük le azokat, akik "doctor" szerepkörrel rendelkeznek
        db.collection("Users")
                .whereEqualTo("role", "doctor")
                .whereEqualTo("section", sectionName)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        doctorNames.clear();
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            doctorNames.add(document.getString("name"));
                        }
                        doctorAdapter.notifyDataSetChanged();
                        
                        if (doctorNames.isEmpty()) {
                            Toast.makeText(getContext(), "Nincs orvos ebben a szekcióban.", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    private void setupCalendar() {
        days.clear();
        String[] weekDays = {"L", "M", "M", "J", "V", "S", "D"};
        for (String day : weekDays) {
            days.add(new DayStatus(day, Color.LTGRAY, true));
        }

        Random random = new Random();
        int[] colors = {Color.RED, Color.YELLOW, Color.GREEN};
        
        for (int i = 1; i <= 30; i++) {
            int color = colors[random.nextInt(colors.length)];
            days.add(new DayStatus(String.valueOf(i), color, false));
        }

        calendarAdapter = new CalendarAdapter();
        calendarGrid.setAdapter(calendarAdapter);
    }

    private void makeAppointment() {
        if (FirebaseAuth.getInstance().getCurrentUser() == null) return;
        
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        Object selectedSection = spinnerSection.getSelectedItem();
        Object selectedDoctor = spinnerDoctor.getSelectedItem();
        
        if (selectedSection == null || selectedDoctor == null) {
            Toast.makeText(getContext(), "Válassz szekciót és orvost!", Toast.LENGTH_SHORT).show();
            return;
        }

        String section = selectedSection.toString();
        String doctor = selectedDoctor.toString();
        String month = spinnerMonth.getSelectedItem().toString();
        String time = spinnerTime.getSelectedItem().toString();

        Map<String, Object> appointment = new HashMap<>();
        appointment.put("patient_id", userId);
        appointment.put("section", section);
        appointment.put("doctor", doctor);
        appointment.put("date", month + " " + "15");
        appointment.put("time", time);
        appointment.put("status", "aktív");

        db.collection("Appointments").add(appointment)
                .addOnSuccessListener(documentReference -> {
                    Toast.makeText(getContext(), "Sikeres foglalás!", Toast.LENGTH_LONG).show();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "Hiba: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private static class DayStatus {
        String text;
        int color;
        boolean isHeader;

        DayStatus(String text, int color, boolean isHeader) {
            this.text = text;
            this.color = color;
            this.isHeader = isHeader;
        }
    }

    private class CalendarAdapter extends BaseAdapter {
        @Override
        public int getCount() { return days.size(); }
        @Override
        public Object getItem(int position) { return days.get(position); }
        @Override
        public long getItemId(int position) { return position; }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            TextView textView;
            if (convertView == null) {
                textView = new TextView(getContext());
                textView.setLayoutParams(new GridView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 100));
                textView.setGravity(android.view.Gravity.CENTER);
                textView.setTextColor(Color.BLACK);
            } else {
                textView = (TextView) convertView;
            }

            DayStatus day = days.get(position);
            textView.setText(day.text);
            
            if (!day.isHeader) {
                textView.setBackgroundColor(day.color);
                textView.setPadding(4, 4, 4, 4);
            } else {
                textView.setBackgroundColor(Color.WHITE);
                textView.setTypeface(null, android.graphics.Typeface.BOLD);
            }

            return textView;
        }
    }
}