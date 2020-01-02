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

    private val skeleton by lazy { "(\\d+)(\\|*)( {1,3})?(.*)"
        .toRegex()
        .let { pattern ->
            Files.readAllLines(Paths.get(javaClass.getResource(input).toURI()))
                .mapIndexed { index, line ->
                    pattern.find(line)?.run { LineItem(index, groupValues[1].toInt(), groupValues[2].length, groupValues[4]) }!!
                }
        }
        .sortedBy { it.priority }
    }
    private val bones = mutableListOf<LineItem>()
    private val output = mutableListOf<String>()
    private val stringBuilder = StringBuilder()

    fun start() = flow<String> {
        var duplication = 0

        skeleton.forEach { currentLine ->
            output.clear()
            stringBuilder.clear()
            val task: CommonTask =  if (--duplication > 0) DuplicationTask() else NormalTask()

            val currentIndex = task.process(currentLine)
            task.print(currentLine, currentIndex, this)
            task.checkDuplication(currentLine)?.let { duplication = it }
        }
    }.flowOn(Dispatchers.IO)

    interface Task {

        fun process(currentLine: LineItem): Int

        suspend fun print(currentLine: LineItem, currentIndex: Int, flow: FlowCollector<String>)
    }

    abstract inner class CommonTask : Task {

        override fun process(currentLine: LineItem): Int {
            bones.sortBy { it.lineNumber }
            val currentIndex = bones.indexOf(currentLine)
            bones.filter { it != currentLine }.forEach { output += it.value }
            output.add(currentIndex, "")

            return currentIndex
        }

        fun checkDuplication(currentLine: LineItem): Int? {
            if (currentLine.duplication != 0) {
                repeat(currentLine.duplication - 1) {
                    bones += currentLine.copy(lineNumber = currentLine.lineNumber + it + 1)
                }
                bones.sortBy { it.lineNumber }
                return currentLine.duplication
            }
            return null
        }

    }

    inner class NormalTask : CommonTask() {

        override fun process(currentLine: LineItem): Int {
            bones += currentLine
            return super.process(currentLine)
        }

        override suspend fun print(currentLine: LineItem, currentIndex: Int, flow: FlowCollector<String>) {
            if (currentLine.value.trim().isEmpty()) { // if it's an empty line
                output[currentIndex] = "\n"
                flow.emit(output.joinToString("\n") { it })
            } else { // normal print
                currentLine.value.forEachIndexed { index, c ->
                    stringBuilder.insert(index, c)
                    output[currentIndex] = stringBuilder.toString()
                    flow.emit(output.joinToString("\n") { it })
                    delay(75)
                }
            }
        }
    }

    inner class DuplicationTask : CommonTask() {

        override fun process(currentLine: LineItem): Int {
            val itemIndex = bones.indexOf(currentLine)
            val removedLine = bones.removeAt(itemIndex)
            bones.add(itemIndex, currentLine)
            stringBuilder.append(removedLine.value)

            return super.process(currentLine)
        }

        override suspend fun print(currentLine: LineItem, currentIndex: Int, flow: FlowCollector<String>) {
            val copied = stringBuilder.toString()
            val filtered = currentLine.value
                .mapIndexed { index, c -> index to c }
                .filter{ (index, c) -> c != copied[index] }

            filtered.forEach { (index, c) ->
                stringBuilder[index] = c
                output[currentIndex] = stringBuilder.toString()
                flow.emit(output.joinToString("\n") { it })
                delay(75)
            }
            output[currentIndex] = currentLine.value
            flow.emit(output.joinToString("\n") { it })
        }
    }
}