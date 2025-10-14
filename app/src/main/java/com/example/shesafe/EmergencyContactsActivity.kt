package com.example.shesafe

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class EmergencyContactsActivity : AppCompatActivity() {
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: EmergencyContactsAdapter
    private lateinit var addContactBtn: Button
    private var contactList = mutableListOf<Contact>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_emergency_contacts)

        recyclerView = findViewById(R.id.contactsRecyclerView)
        addContactBtn = findViewById(R.id.addContactBtn)
        recyclerView.layoutManager = LinearLayoutManager(this)

        contactList = loadContacts()

        adapter = EmergencyContactsAdapter(contactList) { contact ->
            deleteContact(contact)
        }

        recyclerView.adapter = adapter

        addContactBtn.setOnClickListener {
            showAddContactDialog()
        }
    }

    private fun showAddContactDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_contact, null)
        val nameInput = dialogView.findViewById<EditText>(R.id.nameEditText)
        val numberInput = dialogView.findViewById<EditText>(R.id.numberEditText)

        AlertDialog.Builder(this)
            .setTitle("Add Contact")
            .setView(dialogView)
            .setPositiveButton("Save") { _, _ ->
                val name = nameInput.text.toString().trim()
                val number = numberInput.text.toString().trim()
                if (name.isNotEmpty() && number.isNotEmpty()) {
                    val newContact = Contact(name, number)
                    contactList.add(newContact)
                    adapter.notifyItemInserted(contactList.size - 1)
                    saveContacts()
                } else {
                    Toast.makeText(this, "Please enter valid details", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun loadContacts(): MutableList<Contact> {
        val prefs = getSharedPreferences("emergency_contacts", MODE_PRIVATE)
        val stored = prefs.getString("contacts", "") ?: ""
        return stored.split(",")
            .mapNotNull {
                val parts = it.split(":")
                if (parts.size == 2) Contact(parts[0], parts[1]) else null
            }.toMutableList()
    }

    private fun saveContacts() {
        val contactString = contactList.joinToString(",") { "${it.name}:${it.number}" }
        getSharedPreferences("emergency_contacts", MODE_PRIVATE)
            .edit()
            .putString("contacts", contactString)
            .apply()
    }

    private fun deleteContact(contact: Contact) {
        val index = contactList.indexOf(contact)
        if (index != -1) {
            contactList.removeAt(index)
            adapter.notifyItemRemoved(index)
            saveContacts()
            Toast.makeText(this, "${contact.name} removed", Toast.LENGTH_SHORT).show()
        }
    }
}
