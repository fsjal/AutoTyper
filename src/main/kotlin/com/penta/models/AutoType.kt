package com.penta.models

import com.penta.entities.LineItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import java.lang.StringBuilder
import java.nio.file.Files
import java.nio.file.Paths

@ExperimentalCoroutinesApi
class AutoType(private val input: String) {

    private val skeleton by lazy { "(\\d*)\\s?(.*)"
        .toRegex()
        .let { pattern ->
            Files
                .readAllLines(Paths.get(javaClass.getResource(input).toURI()))
                .mapIndexed { index, line ->
                    pattern
                        .find(line)
                        ?.run { LineItem(groupValues[1].toInt(), index, groupValues[2]) }!!
                }
        }
        .sortedBy { it.priority }
    }

    fun start() = flow {
        val bones = mutableListOf<LineItem>()
        // TODO add duplicate lines
        // TODO solve bug when no choreography number exists
        skeleton.forEach { currentLine ->
            val str = mutableListOf<String>()
            val s = StringBuilder()

            bones += currentLine
            bones.sortBy { it.lineNumber }
            val currentIndex = bones.indexOf(currentLine)
            bones.filter { it != currentLine }.forEach { str += it.value }
            str.add(currentIndex, "")
            currentLine.value.forEachIndexed { index, c ->
                s.insert(index, c)
                str[currentIndex] = s.toString()
                emit(str.joinToString("\n") { it })
                delay(50)
            }
        }
    }.flowOn(Dispatchers.IO)

}