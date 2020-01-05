package com.penta.models

import org.fxmisc.richtext.model.StyleSpans
import org.fxmisc.richtext.model.StyleSpansBuilder
import java.util.*
import java.util.regex.Matcher

object AutoTypeRegex {

    private const val PAREN_PATTERN = "\\(|\\)"
    private const val BRACE_PATTERN = "\\{|\\}"
    private const val BRACKET_PATTERN = "\\[|\\]"
    private const val SEMICOLON_PATTERN = "\\;"
    private const val STRING_PATTERN = "\"([^\"\\\\]|\\\\.)*\""
    private const val COMMENT_PATTERN = "//[^\n]*|/\\*(.|\\R)*?\\*/"
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
    private val KEYWORD_PATTERN = "\\b(${KEYWORDS.joinToString("|")})\\b"
    val PATTERN = """   |(?<KEYWORD>$KEYWORD_PATTERN)|
                        |(?<PAREN>$PAREN_PATTERN)|
                        |(?<BRACE>$BRACE_PATTERN)|
                        |(?<BRACKET>$BRACKET_PATTERN)|
                        |(?<SEMICOLON>$SEMICOLON_PATTERN)|
                        |(?<STRING>$STRING_PATTERN)|
                        |(?<COMMENT>$COMMENT_PATTERN)"""
        .trimMargin()
        .toPattern()
    val STYLES = listOf("KEYWORD", "PAREN", "BRACE", "BRACKET", "SEMICOLON", "STRING", "COMMENT")

    fun computeHighlighting(text: String): StyleSpans<Collection<String>> {
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