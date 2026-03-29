package com.abast.homebot

import android.app.Activity
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import java.util.*

class CalculatorActivity : AppCompatActivity() {

    private lateinit var tvExpression: TextView
    private lateinit var tvResult: TextView
    private var expression: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_calculator)

        tvExpression = findViewById(R.id.tvExpression)
        tvResult = findViewById(R.id.tvResult)

        // Dismiss on touch outside
        findViewById<View>(R.id.calculator_card).setOnTouchListener { _, _ -> true }
        findViewById<View>(android.R.id.content).setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_DOWN) {
                finish()
            }
            true
        }

        setupButtons()
    }

    private fun setupButtons() {
        val clickListener = View.OnClickListener { v ->
            if (v is Button) {
                val text = v.text.toString()
                when (text) {
                    "AC" -> clearAll()
                    "( )" -> handleParentheses()
                    "%" -> appendOperator("%")
                    "÷" -> appendOperator("/")
                    "×" -> appendOperator("*")
                    "−" -> appendOperator("-")
                    "+" -> appendOperator("+")
                    "=" -> calculateFinal()
                    else -> appendNumber(text)
                }
            }
        }

        val buttons = listOf(
            R.id.btn0, R.id.btn1, R.id.btn2, R.id.btn3, R.id.btn4, 
            R.id.btn5, R.id.btn6, R.id.btn7, R.id.btn8, R.id.btn9,
            R.id.btnDot, R.id.btnAC, R.id.btnParentheses, R.id.btnPercent,
            R.id.btnDiv, R.id.btnMul, R.id.btnSub, R.id.btnAdd, R.id.btnEquals
        )

        buttons.forEach { findViewById<View>(it).setOnClickListener(clickListener) }

        findViewById<View>(R.id.btnBack).setOnClickListener {
            if (expression.isNotEmpty()) {
                expression = expression.dropLast(1)
                updateDisplay()
            }
        }

        findViewById<View>(R.id.btnBack).setOnLongClickListener {
            clearAll()
            true
        }
    }

    private fun clearAll() {
        expression = ""
        updateDisplay()
        tvResult.text = "0"
    }

    private fun handleParentheses() {
        val openCount = expression.count { it == '(' }
        val closeCount = expression.count { it == ')' }
        if (openCount == closeCount || expression.lastOrNull() == '(') {
            expression += "("
        } else {
            expression += ")"
        }
        updateDisplay()
    }

    private fun appendNumber(num: String) {
        expression += num
        updateDisplay()
        calculateRealTime()
    }

    private fun appendOperator(op: String) {
        if (expression.isNotEmpty() && expression.last() in "0123456789).") {
            expression += op
            updateDisplay()
        }
    }

    private fun updateDisplay() {
        tvExpression.text = expression.replace("*", "×").replace("/", "÷").replace("-", "−")
    }

    private fun calculateRealTime() {
        val res = evaluateMath(expression)
        if (res != null && res != "Error") {
            tvResult.text = res
        }
    }

    private fun calculateFinal() {
        val res = evaluateMath(expression)
        if (res != null) {
            if (res == "Error") {
                Toast.makeText(this, "Invalid Expression", Toast.LENGTH_SHORT).show()
            } else {
                expression = res
                updateDisplay()
                tvResult.text = ""
            }
        }
    }

    private fun evaluateMath(query: String): String? {
        val clean = query.replace(" ", "")
        if (clean.isEmpty() || !clean.any { it in "0123456789" }) return null
        
        return try {
            object : Any() {
                var pos = -1
                var ch = 0

                fun nextChar() {
                    ch = if (++pos < clean.length) clean[pos].toInt() else -1
                }

                fun eat(charToEat: Int): Boolean {
                    while (Character.isWhitespace(ch)) nextChar()
                    if (ch == charToEat) {
                        nextChar()
                        return true
                    }
                    return false
                }

                fun parse(): Double {
                    nextChar()
                    val x = parseExpression()
                    if (pos < clean.length) return Double.NaN
                    return x
                }

                fun parseExpression(): Double {
                    var x = parseTerm()
                    while (true) {
                        if (eat('+'.toInt())) x += parseTerm()
                        else if (eat('-'.toInt())) x -= parseTerm()
                        else return x
                    }
                }

                fun parseTerm(): Double {
                    var x = parseFactor()
                    while (true) {
                        if (eat('*'.toInt())) x *= parseFactor()
                        else if (eat('/'.toInt())) x /= parseFactor()
                        else if (eat('%'.toInt())) x %= parseFactor()
                        else return x
                    }
                }

                fun parseFactor(): Double {
                    if (eat('+'.toInt())) return parseFactor()
                    if (eat('-'.toInt())) return -parseFactor()
                    var x: Double
                    val startPos = pos
                    if (eat('('.toInt())) {
                        x = parseExpression()
                        eat(')'.toInt())
                    } else if (ch >= '0'.toInt() && ch <= '9'.toInt() || ch == '.'.toInt()) {
                        while (ch >= '0'.toInt() && ch <= '9'.toInt() || ch == '.'.toInt()) nextChar()
                        x = clean.substring(startPos, pos).toDouble()
                    } else {
                        return Double.NaN
                    }
                    if (eat('^'.toInt())) x = Math.pow(x, parseFactor())
                    return x
                }
            }.parse().let {
                if (it.isInfinite() || it.isNaN()) "Error"
                else if (it == it.toLong().toDouble()) it.toLong().toString()
                else String.format("%.4f", it).trimEnd('0').trimEnd('.')
            }
        } catch (e: Exception) {
            null
        }
    }
}
