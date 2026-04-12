---
phase: quick
plan: 260411-tll
type: execute
wave: 1
depends_on: []
files_modified:
  - app/src/main/kotlin/com/acalc/ui/screens/ConverterScreen.kt
  - app/src/main/kotlin/com/acalc/ui/viewmodel/ConverterViewModel.kt
autonomous: true
must_haves:
  truths:
    - "Numpad row 3 right button shows calculator icon and opens expression calculator dialog"
    - "Expression calculator dialog shows active unit name as title, full arithmetic numpad, and expression display"
    - "Pressing = in expression dialog evaluates expression, fills active row, recomputes all other rows, and dismisses dialog"
    - "Row remove X button is gone; rows use the existing design but without separate close icon for rows with size > 2"
  artifacts:
    - path: "app/src/main/kotlin/com/acalc/ui/screens/ConverterScreen.kt"
      provides: "ExpressionCalcDialog composable, updated numpad, updated row layout"
    - path: "app/src/main/kotlin/com/acalc/ui/viewmodel/ConverterViewModel.kt"
      provides: "onExprCalcCommit function"
  key_links:
    - from: "ExpressionCalcDialog"
      to: "ConverterViewModel.onExprCalcCommit"
      via: "callback passed through ConverterScreen"
    - from: "ConverterViewModel.onExprCalcCommit"
      to: "ExpressionEvaluator.evaluate + recomputeFrom"
      via: "evaluates expression string then triggers multi-row recompute"
---

<objective>
Redesign the ConverterScreen to match the DL Calculator reference app. The main changes are:
1. Replace the numpad row-3-right Tune icon with a calculator icon that opens an expression calculator dialog
2. Add an ExpressionCalcDialog (ModalBottomSheet) with full arithmetic numpad for in-place expression evaluation
3. Add ViewModel support for committing expression results back to the active row
4. Remove per-row close (X) IconButton -- use long-press-to-remove or keep rows without explicit removal for now

Purpose: Match the DL Calculator reference UX where the converter numpad includes an expression calculator shortcut.
Output: Updated ConverterScreen.kt and ConverterViewModel.kt
</objective>

<execution_context>
@$HOME/.claude/get-shit-done/workflows/execute-plan.md
@$HOME/.claude/get-shit-done/templates/summary.md
</execution_context>

<context>
@app/src/main/kotlin/com/acalc/ui/screens/ConverterScreen.kt
@app/src/main/kotlin/com/acalc/ui/viewmodel/ConverterViewModel.kt
@app/src/main/kotlin/com/acalc/domain/ExpressionEvaluator.kt
</context>

<tasks>

<task type="auto">
  <name>Task 1: Add onExprCalcCommit to ConverterViewModel</name>
  <files>app/src/main/kotlin/com/acalc/ui/viewmodel/ConverterViewModel.kt</files>
  <action>
Add a new public function to ConverterViewModel:

```kotlin
fun onExprCalcCommit(expression: String) {
    val state = _state.value
    val activeIndex = state.activeRowIndex
    val result = evaluator.evaluate(expression) ?: return
    val bd = BigDecimal(result.toString())
    val rows = state.rows.toMutableList()
    rows[activeIndex] = rows[activeIndex].copy(value = formatConverted(bd))
    recomputeFrom(state, rows, activeIndex, bd)
}
```

This evaluates the expression string from the dialog, sets the active row value to the formatted result, and recomputes all other rows. The existing `evaluator` and `recomputeFrom` are already available in the class. No other changes to the ViewModel.
  </action>
  <verify>
    <automated>cd /home/sanya/Desktop/ACALC && grep -n "fun onExprCalcCommit" app/src/main/kotlin/com/acalc/ui/viewmodel/ConverterViewModel.kt</automated>
  </verify>
  <done>ConverterViewModel has onExprCalcCommit(expression: String) that evaluates, sets active row, and recomputes others.</done>
</task>

<task type="auto">
  <name>Task 2: Add ExpressionCalcDialog and update ConverterScreen</name>
  <files>app/src/main/kotlin/com/acalc/ui/screens/ConverterScreen.kt</files>
  <action>
Make these changes to ConverterScreen.kt:

**A. Add ExpressionCalcDialog composable** (private, inside same file):

```kotlin
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExpressionCalcDialog(
    unitName: String,
    initialValue: String,
    onCommit: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var expression by remember { mutableStateOf(initialValue) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Title: unit name
            Text(unitName, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(12.dp))

            // Expression display area
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        MaterialTheme.colorScheme.surfaceVariant,
                        RoundedCornerShape(8.dp)
                    )
                    .padding(horizontal = 16.dp, vertical = 20.dp),
                contentAlignment = Alignment.CenterEnd
            ) {
                Text(
                    text = expression.ifEmpty { "0" },
                    style = MaterialTheme.typography.headlineMedium,
                    textAlign = TextAlign.End,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(Modifier.height(12.dp))

            // Full calculator numpad
            val shape = RoundedCornerShape(10.dp)
            val onKey: (String) -> Unit = { key ->
                expression = when (key) {
                    "C" -> ""
                    "DEL" -> if (expression.isNotEmpty()) expression.dropLast(1) else ""
                    "=" -> {
                        onCommit(expression)
                        expression // value doesn't matter, dialog dismisses
                    }
                    else -> expression + key
                }
                if (key == "=") onDismiss()
            }

            // Row 1: C  ⌫  %  div
            Row(Modifier.fillMaxWidth()) {
                ExprCalcBtn("C", shape, { onKey("C") }, Modifier.weight(1f), isOperator = true)
                ExprCalcBtn(null, shape, { onKey("DEL") }, Modifier.weight(1f), isOperator = true) {
                    Icon(Icons.Default.Backspace, contentDescription = "Delete", modifier = Modifier.size(20.dp))
                }
                ExprCalcBtn("%", shape, { onKey("%") }, Modifier.weight(1f), isOperator = true)
                ExprCalcBtn("\u00F7", shape, { onKey("\u00F7") }, Modifier.weight(1f), isOperator = true)
            }
            // Row 2: 7  8  9  x
            Row(Modifier.fillMaxWidth()) {
                ExprCalcBtn("7", shape, { onKey("7") }, Modifier.weight(1f))
                ExprCalcBtn("8", shape, { onKey("8") }, Modifier.weight(1f))
                ExprCalcBtn("9", shape, { onKey("9") }, Modifier.weight(1f))
                ExprCalcBtn("\u00D7", shape, { onKey("\u00D7") }, Modifier.weight(1f), isOperator = true)
            }
            // Row 3: 4  5  6  -
            Row(Modifier.fillMaxWidth()) {
                ExprCalcBtn("4", shape, { onKey("4") }, Modifier.weight(1f))
                ExprCalcBtn("5", shape, { onKey("5") }, Modifier.weight(1f))
                ExprCalcBtn("6", shape, { onKey("6") }, Modifier.weight(1f))
                ExprCalcBtn("-", shape, { onKey("-") }, Modifier.weight(1f), isOperator = true)
            }
            // Row 4: 1  2  3  +
            Row(Modifier.fillMaxWidth()) {
                ExprCalcBtn("1", shape, { onKey("1") }, Modifier.weight(1f))
                ExprCalcBtn("2", shape, { onKey("2") }, Modifier.weight(1f))
                ExprCalcBtn("3", shape, { onKey("3") }, Modifier.weight(1f))
                ExprCalcBtn("+", shape, { onKey("+") }, Modifier.weight(1f), isOperator = true)
            }
            // Row 5: 0  00  .  =
            Row(Modifier.fillMaxWidth()) {
                ExprCalcBtn("0", shape, { onKey("0") }, Modifier.weight(1f))
                ExprCalcBtn("00", shape, { onKey("00") }, Modifier.weight(1f))
                ExprCalcBtn(".", shape, { onKey(".") }, Modifier.weight(1f))
                ExprCalcBtn("=", shape, { onKey("=") }, Modifier.weight(1f), isOperator = true)
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}
```

Add helper composable `ExprCalcBtn`:

```kotlin
@Composable
private fun ExprCalcBtn(
    label: String?,
    shape: RoundedCornerShape,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isOperator: Boolean = false,
    content: (@Composable () -> Unit)? = null
) {
    if (isOperator) {
        Button(onClick = onClick, shape = shape, modifier = modifier.padding(3.dp).height(52.dp)) {
            content?.invoke() ?: Text(label ?: "", style = MaterialTheme.typography.titleMedium)
        }
    } else {
        FilledTonalButton(onClick = onClick, shape = shape, modifier = modifier.padding(3.dp).height(52.dp)) {
            content?.invoke() ?: Text(label ?: "", style = MaterialTheme.typography.titleMedium)
        }
    }
}
```

**B. Update ConverterScreen composable:**

1. Add state variable for expression calc dialog visibility:
   ```kotlin
   var showExprCalc by remember { mutableStateOf(false) }
   ```

2. Replace the `onUnitPicker` parameter in the `ConverterNumpad` call with `onExprCalc`:
   Change `ConverterNumpad` signature: rename `onUnitPicker` to `onExprCalc`.
   In the call site, pass `onExprCalc = { showExprCalc = true }`.

3. Add ExpressionCalcDialog rendering at the bottom of ConverterScreen (alongside UnitPickerSheet):
   ```kotlin
   if (showExprCalc) {
       val activeRow = state.rows.getOrNull(state.activeRowIndex)
       val activeUnitName = activeRow?.let {
           unitOptions.getOrNull(it.unitIndex)?.second
       } ?: ""
       ExpressionCalcDialog(
           unitName = activeUnitName,
           initialValue = activeRow?.value ?: "",
           onCommit = { expr -> vm.onExprCalcCommit(expr) },
           onDismiss = { showExprCalc = false }
       )
   }
   ```

**C. Update ConverterNumpad:**

1. Rename parameter `onUnitPicker` to `onExprCalc`.
2. In row 3, change `Icons.Default.Tune` to `Icons.Default.Calculate` with contentDescription "Expression calculator".
3. Change the onClick from `onUnitPicker` to `onExprCalc`.

**D. Update ConverterRowItem:**

1. Remove the `canRemove` parameter entirely.
2. Remove the `onRemove` parameter entirely.
3. Remove the `if (canRemove)` block and the `else Spacer` after the value Box. Just end the Row after the value Box.
4. Update the call site in ConverterScreen to remove `canRemove` and `onRemove` arguments.

**E. Add import** for `Icons.Default.Calculate` (it is in material-icons-core, already a dependency).

**Note on ExpressionEvaluator compatibility:** The existing `ExpressionEvaluator.evaluate()` uses `x` for multiplication internally (per Phase 03 decisions). The expression dialog uses Unicode multiplication sign (U+00D7) and division sign (U+00F7). In `onExprCalcCommit`, before passing to `evaluator.evaluate()`, replace these characters:
- In ConverterViewModel.onExprCalcCommit, sanitize the expression:
  ```kotlin
  val sanitized = expression.replace("\u00D7", "*").replace("\u00F7", "/")
  val result = evaluator.evaluate(sanitized) ?: return
  ```
  </action>
  <verify>
    <automated>cd /home/sanya/Desktop/ACALC && grep -c "ExpressionCalcDialog" app/src/main/kotlin/com/acalc/ui/screens/ConverterScreen.kt && grep -c "Icons.Default.Calculate" app/src/main/kotlin/com/acalc/ui/screens/ConverterScreen.kt && grep -c "canRemove" app/src/main/kotlin/com/acalc/ui/screens/ConverterScreen.kt</automated>
  </verify>
  <done>ExpressionCalcDialog exists (count > 0), Calculate icon used (count > 0), canRemove removed (count = 0). Numpad row 3 right opens expression calc dialog. Dialog has full arithmetic numpad, displays expression, evaluates on "=", commits to ViewModel, and dismisses.</done>
</task>

</tasks>

<verification>
1. `grep "fun onExprCalcCommit" app/src/main/kotlin/com/acalc/ui/viewmodel/ConverterViewModel.kt` -- function exists
2. `grep "ExpressionCalcDialog" app/src/main/kotlin/com/acalc/ui/screens/ConverterScreen.kt` -- dialog composable exists
3. `grep "Icons.Default.Calculate" app/src/main/kotlin/com/acalc/ui/screens/ConverterScreen.kt` -- new icon used
4. `grep "canRemove" app/src/main/kotlin/com/acalc/ui/screens/ConverterScreen.kt` returns nothing -- close button removed
5. `grep "Icons.Default.Tune" app/src/main/kotlin/com/acalc/ui/screens/ConverterScreen.kt` returns nothing -- old icon removed
6. Build compiles: `cd /home/sanya/Desktop/ACALC && ./gradlew assembleDebug` succeeds
</verification>

<success_criteria>
- ConverterScreen numpad row 3 right button shows Calculate icon and opens ExpressionCalcDialog
- ExpressionCalcDialog is a ModalBottomSheet with unit name title, expression display, full arithmetic numpad (C, backspace, %, division, 7-9, multiply, 4-6, minus, 1-3, plus, 0, 00, dot, equals)
- Pressing "=" evaluates expression via ConverterViewModel.onExprCalcCommit, fills active row, recomputes all other rows, dismisses dialog
- Per-row close (X) button removed from ConverterRowItem
- Unicode multiplication/division signs properly sanitized before evaluation
- Project compiles without errors
</success_criteria>

<output>
After completion, create `.planning/quick/260411-tll-redesign-converterscreen-to-match-dl-cal/260411-tll-SUMMARY.md`
</output>
