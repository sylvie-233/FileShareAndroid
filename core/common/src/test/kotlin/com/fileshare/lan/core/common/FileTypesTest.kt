package com.fileshare.lan.core.common

import org.junit.Assert.assertEquals
import org.junit.Test

class FileTypesTest {

    @Test
    fun `badge label from extension`() {
        assertEquals("PDF", fileBadgeLabel("a.pdf"))
        assertEquals("JPEG", fileBadgeLabel("photo.jpeg"))
        assertEquals("FILE", fileBadgeLabel("README"))
        assertEquals("FILE", fileBadgeLabel("archive.tar."))
    }

    @Test
    fun `tone mapping`() {
        assertEquals(FileTone.DOC, fileTone("report.docx"))
        assertEquals(FileTone.IMAGE, fileTone("photo.JPG"))
        assertEquals(FileTone.VIDEO, fileTone("movie.mp4"))
        assertEquals(FileTone.AUDIO, fileTone("song.flac"))
        assertEquals(FileTone.ARCHIVE, fileTone("app.apk"))
        assertEquals(FileTone.CODE, fileTone("Main.kt"))
        assertEquals(FileTone.OTHER, fileTone("unknown.xyz"))
    }
}
