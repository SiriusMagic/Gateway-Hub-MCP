package com.zirakusa.gatewayhub.assistant

import android.content.Context
import android.net.Uri
import android.provider.ContactsContract
import android.provider.ContactsContract.Contacts
import android.provider.ContactsContract.CommonDataKinds.Phone

/** Remember the OS contact identity, never a cached number. */
internal object ContactAliases {
    fun picked(context: Context, uri: Uri): Uri? = runCatching {
        if (uri.scheme != "content" || uri.authority != ContactsContract.AUTHORITY) return null
        context.contentResolver.query(uri, arrayOf(Phone.CONTACT_ID, Phone.LOOKUP_KEY), null, null, null)?.use { row ->
            if (row.moveToFirst()) Contacts.getLookupUri(row.getLong(0), row.getString(1)) else null
        }
    }.getOrNull()

    fun resolve(context: Context, lookup: Uri): Person? = runCatching {
        val contact = Contacts.lookupContact(context.contentResolver, lookup) ?: return null
        val numbers = linkedMapOf<String, Person>()
        context.contentResolver.query(Phone.CONTENT_URI, arrayOf(Phone.DISPLAY_NAME_PRIMARY, Phone.NUMBER, Phone.IS_SUPER_PRIMARY),
            "${Phone.CONTACT_ID} = ?", arrayOf(contact.lastPathSegment), null)?.use { row ->
            while (row.moveToNext()) {
                val name = row.getString(0) ?: continue
                val phone = row.getString(1) ?: continue
                val person = Person(name, phone)
                if (row.getInt(2) == 1) return person
                numbers[phone.filter(Char::isDigit)] = person
            }
        }
        if (numbers.size > 1) throw Hands.AmbiguousContact()
        numbers.values.singleOrNull()
    }.getOrElse { if (it is Hands.AmbiguousContact) throw it else null }
}
