package com.example

import android.app.Application
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.CalculatorDatabase
import com.example.data.HistoryEntry
import com.example.data.HistoryRepository
import com.example.ui.theme.*
import com.example.util.UnitConverter
import com.example.viewmodel.CalculatorViewModel
import com.example.viewmodel.CalculatorViewModelFactory
import kotlinx.coroutines.launch

// Custom dot-matrix background modifier drawing elegant high-contrast grey dots on a grid
fun Modifier.nothingDotMatrixBackground(
    dotColor: Color = Color(0xFF222222),
    dotRadius: Float = 1.5f,
    spacing: Dp = 14.dp,
    alpha: Float = 1.0f
) = this.drawBehind {
    val width = size.width
    val height = size.height
    val spacingPx = spacing.toPx()
    
    var x = spacingPx / 2
    while (x < width) {
        var y = spacingPx / 2
        while (y < height) {
            drawCircle(
                color = dotColor,
                radius = dotRadius,
                center = Offset(x, y),
                alpha = alpha
            )
            y += spacingPx
        }
        x += spacingPx
    }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        // Instantiate Room Database and Repository
        val database = CalculatorDatabase.getDatabase(this)
        val repository = HistoryRepository(database.historyDao())
        
        setContent {
            MyApplicationTheme {
                val viewModel: CalculatorViewModel by viewModels {
                    CalculatorViewModelFactory(application, repository)
                }
                
                Scaffold(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(NothingBlack),
                    containerColor = NothingBlack
                ) { innerPadding ->
                    CalculatorApp(
                        viewModel = viewModel,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    )
                }
            }
        }
    }
}

@Composable
fun CalculatorApp(
    viewModel: CalculatorViewModel,
    modifier: Modifier = Modifier
) {
    var showHistory by remember { mutableStateOf(false) }
    
    // Handle back button presses according to user rules
    if (showHistory) {
        BackHandler {
            showHistory = false
        }
    } else if (viewModel.activeTab != "Simple") {
        BackHandler {
            viewModel.selectTab("Simple")
        }
    }
    
    Box(
        modifier = modifier
            .background(NothingBlack)
            .nothingDotMatrixBackground()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top Header: Tabs and History Button
            TopHeaderRow(
                activeTab = viewModel.activeTab,
                onTabSelect = { viewModel.selectTab(it) },
                onHistoryClick = { showHistory = true }
            )
            
            // Screen Content depending on selected tab
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                when (viewModel.activeTab) {
                    "Simple" -> SimpleCalculatorScreen(viewModel)
                    "Scientific" -> ScientificCalculatorScreen(viewModel)
                    "Converter" -> ConverterScreen(viewModel)
                }
            }
        }
        
        // History Overlay Slide-Up
        AnimatedVisibility(
            visible = showHistory,
            enter = slideInVertically(initialOffsetY = { it }),
            exit = slideOutVertically(targetOffsetY = { it })
        ) {
            HistoryOverlay(
                viewModel = viewModel,
                onClose = { showHistory = false }
            )
        }
    }
}

@Composable
fun TopHeaderRow(
    activeTab: String,
    onTabSelect: (String) -> Unit,
    onHistoryClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Tab Pills
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(50))
                .background(NothingDarkGray)
                .border(1.dp, NothingLightGray, RoundedCornerShape(50))
                .padding(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val tabs = listOf("Simple", "Scientific", "Converter")
            tabs.forEach { tab ->
                val isActive = activeTab == tab
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(if (isActive) NothingRed else Color.Transparent)
                        .clickable { onTabSelect(tab) }
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = tab,
                        color = if (isActive) NothingTextWhite else NothingTextGray,
                        fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium,
                        fontSize = 14.sp,
                        fontFamily = FontFamily.SansSerif
                    )
                }
            }
        }
        
        // History Button
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(NothingDarkGray)
                .border(1.dp, NothingLightGray, CircleShape)
                .clickable(onClick = onHistoryClick),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.History,
                contentDescription = "History",
                tint = NothingTextWhite,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
fun SimpleCalculatorScreen(viewModel: CalculatorViewModel) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Upper Display Area
        DisplayArea(
            lastExpression = viewModel.lastExpression,
            expression = viewModel.expression,
            result = viewModel.result,
            isLedActive = viewModel.isLedActive,
            ledLabel = "LED ACTIVE"
        )
        
        // Keypad Grid (5 rows x 4 columns)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Row 1: AC, %, (), ÷
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                NothingButton(
                    text = "AC",
                    backgroundColor = NothingRed,
                    textColor = NothingTextWhite,
                    onClick = { viewModel.onCalculatorButtonClick("AC") },
                    modifier = Modifier.weight(1f),
                    isBold = true,
                    testTag = "btn_ac"
                )
                NothingButton(
                    text = "%",
                    backgroundColor = NothingButtonGray,
                    textColor = NothingTextWhite,
                    onClick = { viewModel.onCalculatorButtonClick("%") },
                    modifier = Modifier.weight(1f),
                    testTag = "btn_percent"
                )
                NothingButton(
                    text = "()",
                    backgroundColor = NothingButtonGray,
                    textColor = NothingTextWhite,
                    onClick = { viewModel.onCalculatorButtonClick("()") },
                    modifier = Modifier.weight(1f),
                    testTag = "btn_parens"
                )
                NothingButton(
                    text = "÷",
                    backgroundColor = NothingButtonGray,
                    textColor = NothingRed,
                    onClick = { viewModel.onCalculatorButtonClick("÷") },
                    modifier = Modifier.weight(1f),
                    fontSize = 24.sp,
                    testTag = "btn_divide"
                )
            }
            
            // Row 2: 7, 8, 9, ×
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                NothingButton(
                    text = "7",
                    onClick = { viewModel.onCalculatorButtonClick("7") },
                    modifier = Modifier.weight(1f),
                    testTag = "btn_7"
                )
                NothingButton(
                    text = "8",
                    onClick = { viewModel.onCalculatorButtonClick("8") },
                    modifier = Modifier.weight(1f),
                    testTag = "btn_8"
                )
                NothingButton(
                    text = "9",
                    onClick = { viewModel.onCalculatorButtonClick("9") },
                    modifier = Modifier.weight(1f),
                    testTag = "btn_9"
                )
                NothingButton(
                    text = "×",
                    backgroundColor = NothingButtonGray,
                    textColor = NothingRed,
                    onClick = { viewModel.onCalculatorButtonClick("×") },
                    modifier = Modifier.weight(1f),
                    fontSize = 24.sp,
                    testTag = "btn_multiply"
                )
            }
            
            // Row 3: 4, 5, 6, −
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                NothingButton(
                    text = "4",
                    onClick = { viewModel.onCalculatorButtonClick("4") },
                    modifier = Modifier.weight(1f),
                    testTag = "btn_4"
                )
                NothingButton(
                    text = "5",
                    onClick = { viewModel.onCalculatorButtonClick("5") },
                    modifier = Modifier.weight(1f),
                    testTag = "btn_5"
                )
                NothingButton(
                    text = "6",
                    onClick = { viewModel.onCalculatorButtonClick("6") },
                    modifier = Modifier.weight(1f),
                    testTag = "btn_6"
                )
                NothingButton(
                    text = "−",
                    backgroundColor = NothingButtonGray,
                    textColor = NothingRed,
                    onClick = { viewModel.onCalculatorButtonClick("−") },
                    modifier = Modifier.weight(1f),
                    fontSize = 24.sp,
                    testTag = "btn_subtract"
                )
            }
            
            // Row 4: 1, 2, 3, +
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                NothingButton(
                    text = "1",
                    onClick = { viewModel.onCalculatorButtonClick("1") },
                    modifier = Modifier.weight(1f),
                    testTag = "btn_1"
                )
                NothingButton(
                    text = "2",
                    onClick = { viewModel.onCalculatorButtonClick("2") },
                    modifier = Modifier.weight(1f),
                    testTag = "btn_2"
                )
                NothingButton(
                    text = "3",
                    onClick = { viewModel.onCalculatorButtonClick("3") },
                    modifier = Modifier.weight(1f),
                    testTag = "btn_3"
                )
                NothingButton(
                    text = "+",
                    backgroundColor = NothingButtonGray,
                    textColor = NothingRed,
                    onClick = { viewModel.onCalculatorButtonClick("+") },
                    modifier = Modifier.weight(1f),
                    fontSize = 24.sp,
                    testTag = "btn_add"
                )
            }
            
            // Row 5: 0, . , ⌫ , =
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                NothingButton(
                    text = "0",
                    onClick = { viewModel.onCalculatorButtonClick("0") },
                    modifier = Modifier.weight(1f),
                    testTag = "btn_0"
                )
                NothingButton(
                    text = ".",
                    onClick = { viewModel.onCalculatorButtonClick(".") },
                    modifier = Modifier.weight(1f),
                    testTag = "btn_dot"
                )
                NothingButton(
                    text = "⌫",
                    onClick = { viewModel.onCalculatorButtonClick("⌫") },
                    modifier = Modifier.weight(1f),
                    testTag = "btn_backspace"
                )
                NothingButton(
                    text = "=",
                    backgroundColor = NothingTextWhite,
                    textColor = NothingBlack,
                    onClick = { viewModel.onCalculatorButtonClick("=") },
                    modifier = Modifier.weight(1f),
                    fontSize = 24.sp,
                    isBold = true,
                    testTag = "btn_equals"
                )
            }
        }
    }
}

@Composable
fun ScientificCalculatorScreen(viewModel: CalculatorViewModel) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Upper Display Area
        Column {
            // DEG/RAD Indicators
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 4.dp),
                horizontalArrangement = Arrangement.Start,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(if (viewModel.isDegree) NothingRed else NothingButtonGray)
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "DEG",
                        color = NothingTextWhite,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
                Spacer(modifier = Modifier.width(6.dp))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(if (!viewModel.isDegree) NothingRed else NothingButtonGray)
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "RAD",
                        color = NothingTextWhite,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
            
            DisplayArea(
                lastExpression = viewModel.lastExpression,
                expression = viewModel.expression,
                result = viewModel.result,
                isLedActive = viewModel.isLedActive,
                ledLabel = "SCIENTIFIC"
            )
        }
        
        // Keypad Grid (6 rows x 5 columns)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Row 1: DEG/RAD toggle, INV, ⌫, (), AC
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                NothingButton(
                    text = "DEG\nRAD",
                    fontSize = 11.sp,
                    backgroundColor = NothingButtonGray,
                    textColor = if (viewModel.isDegree) NothingRed else NothingTextWhite,
                    onClick = { viewModel.onCalculatorButtonClick("DEG/RAD") },
                    modifier = Modifier.weight(1f),
                    isCircle = false,
                    testTag = "btn_sci_degrad"
                )
                NothingButton(
                    text = "INV",
                    fontSize = 12.sp,
                    backgroundColor = if (viewModel.isInverse) NothingMutedRed else NothingButtonGray,
                    textColor = if (viewModel.isInverse) NothingRed else NothingTextWhite,
                    onClick = { viewModel.onCalculatorButtonClick("INV") },
                    modifier = Modifier.weight(1f),
                    isCircle = false,
                    testTag = "btn_sci_inv"
                )
                NothingButton(
                    text = "⌫",
                    backgroundColor = NothingButtonGray,
                    textColor = NothingTextWhite,
                    onClick = { viewModel.onCalculatorButtonClick("⌫") },
                    modifier = Modifier.weight(1f),
                    isCircle = false,
                    testTag = "btn_sci_backspace"
                )
                NothingButton(
                    text = "()",
                    backgroundColor = NothingButtonGray,
                    textColor = NothingTextWhite,
                    onClick = { viewModel.onCalculatorButtonClick("()") },
                    modifier = Modifier.weight(1f),
                    isCircle = false,
                    testTag = "btn_sci_parens"
                )
                NothingButton(
                    text = "AC",
                    backgroundColor = NothingRed,
                    textColor = NothingTextWhite,
                    onClick = { viewModel.onCalculatorButtonClick("AC") },
                    modifier = Modifier.weight(1f),
                    isCircle = false,
                    isBold = true,
                    testTag = "btn_sci_ac"
                )
            }
            
            // Row 2: sin, cos, tan, x^y, ÷
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                NothingButton(
                    text = if (viewModel.isInverse) "asin" else "sin",
                    fontSize = 13.sp,
                    backgroundColor = NothingButtonGray,
                    textColor = NothingTextGray,
                    onClick = { viewModel.onCalculatorButtonClick(if (viewModel.isInverse) "asin(" else "sin(") },
                    modifier = Modifier.weight(1f),
                    isCircle = false,
                    testTag = "btn_sci_sin"
                )
                NothingButton(
                    text = if (viewModel.isInverse) "acos" else "cos",
                    fontSize = 13.sp,
                    backgroundColor = NothingButtonGray,
                    textColor = NothingTextGray,
                    onClick = { viewModel.onCalculatorButtonClick(if (viewModel.isInverse) "acos(" else "cos(") },
                    modifier = Modifier.weight(1f),
                    isCircle = false,
                    testTag = "btn_sci_cos"
                )
                NothingButton(
                    text = if (viewModel.isInverse) "atan" else "tan",
                    fontSize = 13.sp,
                    backgroundColor = NothingButtonGray,
                    textColor = NothingTextGray,
                    onClick = { viewModel.onCalculatorButtonClick(if (viewModel.isInverse) "atan(" else "tan(") },
                    modifier = Modifier.weight(1f),
                    isCircle = false,
                    testTag = "btn_sci_tan"
                )
                NothingButton(
                    text = "x^y",
                    fontSize = 14.sp,
                    backgroundColor = NothingButtonGray,
                    textColor = NothingRed,
                    onClick = { viewModel.onCalculatorButtonClick("^") },
                    modifier = Modifier.weight(1f),
                    isCircle = false,
                    testTag = "btn_sci_pow"
                )
                NothingButton(
                    text = "÷",
                    backgroundColor = NothingButtonGray,
                    textColor = NothingRed,
                    onClick = { viewModel.onCalculatorButtonClick("÷") },
                    modifier = Modifier.weight(1f),
                    isCircle = false,
                    fontSize = 20.sp,
                    testTag = "btn_sci_divide"
                )
            }
            
            // Row 3: π, 7, 8, 9, ×
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                NothingButton(
                    text = "π",
                    backgroundColor = NothingButtonGray,
                    textColor = NothingTextGray,
                    onClick = { viewModel.onCalculatorButtonClick("π") },
                    modifier = Modifier.weight(1f),
                    isCircle = false,
                    testTag = "btn_sci_pi"
                )
                NothingButton(
                    text = "7",
                    onClick = { viewModel.onCalculatorButtonClick("7") },
                    modifier = Modifier.weight(1f),
                    isCircle = false,
                    testTag = "btn_sci_7"
                )
                NothingButton(
                    text = "8",
                    onClick = { viewModel.onCalculatorButtonClick("8") },
                    modifier = Modifier.weight(1f),
                    isCircle = false,
                    testTag = "btn_sci_8"
                )
                NothingButton(
                    text = "9",
                    onClick = { viewModel.onCalculatorButtonClick("9") },
                    modifier = Modifier.weight(1f),
                    isCircle = false,
                    testTag = "btn_sci_9"
                )
                NothingButton(
                    text = "×",
                    backgroundColor = NothingButtonGray,
                    textColor = NothingRed,
                    onClick = { viewModel.onCalculatorButtonClick("×") },
                    modifier = Modifier.weight(1f),
                    isCircle = false,
                    fontSize = 20.sp,
                    testTag = "btn_sci_multiply"
                )
            }
            
            // Row 4: e, 4, 5, 6, −
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                NothingButton(
                    text = "e",
                    backgroundColor = NothingButtonGray,
                    textColor = NothingTextGray,
                    onClick = { viewModel.onCalculatorButtonClick("e") },
                    modifier = Modifier.weight(1f),
                    isCircle = false,
                    testTag = "btn_sci_e"
                )
                NothingButton(
                    text = "4",
                    onClick = { viewModel.onCalculatorButtonClick("4") },
                    modifier = Modifier.weight(1f),
                    isCircle = false,
                    testTag = "btn_sci_4"
                )
                NothingButton(
                    text = "5",
                    onClick = { viewModel.onCalculatorButtonClick("5") },
                    modifier = Modifier.weight(1f),
                    isCircle = false,
                    testTag = "btn_sci_5"
                )
                NothingButton(
                    text = "6",
                    onClick = { viewModel.onCalculatorButtonClick("6") },
                    modifier = Modifier.weight(1f),
                    isCircle = false,
                    testTag = "btn_sci_6"
                )
                NothingButton(
                    text = "−",
                    backgroundColor = NothingButtonGray,
                    textColor = NothingRed,
                    onClick = { viewModel.onCalculatorButtonClick("−") },
                    modifier = Modifier.weight(1f),
                    isCircle = false,
                    fontSize = 20.sp,
                    testTag = "btn_sci_subtract"
                )
            }
            
            // Row 5: ln, 1, 2, 3, +
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                NothingButton(
                    text = "ln",
                    backgroundColor = NothingButtonGray,
                    textColor = NothingTextGray,
                    onClick = { viewModel.onCalculatorButtonClick("ln(") },
                    modifier = Modifier.weight(1f),
                    isCircle = false,
                    testTag = "btn_sci_ln"
                )
                NothingButton(
                    text = "1",
                    onClick = { viewModel.onCalculatorButtonClick("1") },
                    modifier = Modifier.weight(1f),
                    isCircle = false,
                    testTag = "btn_sci_1"
                )
                NothingButton(
                    text = "2",
                    onClick = { viewModel.onCalculatorButtonClick("2") },
                    modifier = Modifier.weight(1f),
                    isCircle = false,
                    testTag = "btn_sci_2"
                )
                NothingButton(
                    text = "3",
                    onClick = { viewModel.onCalculatorButtonClick("3") },
                    modifier = Modifier.weight(1f),
                    isCircle = false,
                    testTag = "btn_sci_3"
                )
                NothingButton(
                    text = "+",
                    backgroundColor = NothingButtonGray,
                    textColor = NothingRed,
                    onClick = { viewModel.onCalculatorButtonClick("+") },
                    modifier = Modifier.weight(1f),
                    isCircle = false,
                    fontSize = 20.sp,
                    testTag = "btn_sci_add"
                )
            }
            
            // Row 6: log, √, 0, ., =
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                NothingButton(
                    text = "log",
                    backgroundColor = NothingButtonGray,
                    textColor = NothingTextGray,
                    onClick = { viewModel.onCalculatorButtonClick("log(") },
                    modifier = Modifier.weight(1f),
                    isCircle = false,
                    testTag = "btn_sci_log"
                )
                NothingButton(
                    text = "√",
                    backgroundColor = NothingButtonGray,
                    textColor = NothingTextGray,
                    onClick = { viewModel.onCalculatorButtonClick("√(") },
                    modifier = Modifier.weight(1f),
                    isCircle = false,
                    testTag = "btn_sci_sqrt"
                )
                NothingButton(
                    text = "0",
                    onClick = { viewModel.onCalculatorButtonClick("0") },
                    modifier = Modifier.weight(1f),
                    isCircle = false,
                    testTag = "btn_sci_0"
                )
                NothingButton(
                    text = ".",
                    onClick = { viewModel.onCalculatorButtonClick(".") },
                    modifier = Modifier.weight(1f),
                    isCircle = false,
                    testTag = "btn_sci_dot"
                )
                NothingButton(
                    text = "=",
                    backgroundColor = NothingTextWhite,
                    textColor = NothingBlack,
                    onClick = { viewModel.onCalculatorButtonClick("=") },
                    modifier = Modifier.weight(1f),
                    isCircle = false,
                    fontSize = 20.sp,
                    isBold = true,
                    testTag = "btn_sci_equals"
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ConverterScreen(viewModel: CalculatorViewModel) {
    var fromMenuOpen by remember { mutableStateOf(false) }
    var toMenuOpen by remember { mutableStateOf(false) }
    
    val unitList = UnitConverter.unitsByCategory[viewModel.converterCategory] ?: emptyList()
    
    val isFromActive = viewModel.activeConverterRow == "from"
    val isToActive = viewModel.activeConverterRow == "to"
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 4.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Upper interactive Cards
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Row 1: Source (From)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(NothingDarkGray)
                    .border(
                        width = if (isFromActive) 1.5.dp else 1.dp,
                        color = if (isFromActive) NothingRed else NothingLightGray,
                        shape = RoundedCornerShape(16.dp)
                    )
                    .clickable { viewModel.activeConverterRow = "from" }
                    .padding(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Left: Dropdown Selector
                    Box {
                        val currentUnitOpt = unitList.find { it.code == viewModel.fromUnit }
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(NothingLightGray)
                                .clickable { fromMenuOpen = true }
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = viewModel.fromUnit,
                                    color = NothingTextWhite,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                                Text(
                                    text = currentUnitOpt?.fullName ?: "",
                                    color = NothingTextGray,
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.SansSerif
                                )
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(
                                imageVector = Icons.Default.KeyboardArrowDown,
                                contentDescription = "Select unit",
                                tint = NothingTextGray,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        
                        DropdownMenu(
                            expanded = fromMenuOpen,
                            onDismissRequest = { fromMenuOpen = false },
                            modifier = Modifier
                                .background(NothingBlack)
                                .border(1.dp, NothingLightGray, RoundedCornerShape(8.dp))
                                .nothingDotMatrixBackground()
                        ) {
                            unitList.forEach { option ->
                                DropdownMenuItem(
                                    text = {
                                        Column {
                                            Text(option.code, color = NothingTextWhite, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                                            Text(option.fullName, color = NothingTextGray, fontSize = 11.sp)
                                        }
                                    },
                                    onClick = {
                                        viewModel.fromUnit = option.code
                                        fromMenuOpen = false
                                        viewModel.selectConverterCategory(viewModel.converterCategory)
                                    }
                                )
                            }
                        }
                    }
                    
                    // Right: Value (with auto scaling to avoid overlap)
                    val fromFontSize = when {
                        viewModel.fromValueStr.length > 12 -> 18.sp
                        viewModel.fromValueStr.length > 8 -> 24.sp
                        else -> 32.sp
                    }
                    Text(
                        text = viewModel.fromValueStr,
                        color = if (isFromActive) NothingTextWhite else NothingTextGray,
                        fontSize = fromFontSize,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.End,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .weight(1f)
                            .padding(start = 12.dp)
                    )
                }
            }
            
            // Clean, non-overlapping middle divider with Swap button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(1.dp)
                        .background(NothingLightGray.copy(alpha = 0.5f))
                )
                Box(
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(NothingDarkGray)
                        .border(1.dp, NothingLightGray, CircleShape)
                        .clickable { viewModel.swapConverterUnits() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.SwapVert,
                        contentDescription = "Swap Units",
                        tint = NothingRed,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(1.dp)
                        .background(NothingLightGray.copy(alpha = 0.5f))
                )
            }
            
            // Row 2: Target (To)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(NothingDarkGray)
                    .border(
                        width = if (isToActive) 1.5.dp else 1.dp,
                        color = if (isToActive) NothingRed else NothingLightGray,
                        shape = RoundedCornerShape(16.dp)
                    )
                    .clickable { viewModel.activeConverterRow = "to" }
                    .padding(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Left: Dropdown Selector
                    Box {
                        val currentUnitOpt = unitList.find { it.code == viewModel.toUnit }
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(NothingLightGray)
                                .clickable { toMenuOpen = true }
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = viewModel.toUnit,
                                    color = NothingTextWhite,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                                Text(
                                    text = currentUnitOpt?.fullName ?: "",
                                    color = NothingTextGray,
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.SansSerif
                                )
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(
                                imageVector = Icons.Default.KeyboardArrowDown,
                                contentDescription = "Select unit",
                                tint = NothingTextGray,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        
                        DropdownMenu(
                            expanded = toMenuOpen,
                            onDismissRequest = { toMenuOpen = false },
                            modifier = Modifier
                                .background(NothingBlack)
                                .border(1.dp, NothingLightGray, RoundedCornerShape(8.dp))
                                .nothingDotMatrixBackground()
                        ) {
                            unitList.forEach { option ->
                                DropdownMenuItem(
                                    text = {
                                        Column {
                                            Text(option.code, color = NothingTextWhite, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                                            Text(option.fullName, color = NothingTextGray, fontSize = 11.sp)
                                        }
                                    },
                                    onClick = {
                                        viewModel.toUnit = option.code
                                        toMenuOpen = false
                                        viewModel.selectConverterCategory(viewModel.converterCategory)
                                    }
                                )
                            }
                        }
                    }
                    
                    // Right: Value (with auto scaling to avoid overlap)
                    val toFontSize = when {
                        viewModel.toValueStr.length > 12 -> 18.sp
                        viewModel.toValueStr.length > 8 -> 24.sp
                        else -> 32.sp
                    }
                    Text(
                        text = viewModel.toValueStr,
                        color = if (isToActive) NothingTextWhite else NothingTextGray,
                        fontSize = toFontSize,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.End,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .weight(1f)
                            .padding(start = 12.dp)
                    )
                }
            }
        }
        
        // Refresh indicator / Updating rates
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (viewModel.converterCategory == UnitConverter.Category.CURRENCY) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Refreshing rates",
                    tint = NothingTextGray,
                    modifier = Modifier
                        .size(12.dp)
                        .graphicsLayer {
                            if (viewModel.isUpdatingRates) {
                                rotationZ = (System.currentTimeMillis() % 1000) * 0.36f
                            }
                        }
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (viewModel.isUpdatingRates) "Updating rates..." else "Rates updated",
                    color = NothingTextGray,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
        
        // Category Row and Keypad Container
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "CATEGORY",
                color = NothingTextGray,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.padding(bottom = 6.dp)
            )
            
            // Reorganized Category row (Single Horizontal Row)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                CategoryPill(UnitConverter.Category.CURRENCY, "CURR", viewModel, Modifier.weight(1f))
                CategoryPill(UnitConverter.Category.LENGTH, "LEN", viewModel, Modifier.weight(1f))
                CategoryPill(UnitConverter.Category.WEIGHT, "WGT", viewModel, Modifier.weight(1f))
                CategoryPill(UnitConverter.Category.TEMPERATURE, "TEMP", viewModel, Modifier.weight(1f))
            }
            
            // Highly Organized 4-Column Keypad with compact custom heights
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 4.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val converterButtons = listOf(
                    listOf("7", "8", "9", "⌫"),
                    listOf("4", "5", "6", "AC"),
                    listOf("1", "2", "3", "."),
                    listOf("0")
                )
                
                converterButtons.forEach { row ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (row.size == 1 && row[0] == "0") {
                            // Centered "0" button spanning the middle 2 columns
                            Spacer(modifier = Modifier.weight(1f))
                            NothingButton(
                                text = "0",
                                backgroundColor = NothingButtonGray,
                                textColor = NothingTextWhite,
                                onClick = { viewModel.onConverterNumberPress("0") },
                                modifier = Modifier.weight(2f),
                                isCircle = false,
                                isBold = false,
                                aspectRatio = 3.2f, // Perfectly proportioned to match other rows
                                testTag = "btn_conv_0"
                            )
                            Spacer(modifier = Modifier.weight(1f))
                        } else {
                            row.forEach { char ->
                                val isActionKey = char in setOf("AC", "⌫")
                                val btnBg = when (char) {
                                    "AC" -> NothingRed
                                    "⌫" -> NothingButtonGray
                                    else -> NothingButtonGray
                                }
                                val btnFg = NothingTextWhite
                                
                                NothingButton(
                                    text = char,
                                    backgroundColor = btnBg,
                                    textColor = btnFg,
                                    onClick = { viewModel.onConverterNumberPress(char) },
                                    modifier = Modifier.weight(1f),
                                    isCircle = false,
                                    isBold = isActionKey,
                                    aspectRatio = 1.6f, // Perfect height ratio to prevent vertical overlap
                                    testTag = "btn_conv_$char"
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CategoryPill(
    category: UnitConverter.Category,
    label: String,
    viewModel: CalculatorViewModel,
    modifier: Modifier = Modifier
) {
    val isSelected = viewModel.converterCategory == category
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(if (isSelected) NothingRed else NothingDarkGray)
            .border(1.dp, if (isSelected) NothingRed else NothingLightGray, RoundedCornerShape(12.dp))
            .clickable { viewModel.selectConverterCategory(category) }
            .padding(vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label.uppercase(),
            color = if (isSelected) NothingTextWhite else NothingTextGray,
            fontWeight = FontWeight.Bold,
            fontSize = 10.sp,
            fontFamily = FontFamily.Monospace
        )
    }
}

@Composable
fun DisplayArea(
    lastExpression: String,
    expression: String,
    result: String,
    isLedActive: Boolean,
    ledLabel: String
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp)
            .padding(horizontal = 8.dp),
        verticalArrangement = Arrangement.SpaceBetween,
        horizontalAlignment = Alignment.End
    ) {
        // LED Indicator & Mode
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(if (isLedActive) NothingRed else NothingLightGray)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = if (isLedActive) "$ledLabel ACTIVE" else "$ledLabel STANDBY",
                color = if (isLedActive) NothingRed else NothingTextGray,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
        }
        
        // Expressions and Results Column
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.End
        ) {
            // Previous calculation line
            if (lastExpression.isNotEmpty()) {
                Text(
                    text = lastExpression,
                    color = NothingTextGray,
                    fontSize = 16.sp,
                    fontFamily = FontFamily.Monospace,
                    textAlign = TextAlign.End,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(bottom = 6.dp)
                )
            }
            
            // Main expression display
            val currentDisplayText = if (expression.isEmpty()) "0" else expression
            
            // Dynamic text resizing based on length to prevent overflows
            val fontSize = when {
                currentDisplayText.length > 16 -> 24.sp
                currentDisplayText.length > 12 -> 32.sp
                currentDisplayText.length > 8 -> 42.sp
                else -> 52.sp
            }
            
            Text(
                text = currentDisplayText,
                color = NothingTextWhite,
                fontSize = fontSize,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.End,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun NothingButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    backgroundColor: Color = NothingDarkGray,
    textColor: Color = NothingTextWhite,
    isCircle: Boolean = true,
    isBold: Boolean = false,
    fontSize: TextUnit = 20.sp,
    aspectRatio: Float? = null,
    testTag: String = ""
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.90f else 1.0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "button_scale"
    )
    
    val alpha by animateFloatAsState(
        targetValue = if (isPressed) 0.75f else 1.0f,
        animationSpec = tween(80),
        label = "button_alpha"
    )

    val shape = if (isCircle) CircleShape else RoundedCornerShape(16.dp)

    val baseModifier = modifier
        .testTag(testTag)
        .scale(scale)
        .graphicsLayer(alpha = alpha)

    val aspectModifier = if (aspectRatio != null) {
        if (aspectRatio > 0f) baseModifier.aspectRatio(aspectRatio) else baseModifier
    } else {
        baseModifier.aspectRatio(if (isCircle) 1f else 1.2f)
    }

    Box(
        contentAlignment = Alignment.Center,
        modifier = aspectModifier
            .clip(shape)
            .background(backgroundColor)
            .clickable(
                interactionSource = interactionSource,
                indication = null, // Disable default system indications for custom tactile feedback
                onClick = onClick
            )
    ) {
        Text(
            text = text,
            color = textColor,
            fontSize = fontSize,
            fontWeight = if (isBold) FontWeight.Bold else FontWeight.Medium,
            fontFamily = FontFamily.SansSerif,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun HistoryOverlay(
    viewModel: CalculatorViewModel,
    onClose: () -> Unit
) {
    val historyList by viewModel.historyState.collectAsStateWithLifecycle()
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(NothingBlack)
            .nothingDotMatrixBackground()
            .padding(16.dp)
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "CALCULATION HISTORY",
                color = NothingTextWhite,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
            
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(NothingDarkGray)
                    .clickable(onClick = onClose),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close history",
                    tint = NothingTextWhite,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
        
        // List of history items
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (historyList.isEmpty()) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 80.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.History,
                            contentDescription = "No history",
                            tint = NothingLightGray,
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "No calculations made yet",
                            color = NothingTextGray,
                            fontSize = 14.sp,
                            fontFamily = FontFamily.SansSerif
                        )
                    }
                }
            } else {
                items(historyList) { entry ->
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(NothingDarkGray)
                            .border(1.dp, NothingLightGray, RoundedCornerShape(12.dp))
                            .clickable {
                                viewModel.loadHistoryEntry(entry)
                                onClose()
                            }
                            .padding(16.dp),
                        horizontalAlignment = Alignment.End
                    ) {
                        Text(
                            text = entry.expression,
                            color = NothingTextGray,
                            fontSize = 14.sp,
                            fontFamily = FontFamily.Monospace,
                            textAlign = TextAlign.End
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "= ${entry.result}",
                            color = NothingTextWhite,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            textAlign = TextAlign.End
                        )
                    }
                }
            }
        }
        
        // Clear history button
        if (historyList.isNotEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp)
                    .clip(RoundedCornerShape(50))
                    .background(NothingRed)
                    .clickable { viewModel.clearHistory() }
                    .padding(vertical = 14.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Clear History",
                    color = NothingTextWhite,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.SansSerif
                )
            }
        }
    }
}
