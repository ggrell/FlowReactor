/*
 * Copyright (c) 2020, Gyuri Grell and RxReactor contributors. All rights reserved
 *
 * Licensed under BSD 3-Clause License.
 * https://opensource.org/licenses/BSD-3-Clause
 */

package com.gyurigrell.flowreactor

import android.annotation.SuppressLint
import android.app.Activity
import android.net.Uri
import android.provider.ContactsContract.CommonDataKinds
import android.provider.ContactsContract.Contacts
import android.provider.ContactsContract.Profile

class ContactServiceImpl(
    private val context: Activity
) : ContactService {
    override suspend fun loadEmails(): List<String> {
        @SuppressLint("Recycle") // cursor.use will close the cursor
        val cursor = context.contentResolver.query(
            Uri.withAppendedPath(Profile.CONTENT_URI, Contacts.Data.CONTENT_DIRECTORY),
            arrayOf(CommonDataKinds.Email.ADDRESS, CommonDataKinds.Email.IS_PRIMARY),
            Contacts.Data.MIMETYPE + " = ?",
            arrayOf(CommonDataKinds.Email.CONTENT_ITEM_TYPE),
            Contacts.Data.IS_PRIMARY + " DESC"
        ) ?: return emptyList()

        cursor.use {
            val emails = mutableListOf<String>()
            while (cursor.moveToNext()) {
                emails.add(cursor.getString(0))
            }
            return emails.apply { addAll(listOf("anEmail@example.com", "anotherEmail@example.com", "android@example.com")) }
        }
    }
}
