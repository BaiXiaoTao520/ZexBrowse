/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.zex.zexbrowse.download

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.nio.file.Files

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
        assertEquals(" report -id.pdf", downloadTargetName(" report .pdf", "id"))
    }

    @Test fun acceptsOnlySingleFileNames() {
        listOf("report.pdf", "文件.zip", ".hidden", "a..b").forEach { assertTrue(it, DownloadTarget.isValidName(it)) }
        listOf("", " ", ".", "..", "../settings", "/absolute", "dir/file", "dir\\file", "bad\u0000name")
            .forEach { assertFalse(it, DownloadTarget.isValidName(it)) }
    }

    @Test fun rejectsEscapingNamesAndSavedTargets() {
        val root = temporaryFolder.newFolder("app")
        val downloads = File(root, "downloads").apply { mkdir() }
        val safe = File(downloads, "file.zip")
        assertEquals(safe.canonicalFile, DownloadTarget.create(downloads, "file.zip"))
        assertEquals(safe.canonicalFile, DownloadTarget.checkedFile(downloads, safe))
        assertThrows(IllegalArgumentException::class.java) { DownloadTarget.create(downloads, "../settings") }
        assertThrows(IllegalArgumentException::class.java) { DownloadTarget.checkedFile(downloads, File(downloads, "../settings")) }
        assertThrows(IllegalArgumentException::class.java) { DownloadTarget.checkedFile(downloads, File(root, "downloads-other/file")) }
        assertThrows(IllegalArgumentException::class.java) { DownloadTarget.checkedFile(downloads, downloads) }
    }

    @Test fun rejectsSymlinksToFilesOutsideDownloads() {
        val root = temporaryFolder.newFolder("app")
        val downloads = File(root, "downloads").apply { mkdir() }
        val outside = File(root, "settings").apply { writeText("unchanged") }
        val link = File(downloads, "link")
        Files.createSymbolicLink(link.toPath(), outside.toPath())
        assertThrows(IllegalArgumentException::class.java) { DownloadTarget.create(downloads, "link") }
        assertThrows(IllegalArgumentException::class.java) { DownloadTarget.checkedFile(downloads, link) }
        assertEquals("unchanged", outside.readText())
    }

    @Test fun `internal creation does not adopt a dangling symlink`() {
        val downloads = temporaryFolder.newFolder("downloads")
        val target = File(downloads, "target")
        Files.createSymbolicLink(File(downloads, "link").toPath(), target.toPath())

        assertThrows(IllegalStateException::class.java) { createInternalTarget(downloads, "link") }
        assertFalse(target.exists())
    }
}
