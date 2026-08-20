package com.example.notesapp

import android.content.Context
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.appbar.MaterialToolbar
import org.json.JSONArray
import org.json.JSONObject

class NoteEditorActivity : AppCompatActivity() {

    private lateinit var contentEdit: EditText
    private var noteId: String? = null
    private var initialContent = ""
    private val prefs by lazy { getSharedPreferences("notes_prefs", Context.MODE_PRIVATE) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_note_editor)

        val toolbar: MaterialToolbar = findViewById(R.id.editorToolbar)
        contentEdit = findViewById(R.id.noteEditor)
        noteId = intent.getStringExtra(EXTRA_NOTE_ID)

        toolbar.setNavigationOnClickListener { finish() }

        val note = noteId?.let(::findNote)
        if (note == null) {
            finish()
            return
        }

        initialContent = note.content
        contentEdit.setText(note.content)
        contentEdit.setSelection(contentEdit.text.length)

        contentEdit.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                saveCurrentNote(s?.toString().orEmpty())
            }
            override fun afterTextChanged(s: Editable?) = Unit
        })

        contentEdit.requestFocus()
        contentEdit.postDelayed({
            val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
            imm.showSoftInput(contentEdit, InputMethodManager.SHOW_IMPLICIT)
        }, 200)
    }

    override fun onPause() {
        saveCurrentNote(contentEdit.text.toString())
        super.onPause()
    }

    override fun finish() {
        val content = contentEditOrNull()?.text?.toString().orEmpty()
        if (content.isBlank() && initialContent.isBlank()) {
            deleteCurrentNote()
        } else {
            saveCurrentNote(content)
        }
        super.finish()
    }

    private fun contentEditOrNull(): EditText? = if (::contentEdit.isInitialized) contentEdit else null

    private fun saveCurrentNote(content: String) {
        val id = noteId ?: return
        val json = prefs.getString("notes", "[]") ?: "[]"
        val array = try { JSONArray(json) } catch (_: Exception) { JSONArray() }

        for (i in 0 until array.length()) {
            val obj = array.getJSONObject(i)
            if (obj.optString("id") == id) {
                obj.put("content", content)
                obj.put("title", makeTitle(content))
                array.put(i, obj)
                prefs.edit().putString("notes", array.toString()).apply()
                return
            }
        }
    }

    private fun findNote(id: String): Note? {
        val json = prefs.getString("notes", "[]") ?: "[]"
        return try {
            val array = JSONArray(json)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                if (obj.optString("id") == id) {
                    return Note(
                        id = obj.getString("id"),
                        title = obj.optString("title", ""),
                        content = obj.optString("content", ""),
                        createdAt = obj.getLong("createdAt"),
                        isCompleted = obj.optBoolean("isCompleted", false)
                    )
                }
            }
            null
        } catch (_: Exception) {
            null
        }
    }

    private fun deleteCurrentNote() {
        val id = noteId ?: return
        val json = prefs.getString("notes", "[]") ?: "[]"
        val array = try { JSONArray(json) } catch (_: Exception) { JSONArray() }
        val result = JSONArray()

        for (i in 0 until array.length()) {
            val obj = array.getJSONObject(i)
            if (obj.optString("id") != id) result.put(obj)
        }
        prefs.edit().putString("notes", result.toString()).apply()
    }

    private fun makeTitle(content: String): String {
        return content.trim()
            .split(Regex("\\s+"))
            .filter { it.isNotEmpty() }
            .take(3)
            .joinToString(" ")
    }

    companion object {
        const val EXTRA_NOTE_ID = "note_id"
    }
}
