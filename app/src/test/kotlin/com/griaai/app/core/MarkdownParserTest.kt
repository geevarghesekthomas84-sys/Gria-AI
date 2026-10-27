package com.griaai.app.core.components

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MarkdownParserTest {

    @Test
    fun testParseParagraphAndHeaders() {
        val markdown = """
            # Heading 1
            ## Heading 2
            ### Heading 3
            This is a normal paragraph.
            - Bullet 1
            - Bullet 2
        """.trimIndent()

        val blocks = MarkdownParser.parse(markdown)
        assertEquals(6, blocks.size)
        assertTrue(blocks[0] is MarkdownBlock.Header && (blocks[0] as MarkdownBlock.Header).level == 1)
        assertTrue(blocks[1] is MarkdownBlock.Header && (blocks[1] as MarkdownBlock.Header).level == 2)
        assertTrue(blocks[2] is MarkdownBlock.Header && (blocks[2] as MarkdownBlock.Header).level == 3)
        assertTrue(blocks[3] is MarkdownBlock.Paragraph)
        assertTrue(blocks[4] is MarkdownBlock.BulletItem)
        assertTrue(blocks[5] is MarkdownBlock.BulletItem)
    }

    @Test
    fun testParseCodeBlock() {
        val markdown = """
            Here is some code:
            ```kotlin
            val message = "Hello, world!"
            println(message)
            ```
            That's it!
        """.trimIndent()

        val blocks = MarkdownParser.parse(markdown)
        assertEquals(3, blocks.size)
        assertTrue(blocks[0] is MarkdownBlock.Paragraph)
        assertTrue(blocks[1] is MarkdownBlock.Code)
        val codeBlock = blocks[1] as MarkdownBlock.Code
        assertEquals("kotlin", codeBlock.language)
        assertTrue(codeBlock.code.contains("val message = \"Hello, world!\""))
        assertTrue(blocks[2] is MarkdownBlock.Paragraph)
    }
}
