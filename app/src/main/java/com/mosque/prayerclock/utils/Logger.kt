package com.mosque.prayerclock.utils

import android.util.Log
import com.mosque.prayerclock.BuildConfig

/**
 * Centralized logging utility for the application.
 * 
 * In production builds (release), only errors and warnings are logged.
 * In debug builds, all log levels are available.
 * 
 * Usage:
 * Logger.d(TAG, "Debug message")
 * Logger.i(TAG, "Info message")
 * Logger.w(TAG, "Warning message")
 * Logger.e(TAG, "Error message", exception)
 */
object Logger {
    private const val MAX_TAG_LENGTH = 23 // Android log tag max length
    
    /**
     * Truncate tag if it exceeds Android's max length
     */
    private fun sanitizeTag(tag: String): String =
        if (tag.length > MAX_TAG_LENGTH) {
            tag.substring(0, MAX_TAG_LENGTH)
        } else {
            tag
        }
    
    /**
     * Debug log - only in debug builds
     */
    fun d(tag: String, message: String) {
        if (BuildConfig.DEBUG) {
            Log.d(sanitizeTag(tag), message)
        }
    }
    
    /**
     * Info log - only in debug builds
     */
    fun i(tag: String, message: String) {
        if (BuildConfig.DEBUG) {
            Log.i(sanitizeTag(tag), message)
        }
    }
    
    /**
     * Warning log - always logged
     */
    fun w(tag: String, message: String) {
        Log.w(sanitizeTag(tag), message)
    }
    
    /**
     * Warning log with exception - always logged
     */
    fun w(tag: String, message: String, throwable: Throwable) {
        Log.w(sanitizeTag(tag), message, throwable)
    }
    
    /**
     * Error log - always logged
     */
    fun e(tag: String, message: String) {
        Log.e(sanitizeTag(tag), message)
    }
    
    /**
     * Error log with exception - always logged
     */
    fun e(tag: String, message: String, throwable: Throwable) {
        Log.e(sanitizeTag(tag), message, throwable)
    }
    
    /**
     * Verbose log - only in debug builds with emojis
     */
    fun v(tag: String, message: String) {
        if (BuildConfig.DEBUG) {
            Log.v(sanitizeTag(tag), message)
        }
    }
}
