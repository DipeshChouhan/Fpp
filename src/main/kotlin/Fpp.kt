import compiler.CodeGeneration
import lexer.Lex
import lexer.Token
import parser.Parser
import java.io.File

data class FppData(val codeLines: List<String>, val codeGeneration: CodeGeneration, val tokens: List<Token>)
open class Fpp (private val filePath: String){
//    private val codeLines: List<String>
//
//    init {
//        val file = File(filePath)
//        if (file.exists()){
//            if (file.isFile) {
//                codeLines = file.readLines()
//            }else {
//                error("$filePath is not file.")
//            }
//        }else {
//            error("$filePath not exist.")
//        }
//    }

    fun compile() {
        val file = File(filePath)
        val codeLines: List<String>
        if (file.exists()){
            if (file.isFile) {
                codeLines = file.readLines()
            }else {
                error("$filePath is not file.")
            }
        }else {
            error("$filePath not exist.")
        }
        Parser(FppData(codeLines, CodeGeneration(), Lex(codeLines).lex())).parseStatements()
    }
}
