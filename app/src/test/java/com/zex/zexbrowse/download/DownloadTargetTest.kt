/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.zex.zexbrowse.download

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertThrows
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class DownloadTargetTest {
    @get:Rule val temporaryFolder = TemporaryFolder()

    @Test fun `same requested name creates distinct files without changing the original`() {
        val directory = temporaryFolder.root
        val original = File(directory, "report.pdf").apply { writeText("original") }
        val first = createInternalTarget(directory, downloadTargetName("report.pdf", "first"))
        val second = createInternalTarget(directory, downloadTargetName("report.pdf", "second"))
        first.writeText("first download")
        second.delete()

        assertNotEquals(first, second)
        assertEquals("original", original.readText())
        assertEquals("first download", first.readText())
    }

    @Test fun `an existing generated name is never adopted or truncated`() {
        val directory = temporaryFolder.root
        val name = downloadTargetName("report.pdf", "id")
        val existing = File(directory, name).apply { writeText("keep") }

        assertThrows(IllegalStateException::class.java) { createInternalTarget(directory, name) }
        assertEquals("keep", existing.readText())
    }

    @Test fun `download names cannot escape the destination directory`() {
        assertEquals("report-id.pdf", downloadTargetName("../../report.pdf", "id"))
        assertEquals("report-id.pdf", downloadTargetName("folder\\report.pdf", "id"))
        assertEquals("download-id", downloadTargetName("..", "id"))
        assertEquals(".hidden-id", downloadTargetName(".hidden", "id"))
    }
}
