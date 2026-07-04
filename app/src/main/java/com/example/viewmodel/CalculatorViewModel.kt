package com.example.viewmodel

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.CalculatorDatabase
import com.example.data.HistoryEntry
import com.example.data.HistoryRepository
import com.example.util.CalculatorEvaluator
import com.example.util.UnitConverter
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

class CalculatorViewModel(
    application: Application,
    private val repository: HistoryRepository
) : AndroidViewModel(application) {

    // Tab state: "Simple", "Scientific", "Converter"
    var activeTab by mutableStateOf("Simple")
        private set

    // Calculator state
    var expression by mutableStateOf("")
        private set
    
    var lastExpression by mutableStateOf("")
        private set

    var result by mutableStateOf("0")
        private set

    var isDegree by mutableStateOf(true)
        private set

    var isInverse by mutableStateOf(false)
        private set

    var isLedActive by mutableStateOf(false)
        private set

    private var isEvaluationComplete = false

    // Converter state
    var converterCategory by mutableStateOf(UnitConverter.Category.CURRENCY)
        private set

    var fromUnit by mutableStateOf("USD")
    var toUnit by mutableStateOf("INR")

    var fromValueStr by mutableStateOf("100")
        private set

    var toValueStr by mutableStateOf("8312")
        private set

    var isUpdatingRates by mutableStateOf(false)
        private set

    // Active converter row ("from" or "to" to know which one is being edited)
    var activeConverterRow by mutableStateOf("from")

    // Database History
    val historyState: StateFlow<List<HistoryEntry>> = repository.allHistory
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun selectTab(tab: String) {
        activeTab = tab
    }

    fun onCalculatorButtonClick(value: String) {
        isLedActive = true
        when (value) {
            "AC" -> {
                expression = ""
                lastExpression = ""
                result = "0"
                isEvaluationComplete = false
                isLedActive = false
            }
            "⌫" -> {
                if (isEvaluationComplete) {
                    expression = ""
                    isEvaluationComplete = false
                } else if (expression.isNotEmpty()) {
                    expression = expression.substring(0, expression.length - 1)
                }
                if (expression.isEmpty()) {
                    isLedActive = false
                }
            }
            "=" -> {
                if (expression.isEmpty()) return
                try {
                    // Standardize visual symbols
                    val parsedExpression = expression
                        .replace("×", "×")
                        .replace("÷", "÷")
                        .replace("−", "−")
                    
                    val evaluated = CalculatorEvaluator.evaluate(parsedExpression, isDegree)
                    val formatted = formatDouble(evaluated)
                    
                    // Save to history
                    val entry = HistoryEntry(
                        expression = expression,
                        result = formatted
                    )
                    viewModelScope.launch {
                        repository.insert(entry)
                    }

                    lastExpression = expression
                    result = formatted
                    expression = formatted
                    isEvaluationComplete = true
                    isLedActive = false
                } catch (e: Exception) {
                    result = "Unexpected end of expression"
                    isLedActive = false
                }
            }
            "DEG/RAD" -> {
                isDegree = !isDegree
            }
            "INV" -> {
                isInverse = !isInverse
            }
            "()" -> {
                if (isEvaluationComplete) {
                    expression = "("
                    isEvaluationComplete = false
                } else {
                    val openCount = expression.count { it == '(' }
                    val closeCount = expression.count { it == ')' }
                    val lastChar = expression.lastOrNull()
                    if (openCount > closeCount && lastChar != null && (lastChar.isDigit() || lastChar == ')' || lastChar == '%')) {
                        expression += ")"
                    } else {
                        expression += "("
                    }
                }
            }
            else -> {
                // Operator or basic numbers
                val isOperator = value in setOf("+", "−", "×", "÷", "^")
                if (isEvaluationComplete) {
                    if (isOperator) {
                        expression = result + value
                    } else {
                        expression = value
                    }
                    isEvaluationComplete = false
                } else {
                    if (isOperator && expression.isNotEmpty()) {
                        val lastChar = expression.last().toString()
                        if (lastChar in setOf("+", "−", "×", "÷", "^")) {
                            expression = expression.substring(0, expression.length - 1) + value
                            return
                        }
                    }
                    expression += value
                }
            }
        }
    }

    fun loadHistoryEntry(entry: HistoryEntry) {
        expression = entry.expression
        result = entry.result
        lastExpression = ""
        isEvaluationComplete = false
        activeTab = "Simple"
    }

    fun clearHistory() {
        viewModelScope.launch {
            repository.clearAll()
        }
    }

    fun selectConverterCategory(category: UnitConverter.Category) {
        converterCategory = category
        val list = UnitConverter.unitsByCategory[category] ?: emptyList()
        if (list.isNotEmpty()) {
            fromUnit = list[0].code
            toUnit = if (list.size > 1) list[1].code else list[0].code
        }
        fromValueStr = "100"
        updateConversion()
        
        if (category == UnitConverter.Category.CURRENCY) {
            triggerRatesUpdate()
        }
    }

    fun swapConverterUnits() {
        val tempUnit = fromUnit
        fromUnit = toUnit
        toUnit = tempUnit
        
        val tempVal = fromValueStr
        fromValueStr = toValueStr
        toValueStr = tempVal
        
        updateConversion()
    }

    fun triggerRatesUpdate() {
        viewModelScope.launch {
            isUpdatingRates = true
            delay(800) // Brief simulated network update delay
            isUpdatingRates = false
        }
    }

    fun onConverterNumberPress(char: String) {
        when (char) {
            "AC" -> {
                fromValueStr = "0"
            }
            "⌫" -> {
                if (fromValueStr.isNotEmpty()) {
                    fromValueStr = fromValueStr.substring(0, fromValueStr.length - 1)
                }
                if (fromValueStr.isEmpty() || fromValueStr == "-") {
                    fromValueStr = "0"
                }
            }
            "." -> {
                if (!fromValueStr.contains(".")) {
                    fromValueStr += "."
                }
            }
            "+/-" -> {
                fromValueStr = if (fromValueStr.startsWith("-")) {
                    fromValueStr.substring(1)
                } else {
                    if (fromValueStr == "0") "-" else "-$fromValueStr"
                }
            }
            "00" -> {
                if (fromValueStr != "0") {
                    fromValueStr += "00"
                }
            }
            "⇄" -> {
                swapConverterUnits()
                return
            }
            else -> {
                if (fromValueStr == "0") {
                    fromValueStr = char
                } else if (fromValueStr == "-0") {
                    fromValueStr = "-$char"
                } else {
                    fromValueStr += char
                }
            }
        }
        updateConversion()
    }

    private fun updateConversion() {
        val value = fromValueStr.toDoubleOrNull() ?: 0.0
        val converted = UnitConverter.convert(value, fromUnit, toUnit, converterCategory)
        toValueStr = formatDouble(converted)
    }

    private fun formatDouble(value: Double): String {
        if (value.isNaN()) return "Error"
        if (value.isInfinite()) return "Infinity"
        
        val symbols = DecimalFormatSymbols(Locale.US)
        val df = DecimalFormat("#.########", symbols)
        return df.format(value)
    }
}

class CalculatorViewModelFactory(
    private val application: Application,
    private val repository: HistoryRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(CalculatorViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return CalculatorViewModel(application, repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
