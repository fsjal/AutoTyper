package com.penta.views

import com.penta.models.AutoType
import com.penta.models.AutoTypeRegex.PATTERN
import com.penta.models.AutoTypeRegex.STYLES
import javafx.application.Application
import javafx.scene.Scene
import javafx.scene.layout.StackPane
import javafx.stage.Stage
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.javafx.JavaFx
import org.fxmisc.flowless.VirtualizedScrollPane
import org.fxmisc.richtext.CodeArea
import org.fxmisc.richtext.LineNumberFactory
import org.fxmisc.richtext.model.StyleSpans
import org.fxmisc.richtext.model.StyleSpansBuilder
import java.util.*
import java.util.regex.Matcher

@ExperimentalCoroutinesApi
class AutoTypeApp : Application(), CoroutineScope {

    private val job = Job()
    override val coroutineContext = Dispatchers.JavaFx + job
    private val codeArea = CodeArea()

    override fun start(primaryStage: Stage) {
        codeArea.paragraphGraphicFactory = LineNumberFactory.get(codeArea)
        codeArea.multiPlainChanges().subscribe { codeArea.setStyleSpans(0, computeHighlighting(codeArea.text)) }
        codeArea.style = "-fx-font-size: 4em;"
        val scene = Scene(StackPane(VirtualizedScrollPane(codeArea)), 1280.0, 720.0)
        scene.stylesheets.add(javaClass.getResource("/css/java-keywords.css").toExternalForm())
        primaryStage.scene = scene
        primaryStage.title = "AutoType"
        primaryStage.show()
        primaryStage.setOnCloseRequest { job.cancel() }
        launch {
            //delay(5000L)
            AutoType("/input.data").start().collect { codeArea.replaceText(it) }
        }
    }

    private fun computeHighlighting(text: String): StyleSpans<Collection<String>> {
        val matcher: Matcher = PATTERN.matcher(text)
        val spansBuilder = StyleSpansBuilder<Collection<String>>()
        var lastKwEnd = 0

        while (matcher.find()) {
            val styleClass = STYLES.first { matcher.group(it) != null }.toLowerCase()

            spansBuilder.add(Collections.emptyList(), matcher.start() - lastKwEnd)
            spansBuilder.add(Collections.singleton(styleClass), matcher.end() - matcher.start())
            lastKwEnd = matcher.end()
        }
        spansBuilder.add(Collections.emptyList(), text.length - lastKwEnd)

        return spansBuilder.create()
    }
}
