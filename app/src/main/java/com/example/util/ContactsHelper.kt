package com.example.util

import android.content.Context
import android.net.Uri
import android.provider.ContactsContract

object ContactsHelper {
    data class ContactInfo(val name: String, val phone: String)

    fun retrieveContact(context: Context, contactUri: Uri): ContactInfo? {
        val contentResolver = context.contentResolver
        var name = ""
        var phone = ""

        try {
            contentResolver.query(contactUri, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val idIndex = cursor.getColumnIndex(ContactsContract.Contacts._ID)
                    val nameIndex = cursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME)
                    val hasPhoneIndex = cursor.getColumnIndex(ContactsContract.Contacts.HAS_PHONE_NUMBER)

                    if (nameIndex != -1) {
                        name = cursor.getString(nameIndex) ?: ""
                    }

                    val hasPhone = if (hasPhoneIndex != -1) cursor.getInt(hasPhoneIndex) else 0
                    val contactId = if (idIndex != -1) cursor.getString(idIndex) else ""

                    if (hasPhone > 0 && contactId.isNotEmpty()) {
                        contentResolver.query(
                            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                            null,
                            "${ContactsContract.CommonDataKinds.Phone.CONTACT_ID} = ?",
                            arrayOf(contactId),
                            null
                        )?.use { phoneCursor ->
                            if (phoneCursor.moveToFirst()) {
                                val pIndex = phoneCursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                                if (pIndex != -1) {
                                    phone = phoneCursor.getString(pIndex) ?: ""
                                }
                            }
                        }
                    }
                }
            }
        } catch (_: Exception) {
            // Permission or cursor read issue handled gracefully
        }

        return if (name.isNotBlank() || phone.isNotBlank()) ContactInfo(name, phone) else null
    }
}
