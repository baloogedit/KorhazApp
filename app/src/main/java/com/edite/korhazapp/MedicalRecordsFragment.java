package com.edite.korhazapp;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class MedicalRecordsFragment extends Fragment {

    private FirebaseFirestore db;
    private RecyclerView recyclerView;
    private RecordAdapter adapter;
    private List<MedicalRecord> recordList = new ArrayList<>();
    private TextView tvEmptyRecords, tvTitle;
    private String mode; // "patient" or "doctor"

    public static MedicalRecordsFragment newInstance(String mode) {
        MedicalRecordsFragment fragment = new MedicalRecordsFragment();
        Bundle args = new Bundle();
        args.putString("mode", mode);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_medical_records, container, false);
        
        if (getArguments() != null) {
            mode = getArguments().getString("mode");
        }

        db = FirebaseFirestore.getInstance();
        recyclerView = view.findViewById(R.id.rvMedicalRecords);
        tvEmptyRecords = view.findViewById(R.id.tvEmptyRecords);
        tvTitle = view.findViewById(R.id.tvRecordsTitle);

        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new RecordAdapter(recordList);
        recyclerView.setAdapter(adapter);

        view.findViewById(R.id.btnBackFromRecords).setOnClickListener(v -> {
            if (getActivity() != null) {
                getActivity().getSupportFragmentManager().popBackStack();
            }
        });

        loadRecords();

        return view;
    }

    private void loadRecords() {
        if (FirebaseAuth.getInstance().getCurrentUser() == null) return;
        
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        Query query;

        if ("doctor".equals(mode)) {
            tvTitle.setText(R.string.medical_records_title);
            query = db.collection("MedicalRecords")
                    .whereEqualTo("doctor_id", userId)
                    .orderBy("date", Query.Direction.DESCENDING);
        } else {
            tvTitle.setText(R.string.past_appointments_title);
            query = db.collection("MedicalRecords")
                    .whereEqualTo("patient_id", userId)
                    .orderBy("date", Query.Direction.DESCENDING);
        }

        query.get().addOnCompleteListener(task -> {
            if (task.isSuccessful() && task.getResult() != null) {
                recordList.clear();
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy.MM.dd HH:mm", Locale.getDefault());
                
                for (QueryDocumentSnapshot document : task.getResult()) {
                    String dateStr = "";
                    Object dateObj = document.get("date");
                    if (dateObj instanceof Timestamp) {
                        dateStr = sdf.format(((Timestamp) dateObj).toDate());
                    } else if (dateObj instanceof String) {
                        dateStr = (String) dateObj;
                    }

                    recordList.add(new MedicalRecord(
                            dateStr,
                            document.getString("doctor_name"),
                            document.getString("section"),
                            document.getString("diagnosis")
                    ));
                }
                adapter.notifyDataSetChanged();
                tvEmptyRecords.setVisibility(recordList.isEmpty() ? View.VISIBLE : View.GONE);
            } else {
                if (task.getException() != null) {
                    // Check for missing index error in logcat if it still doesn't work
                    Toast.makeText(getContext(), "Hiba: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                }
            }
        });
    }

    private static class MedicalRecord {
        String date, doctorName, section, diagnosis;
        MedicalRecord(String date, String doctorName, String section, String diagnosis) {
            this.date = date; this.doctorName = doctorName; this.section = section; this.diagnosis = diagnosis;
        }
    }

    private class RecordAdapter extends RecyclerView.Adapter<RecordAdapter.ViewHolder> {
        private List<MedicalRecord> records;
        RecordAdapter(List<MedicalRecord> records) { this.records = records; }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_record, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            MedicalRecord record = records.get(position);
            holder.tvDate.setText(record.date);
            holder.tvHeader.setText(record.doctorName + " - " + record.section);
            holder.tvContent.setText("Diagnózis: " + record.diagnosis);
        }

        @Override
        public int getItemCount() { return records.size(); }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvDate, tvHeader, tvContent;
            ViewHolder(View itemView) {
                super(itemView);
                tvDate = itemView.findViewById(R.id.tvRecordDate);
                tvHeader = itemView.findViewById(R.id.tvRecordHeader);
                tvContent = itemView.findViewById(R.id.tvRecordContent);
            }
        }
    }
}