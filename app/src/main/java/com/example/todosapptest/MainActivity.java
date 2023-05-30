package com.example.todosapptest;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.content.DialogInterface;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.todosapptest.adapter.TaskAdapter;
import com.example.todosapptest.model.Task;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements TaskAdapter.OnTaskClickListener {
    private EditText taskEditText;

    private Button addButton;
    private RecyclerView recyclerView;
    private TaskAdapter adapter;
    private List<Task> tasks;
    private DatabaseReference tasksRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // text and button
        taskEditText = findViewById(R.id.taskEditText);
        addButton = findViewById(R.id.addButton);

        // Inisialisasi RecyclerView
        recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        tasks = new ArrayList<>();
        adapter = new TaskAdapter(tasks);
        adapter.setOnTaskClickListener(this);
        recyclerView.setAdapter(adapter);

        // Inisialisasi Firebase
        tasksRef = FirebaseDatabase.getInstance().getReference("tasks");

        // Mengambil daftar task dari Firebase
        tasksRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                tasks.clear();
                for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                    Task task = dataSnapshot.getValue(Task.class);
                    tasks.add(task);
                }
                adapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(MainActivity.this, "Gagal mengambil data dari Firebase", Toast.LENGTH_SHORT).show();
            }
        });

        // Tambahkan tombol tambah task jika diperlukan
        addButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String title = taskEditText.getText().toString();
                String taskId = tasksRef.push().getKey();
                Task task = new Task(taskId, title, false);
                tasksRef.child(taskId).setValue(task);
                taskEditText.setText("");
            }
        });

        tasksRef.addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot dataSnapshot, String previousChildName) {
                Task task = dataSnapshot.getValue(Task.class);
                tasks.add(task);
                adapter.notifyDataSetChanged();
            }

            @Override
            public void onChildChanged(DataSnapshot dataSnapshot, String previousChildName) {
                Task task = dataSnapshot.getValue(Task.class);
                int index = getIndexByKey(task.getId());
                if (index != -1) {
                    tasks.set(index, task);
                    adapter.notifyItemChanged(index);
                }
            }

            @Override
            public void onChildRemoved(DataSnapshot dataSnapshot) {
                Task task = dataSnapshot.getValue(Task.class);
                int index = getIndexByKey(task.getId());
                if (index != -1) {
                    tasks.remove(index);
                    adapter.notifyItemRemoved(index);
                }
            }

            @Override
            public void onChildMoved(DataSnapshot dataSnapshot, String previousChildName) {
                // Tidak diperlukan untuk implementasi saat ini
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Toast.makeText(MainActivity.this, "Gagal membaca data dari Firebase.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private int getIndexByKey(String key) {
        for (int i = 0; i < tasks.size(); i++) {
            if (tasks.get(i).getId().equals(key)) {
                return i;
            }
        }
        return -1;
    }

    @Override
    public void onTaskDelete(int position) {
        Task task = tasks.get(position);
        tasksRef.child(task.getId()).removeValue();
    }

//    @Override
//    public void onTaskUpdate(int position) {
//        Task task = tasks.get(position);
//
//        // Lakukan aksi update task di sini, misalnya dengan dialog atau form pengeditan
//        // ...
//
//        // Setelah melakukan update, simpan perubahan ke Firebase
//        tasksRef.child(task.getId()).setValue(task);
//    }

    @Override
    public void onTaskUpdate(int position) {
        Task task = tasks.get(position);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Update Task");

        final EditText input = new EditText(this);
        input.setText(task.getTitle());
        builder.setView(input);

        builder.setPositiveButton("Update", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String newTitle = input.getText().toString().trim();
                if (!newTitle.isEmpty()) {
                    task.setTitle(newTitle);
                    tasksRef.child(task.getId()).setValue(task);
                }
            }
        });

        builder.setNegativeButton("Cancel", null);

        builder.create().show();
    }

    @Override
    public void onAddButtonClick(View view) {

    }

    @Override
    public void onTaskComplete(int position, boolean completed) {
        Task task = tasks.get(position);
        task.setCompleted(completed);
        tasksRef.child(task.getId()).setValue(task);
    }
}

