package compiler

import parser.Expression
import parser.IdentifierInfo
import java.io.File
import kotlin.text.StringBuilder

data class Instruction(val opCode: String,  var destination: String, var source: String)
data class Replacement(val instruction: Instruction, val value: Expression.Value, val changeSource: Boolean = false)

class CodeGeneration() {
    private val startSection = StringBuilder()
    private val dataSection = StringBuilder()
    private var reg: String = "rdx"
    val instructions: ArrayList<Instruction> = ArrayList()
    val variableSizes = ArrayList<ArrayList<Int>>()
    private  val replacements: ArrayList<Replacement> = ArrayList()

    init {
        dataSection.appendLine("SECTION .data")
        startSection.appendLine(
            """
            SECTION .text
            global _start
            print:
                  mov     r9, -3689348814741910323
                  sub     rsp, 40
                  mov     BYTE [rsp+31], 10
                  lea     rcx, [rsp+30]
              .L2:
                  mov     rax, rdi
                  lea     r8, [rsp+32]
                  mul     r9
                  mov     rax, rdi
                  sub     r8, rcx
                  shr     rdx, 3
                  lea     rsi, [rdx+rdx*4]
                  add     rsi, rsi
                  sub     rax, rsi
                  add     eax, 48
                  mov     BYTE [rcx], al
                  mov     rax, rdi
                  mov     rdi, rdx
                  mov     rdx, rcx
                  sub     rcx, 1
                  cmp     rax, 9
                  ja      .L2
                  lea     rax, [rsp+32]
                  mov     edi, 1
                  sub     rdx, rax
                  xor     eax, eax
                  lea     rsi, [rsp+32+rdx]
                  mov     rdx, r8
                  mov     rax, 1
                  syscall
                  add     rsp, 40
                  ret
            _start:
        """.trimIndent()
        )
    }

    fun createGlobal(name: String, value: Long = 0) {
        dataSection.appendLine("$name: dq $value")
    }

    private fun resolveReplacements() {
        println(replacements.size)
        replacements.forEach {
            var fromIndex = (it.value.type - 1)
            val identifierInfo = it.value.value as IdentifierInfo
            val toIndex = identifierInfo.scopeDepth
            val index = identifierInfo.index
            var addr = 0

            while ((fromIndex != toIndex)) {
                addr += variableSizes[fromIndex--][0] + 8   // rbp
            }

            if (variableSizes[it.value.type].size == 1 && addr <= 8) {
                if (it.changeSource){
                    it.instruction.source = identifierInfo.address
                } else {
                    it.instruction.destination = identifierInfo.address
                }
            } else {
                var size = variableSizes[toIndex].size - 1
                while (size >= index) {
                    addr += variableSizes[toIndex][size--]
                }
                it.instruction.source = "[rbp + $addr]"
            }
        }
    }

    fun assignGlobal(name: String) {
        instructions.add(Instruction("mov", "[$name]", "rax"))
//        startSection.appendLine("mov [$name], rax")
    }

    fun pushRegister(register: String) {
        instructions.add(Instruction("push", register, ""))
    }

    fun popRegister(register: String) {
        instructions.add(Instruction("pop", register, ""))
    }

    fun assignLocal(address: Int) {
        instructions.add(Instruction("mov",  "[rbp - $address]", "rax"))
    }

    fun mov(destination: String, value: Expression.Value, isSource: Boolean = false) {

        val instruction = if (isSource) {
            Instruction("mov", "", destination)
        } else {
            Instruction("mov", destination, "")
        }
        instructions.add(instruction)
        replacements.add(Replacement(instruction, value, isSource))
    }

    fun mov(destination: String, source: String) {
        instructions.add(Instruction("mov", destination, source))
//        startSection.appendLine("mov $destination, $source")
    }

    fun addRegisters(destination: String, source: String) {
        instructions.add(Instruction("add", destination, source))
//        startSection.appendLine("add $destination, $source")
    }

    fun subRegisters(destination: String, source: String) {
        instructions.add(Instruction("sub", destination, source))
//        startSection.appendLine("sub $destination, $source")
    }

    fun uMinus(destination: String) {
        instructions.add(Instruction("neg", destination, ""))
//        startSection.appendLine("neg $destination")
    }

    fun signedMultiply(destination: String, source: String) {
        instructions.add(Instruction("imul", destination, source))

//        startSection.appendLine("imul $destination, $source")
    }

    fun bitwiseXor(destination: String, source: String) {
        instructions.add(Instruction("xor", destination, source))
//        startSection.appendLine("xor $destination, $source")
    }

    fun bitwiseAnd(destination: String, source: String) {
        instructions.add(Instruction("and", destination, source))
//        startSection.appendLine("and $destination, $source")
    }

    fun bitwiseOr(destination: String, source: String) {
        instructions.add(Instruction("or", destination, source))
//        startSection.appendLine("or $destination, $source")
    }

    fun bitwiseNot(destination: String) {
        instructions.add(Instruction("not", destination, ""))
//        startSection.appendLine("not $destination")
    }

    // In shifts instruction destination is actually CL register
    fun leftShift(destination: String, source: String) {
        // TODO: Left shift
    }
    fun rightShift(destination: String, source: String) {
        // TODO: Right shift
    }

    fun lessThan(destination: String, source: String){
        // TODO: <
    }
    fun greaterThan(destination: String, source: String){
        // TODO: >
    }
    fun lessEqual(destination: String, source: String){
        // TODO: <=
    }
    fun greaterEqual(destination: String, source: String){
        // TODO: >=
    }
    fun equals(destination: String, source: String){
        // TODO: ==
    }
    fun logicalAnd(destination: String, source: String) {
    }
    fun logicalOr(destination: String, source: String) {

    }
    fun logicalNot(destination: String) {

    }
    fun notEqual(destination: String, source: String) {

    }

    private fun appendInstructions() {
        instructions.forEach {
            if (it.source.isEmpty()) {
                startSection.appendLine("${it.opCode} ${it.destination}")
            } else {
                startSection.appendLine("${it.opCode} ${it.destination}, ${it.source}")
            }
        }
        instructions.clear()
    }

    fun generateForBlock() {
        resolveReplacements()
        variableSizes.clear()
        replacements.clear()
        appendInstructions()
    }

    fun generate() {
        if (instructions.isNotEmpty()) {
            appendInstructions()
        }
        startSection.appendLine(
            """
                mov rdi, rax
                call print
                mov rbx, 0  ; return 0 status on exit _ 'No Errors'
                mov rax, 1  ; invoke SYS_EXIT (kernel opcode 1)
                int 80h
        """.trimIndent()
        )
        dataSection.appendLine(startSection.toString())
        File("basic.asm").writeText(dataSection.toString())
    }
}