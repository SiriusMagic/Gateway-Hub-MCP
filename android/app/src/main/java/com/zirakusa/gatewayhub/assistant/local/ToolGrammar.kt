package com.zirakusa.gatewayhub.assistant.local

/**
 * GBNF grammars for the local model (llama.cpp): what it writes can only be valid JSON with a real tool name, or Lucía's
 * orders. No made-up tools, no broken JSON, no chatter around it. Pure.
 */
internal object ToolGrammar {
    private const val JSON = """
object ::= "{" ws ( pair ( ws "," ws pair )* )? ws "}"
pair ::= string ws ":" ws value
value ::= object | array | string | number | "true" | "false" | "null"
array ::= "[" ws ( value ( ws "," ws value )* )? ws "]"
string ::= "\"" ( [^"\\\x7F\x00-\x1F] | "\\" ["\\/bfnrt] )* "\""
number ::= "-"? [0-9]+ ( "." [0-9]+ )?
ws ::= [ \t\n]*
"""

    /** {"tool":"<one of [names]>","arguments":{…}} or {} (nothing fits, or the request is already done). */
    fun forTools(names: List<String>): String {
        val safe = names.filter { it.matches(Regex("[A-Za-z0-9_./-]{1,128}")) }.distinct()
        if (safe.isEmpty()) return "root ::= \"{}\"\n"
        return "root ::= \"{}\" | \"{\" ws \"\\\"tool\\\"\" ws \":\" ws name ws \",\" ws \"\\\"arguments\\\"\" ws \":\" ws object ws \"}\"\n" +
            "name ::= " + safe.joinToString(" | ") { "\"\\\"$it\\\"\"" } + "\n" + JSON
    }

    /** {"ordenes":["…", …]}: Lucía's own orders, short strings only. */
    val orders: String = "root ::= \"{\" ws \"\\\"ordenes\\\"\" ws \":\" ws \"[\" ws ( string ( ws \",\" ws string )* )? ws \"]\" ws \"}\"\n" + JSON
}
