package com.acalc.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.acalc.ui.AcalcTheme
import com.acalc.ui.viewmodel.CalculatorState
import com.acalc.ui.viewmodel.CalculatorViewModel

@Composable
fun CalculatorScreen(modifier: Modifier = Modifier) {
    val vm = viewModel<CalculatorViewModel> { CalculatorViewModel() }
    val state by vm.state.collectAsStateWithLifecycle()

    CalculatorContent(
        state = state,
        onDigit = vm::onDigit,
        onOperator = vm::onOperator,
        onDecimal = vm::onDecimal,
        onClear = vm::onClear,
        onBackspace = vm::onBackspace,
        onPercent = vm::onPercent,
        onEquals = vm::onEquals,
        modifier = modifier
    )
}

@Composable
private fun CalculatorContent(
    state: CalculatorState,
    onDigit: (String) -> Unit,
    onOperator: (String) -> Unit,
    onDecimal: () -> Unit,
    onClear: () -> Unit,
    onBackspace: () -> Unit,
    onPercent: () -> Unit,
    onEquals: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxSize()) {
        // Display area — takes all remaining space
        DisplayArea(
            expression = state.expression,
            result = state.result,
            isError = state.isError,
            modifier = Modifier.weight(1f)
        )

        // Button grid — at the bottom
        ButtonGrid(
            onDigit = onDigit,
            onOperator = onOperator,
            onDecimal = onDecimal,
            onClear = onClear,
            onBackspace = onBackspace,
            onPercent = onPercent,
            onEquals = onEquals,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun DisplayArea(
    expression: String,
    result: String,
    isError: Boolean,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 16.dp)
    ) {
        Spacer(Modifier.weight(1f))

        // Expression line
        Text(
            text = expression,
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.End,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.fillMaxWidth()
        )

        // Result line
        Text(
            text = result,
            style = MaterialTheme.typography.displaySmall,
            textAlign = TextAlign.End,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            color = if (isError) MaterialTheme.colorScheme.error
                    else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun ButtonGrid(
    onDigit: (String) -> Unit,
    onOperator: (String) -> Unit,
    onDecimal: () -> Unit,
    onClear: () -> Unit,
    onBackspace: () -> Unit,
    onPercent: () -> Unit,
    onEquals: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(16.dp)

    Column(modifier = modifier.padding(horizontal = 4.dp, vertical = 4.dp)) {
        // Row 1: C, <-, %, /
        Row(modifier = Modifier.fillMaxWidth()) {
            ActionBtn(label = "C", shape = shape, onClick = onClear, modifier = Modifier.weight(1f))
            ActionBtn(label = "⌫", shape = shape, onClick = onBackspace, modifier = Modifier.weight(1f))
            ActionBtn(label = "%", shape = shape, onClick = onPercent, modifier = Modifier.weight(1f))
            OperatorBtn(label = "/", shape = shape, onClick = { onOperator("/") }, modifier = Modifier.weight(1f))
        }

        // Row 2: 7, 8, 9, x
        Row(modifier = Modifier.fillMaxWidth()) {
            DigitBtn(label = "7", shape = shape, onClick = { onDigit("7") }, modifier = Modifier.weight(1f))
            DigitBtn(label = "8", shape = shape, onClick = { onDigit("8") }, modifier = Modifier.weight(1f))
            DigitBtn(label = "9", shape = shape, onClick = { onDigit("9") }, modifier = Modifier.weight(1f))
            OperatorBtn(label = "x", shape = shape, onClick = { onOperator("x") }, modifier = Modifier.weight(1f))
        }

        // Row 3: 4, 5, 6, -
        Row(modifier = Modifier.fillMaxWidth()) {
            DigitBtn(label = "4", shape = shape, onClick = { onDigit("4") }, modifier = Modifier.weight(1f))
            DigitBtn(label = "5", shape = shape, onClick = { onDigit("5") }, modifier = Modifier.weight(1f))
            DigitBtn(label = "6", shape = shape, onClick = { onDigit("6") }, modifier = Modifier.weight(1f))
            OperatorBtn(label = "-", shape = shape, onClick = { onOperator("-") }, modifier = Modifier.weight(1f))
        }

        // Row 4: 1, 2, 3, +
        Row(modifier = Modifier.fillMaxWidth()) {
            DigitBtn(label = "1", shape = shape, onClick = { onDigit("1") }, modifier = Modifier.weight(1f))
            DigitBtn(label = "2", shape = shape, onClick = { onDigit("2") }, modifier = Modifier.weight(1f))
            DigitBtn(label = "3", shape = shape, onClick = { onDigit("3") }, modifier = Modifier.weight(1f))
            OperatorBtn(label = "+", shape = shape, onClick = { onOperator("+") }, modifier = Modifier.weight(1f))
        }

        // Row 5: 0 (wide), ., =
        Row(modifier = Modifier.fillMaxWidth()) {
            DigitBtn(label = "0", shape = shape, onClick = { onDigit("0") }, modifier = Modifier.weight(2f))
            DigitBtn(label = ".", shape = shape, onClick = onDecimal, modifier = Modifier.weight(1f))
            EqualsBtn(shape = shape, onClick = onEquals, modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun DigitBtn(
    label: String,
    shape: RoundedCornerShape,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    FilledTonalButton(
        onClick = onClick,
        shape = shape,
        modifier = modifier
            .padding(4.dp)
            .aspectRatio(1f)
    ) {
        Text(text = label, style = MaterialTheme.typography.titleLarge)
    }
}

@Composable
private fun OperatorBtn(
    label: String,
    shape: RoundedCornerShape,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        shape = shape,
        modifier = modifier
            .padding(4.dp)
            .aspectRatio(1f)
    ) {
        Text(text = label, style = MaterialTheme.typography.titleLarge)
    }
}

@Composable
private fun ActionBtn(
    label: String,
    shape: RoundedCornerShape,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedButton(
        onClick = onClick,
        shape = shape,
        modifier = modifier
            .padding(4.dp)
            .aspectRatio(1f)
    ) {
        Text(text = label, style = MaterialTheme.typography.titleLarge)
    }
}

@Composable
private fun EqualsBtn(
    shape: RoundedCornerShape,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        shape = shape,
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.tertiary,
            contentColor = MaterialTheme.colorScheme.onTertiary
        ),
        modifier = modifier
            .padding(4.dp)
            .aspectRatio(1f)
    ) {
        Text(text = "=", style = MaterialTheme.typography.titleLarge)
    }
}

@Preview(showBackground = true)
@Composable
private fun CalculatorContentPreview() {
    AcalcTheme {
        CalculatorContent(
            state = CalculatorState(expression = "12+34x2", result = "80"),
            onDigit = {},
            onOperator = {},
            onDecimal = {},
            onClear = {},
            onBackspace = {},
            onPercent = {},
            onEquals = {}
        )
    }
}
