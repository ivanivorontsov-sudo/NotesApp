package com.example.notesapp

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

data class Note(
    val id: String,
    var title: String,
    var content: String,
    val createdAt: Long
)

class NotesAdapter(
    private val notes: MutableList<Note>,
    private val onEdit: (Note) -> Unit,
    private val onDelete: (Note) -> Unit
) : RecyclerView.Adapter<NotesAdapter.NoteViewHolder>() {

    class NoteViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val title: TextView = view.findViewById(R.id.noteTitle)
        val content: TextView = view.findViewById(R.id.noteContent)
        val date: TextView = view.findViewById(R.id.noteDate)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NoteViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_note, parent, false)
        return NoteViewHolder(view)
    }

    override fun onBindViewHolder(holder: NoteViewHolder, position: Int) {
        val note = notes[position]
        holder.title.text = note.title.ifEmpty { "Без названия" }
        holder.content.text = note.content
        val sdf = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())
        holder.date.text = sdf.format(Date(note.createdAt))

        holder.itemView.setOnClickListener { onEdit(note) }
        holder.itemView.setOnLongClickListener {
            onDelete(note)
            true
        }
    }

    override fun getItemCount() = notes.size
}

class MainActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var emptyText: TextView
    private lateinit var adapter: NotesAdapter
    private val notes = mutableListOf<Note>()
    private val prefs by lazy { getSharedPreferences("notes_prefs", Context.MODE_PRIVATE) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        recyclerView = findViewById(R.id.recyclerView)
        emptyText = findViewById(R.id.emptyText)
        val fab: FloatingActionButton = findViewById(R.id.fab)

        adapter = NotesAdapter(notes, ::showEditDialog, ::confirmDelete)
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter

        fab.setOnClickListener { showAddDialog() }

        loadNotes()
        updateEmptyState()
    }

    private fun showAddDialog() {
        val view = layoutInflater.inflate(R.layout.dialog_note, null)
        val titleEdit = view.findViewById<EditText>(R.id.editTitle)
        val contentEdit = view.findViewById<EditText>(R.id.editContent)

        AlertDialog.Builder(this)
            .setTitle("Новая заметка")
            .setView(view)
            .setPositiveButton("Сохранить") { _, _ ->
                val title = titleEdit.text.toString().trim()
                val content = contentEdit.text.toString().trim()
                if (title.isEmpty() && content.isEmpty()) {
                    Toast.makeText(this, "Заметка пуста", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                val note = Note(
                    id = UUID.randomUUID().toString(),
                    title = title,
                    content = content,
                    createdAt = System.currentTimeMillis()
                )
                notes.add(0, note)
                adapter.notifyItemInserted(0)
                saveNotes()
                updateEmptyState()
                recyclerView.scrollToPosition(0)
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    private fun showEditDialog(note: Note) {
        val view = layoutInflater.inflate(R.layout.dialog_note, null)
        val titleEdit = view.findViewById<EditText>(R.id.editTitle)
        val contentEdit = view.findViewById<EditText>(R.id.editContent)
        titleEdit.setText(note.title)
        contentEdit.setText(note.content)

        AlertDialog.Builder(this)
            .setTitle("Редактировать заметку")
            .setView(view)
            .setPositiveButton("Сохранить") { _, _ ->
                note.title = titleEdit.text.toString().trim()
                note.content = contentEdit.text.toString().trim()
                val index = notes.indexOfFirst { it.id == note.id }
                if (index >= 0) {
                    adapter.notifyItemChanged(index)
                    saveNotes()
                }
            }
            .setNegativeButton("Отмена", null)
            .setNeutralButton("Удалить") { _, _ -> confirmDelete(note) }
            .show()
    }

    private fun confirmDelete(note: Note) {
        AlertDialog.Builder(this)
            .setTitle("Удалить заметку?")
            .setMessage("Это действие нельзя отменить")
            .setPositiveButton("Удалить") { _, _ ->
                val index = notes.indexOfFirst { it.id == note.id }
                if (index >= 0) {
                    notes.removeAt(index)
                    adapter.notifyItemRemoved(index)
                    saveNotes()
                    updateEmptyState()
                }
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    private fun saveNotes() {
        val jsonArray = JSONArray()
        notes.forEach { note ->
            val obj = JSONObject()
            obj.put("id", note.id)
            obj.put("title", note.title)
            obj.put("content", note.content)
            obj.put("createdAt", note.createdAt)
            jsonArray.put(obj)
        }
        prefs.edit().putString("notes", jsonArray.toString()).apply()
    }

    private fun loadNotes() {
        notes.clear()
        val json = prefs.getString("notes", "[]") ?: "[]"
        try {
            val jsonArray = JSONArray(json)
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                notes.add(
                    Note(
                        id = obj.getString("id"),
                        title = obj.getString("title"),
                        content = obj.getString("content"),
                        createdAt = obj.getLong("createdAt")
                    )
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        adapter.notifyDataSetChanged()
    }

    private fun updateEmptyState() {
        if (notes.isEmpty()) {
            emptyText.visibility = View.VISIBLE
            recyclerView.visibility = View.GONE
        } else {
            emptyText.visibility = View.GONE
            recyclerView.visibility = View.VISIBLE
        }
    }
}
