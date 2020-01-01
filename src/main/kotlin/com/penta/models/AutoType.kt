package com.penta.models

import com.penta.entities.LineItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import java.lang.StringBuilder
import java.nio.file.Files
import java.nio.file.Paths

@ExperimentalCoroutinesApi
class AutoType(private val input: String) {

    private val skeleton by lazy { "(\\d+)(\\|*)( {0,3})?(.*)"
        .toRegex()
        .let { pattern ->
            Files.readAllLines(Paths.get(javaClass.getResource(input).toURI()))
                .mapIndexed { index, line ->
                    pattern.find(line)?.run { LineItem(index, groupValues[1].toInt(), groupValues[2].length, groupValues[4]) }!!
                }
        }
        .sortedBy { it.priority }
    }

    fun start() = flow {
        val bones = mutableListOf<LineItem>()
        var duplication = 0

        skeleton.forEach { currentLine ->
            val str = mutableListOf<String>()
            val s = StringBuilder()

            if (--duplication > 0) {
                val itemIndex = bones.indexOf(currentLine)
                val line = bones.removeAt(itemIndex)
                bones.add(itemIndex, currentLine)
                s.append(line.value)
            } else {
                bones += currentLine
            }
            bones.sortBy { it.lineNumber }
            val currentIndex = bones.indexOf(currentLine)
            bones.filter { it != currentLine }.forEach { str += it.value }
            str.add(currentIndex, "")
            if (s.isEmpty()) {
                if (currentLine.value.trim().isEmpty()) {
                    str[currentIndex] = "\n"
                    emit(str.joinToString("\n") { it })
                } else {
                    currentLine.value.forEachIndexed { index, c ->
                        s.insert(index, c)
                        str[currentIndex] = s.toString()
                        emit(str.joinToString("\n") { it })
                        delay(75)
                    }
                }
            } else {
                val copied = s.toString()
                val filtered = currentLine.value
                    .mapIndexed { index, c -> index to c }
                    .filter{ (index, c) -> c != copied[index] }

                filtered.forEach { (index, c) ->
                    s[index] = c
                    str[currentIndex] = s.toString()
                    emit(str.joinToString("\n") { it })
                    delay(75)
                }
                str[currentIndex] = currentLine.value
                emit(str.joinToString("\n") { it })
            }
            if (currentLine.duplication != 0) {
                duplication = currentLine.duplication
                repeat(currentLine.duplication - 1) { bones += currentLine.copy(lineNumber = currentLine.lineNumber + it + 1) }
                bones.sortBy { it.lineNumber }
            }
        }
    }
}