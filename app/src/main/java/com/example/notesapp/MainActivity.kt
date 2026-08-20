package com.example.notesapp

import android.content.Context
import android.content.Intent
import android.graphics.Paint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.TextView
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
    val createdAt: Long,
    var isCompleted: Boolean = false
)

class NotesAdapter(
    private val notes: MutableList<Note>,
    private val onEdit: (Note) -> Unit,
    private val onDelete: (Note) -> Unit,
    private val onToggleComplete: (Note) -> Unit
) : RecyclerView.Adapter<NotesAdapter.NoteViewHolder>() {

    class NoteViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val checkBox: CheckBox = view.findViewById(R.id.noteCheckBox)
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

        holder.checkBox.setOnCheckedChangeListener(null)
        holder.checkBox.isChecked = note.isCompleted
        holder.title.text = note.title.ifEmpty { "Без названия" }
        holder.content.text = note.content

        val sdf = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())
        holder.date.text = sdf.format(Date(note.createdAt))
        applyStrikethrough(holder, note.isCompleted)

        holder.checkBox.setOnCheckedChangeListener { _, isChecked ->
            if (note.isCompleted != isChecked) {
                note.isCompleted = isChecked
                applyStrikethrough(holder, isChecked)
                onToggleComplete(note)
            }
        }

        holder.itemView.setOnClickListener { onEdit(note) }
        holder.itemView.setOnLongClickListener {
            onDelete(note)
            true
        }
    }

    private fun applyStrikethrough(holder: NoteViewHolder, completed: Boolean) {
        if (completed) {
            holder.title.paintFlags = holder.title.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
            holder.content.paintFlags = holder.content.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
            holder.title.alpha = 0.55f
            holder.content.alpha = 0.55f
        } else {
            holder.title.paintFlags = holder.title.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
            holder.content.paintFlags = holder.content.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
            holder.title.alpha = 1f
            holder.content.alpha = 1f
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

        adapter = NotesAdapter(notes, ::openEditor, ::confirmDelete, ::onToggleComplete)
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter
        fab.setOnClickListener { createAndOpenNote() }

        loadNotes()
        updateEmptyState()
    }

    override fun onResume() {
        super.onResume()
        if (::adapter.isInitialized) {
            loadNotes()
            updateEmptyState()
        }
    }

    private fun createAndOpenNote() {
        val note = Note(
            id = UUID.randomUUID().toString(),
            title = "",
            content = "",
            createdAt = System.currentTimeMillis(),
            isCompleted = false
        )
        notes.add(0, note)
        adapter.notifyItemInserted(0)
        saveNotes()
        updateEmptyState()
        recyclerView.scrollToPosition(0)
        openEditor(note)
    }

    private fun openEditor(note: Note) {
        startActivity(
            Intent(this, NoteEditorActivity::class.java)
                .putExtra(NoteEditorActivity.EXTRA_NOTE_ID, note.id)
        )
    }

    private fun onToggleComplete(note: Note) = saveNotes()

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
            obj.put("isCompleted", note.isCompleted)
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
                val content = obj.optString("content", "")
                notes.add(
                    Note(
                        id = obj.getString("id"),
                        title = makeTitle(content),
                        content = content,
                        createdAt = obj.getLong("createdAt"),
                        isCompleted = obj.optBoolean("isCompleted", false)
                    )
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        saveNotes()
        adapter.notifyDataSetChanged()
    }

    private fun makeTitle(content: String): String {
        return content.trim()
            .split(Regex("\\s+"))
            .filter { it.isNotEmpty() }
            .take(3)
            .joinToString(" ")
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
