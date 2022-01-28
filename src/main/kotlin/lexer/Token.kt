package lexer

const val UnaryPlus = -2
const val UnaryMinus = -1
const val Plus = 0
const val Minus = 1
const val Star = 2
const val Slash = 3
const val Modulo = 4

const val LessThan = 5
const val GreaterThan = 6
const val Assign = 7
const val Equal = 8
const val LessEqual = 9
const val GreaterEqual = 10
const val Bang = 11
const val BangEqual = 12

const val BitOr = 13
const val BitNot = 14
const val BitXor = 15
const val BitAnd = 16
const val LeftShift = 17
const val RightShift = 18
const val Or = 19
const val And = 20

const val LeftParen = 21
const val RightParen = 22
const val LeftBracket = 23
const val RightBracket = 24
const val RightBrace = 26
const val Comma = 27
const val Colon = 28
const val SemiColon = 51

const val LeftBrace = 25

// keywords
const val If = 29
const val Else = 30
const val Elif = 31
const val While = 32
const val For = 33
const val True = 34
const val False = 35
const val In = 39
const val Proc = 40
const val Var = 41

//const val Enum = 42
const val Const = 43
const val Struct = 44

const val Identifier = 45
const val Break = 46
const val Continue = 47
const val register = 48
const val Replacement = 49
const val Number = 0xFF00
const val Eof = 100

data class Token(var type: Int, val line: Int, val col: Int, val size: Int) {
    override fun toString(): String {
        var temp = ""
        temp = when (type) {
            UnaryPlus -> "(-)"
            UnaryMinus -> "(+)"
            Plus -> "+"
            Minus -> "-"
            Star -> "*"
            Slash -> "/"
            Modulo -> "%"
            LessThan -> "<"
            GreaterThan -> ">"
            LessEqual -> "<="
            GreaterEqual -> ">="
            Equal -> "=="
            Assign -> "'='"
            Bang -> "!"
            BangEqual -> "!="
            BitAnd -> "&"
            BitOr -> "|"
            BitXor -> "^"
            BitNot -> "~"
            And -> "and"
            Or -> "Or"
            LeftShift -> "<<"
            RightShift -> ">>"
            LeftParen -> "("
            RightParen -> ")"
            LeftBrace -> "{"
            RightBrace -> "}"
            LeftBracket -> "["
            RightBracket -> "]"
            Comma -> ","
            Colon -> ":"
            If -> "if"
            Elif -> "elif"
            Else -> "else"
            While -> "while"
            Proc -> "proc"
            Var -> "var"
            Identifier -> "Identifier"
            Eof -> "Eof"
            else -> {
                if (type > Number) {
                    "Number"
                } else {
                    error("Illegal type = $type in toString()")
                }
            }
        }
        return "Token(type = $temp, line = $line, col = $col, size = $size)"
    }
}