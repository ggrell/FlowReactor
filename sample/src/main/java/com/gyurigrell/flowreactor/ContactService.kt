/*
 * Copyright (c) 2020, Gyuri Grell and RxReactor contributors. All rights reserved
 *
 * Licensed under BSD 3-Clause License.
 * https://opensource.org/licenses/BSD-3-Clause
 */

package com.gyurigrell.flowreactor

/**
 * Handle access to a list of contacts that will be used in the email text view
 */
interface ContactService {
    suspend fun loadEmails(): List<String>
}
