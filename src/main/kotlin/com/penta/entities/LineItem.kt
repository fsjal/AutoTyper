package com.penta.entities

data class LineItem(val lineNumber: Int, val priority: Int, val duplication: Int, val value: String) {

    override fun equals(other: Any?) = other is LineItem && other.priority == priority

    override fun hashCode() = 31 * priority + 31 * duplication + value.hashCode()
}