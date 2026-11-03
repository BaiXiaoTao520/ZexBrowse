/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.zex.zexbrowse.download

import java.io.File

internal object DownloadTarget {
    fun isValidName(name: String): Boolean =
        name.isNotBlank() && name != "." && name != ".." &&
            '/' !in name && '\\' !in name && '\u0000' !in name

    fun create(directory: File, name: String): File {
        require(isValidName(name)) { "Invalid download filename" }
        return checkedFile(directory, File(directory, name))
    }

    // Saved URIs may have been created by an older version. Resolve symlinks and
    // require a direct child so they cannot read, overwrite or delete other files.
    fun checkedFile(directory: File, target: File): File {
        val canonicalDirectory = directory.canonicalFile
        val canonicalTarget = target.canonicalFile
        require(canonicalTarget.parentFile == canonicalDirectory) { "Invalid download target" }
        return canonicalTarget
    }
}
