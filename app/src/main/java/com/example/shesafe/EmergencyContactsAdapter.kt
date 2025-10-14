package com.example.shesafe

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class EmergencyContactsAdapter(
    private val contacts: MutableList<Contact>,
    private val onDeleteClick: (Contact) -> Unit
) : RecyclerView.Adapter<EmergencyContactsAdapter.ContactViewHolder>() {

    inner class ContactViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val nameText: TextView = itemView.findViewById(R.id.contactName)
        val numberText: TextView = itemView.findViewById(R.id.contactNumber)
        val deleteBtn: ImageButton = itemView.findViewById(R.id.deleteButton)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ContactViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.contact_item, parent, false)
        return ContactViewHolder(view)
    }

    override fun onBindViewHolder(holder: ContactViewHolder, position: Int) {
        val contact = contacts[position]
        holder.nameText.text = contact.name
        holder.numberText.text = contact.number
        holder.deleteBtn.setOnClickListener {
            onDeleteClick(contact)
        }
    }

    override fun getItemCount(): Int = contacts.size
}