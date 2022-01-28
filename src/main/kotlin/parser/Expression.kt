package parser

import lexer.*
import java.util.*
import kotlin.system.exitProcess
import lexer.Number as Number

// Precedence and Associativity table

// Change with project
/**
 * Precedence       Operator        Associativity
 *     1              +,- (unary)       Right-to-left
 *                    !, ~
 *
 *     2               *, /, %          Left-to-right
 *
 *     3               +, -             Left-to-right
 *
 *     4               <<, >>           Left-to-right
 *
 *     5               <, <=, >, >=     Left-to-right
 *
 *     6               ==, !=           Left-to-right
 *
 *     7                &               Left-to-right
 *
 *     8                ^               Left-to-right
 *
 *     9                |               Left-to-right
 *
 *     10               and              Left-to-right
 *
 *     11               or             Left-to-right
 *
 *     12               =               Right-to-left
 *
 *     13               ,               Left-to-right
 *
 */


const val RAX = 0
const val RBX = 1
const val PushRegister = 2
class Expression(private val parser: Parser) {
    data class Value(var type: Int, val token: Int, val value: Any)

    data class Precedence(val precedence: Int, val associativity: Int)
    private val leftAssociativity = 0
    private val rightAssociativity = 1
    private val opStack: Stack<Int> = Stack()


    private val precedenceAssociativity: Map<Int, Precedence> = mapOf(UnaryMinus to Precedence(1, rightAssociativity), UnaryPlus to Precedence(1, rightAssociativity),
        Bang to Precedence(1, rightAssociativity), BitNot to Precedence(1, rightAssociativity), RightShift to Precedence(4, leftAssociativity),
        LessThan to Precedence(5, leftAssociativity), GreaterThan to Precedence(5, leftAssociativity), LessEqual to Precedence(5, leftAssociativity),
        GreaterEqual to Precedence(5, leftAssociativity), Equal to Precedence(6, leftAssociativity), BangEqual to Precedence(6, leftAssociativity),
        BitAnd to Precedence(7, leftAssociativity), BitXor to Precedence(8, leftAssociativity), BitOr to Precedence(9, leftAssociativity),
        Star to Precedence(2, leftAssociativity), Slash to Precedence(2, leftAssociativity), LeftShift to Precedence(4, leftAssociativity),
        Modulo to Precedence(2, leftAssociativity), Plus to Precedence(3, leftAssociativity), Minus to Precedence(3, leftAssociativity),

        And to Precedence(10, leftAssociativity), Or to Precedence(11, leftAssociativity), Assign to Precedence(12, rightAssociativity))

    fun parse(constantExp: Boolean = false, scopeDepth: Int = 0){
        var unaryOp = true
        val output: MutableList<Value> = mutableListOf()
        while (parser.tk < parser.tokens.size) {
            val token = parser.tokens[parser.tk]
            if (token.type > Number) {
                unaryOp = false
                output.add(Value(Number, parser.tk, parser.tokenName(token).toLong(token.type and 0x00FF)))
            } else if (token.type == Identifier) {
                //
                unaryOp = false
                val identifier = parser.getIdentifier(scopeDepth, token)
                if (identifier != null) {
                    if (scopeDepth == 0 || identifier.scopeDepth + 1 == scopeDepth) {   // If it is global or in its own scope
                        output.add(Value(Identifier, parser.tk, identifier.address))
                    } else {
                        output.add(Value(Replacement, parser.tk, identifier))
                    }
                } else {
                    error("Identifier '${parser.tokenName(token)}' is not declared")
                }
            } else if (token.type in Plus..And) {
                // operator
                if ((token.type == Plus || token.type == Minus) && unaryOp) {
                    // detecting unary operators
                    token.type -= 2
                    unaryOp = false
                } else {
                    unaryOp = true
                }
                while (opStack.isNotEmpty() && parser.tokens[opStack.peek()].type != LeftParen) {
                    val op1 = precedenceAssociativity.getValue(token.type)
                    val op2 = precedenceAssociativity.getValue(parser.tokens[opStack.peek()].type)
                    if (op2.precedence < op1.precedence || ((op1.precedence == op2.precedence) && op1.associativity == leftAssociativity)) {    // lower precedence is higher
                        val operator = opStack.pop()
                        output.add(Value(parser.tokens[operator].type, operator, 0))
                        continue
                    }
                    break
                }

                opStack.push(parser.tk)
            } else if (token.type == LeftParen) {
                unaryOp = true
                opStack.push(parser.tk)
            } else if (token.type == RightParen) {
                unaryOp = false
                while (true) {
                    if (opStack.isEmpty()) {
                        // error
                        exitProcess(0)
                    }
                    if (parser.tokens[opStack.peek()].type != LeftParen) {
                        val operator = opStack.pop()
                        output.add(Value(parser.tokens[operator].type, operator, 0))
                        continue
                    }
                    opStack.pop()
                    break
                }
            } else {
                break
            }
            ++parser.tk
        }
        var operator: Int
        while (opStack.isNotEmpty()) {
            operator = opStack.pop()
            output.add(Value(parser.tokens[operator].type, operator, 0))
        }
//        output.forEach { println(parser.tokens[it.token]) }
        evaluate(output, scopeDepth)
    }

    // Evaluation of constant expression and generating assembly
    private  fun evaluate (output: MutableList<Value>, scopeDepth: Int) {
        // empty registers
        val registers = arrayOf(1, 1)
        val registerValues = arrayOf("rax", "rbx")
        val valueStack: Stack<Int> = Stack()

        var index = 0

        fun getRegister(value: Int): Int {
            var register: Int = output[value].type
            fun findEmptyRegister(): Int {
                if (registers[0] == 1) {
                    registers[0] = 0
                    return  RAX
                }
                if (registers[1] == 1) {
                    registers[1] = 0
                    return RBX
                }
                if (valueStack.isEmpty()) error("Illegal Expression!")
                val registerType = output[valueStack.peek()].type
                parser.assembly.pushRegister(registerValues[registerType])
                output[valueStack.peek()].type = PushRegister
                return registerType
            }

            when (output[value].type) {
                Number, Identifier  -> {
                    register = findEmptyRegister()
                    parser.assembly.mov(registerValues[register], output[value].value.toString())
                }
                Replacement -> {
                    register = findEmptyRegister()
                    output[value].type = scopeDepth - 1
                    parser.assembly.mov(registerValues[register], output[value])
                }
                PushRegister -> {
                    register = findEmptyRegister()
                    parser.assembly.popRegister(registerValues[register])
                }
            }
            return  register
        }
        while (index < output.size) {
            val valueType = output[index].type
            if (valueType == Number || valueType == Identifier || valueType == Replacement) {
                valueStack.push(index)
                ++index
                continue
            }
            if (valueStack.size < 1) error("Illegal expression")
            val v2 = valueStack.pop()
            val reg2: Int = getRegister(v2)
            if (valueType == UnaryPlus) {
                ++index
                continue
            }

            if (valueType == UnaryMinus) {
                parser.assembly.uMinus(registerValues[reg2])
                output[v2].type = reg2
                valueStack.push(v2)
                ++index
                continue
            }

            if (valueType == Bang) {
            }

            if (valueType == BitNot) {
                parser.assembly.bitwiseNot(registerValues[reg2])
                output[v2].type = reg2
                valueStack.push(v2)
                ++index
                continue
            }
            if (valueStack.size < 1) error("Illegal express")
            val v1 = valueStack.pop()
            val reg1: Int = getRegister(v1)
            when (valueType) {
                Plus -> {
                    parser.assembly.addRegisters(registerValues[reg1], registerValues[reg2])
                }
                Minus -> {
                    parser.assembly.subRegisters(registerValues[reg1], registerValues[reg2])
                }
                Star -> {
                    parser.assembly.signedMultiply(registerValues[reg1], registerValues[reg2])
                }
                BitOr -> {
                    parser.assembly.bitwiseOr(registerValues[reg1], registerValues[reg2])
                }
                BitAnd -> {
                    parser.assembly.bitwiseAnd(registerValues[reg1], registerValues[reg2])
                }
                BitXor -> {
                    parser.assembly.bitwiseXor(registerValues[reg1], registerValues[reg2])
                }
                And -> {
                    // TODO: Logical and
                    error("And is not implemented")
                }
                Or -> {
                    // TODO: Logical or
                    error("Or is not implemented")
                }
                LessThan -> {

                    error("< is not implemented")
                }
                GreaterThan -> {
                    error("> is not implemented")

                }
                LessEqual -> {
                    error("<= is not implemented")

                }
                GreaterEqual -> {
                    error(">= is not implemented")

                }
                Equal -> {

                    error("== is not implemented")
                }
                BangEqual -> {
                    error("!= is not implemented")

                }
                Assign -> {
                    error("= is not implemented")
                }
            }
            registers[reg2] = 1
            output[v1].type = reg1
            valueStack.push(v1)
            ++index
        }
        if (output[valueStack.peek()].type == Number) {
            parser.assembly.mov("rax", output[valueStack.peek()].value.toString())
        }

    }

}