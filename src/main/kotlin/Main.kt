// TODO: Do better error in parsing. Remove String Messages
// TODO: Local variables {Today Jan 16 2022}
private const val Version = "0.0.0"


private fun cmd (args: Array<String>) {
    val usage = """
            Usage:
                    fpp <command> [arguments]
                    
            The commands are:
            
                    run         compile and run Fpp program
                    version     print Fpp version
        """.trimIndent()

    if (args.isEmpty()) {
        println("Fpp is compiled stack based programming language.\n\n$usage")
    }else {
        when (args[0]) {
            "version" -> println(Version)
            "run" -> {
                if (args.size < 2) {
                    println("No files to compile and run.")
                    return
                }
                // compile
                // returns function for optimizations
                Fpp(args[1]).compile()
            }

            else -> {
                println("Command '${args[0]}' not supported.\n\n$usage")
            }
        }
    }


}

fun main(args: Array<String>) {
    cmd(args)
}