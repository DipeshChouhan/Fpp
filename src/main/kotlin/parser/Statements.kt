// TODO: New statments
package parser

import lexer.*
class Statements(private val parser: Parser) {
    private var scopeDepth = 0
    private var insideLoop = false
    fun parse() {
        while (parser.tk < parser.tokens.size) {
            when (parser.token().type) {
                Var -> varDeclaration()
                LeftBrace -> block()
                If -> ifStatement()
                Identifier -> {
                    when (parser.nextToken().type) {
                        LeftParen -> functionCall()
                        Assign -> varAssignment()
                        Eof -> {
                            parser.errorMsg(parser.tk - 1, "Unexpected end of file.")
                        }
                        else -> {
                            parser.errorMsg(parser.tk, "Did you mean '(' or '='")
                            parser.recover(scopeDepth > 0)
                        }
                    }
                }
                While -> whileStatement()
                Proc -> functionDefinition()
                Const -> constStatement()
                Struct -> structDeclaration()
                Eof -> break
                else -> {
                    val type = parser.tokenType()
                    if (scopeDepth > 0) {
                        if (insideLoop) {
                            if (type == Continue) {
                                // generate for continue
                            } else if (type == Break) {

                            }
                        }

                        if (type == RightBrace) break
                    }
                    parser.errorMsg(parser.tk++, "Token is not a statement.")
                    parser.recover(scopeDepth > 0)
                }
            }
        }
    }

    private fun varDeclaration() {
        when (parser.nextTokenType()) {
            Identifier  -> {
                // It's identifier
                ++parser.tk
                if (parser.searchIdentifierInScope(scopeDepth, parser.tk)) {
                    parser.errorMsg(parser.tk, "Variable already declared.")
                    parser.recover(scopeDepth > 0)
                    return
                }
                val name = parser.tokenName()

                when (parser.nextTokenType()) {
                    Assign -> {
                        // It's '=' of form var <name> = expression
                        parser.tk += 2
                        parser.parseExpression(false, scopeDepth)
                    }
                    else -> {
                        // var <identifier> ; valid
                        ++parser.tk
                    }
                }

                if (parser.tokenType() != SemiColon) {
                    parser.errorMsg(parser.tk, "; expected")
                    parser.recover(scopeDepth > 0)
                } else {
                    ++parser.tk
                }
                if (scopeDepth == 0) {
                    parser.assembly.createGlobal(name)
                    parser.identifierTables[scopeDepth][name] = IdentifierInfo(global, tk = parser.tk - 2, "[$name]")
                    parser.assembly.assignGlobal(name)
                    return
                }
//                        parser.assembly.assignLocal(address)
                var varSize = 8
                val varList = parser.assembly.variableSizes[scopeDepth - 1]
                varList.add(varSize)
                varSize += varList[0]
                varList[0] = varSize
                parser.assembly.assignLocal(varSize)
                parser.identifierTables[scopeDepth][name] = IdentifierInfo(scopeDepth - 1, tk = parser.tk - 2, "[rbp - $varSize]", varList.size - 1)
            }

            Eof -> {
                // End of file
                parser.errorMsg(parser.tk++, "Unexpected end of file. Identifier expected")
            }
            else -> {
                // Not an identifier
                parser.errorMsg(++parser.tk, "Unexpected token. Identifier expected")
                parser.recover(scopeDepth > 0)
            }
        }
    }

    private fun varAssignment() {
        val identifier = parser.getIdentifier(scopeDepth, parser.tokens[parser.tk - 1])
        val tokenNumber = parser.tk - 1
        if (identifier != null) {
            ++parser.tk
            parser.parseExpression(false, scopeDepth)
            if (scopeDepth == 0 || scopeDepth == identifier.scopeDepth + 1) {
                parser.assembly.mov(identifier.address, "rax")
            } else {
                parser.assembly.mov("rax", Expression.Value(scopeDepth - 1, tokenNumber, identifier))
            }
            if (parser.tokenType() != SemiColon) {
                parser.errorMsg(parser.tk, "; expected")
                parser.recover(scopeDepth > 0)
            } else {
                ++parser.tk
            }


        } else {
            parser.errorMsg(parser.tk - 1, "Variable '${parser.tokenName(parser.tk - 1)}' is not declared.")
            parser.recover(scopeDepth > 0)
        }
    }

    private fun block() {
        parser.assembly.variableSizes.add(scopeDepth, arrayListOf(0))
        ++scopeDepth
        parser.identifierTables.add(scopeDepth, mutableMapOf())
        ++parser.tk
        val index = parser.assembly.instructions.size
        parser.assembly.pushRegister("rbp")
        parser.assembly.mov("rbp", "rsp")
        parser.assembly.subRegisters("rsp", "")
        parse()
        if (parser.token().type == Eof) {
            parser.errorMsg(parser.tk, "Unexpected end of file. '}' expected")
        }
        parser.identifierTables.drop(scopeDepth)
        if (parser.assembly.variableSizes[scopeDepth - 1][0] != 0) {
            parser.assembly.instructions[index + 2].source = parser.assembly.variableSizes[scopeDepth - 1][0].toString()
            parser.assembly.mov("rsp", "rbp")
            parser.assembly.popRegister("rbp")
        } else {
            parser.assembly.instructions.removeAt(index)
            parser.assembly.instructions.removeAt(index)
            parser.assembly.instructions.removeAt(index)
        }
        --scopeDepth
        if (scopeDepth == 0) {  // end of a parent block
            parser.assembly.generateForBlock()
        }
        ++parser.tk
    }

    private fun elifStatement(){
        do {
            ++parser.tk
            parser.parseExpression()
            if (parser.token().type == LeftBrace) {
                block()
            } else {
                parser.errorMsg(parser.tk, "Unexpected Token '{' expected")
                parser.recover(true)
            }
        } while (parser.token().type == Elif)

        if (parser.token().type == Else) {
            elseStatement()
        }
    }

    private fun elseStatement(){
        ++parser.tk
        if (parser.token().type == LeftBrace) {
            block()
        } else {
            parser.errorMsg(parser.tk, "Unexpected Token '{' expected")
            parser.recover(true)
        }
    }

    private fun ifStatement() {
        ++parser.tk
        parser.parseExpression()
        if (parser.token().type == LeftBrace){
            block()
        }else {
            parser.errorMsg(parser.tk, "Unexpected Token '{' expected")
            parser.recover(true)
        }

        when (parser.token().type) {
            Elif -> {
               elifStatement()
            }
            Else -> {
                elseStatement()
            }
            else -> {
                return
            }
        }
    }

    private fun whileStatement() {
        ++parser.tk
        parser.parseExpression()
        if (parser.token().type == LeftBrace){
            block()
        }else {
            parser.errorMsg(parser.tk, "Unexpected Token '{' expected")
            parser.recover(true)
        }
    }

    private fun functionDefinition(){
    }

    private fun constStatement(){
        ++parser.tk
        if (parser.token().type != LeftBrace) {
            parser.errorMsg(parser.tk, "Unexpected token '{' expected.")
            parser.recover(true)
            return
        }
    }

    private fun structDeclaration(){

    }

    private fun functionCall(){

    }
}