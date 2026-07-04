package com.example.util

import kotlin.math.*

object CalculatorEvaluator {

    fun evaluate(expression: String, isDegree: Boolean = true): Double {
        val cleaned = preprocess(expression)
        if (cleaned.isEmpty()) return 0.0
        
        return Parser(cleaned, isDegree).parse()
    }

    private fun preprocess(expr: String): String {
        var temp = expr.trim()
        
        // Standardize operators
        temp = temp.replace("×", "*")
        temp = temp.replace("÷", "/")
        temp = temp.replace("−", "-")
        temp = temp.replace("π", "PI")
        temp = temp.replace("e", "E")
        
        // Remove trailing operators repeatedly
        val operators = setOf('+', '-', '*', '/', '^')
        while (temp.isNotEmpty() && operators.contains(temp.last())) {
            temp = temp.substring(0, temp.length - 1).trim()
        }
        
        if (temp.isEmpty()) return ""
        
        // Balance parentheses
        var openCount = 0
        for (char in temp) {
            if (char == '(') openCount++
            else if (char == ')') openCount--
        }
        while (openCount > 0) {
            temp += ")"
            openCount--
        }
        
        return temp
    }

    private class Parser(val input: String, val isDegree: Boolean) {
        var pos = -1
        var ch = ' '

        fun nextChar() {
            pos++
            ch = if (pos < input.length) input[pos] else '\u0000'
        }

        fun eat(charToEat: Char): Boolean {
            while (ch == ' ') nextChar()
            if (ch == charToEat) {
                nextChar()
                return true
            }
            return false
        }

        fun parse(): Double {
            nextChar()
            val x = parseExpression()
            if (pos < input.length) throw RuntimeException("Unexpected character: $ch")
            return x
        }

        fun parseExpression(): Double {
            var x = parseTerm()
            while (true) {
                if (eat('+')) x += parseTerm()
                else if (eat('-')) x -= parseTerm()
                else break
            }
            return x
        }

        fun parseTerm(): Double {
            var x = parseFactor()
            while (true) {
                if (eat('*')) x *= parseFactor()
                else if (eat('/')) {
                    val divisor = parseFactor()
                    if (divisor == 0.0) throw ArithmeticException("Division by zero")
                    x /= divisor
                }
                else break
            }
            return x
        }

        fun parseFactor(): Double {
            var x = parseBase()
            while (eat('%')) {
                x /= 100.0
            }
            if (eat('^')) {
                x = x.pow(parseFactor())
            }
            return x
        }

        fun parseBase(): Double {
            if (eat('-')) return -parseBase()
            if (eat('+')) return parseBase()

            var x: Double
            val startPos = this.pos
            if (eat('(')) {
                x = parseExpression()
                eat(')')
            } else if (ch in '0'..'9' || ch == '.') {
                while (ch in '0'..'9' || ch == '.') nextChar()
                x = input.substring(startPos, this.pos).toDouble()
            } else if (ch in 'a'..'z' || ch in 'A'..'Z' || ch == '√') {
                var func = ""
                if (ch == '√') {
                    func = "sqrt"
                    nextChar()
                } else {
                    while (ch in 'a'..'z' || ch in 'A'..'Z' || ch in '0'..'9') {
                        func += ch
                        nextChar()
                    }
                }
                
                if (func == "PI") {
                    x = Math.PI
                } else if (func == "E") {
                    x = Math.E
                } else {
                    val hasParen = eat('(')
                    val arg = parseExpression()
                    if (hasParen) eat(')')
                    
                    x = when (func) {
                        "sqrt" -> sqrt(arg)
                        "sin" -> {
                            val rad = if (isDegree) Math.toRadians(arg) else arg
                            sin(rad)
                        }
                        "cos" -> {
                            val rad = if (isDegree) Math.toRadians(arg) else arg
                            cos(rad)
                        }
                        "tan" -> {
                            val rad = if (isDegree) Math.toRadians(arg) else arg
                            tan(rad)
                        }
                        "ln" -> ln(arg)
                        "log" -> log10(arg)
                        else -> throw RuntimeException("Unknown function: $func")
                    }
                }
            } else {
                throw RuntimeException("Unexpected character: $ch")
            }
            
            while (eat('%')) {
                x /= 100.0
            }
            
            return x
        }
    }
}
