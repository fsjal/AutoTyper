package com.penta.views

import javafx.application.Application
import javafx.application.Platform
import javafx.event.Event
import javafx.scene.Scene
import javafx.scene.input.KeyCode
import javafx.scene.input.KeyEvent
import javafx.scene.layout.StackPane
import javafx.stage.Stage
import kotlinx.coroutines.*
import kotlinx.coroutines.javafx.JavaFx
import org.fxmisc.flowless.VirtualizedScrollPane
import org.fxmisc.richtext.CodeArea
import org.fxmisc.richtext.LineNumberFactory
import org.fxmisc.richtext.model.PlainTextChange
import org.fxmisc.richtext.model.StyleSpans
import org.fxmisc.richtext.model.StyleSpansBuilder
import org.reactfx.Subscription
import java.lang.StringBuilder
import java.nio.file.Files
import java.nio.file.Paths
import java.util.*
import java.util.regex.Matcher
import java.util.regex.Pattern

class AutoTyperApp : Application(), CoroutineScope {

    private val KEYWORDS = arrayOf(
        "abstract", "assert", "boolean", "break", "byte",
        "case", "catch", "char", "class", "const",
        "continue", "default", "do", "double", "else",
        "enum", "extends", "final", "finally", "float",
        "for", "goto", "if", "implements", "import",
        "instanceof", "int", "interface", "long", "native",
        "new", "package", "private", "protected", "public",
        "return", "short", "static", "strictfp", "super",
        "switch", "synchronized", "this", "throw", "throws",
        "transient", "try", "void", "volatile", "while",
        "include", "using", "namespace"
    )
    private val KEYWORD_PATTERN = "\\b(" + java.lang.String.join("|", *KEYWORDS) + ")\\b"
    private val PAREN_PATTERN = "\\(|\\)"
    private val BRACE_PATTERN = "\\{|\\}"
    private val BRACKET_PATTERN = "\\[|\\]"
    private val SEMICOLON_PATTERN = "\\;"
    private val STRING_PATTERN = "\"([^\"\\\\]|\\\\.)*\""
    private val COMMENT_PATTERN = "//[^\n]*" + "|" + "/\\*(.|\\R)*?\\*/"

    private val PATTERN: Pattern = Pattern.compile(
        "(?<KEYWORD>" + KEYWORD_PATTERN + ")"
                + "|(?<PAREN>" + PAREN_PATTERN + ")"
                + "|(?<BRACE>" + BRACE_PATTERN + ")"
                + "|(?<BRACKET>" + BRACKET_PATTERN + ")"
                + "|(?<SEMICOLON>" + SEMICOLON_PATTERN + ")"
                + "|(?<STRING>" + STRING_PATTERN + ")"
                + "|(?<COMMENT>" + COMMENT_PATTERN + ")"
    )
    private val codeArea = CodeArea()

    override fun start(primaryStage: Stage) {
        // add line numbers to the left of area
        codeArea.paragraphGraphicFactory = LineNumberFactory.get(codeArea)
        // recompute the syntax highlighting 500 ms after user stops editing area
        val cleanupWhenNoLongerNeedIt: Subscription =
            codeArea // plain changes = ignore style changes that are emitted when syntax highlighting is reapplied
// multi plain changes = save computation by not rerunning the code multiple times
//   when making multiple changes (e.g. renaming a method at multiple parts in file)
                .multiPlainChanges() // do not emit an event until 500 ms have passed since the last emission of previous stream
                //.successionEnds(Duration.ofMillis(500)) // run the following code block when previous stream emits an event
                .subscribe { ignore: List<PlainTextChange?>? ->
                    codeArea.setStyleSpans(
                        0,
                        computeHighlighting(codeArea.text)
                    )
                }
        // when no longer need syntax highlighting and wish to clean up memory leaks
// run: `cleanupWhenNoLongerNeedIt.unsubscribe();`
// auto-indent: insert previous line's indents on enter
        val whiteSpace: Pattern = Pattern.compile("^\\s+")
        codeArea.addEventHandler(KeyEvent.KEY_PRESSED) { KE: Event ->
            if (KE.target == KeyCode.ENTER) {
                val caretPosition = codeArea.caretPosition
                val currentParagraph = codeArea.currentParagraph
                val m0: Matcher =
                    whiteSpace.matcher(codeArea.getParagraph(currentParagraph - 1).segments[0])
                if (m0.find()) Platform.runLater { codeArea.insertText(caretPosition, m0.group()) }
            }
        }
        //codeArea.replaceText(0, 0, sampleCode)
        codeArea.style = "-fx-font-size: 1.5em;"
        val scene = Scene(StackPane(VirtualizedScrollPane(codeArea)), 1280.0, 720.0)
        scene.stylesheets.add(javaClass.getResource("/css/java-keywords.css").toExternalForm())
        primaryStage.scene = scene
        primaryStage.title = "Java Keywords Demo"
        primaryStage.show()
        run()
    }

    // todo add duplicate lines
    private fun run() {
        val texts = Files.readAllLines(Paths.get(javaClass.getResource("/input.data").toURI()))
        // todo solve bug when no choreography number exists
        val pattern = "(\\d*)\\s?(.*)".toRegex()
        val queue = PriorityQueue<TypeItem>(texts.size, Comparator<TypeItem> { a, b -> when {
            a.priority > b.priority -> 1
            a.priority < b.priority -> -1
            else -> 0
        }})
        texts.forEachIndexed { index, value ->
            val matcher = pattern.find(value)
            matcher?.run {
                queue.add(TypeItem(groupValues[1].toInt(), index, groupValues[2]))
            }
        }
        launch {
            delay(5000)
            val bones = mutableListOf<TypeItem>()

            while (queue.isNotEmpty()) {
                val str = mutableListOf<String>()
                val currentLine = queue.poll()
                bones += currentLine
                bones.sortBy { it.lineNumber }
                val currentIndex = bones.indexOf(currentLine)
                val s = StringBuilder()
                bones.filter { it != currentLine }.forEach { str += it.value }
                str.add(currentIndex, "")
                currentLine.value.forEachIndexed { index, c ->
                    s.insert(index, c)
                    str[currentIndex] = s.toString()
                    codeArea.replaceText(str.joinToString("\n") { it })
                    delay(50)
                }
            }
        }
    }

    private fun computeHighlighting(text: String): StyleSpans<Collection<String>> {
        val matcher: Matcher = PATTERN.matcher(text)
        var lastKwEnd = 0
        val spansBuilder =
            StyleSpansBuilder<Collection<String>>()
        while (matcher.find()) {
            val styleClass =
                (if (matcher.group("KEYWORD") != null) "keyword" else if (matcher.group("PAREN") != null) "paren" else if (matcher.group(
                        "BRACE"
                    ) != null
                ) "brace" else if (matcher.group("BRACKET") != null) "bracket" else if (matcher.group("SEMICOLON") != null) "semicolon" else if (matcher.group(
                        "STRING"
                    ) != null
                ) "string" else if (matcher.group("COMMENT") != null) "comment" else null)!! /* never happens */
            spansBuilder.add(Collections.emptyList(), matcher.start() - lastKwEnd)
            spansBuilder.add(Collections.singleton(styleClass), matcher.end() - matcher.start())
            lastKwEnd = matcher.end()
        }
        spansBuilder.add(Collections.emptyList(), text.length - lastKwEnd)
        return spansBuilder.create()
    }

    override val coroutineContext = Dispatchers.JavaFx
}

data class TypeItem(val priority: Int, val lineNumber: Int, val value: String)
