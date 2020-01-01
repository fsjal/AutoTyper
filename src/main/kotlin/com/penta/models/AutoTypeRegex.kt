package com.penta.models

object AutoTypeRegex {

    const val PAREN_PATTERN = "\\(|\\)"
    const val BRACE_PATTERN = "\\{|\\}"
    const val BRACKET_PATTERN = "\\[|\\]"
    const val SEMICOLON_PATTERN = "\\;"
    const val STRING_PATTERN = "\"([^\"\\\\]|\\\\.)*\""
    const val COMMENT_PATTERN = "//[^\n]*|/\\*(.|\\R)*?\\*/"
    val KEYWORDS = arrayOf(
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
    val KEYWORD_PATTERN = "\\b(${KEYWORDS.joinToString("|")})\\b"
    val PATTERN = "(?<KEYWORD>$KEYWORD_PATTERN)|(?<PAREN>$PAREN_PATTERN)|(?<BRACE>$BRACE_PATTERN)|(?<BRACKET>$BRACKET_PATTERN)|(?<SEMICOLON>$SEMICOLON_PATTERN)|(?<STRING>$STRING_PATTERN)|(?<COMMENT>$COMMENT_PATTERN)".toPattern()
    val STYLES = listOf("KEYWORD", "PAREN", "BRACE", "BRACKET", "SEMICOLON", "STRING", "COMMENT")
}