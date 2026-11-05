package com.zex.zexbrowse.download

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class UpdateFilesTest {
    @get:Rule val files = TemporaryFolder()

    @Test fun updateAndTemporaryFilesStayInInternalUpdatesDirectory() {
        val directory = File(files.root, "updates").canonicalFile
        for (name in listOf("ZexBrowse-12.0.apk", "../outside.apk", "a/../../outside.apk", "..\\outside.apk", ".", "..", "")) {
            val (destination, temporary) = updateFiles(files.root, name)
            assertEquals(directory, destination.canonicalFile.parentFile)
            assertEquals(directory, temporary.canonicalFile.parentFile)
            assertNotEquals(destination, temporary)
        }
        assertEquals("ZexBrowse-12.0.apk", updateFiles(files.root, "ZexBrowse-12.0.apk").first.name)
    }
}
