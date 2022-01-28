package parser
import FppData
import lexer.*

const val global = 0
const val local = 1




data class IdentifierInfo(val scopeDepth: Int, val tk: Int, val address: String, val index: Int = 0)


class Parser(fppData: FppData) {
    var tk = 0
    val tokens = fppData.tokens
    private var isError = false
    private val codeLines = fppData.codeLines
    val assembly = fppData.codeGeneration
    private val expression = Expression(this)
    private val statements = Statements(this)
    private val errors = mutableMapOf<Int, String>()
    val identifierTables = mutableListOf<MutableMap<String, IdentifierInfo>>(mutableMapOf())
    private val constants = mutableMapOf<String, Any>()
    fun token(): Token {
        return tokens[tk]
    }

    fun nextToken(): Token {
        return tokens[++tk]
    }

    fun searchIdentifier(scopeDepth: Int, index: Int): Boolean {
        var depth = scopeDepth
        val key = tokenName(index)
        while(depth >= 0) {
            if (identifierTables[depth].containsKey(key)) {
                return true
            }
            --depth
        }
        return constants.containsKey(key)
    }

    fun getIdentifier(scopeDepth: Int, token: Token): IdentifierInfo? {
        var depth = scopeDepth
        val key = tokenName(token)
        var identifier: IdentifierInfo?
        while (depth >= 0){
            identifier = identifierTables[depth][key]
            if (identifier != null) {
                return identifier
            }
            --depth
        }
        return null
    }


    fun searchIdentifierInScope(scopeDepth: Int, index: Int): Boolean {
        return identifierTables[scopeDepth].containsKey(tokenName(index))
    }

    fun tokenName(index: Int = tk): String {
        return codeLines[tokens[index].line].substring(tokens[index].col, tokens[index].size + tokens[index].col)
    }

    fun tokenName(token: Token): String {
        return codeLines[token.line].substring(token.col, token.size + token.col)
    }

    fun recover(insideBlock: Boolean) {
        var type: Int
        while (tk < tokens.size - 1) {
            type = tokens[tk].type
            if (type in If..Struct || type == LeftBrace ||
                (insideBlock && type == RightBrace)) {
                break
            }
            ++tk
        }
    }


    fun errorMsg(tokenNumber: Int, msg: String) {
        if (!isError) isError = true
        errors[tokenNumber] = msg
    }



    private fun printErrors() {
        errors.forEach {
            println("${tokens[it.key].line}:${tokens[it.key].col} - ${it.value}")
        }
    }

    fun nextTokenType (): Int {
        return tokens[tk + 1].type
    }

    fun tokenType(): Int {
        return tokens[tk].type
    }

    fun parseStatements() {
//        tokens.forEach { println(it)}
        statements.parse()
        if (isError) {
            printErrors()
            return
        }
        assembly.generate()
    }
    fun parseExpression(constExpression: Boolean = false, scopeDepth: Int = 0) {
        expression.parse(constExpression, scopeDepth)
    }
}