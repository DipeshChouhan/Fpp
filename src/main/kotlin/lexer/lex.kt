package lexer


class Lex(private val codeLines: List<String>) {
    private val tokens: MutableList<Token> = mutableListOf()
    private var line: Int = 0
    private var isError = false
    private var curLine:String = ""
    private var errorMsg = StringBuilder()
    private var col: Int = 0
    private val keywords = mapOf<String, Int>("true" to True, "false" to False,
    "or" to Or, "and" to And, "for" to For, "while" to While,
    "if" to If, "elif" to Elif, "else" to Else, "proc" to Proc, "in" to In,
    "const" to Const, "var" to Var, "struct" to Struct)

    private fun recover() {
        isError = true
        while(col < curLine.length && curLine[col] != ' ') {
            ++col
        }
    }


    private fun multilineComment() {
        var count = 1
        while(line < codeLines.size) {
            curLine = codeLines[line]
            while (col < curLine.length) {
                if (curLine[col] == '*' && (col + 1) < curLine.length && curLine[++col] == '/') {
                    --count
                }else if (curLine[col] == '/' && (col + 1) < curLine.length && curLine[++col] == '*') {
                    ++count
                }
                ++col
                if (count == 0) return
            }
            ++line
            if (line < codeLines.size) col = 0
        }

        if (count != 0) {
            isError = true
            printError("[$line:$col] - Syntax Error: end of multiline comment expected")
        }
    }

    private fun printError(msg: String) {

        errorMsg.appendLine(curLine)
        if (curLine.length < 100) {
            val ch = CharArray(col + 1) { ' ' }
            ch[col] = '|'
            errorMsg.appendLine(ch)
        }
        println(errorMsg.appendLine(msg).toString())
        errorMsg.delete(0, errorMsg.length)
    }

    private fun Char.isHexDigit(): Boolean {
        val ch = this.code or 0b00100000

        return ch in 48..57 || ch in 97..102
    }

    private fun captureNumber() {
        val start = col
        if (curLine[col] == '0') {
            ++col
            if (col == curLine.length) {
                if (!isError) tokens.add(Token(Number or 10, line, start, col - start))
                return
            }

            if (curLine[col] == 'b') {
                ++col
                while(col < curLine.length) {
                    if (curLine[col] == '0' || curLine[col] == '1') {
                        ++col
                        continue

                    }
                    if (col - start == 2) {
                        printError("[$line:$col] - Syntax Error: Binary digit expected but got '${curLine[col]}.")
                        recover()
                        return
                    }
                    if (curLine[col].isLetterOrDigit()) {
                        printError("[$line:$col] - Syntax Error: '${curLine[col]}' is not a binary digit.")
                        recover()
                        return
                    }
                    break
                }

                if (col - start == 2) {
                    printError("[$line:$col] - Syntax Error: Binary digit expected but got '${curLine[col]}'.")
                    recover()
                    return
                }

                if (!isError) tokens.add(Token(Number or 2, line, start + 2, col - (start + 2)))
                return

            }else if (curLine[col] == 'x'){
                ++col
                while(col < curLine.length) {
                    if (curLine[col].isHexDigit()) {
                        ++col
                        continue
                    }
                    if (col - start == 2) {
                        printError("[${line + 1}:$col] - Syntax Error: Hex digit expected but got '${curLine[col]}'.")
                        recover()
                        return
                    }
                    if (curLine[col].isLetter()) {
                        printError("[${line + 1}:$col] - Syntax Error: '${curLine[col]}' is not a hexadecimal digit.")
                        recover()
                        return
                    }
                    break
                }
                if (col - start == 2) {
                    printError("[${line + 1}:$col] - Syntax Error: Hex digit expected but got '${curLine[col]}'.")
                    recover()
                    return
                }

                if(!isError) tokens.add(Token(Number or 16, line, start + 2, col - (start + 2)))
                return
            }
        }
        while(col < curLine.length) {
            if (curLine[col].isDigit()) {
                ++col
                continue
            }
            if (curLine[col].isLetter()) {
                printError("[${line + 1}:$col] - Syntax Error: Decimal digit expected but got '${curLine[col]}'.")
                recover()
                return
            }
            break
        }
        if (!isError) tokens.add(Token(Number or 10, line, start, col - start))
    }

    private fun keywordOrIdentifier() {
        val start = col
        ++col
        while(col < curLine.length) {
            if (curLine[col].isLetterOrDigit() || curLine[col] == '_') {
                ++col
                continue
            }
            break
        }
        val type = keywords[curLine.substring(start, col)] ?: Identifier
        if (!isError) tokens.add(Token(type, line, start, col - start))
    }


    fun lex(): List<Token> {
        while(line < codeLines.size) {
            curLine = codeLines[line]
            var ch: Char
            col = 0
            while (col < curLine.length) {
                ch = curLine[col]
                when (ch) {
                    ' ' -> {
                        ++col
                        continue
                    }
                    ';' -> if (!isError) tokens.add(Token(SemiColon, line, col, 1))
                    '+' -> if(!isError) tokens.add(Token(Plus, line, col, 1))
                    '-' -> if(!isError) tokens.add(Token(Minus, line, col, 1))
                    '*' -> if(!isError) tokens.add(Token(Star, line, col, 1))
                    '/' -> {
                        ++col
                        if (col < curLine.length) {
                            if (curLine[col] == '/') {
                                ++col
                                while (col < curLine.length) {
                                    ++col
                                }
                                continue
                            }

                            if (curLine[col] == '*') {
                                ++col
                                multilineComment()
                                continue
                            }

                        }
                        if(!isError) tokens.add(Token(Slash, line, col, 1))
                        continue
                    }
                    '%' -> if(!isError) tokens.add(Token(Modulo, line, col, 1))

                    '(' -> if(!isError) tokens.add(Token(LeftParen,  line, col, 1))
                    ')' -> if(!isError) tokens.add(Token(RightParen,  line, col, 1))
                    '[' -> if(!isError) tokens.add(Token(LeftBracket,  line, col, 1))
                    ']' -> if(!isError) tokens.add(Token(RightBracket,  line, col, 1))
                    '{' -> if(!isError) tokens.add(Token(LeftBrace,  line, col, 1))
                    '}' -> if(!isError) tokens.add(Token(RightBrace,  line, col, 1))

                    ',' -> if(!isError) tokens.add(Token(Comma, line, col, 1))
                    ':' -> if (!isError) tokens.add(Token(Colon, line, col, 1))

                    '|' -> if(!isError) tokens.add(Token(BitOr, line, col, 1))
                    '&' -> if(!isError) tokens.add(Token(BitAnd, line, col, 1))
                    '^' -> if(!isError) tokens.add(Token(BitXor, line, col, 1))
                    '~' -> if(!isError) tokens.add(Token(BitNot, line, col, 1))

                    '=' -> {
                        if ((col + 1) < curLine.length) {
                            if (curLine[col + 1] == '=') {
                                if (!isError) tokens.add(Token(Equal, line, col, 2))
                                col += 2
                                continue
                            }
                        }
                        if (!isError) tokens.add(Token(Assign, line, col, 1))
                    }

                    '!' -> {
                        if ((col + 1) < curLine.length) {
                            if (curLine[col + 1] == '=') {
                                if (!isError) tokens.add(Token(BangEqual, line, col, 2))
                                col += 2
                                continue
                            }
                        }
                        if (!isError) tokens.add(Token(Bang, line, col, 1))
                    }

                    '>' -> {
                        if ((col + 1) < curLine.length) {
                            if (curLine[col + 1] == '=') {
                                if (!isError) tokens.add(Token(GreaterEqual, line, col, 2))
                                col += 2
                                continue
                            }else if(curLine[col + 1] == '>'){
                                if (!isError) tokens.add(Token(RightShift, line, col, 2))
                                col += 2
                                continue
                            }
                        }

                        if (!isError) tokens.add(Token(GreaterThan, line, col, 1))
                    }

                    '<' -> {
                        if ((col + 1) < curLine.length) {
                            if (curLine[col + 1] == '=') {
                                if (!isError) tokens.add(Token(LessEqual, line, col, 2))
                                col += 2
                                continue
                            }else if(curLine[col + 1] == '>'){
                                if (!isError) tokens.add(Token(LeftShift, line, col, 2))
                                col += 2
                                continue
                            }
                        }

                        if (!isError) tokens.add(Token(LessThan, line, col, 1))
                    }
                    else -> {
                        if (ch.isDigit()) {
                            captureNumber()
                            continue
                        }else if(ch.isLetter() || ch == '_') {
                            keywordOrIdentifier()
                            continue
                        }else {
                            // error
                            printError("[${line + 1}:$col] - Syntax Error: Illegal character '$ch'")
                            recover()
                        }
                    }
                }
                ++col
            }
            ++line
        }
        tokens.add(Token(Eof, line, col, 0))
        return tokens
    }
}