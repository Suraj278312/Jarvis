package com.example.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.graphics.Rect
import android.os.Bundle
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.example.data.model.ScreenInfo
import com.example.data.model.ScreenNodeInfo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class JarvisAccessibilityService : AccessibilityService() {

    companion object {
        private const val TAG = "JarvisA11yService"

        @Volatile
        private var instance: JarvisAccessibilityService? = null

        private val _isServiceActive = MutableStateFlow(false)
        val isServiceActive: StateFlow<Boolean> = _isServiceActive.asStateFlow()

        fun isEnabled(): Boolean = instance != null

        fun getInstance(): JarvisAccessibilityService? = instance
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        _isServiceActive.value = true
        Log.i(TAG, "JARVIS Accessibility Service Connected.")

        val info = serviceInfo ?: AccessibilityServiceInfo()
        info.eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED or
                AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED or
                AccessibilityEvent.TYPE_VIEW_CLICKED or
                AccessibilityEvent.TYPE_VIEW_FOCUSED
        info.feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
        info.flags = AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS or
                AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS
        serviceInfo = info
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // Events can be logged or monitored for context awareness
    }

    override fun onInterrupt() {
        Log.w(TAG, "JARVIS Accessibility Service interrupted")
    }

    override fun onDestroy() {
        super.onDestroy()
        if (instance == this) {
            instance = null
            _isServiceActive.value = false
        }
        Log.i(TAG, "JARVIS Accessibility Service destroyed")
    }

    // --- Screen Reading & Inspection ---

    fun inspectCurrentScreen(): ScreenInfo {
        val root = rootInActiveWindow ?: return ScreenInfo(
            packageName = null,
            nodes = emptyList(),
            readableText = "No active screen window accessible.",
            clickableElements = emptyList()
        )

        val nodeList = mutableListOf<ScreenNodeInfo>()
        val textSnippets = mutableListOf<String>()
        val clickableList = mutableListOf<String>()
        val currentPackage = root.packageName?.toString()

        fun traverse(node: AccessibilityNodeInfo?) {
            if (node == null) return

            val bounds = Rect()
            node.getBoundsInScreen(bounds)
            val text = node.text?.toString()?.trim()
            val desc = node.contentDescription?.toString()?.trim()
            val viewId = node.viewIdResourceName
            val className = node.className?.toString()

            val info = ScreenNodeInfo(
                text = text,
                contentDescription = desc,
                className = className,
                viewId = viewId,
                isClickable = node.isClickable,
                isEditable = node.isEditable,
                isScrollable = node.isScrollable,
                bounds = bounds
            )
            nodeList.add(info)

            if (!text.isNullOrBlank()) {
                textSnippets.add(text)
            } else if (!desc.isNullOrBlank()) {
                textSnippets.add(desc)
            }

            if (node.isClickable) {
                val label = text ?: desc ?: viewId
                if (!label.isNullOrBlank()) {
                    clickableList.add(label)
                }
            }

            for (i in 0 until node.childCount) {
                traverse(node.getChild(i))
            }
        }

        try {
            traverse(root)
        } catch (e: Exception) {
            Log.e(TAG, "Error inspecting screen hierarchy", e)
        }

        val fullText = textSnippets.distinct().joinToString("\n")
        return ScreenInfo(
            packageName = currentPackage,
            nodes = nodeList,
            readableText = fullText.ifEmpty { "Screen appears to have no readable text elements." },
            clickableElements = clickableList.distinct()
        )
    }

    // --- Screen Interactions ---

    fun clickElement(target: String): Boolean {
        val root = rootInActiveWindow ?: return false
        val lowerTarget = target.lowercase().trim()

        fun findAndClick(node: AccessibilityNodeInfo?): Boolean {
            if (node == null) return false

            val text = node.text?.toString()?.lowercase() ?: ""
            val desc = node.contentDescription?.toString()?.lowercase() ?: ""
            val viewId = node.viewIdResourceName?.lowercase() ?: ""

            val matches = text.contains(lowerTarget) ||
                    desc.contains(lowerTarget) ||
                    viewId.contains(lowerTarget)

            if (matches) {
                var clickTarget: AccessibilityNodeInfo? = node
                while (clickTarget != null && !clickTarget.isClickable) {
                    clickTarget = clickTarget.parent
                }
                if (clickTarget != null && clickTarget.isClickable) {
                    val clicked = clickTarget.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                    Log.i(TAG, "Clicked element matching '$target': $clicked")
                    return clicked
                }
            }

            for (i in 0 until node.childCount) {
                if (findAndClick(node.getChild(i))) return true
            }
            return false
        }

        return findAndClick(root)
    }

    fun typeText(textToType: String, fieldHint: String? = null): Boolean {
        val root = rootInActiveWindow ?: return false

        fun findEditable(node: AccessibilityNodeInfo?): AccessibilityNodeInfo? {
            if (node == null) return null
            if (node.isEditable) {
                if (fieldHint.isNullOrBlank()) return node
                val text = node.text?.toString()?.lowercase() ?: ""
                val desc = node.contentDescription?.toString()?.lowercase() ?: ""
                if (text.contains(fieldHint.lowercase()) || desc.contains(fieldHint.lowercase())) {
                    return node
                }
            }
            for (i in 0 until node.childCount) {
                val found = findEditable(node.getChild(i))
                if (found != null) return found
            }
            return null
        }

        val targetField = findEditable(root) ?: root.findFocus(AccessibilityNodeInfo.FOCUS_INPUT)
        if (targetField != null) {
            targetField.performAction(AccessibilityNodeInfo.ACTION_FOCUS)
            val arguments = Bundle().apply {
                putCharSequence(
                    AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE,
                    textToType
                )
            }
            val result = targetField.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, arguments)
            Log.i(TAG, "Typed text '$textToType': $result")
            return result
        }
        return false
    }

    fun scroll(forward: Boolean): Boolean {
        val root = rootInActiveWindow ?: return false

        fun findScrollable(node: AccessibilityNodeInfo?): AccessibilityNodeInfo? {
            if (node == null) return null
            if (node.isScrollable) return node
            for (i in 0 until node.childCount) {
                val found = findScrollable(node.getChild(i))
                if (found != null) return found
            }
            return null
        }

        val scrollNode = findScrollable(root)
        return if (scrollNode != null) {
            val action = if (forward) AccessibilityNodeInfo.ACTION_SCROLL_FORWARD else AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD
            val success = scrollNode.performAction(action)
            Log.i(TAG, "Scroll forward=$forward: $success")
            success
        } else {
            false
        }
    }

    fun navigateGlobal(actionType: String): Boolean {
        val globalAction = when (actionType.lowercase()) {
            "back" -> GLOBAL_ACTION_BACK
            "home" -> GLOBAL_ACTION_HOME
            "recents" -> GLOBAL_ACTION_RECENTS
            "notifications" -> GLOBAL_ACTION_NOTIFICATIONS
            else -> GLOBAL_ACTION_BACK
        }
        val result = performGlobalAction(globalAction)
        Log.i(TAG, "Performed global action '$actionType': $result")
        return result
    }
}
