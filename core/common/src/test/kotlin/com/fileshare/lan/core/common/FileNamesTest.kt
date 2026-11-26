package com.fileshare.lan.core.common

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class FileNamesTest {

    @Test
    fun `sanitize strips path prefix`() {
        assertEquals("report.pdf", sanitizeFileName("../../etc/report.pdf"))
        assertEquals("a.txt", sanitizeFileName("C:\\Users\\me\\a.txt"))
    }

    @Test
    fun `sanitize rejects empty and dot names`() {
        assertNull(sanitizeFileName(null))
        assertNull(sanitizeFileName("   "))
        assertNull(sanitizeFileName(".."))
        assertNull(sanitizeFileName("."))
    }

    @Test
    fun `sanitize hides leading dots`() {
        assertEquals("_.gitignore", sanitizeFileName(".gitignore"))
    }

    @Test
    fun `numbered file name keeps extension`() {
        assertEquals("photo.png", numberedFileName("photo.png", 0))
        assertEquals("photo (1).png", numberedFileName("photo.png", 1))
        assertEquals("README (2)", numberedFileName("README", 2))
    }

    @Test
    fun `safe file name rejects traversal`() {
        assertEquals(false, isSafeFileName("../secret.txt"))
        assertEquals(false, isSafeFileName("a/b.txt"))
        assertEquals(false, isSafeFileName("a\\b.txt"))
        assertEquals(false, isSafeFileName(".hidden"))
        assertEquals(true, isSafeFileName("正常 文件.txt"))
    }

    @Test
    fun `content disposition encodes utf8 file names`() {
        val header = contentDisposition("测试 文档.pdf")
        assertEquals(true, header.contains("filename*=UTF-8''%E6%B5%8B%E8%AF%95%20%E6%96%87%E6%A1%A3.pdf"))
        assertEquals(true, header.startsWith("attachment; filename="))
    }

    @Test
    fun `repair latin1 mojibake from multipart headers`() {
        val original = "测试 文档.txt"
        val mojibake = String(original.toByteArray(Charsets.UTF_8), Charsets.ISO_8859_1)
        assertEquals(original, repairUtf8Mojibake(mojibake))
        assertEquals(original, sanitizeFileName(mojibake))
        assertEquals(original, repairUtf8Mojibake(original))
        assertEquals("café.txt", repairUtf8Mojibake("café.txt"))
    }
}
