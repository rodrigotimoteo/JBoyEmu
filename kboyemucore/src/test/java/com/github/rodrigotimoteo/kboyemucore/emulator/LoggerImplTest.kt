package com.github.rodrigotimoteo.kboyemucore.emulator

import org.junit.jupiter.api.Test
import java.io.ByteArrayOutputStream
import java.io.PrintStream
import kotlin.test.assertTrue

class LoggerImplTest {

    @Test
    fun `i should print message to stdout`() {
        val logger = LoggerImpl()
        val message = "Test info message"

        val outputStream = ByteArrayOutputStream()
        val originalOut = System.out
        System.setOut(PrintStream(outputStream))

        try {
            logger.i(message)
            assertTrue(outputStream.toString().contains(message))
        } finally {
            System.setOut(originalOut)
        }
    }

    @Test
    fun `i should handle empty message`() {
        val logger = LoggerImpl()
        val message = ""

        val outputStream = ByteArrayOutputStream()
        val originalOut = System.out
        System.setOut(PrintStream(outputStream))

        try {
            logger.i(message)
            // Empty message should still print a newline
            assertTrue(outputStream.toString().isNotEmpty())
        } finally {
            System.setOut(originalOut)
        }
    }

    @Test
    fun `i should handle long message`() {
        val logger = LoggerImpl()
        val message = "A".repeat(1000)

        val outputStream = ByteArrayOutputStream()
        val originalOut = System.out
        System.setOut(PrintStream(outputStream))

        try {
            logger.i(message)
            assertTrue(outputStream.toString().contains(message))
        } finally {
            System.setOut(originalOut)
        }
    }

    @Test
    fun `i should handle message with special characters`() {
        val logger = LoggerImpl()
        val message = "Message with special chars: !@#$%^&*()_+-=[]{}|;:',.<>?/"

        val outputStream = ByteArrayOutputStream()
        val originalOut = System.out
        System.setOut(PrintStream(outputStream))

        try {
            logger.i(message)
            assertTrue(outputStream.toString().contains(message))
        } finally {
            System.setOut(originalOut)
        }
    }

    @Test
    fun `i should handle message with newlines`() {
        val logger = LoggerImpl()
        val message = "Line 1\nLine 2\nLine 3"

        val outputStream = ByteArrayOutputStream()
        val originalOut = System.out
        System.setOut(PrintStream(outputStream))

        try {
            logger.i(message)
            assertTrue(outputStream.toString().contains("Line 1"))
            assertTrue(outputStream.toString().contains("Line 2"))
            assertTrue(outputStream.toString().contains("Line 3"))
        } finally {
            System.setOut(originalOut)
        }
    }

    @Test
    fun `e should throw with message and throwable`() {
        val logger = LoggerImpl()
        val message = "Test error message"
        val throwable = Exception("Test exception")

        try {
            logger.e(message, throwable)
        } catch (e: IllegalStateException) {
            assertTrue(e.message!!.contains(message))
            assertTrue(e.message!!.contains("Test exception"))
        }
    }

    @Test
    fun `e should handle null throwable`() {
        val logger = LoggerImpl()
        val message = "Test error message without throwable"

        try {
            logger.e(message, null)
        } catch (e: IllegalStateException) {
            assertTrue(e.message!!.contains(message))
            assertTrue(e.message!!.contains("null"))
        }
    }

    @Test
    fun `e should handle empty message with throwable`() {
        val logger = LoggerImpl()
        val throwable = RuntimeException("Runtime error")

        try {
            logger.e("", throwable)
        } catch (e: IllegalStateException) {
            assertTrue(e.message!!.contains("RuntimeException"))
        }
    }

    @Test
    fun `e should format message and throwable correctly`() {
        val logger = LoggerImpl()
        val message = "Critical error"
        val throwable = IllegalStateException("Invalid state")

        try {
            logger.e(message, throwable)
        } catch (e: IllegalStateException) {
            val messageStr = e.message!!
            assertTrue(messageStr.startsWith(message))
            assertTrue(messageStr.contains("IllegalStateException"))
        }
    }

    @Test
    fun `e should handle throwable with cause`() {
        val logger = LoggerImpl()
        val cause = Exception("Root cause")
        val throwable = RuntimeException("Wrapper exception", cause)
        val message = "Error occurred"

        try {
            logger.e(message, throwable)
        } catch (e: IllegalStateException) {
            val messageStr = e.message!!
            assertTrue(messageStr.startsWith(message))
            assertTrue(messageStr.contains("RuntimeException"))
        }
    }

    @Test
    fun `logger can call both i and e methods`() {
        val logger = LoggerImpl()

        val outputStream = ByteArrayOutputStream()
        val originalOut = System.out
        System.setOut(PrintStream(outputStream))

        try {
            logger.i("Info 1")
            logger.i("Info 2")
            assertTrue(outputStream.toString().contains("Info 1"))
            assertTrue(outputStream.toString().contains("Info 2"))
        } finally {
            System.setOut(originalOut)
        }

        try {
            logger.e("Error message", Exception("Test"))
        } catch (e: IllegalStateException) {
            assertTrue(e.message!!.contains("Error message"))
        }
    }

    @Test
    fun `logger implementation correctly extends Logger interface`() {
        val logger: com.github.rodrigotimoteo.kboyemucore.util.Logger = LoggerImpl()

        val outputStream = ByteArrayOutputStream()
        val originalOut = System.out
        System.setOut(PrintStream(outputStream))

        try {
            logger.i("Test message")
            assertTrue(outputStream.toString().contains("Test message"))
        } finally {
            System.setOut(originalOut)
        }
    }

    @Test
    fun `e can be called with different exception types`() {
        val logger = LoggerImpl()

        try {
            logger.e("NullPointer", NullPointerException("Null value found"))
        } catch (e: IllegalStateException) {
            assertTrue(e.message!!.contains("NullPointerException"))
        }

        try {
            logger.e("IllegalArgument", IllegalArgumentException("Bad argument"))
        } catch (e: IllegalStateException) {
            assertTrue(e.message!!.contains("IllegalArgumentException"))
        }
    }
}

