package com.zex.zexbrowse.download

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.nio.file.Files

class DownloadTargetTest {
    @get:Rule val temporary = TemporaryFolder()

    @Test fun acceptsOnlySingleFileNames() {
        listOf("report.pdf", "文件.zip", ".hidden", "a..b").forEach { assertTrue(it, DownloadTarget.isValidName(it)) }
        listOf("", " ", ".", "..", "../settings", "/absolute", "dir/file", "dir\\file", "bad\u0000name")
            .forEach { assertFalse(it, DownloadTarget.isValidName(it)) }
    }

    @Test fun rejectsEscapingNamesAndSavedTargets() {
        val root = temporary.newFolder("app")
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
        val root = temporary.newFolder("app")
        val downloads = File(root, "downloads").apply { mkdir() }
        val outside = File(root, "settings").apply { writeText("unchanged") }
        val link = File(downloads, "link")
        Files.createSymbolicLink(link.toPath(), outside.toPath())
        assertThrows(IllegalArgumentException::class.java) { DownloadTarget.create(downloads, "link") }
        assertThrows(IllegalArgumentException::class.java) { DownloadTarget.checkedFile(downloads, link) }
        assertEquals("unchanged", outside.readText())
    }
}
