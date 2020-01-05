package com.penta.views

import com.penta.models.AutoType
import com.penta.models.AutoTypeRegex.computeHighlighting
import javafx.scene.control.Tab
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collect
import org.fxmisc.flowless.VirtualizedScrollPane
import org.fxmisc.richtext.CodeArea
import org.fxmisc.richtext.LineNumberFactory
import java.io.File

class TypeTab(private val filePath: String) : Tab(){

    private val codeArea = CodeArea()

    init {
        codeArea.paragraphGraphicFactory = LineNumberFactory.get(codeArea)
        codeArea.multiPlainChanges().subscribe { codeArea.setStyleSpans(0, computeHighlighting(codeArea.text)) }
        codeArea.style = "-fx-font-size: ${PIXEL_SIZE}px"
        this.content = VirtualizedScrollPane(codeArea)
        this.text = File(javaClass.getResource(filePath).toURI()).name
    }

    @ExperimentalCoroutinesApi
    suspend fun start() {
        AutoType(filePath).start().collect { (_, str) ->
//            codeArea.scrollYToPixel(PIXEL_SIZE * lineIndex.toDouble())
            codeArea.replaceText(str)
        }
    }

    companion object {
        const val PIXEL_SIZE = 18
    }

}