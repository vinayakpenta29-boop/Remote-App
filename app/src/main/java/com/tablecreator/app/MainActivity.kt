package com.tablecreator.app

import android.content.ContentValues
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.pdf.PdfDocument
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.text.InputType
import android.view.Gravity
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.switchmaterial.SwitchMaterial
import java.io.File
import java.io.FileOutputStream
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var tableView: TableCanvasView
    private lateinit var statusText: TextView
    private lateinit var salesmanSwitch: SwitchMaterial
    private lateinit var helperSwitch: SwitchMaterial
    private val bottomButtons = mutableListOf<MaterialButton>()
    private var suppressSwitchCallbacks = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor("#ECEFF1"))
        }

        // Top App Bar with Three-Dots Menu on Right Side
        val topBar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setBackgroundColor(Color.parseColor("#1B4F72"))
            setPadding(24, 20, 12, 20)
            gravity = Gravity.CENTER_VERTICAL
        }

        val appTitle = TextView(this).apply {
            text = "Table Creator Studio"
            setTextColor(Color.WHITE)
            textSize = 18f
            setTypeface(null, android.graphics.Typeface.BOLD)
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        }

        val exportBtn = MaterialButton(this).apply {
            text = "PDF"
            setBackgroundColor(Color.parseColor("#2E7D32"))
            setTextColor(Color.WHITE)
            setOnClickListener { exportTableToPdf() }
        }

        val menuBtn = ImageButton(this).apply {
            setImageResource(android.R.drawable.ic_menu_more)
            contentDescription = "More Options"
            setBackgroundColor(Color.TRANSPARENT)
            setColorFilter(Color.WHITE)
            setPadding(16, 12, 16, 12)
            setOnClickListener { showTopOverflowMenu(this) }
        }

        topBar.addView(appTitle)
        topBar.addView(exportBtn)
        topBar.addView(menuBtn)
        root.addView(topBar)

        // Salesman & Helper Toggle Switch Row
        val toggleBar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setBackgroundColor(Color.WHITE)
            setPadding(20, 8, 20, 8)
            gravity = Gravity.CENTER_VERTICAL
        }

        salesmanSwitch = SwitchMaterial(this).apply {
            text = "Salesman"
            setTextColor(Color.parseColor("#1B4F72"))
            setTypeface(null, android.graphics.Typeface.BOLD)
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
            setOnCheckedChangeListener { _, isChecked ->
                if (suppressSwitchCallbacks) return@setOnCheckedChangeListener
                suppressSwitchCallbacks = true
                if (isChecked) {
                    helperSwitch.isChecked = false
                    tableView.assignmentMode = AssignmentMode.SALESMAN
                } else {
                    tableView.assignmentMode = AssignmentMode.NONE
                }
                suppressSwitchCallbacks = false
                updateToolbarEnabledState()
                updateStatus()
            }
        }

        helperSwitch = SwitchMaterial(this).apply {
            text = "Helper"
            setTextColor(Color.parseColor("#1B4F72"))
            setTypeface(null, android.graphics.Typeface.BOLD)
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
            setOnCheckedChangeListener { _, isChecked ->
                if (suppressSwitchCallbacks) return@setOnCheckedChangeListener
                suppressSwitchCallbacks = true
                if (isChecked) {
                    salesmanSwitch.isChecked = false
                    tableView.assignmentMode = AssignmentMode.HELPER
                } else {
                    tableView.assignmentMode = AssignmentMode.NONE
                }
                suppressSwitchCallbacks = false
                updateToolbarEnabledState()
                updateStatus()
            }
        }

        toggleBar.addView(salesmanSwitch)
        toggleBar.addView(helperSwitch)
        root.addView(toggleBar)

        // Status Indicator Bar
        statusText = TextView(this).apply {
            setPadding(24, 10, 24, 10)
            setBackgroundColor(Color.parseColor("#D6EAF8"))
            setTextColor(Color.parseColor("#1B4F72"))
            textSize = 12f
        }
        root.addView(statusText)

        // Scrollable Two-Way Canvas Container
        val verticalScroll = ScrollView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                0,
                1f
            )
        }
        val horizontalScroll = HorizontalScrollView(this)

        tableView = TableCanvasView(this).apply {
            onCellSelectedListener = { r, c ->
                updateStatus()
                if (assignmentMode != AssignmentMode.NONE && document.isColumnChecked(c, assignmentMode)) {
                    showNameSpinnerDialog(r, c, assignmentMode)
                }
            }
            onHeaderSelectedListener = { c ->
                showHeaderEditorDialog(c)
            }
        }

        horizontalScroll.addView(tableView)
        verticalScroll.addView(horizontalScroll)
        root.addView(verticalScroll)

        // Bottom Scrollable Action Toolbar
        val toolbarScroll = HorizontalScrollView(this).apply {
            setBackgroundColor(Color.WHITE)
            elevation = 8f
        }
        val toolbar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(12, 12, 12, 16)
        }

        val actions = listOf(
            "Edit Cell" to { showCellEditorDialog() },
            "Bold Toggle" to { toggleCellBold() },
            "Fill Color" to { showColorPickerDialog() },
            "Col Width (cm)" to { showColumnWidthDialog() },
            "Row Height (cm)" to { showRowHeightDialog() },
            "+ Column" to { addColumn() },
            "- Column" to { removeColumn() },
            "+ Row" to { addRow() },
            "- Row" to { removeRow() },
            "Top Header" to { showTopHeaderDialog() },
            "Borders & Curve" to { showBorderStyleDialog() }
        )

        for ((label, action) in actions) {
            val btn = MaterialButton(this, null, com.google.android.material.R.attr.materialButtonOutlinedStyle).apply {
                text = label
                textSize = 12f
                val lp = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
                lp.setMargins(6, 0, 6, 0)
                layoutParams = lp
                setOnClickListener {
                    if (tableView.assignmentMode == AssignmentMode.NONE) {
                        action()
                    }
                }
            }
            bottomButtons.add(btn)
            toolbar.addView(btn)
        }

        toolbarScroll.addView(toolbar)
        root.addView(toolbarScroll)

        setContentView(root)
        updateToolbarEnabledState()
        updateStatus()
    }

    private fun showTopOverflowMenu(anchor: android.view.View) {
        val popup = PopupMenu(this, anchor)
        popup.menu.add(0, 1, 0, "Add Salesman Names")
        popup.menu.add(0, 2, 1, "Add Helper Names")
        popup.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                1 -> {
                    showManageNamesDialog(isSalesman = true)
                    true
                }
                2 -> {
                    showManageNamesDialog(isSalesman = false)
                    true
                }
                else -> false
            }
        }
        popup.show()
    }

    private fun showManageNamesDialog(isSalesman: Boolean) {
        val list = if (isSalesman) tableView.document.salesmanNames else tableView.document.helperNames
        val title = if (isSalesman) "Salesman Names" else "Helper Names"

        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(40, 24, 40, 12)
        }

        val inputRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

        val nameInput = EditText(this).apply {
            hint = if (isSalesman) "Enter salesman name" else "Enter helper name"
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        }

        val addBtn = Button(this).apply {
            text = "Add"
        }

        inputRow.addView(nameInput)
        inputRow.addView(addBtn)
        container.addView(inputRow)

        val listView = ListView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                500
            )
        }

        val adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, list)
        listView.adapter = adapter
        container.addView(listView)

        addBtn.setOnClickListener {
            val newName = nameInput.text.toString().trim()
            if (newName.isEmpty()) {
                Toast.makeText(this, "Please enter a name", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (list.any { it.equals(newName, ignoreCase = true) }) {
                Toast.makeText(this, "Name already exists in list", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            list.add(newName)
            adapter.notifyDataSetChanged()
            nameInput.setText("")
        }

        listView.setOnItemLongClickListener { _, _, position, _ ->
            val removed = list.removeAt(position)
            adapter.notifyDataSetChanged()
            Toast.makeText(this, "Removed: $removed", Toast.LENGTH_SHORT).show()
            true
        }

        AlertDialog.Builder(this)
            .setTitle(title)
            .setMessage("Tap Add to append a name. Long-press an item to remove.")
            .setView(container)
            .setPositiveButton("Done", null)
            .show()
    }

    private fun showNameSpinnerDialog(rowIdx: Int, colIdx: Int, mode: AssignmentMode) {
        val doc = tableView.document
        val cell = doc.rows.getOrNull(rowIdx)?.cells?.getOrNull(colIdx) ?: return
        val masterList = if (mode == AssignmentMode.SALESMAN) doc.salesmanNames else doc.helperNames
        val usedNames = doc.getUsedNamesInCheckedColumns(mode, excludeRow = rowIdx, excludeCol = colIdx)

        val availableNames = masterList.filter { it !in usedNames }
        val spinnerItems = mutableListOf("-- Clear Cell --").apply {
            addAll(availableNames)
            if (cell.text.isNotEmpty() && cell.text !in this) {
                add(cell.text)
            }
        }

        val dialogLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 28, 48, 12)
        }

        val infoLabel = TextView(this).apply {
            text = "Duplicate names in checked columns are hidden (${availableNames.size} available)"
            textSize = 12f
            setPadding(0, 0, 0, 16)
        }

        val spinner = Spinner(this).apply {
            adapter = ArrayAdapter(
                this@MainActivity,
                android.R.layout.simple_spinner_dropdown_item,
                spinnerItems
            )
            val currentIdx = spinnerItems.indexOf(cell.text).coerceAtLeast(0)
            setSelection(currentIdx)
        }

        dialogLayout.addView(infoLabel)
        dialogLayout.addView(spinner)

        val labelType = if (mode == AssignmentMode.SALESMAN) "Salesman" else "Helper"
        AlertDialog.Builder(this)
            .setTitle("Select $labelType [R${rowIdx + 1}, C${colIdx + 1}]")
            .setView(dialogLayout)
            .setPositiveButton("Set Name") { _, _ ->
                val chosen = spinner.selectedItem?.toString().orEmpty()
                if (chosen == "-- Clear Cell --") {
                    cell.text = ""
                } else {
                    if (chosen in usedNames) {
                        Toast.makeText(this, "Duplicate name is not allowed", Toast.LENGTH_SHORT).show()
                        return@setPositiveButton
                    }
                    cell.text = chosen
                    cell.isBold = true
                }
                tableView.invalidate()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun updateToolbarEnabledState() {
        val enabled = tableView.assignmentMode == AssignmentMode.NONE
        for (btn in bottomButtons) {
            btn.isEnabled = enabled
            btn.alpha = if (enabled) 1.0f else 0.4f
        }
    }

    private fun updateStatus() {
        val doc = tableView.document
        val r = tableView.selectedRow
        val c = tableView.selectedCol
        val colW = doc.columns.getOrNull(c)?.widthCm ?: 0f
        val rowH = doc.rows.getOrNull(r)?.heightCm ?: 0f

        statusText.text = when (tableView.assignmentMode) {
            AssignmentMode.SALESMAN -> {
                val checked = if (doc.isColumnChecked(c, AssignmentMode.SALESMAN)) "CHECKED" else "UNCHECKED"
                "SALESMAN MODE: Tap header checkbox to toggle column | Col ${c + 1} is $checked | Tap checked cell to pick name"
            }
            AssignmentMode.HELPER -> {
                val checked = if (doc.isColumnChecked(c, AssignmentMode.HELPER)) "CHECKED" else "UNCHECKED"
                "HELPER MODE: Tap header checkbox to toggle column | Col ${c + 1} is $checked | Tap checked cell to pick name"
            }
            AssignmentMode.NONE -> String.format(
                Locale.US,
                "Cell [R%d, C%d] | Width: %.2f cm | Height: %.2f cm (Tap header to edit)",
                r + 1, c + 1, colW, rowH
            )
        }
    }

    private fun showCellEditorDialog() {
        val doc = tableView.document
        val r = tableView.selectedRow
        val c = tableView.selectedCol
        val cell = doc.rows.getOrNull(r)?.cells?.getOrNull(c) ?: return

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 24, 48, 12)
        }

        val input = EditText(this).apply {
            setText(cell.text)
            hint = "Enter cell text"
        }
        val boldCheck = CheckBox(this).apply {
            text = "Bold Text"
            isChecked = cell.isBold
        }
        val centerCheck = CheckBox(this).apply {
            text = "Center Align Text"
            isChecked = cell.alignCenter
        }

        layout.addView(input)
        layout.addView(boldCheck)
        layout.addView(centerCheck)

        AlertDialog.Builder(this)
            .setTitle("Edit Cell [Row ${r + 1}, Col ${c + 1}]")
            .setView(layout)
            .setPositiveButton("Save") { _, _ ->
                cell.text = input.text.toString()
                cell.isBold = boldCheck.isChecked
                cell.alignCenter = centerCheck.isChecked
                tableView.invalidate()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showHeaderEditorDialog(colIdx: Int) {
        val col = tableView.document.columns.getOrNull(colIdx) ?: return
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 24, 48, 12)
        }

        val nameInput = EditText(this).apply {
            setText(col.headerText)
            hint = "Column Header Title"
        }
        val widthInput = EditText(this).apply {
            inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
            setText(String.format(Locale.US, "%.2f", col.widthCm))
            hint = "Column Width in cm"
        }

        layout.addView(TextView(this).apply { text = "Header Text:" })
        layout.addView(nameInput)
        layout.addView(TextView(this).apply { text = "Width (cm):" })
        layout.addView(widthInput)

        AlertDialog.Builder(this)
            .setTitle("Column ${colIdx + 1} Settings")
            .setView(layout)
            .setPositiveButton("Apply") { _, _ ->
                col.headerText = nameInput.text.toString()
                val newW = widthInput.text.toString().toFloatOrNull()
                if (newW != null && newW in 0.5f..25f) {
                    col.widthCm = newW
                }
                tableView.requestLayout()
                tableView.invalidate()
                updateStatus()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun toggleCellBold() {
        val cell = tableView.document.rows
            .getOrNull(tableView.selectedRow)?.cells
            ?.getOrNull(tableView.selectedCol) ?: return
        cell.isBold = !cell.isBold
        tableView.invalidate()
    }

    private fun showColorPickerDialog() {
        val cell = tableView.document.rows
            .getOrNull(tableView.selectedRow)?.cells
            ?.getOrNull(tableView.selectedCol) ?: return

        val colors = arrayOf(
            "White (Default)" to Color.WHITE,
            "Soft Cream Yellow" to Color.parseColor("#FFF9DB"),
            "Mint Green" to Color.parseColor("#E8F5E9"),
            "Sky Blue" to Color.parseColor("#E3F2FD"),
            "Rose Pink" to Color.parseColor("#FCE4EC"),
            "Warm Peach" to Color.parseColor("#FBE9E7"),
            "Light Gray" to Color.parseColor("#ECEFF1")
        )

        AlertDialog.Builder(this)
            .setTitle("Select Cell Fill Color")
            .setItems(colors.map { it.first }.toTypedArray()) { _, idx ->
                cell.bgColor = colors[idx].second
                tableView.invalidate()
            }
            .show()
    }

    private fun showColumnWidthDialog() {
        val c = tableView.selectedCol
        val col = tableView.document.columns.getOrNull(c) ?: return

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 24, 48, 12)
        }
        val label = TextView(this).apply {
            text = String.format(Locale.US, "Width: %.2f cm", col.widthCm)
            textSize = 16f
        }
        val seekBar = SeekBar(this).apply {
            max = 95
            progress = ((col.widthCm - 0.5f) * 10).toInt().coerceIn(0, 95)
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(sb: SeekBar?, p: Int, fromUser: Boolean) {
                    val cm = 0.5f + (p / 10f)
                    label.text = String.format(Locale.US, "Width: %.2f cm", cm)
                    col.widthCm = cm
                    tableView.requestLayout()
                    tableView.invalidate()
                    updateStatus()
                }
                override fun onStartTrackingTouch(sb: SeekBar?) {}
                override fun onStopTrackingTouch(sb: SeekBar?) {}
            })
        }

        layout.addView(label)
        layout.addView(seekBar)

        AlertDialog.Builder(this)
            .setTitle("Resize Column ${c + 1} (${col.headerText})")
            .setView(layout)
            .setPositiveButton("Done", null)
            .show()
    }

    private fun showRowHeightDialog() {
        val r = tableView.selectedRow
        val row = tableView.document.rows.getOrNull(r) ?: return

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 24, 48, 12)
        }
        val label = TextView(this).apply {
            text = String.format(Locale.US, "Row Height: %.2f cm", row.heightCm)
            textSize = 16f
        }
        val seekBar = SeekBar(this).apply {
            max = 46
            progress = ((row.heightCm - 0.4f) * 10).toInt().coerceIn(0, 46)
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(sb: SeekBar?, p: Int, fromUser: Boolean) {
                    val cm = 0.4f + (p / 10f)
                    label.text = String.format(Locale.US, "Row Height: %.2f cm", cm)
                    row.heightCm = cm
                    tableView.requestLayout()
                    tableView.invalidate()
                    updateStatus()
                }
                override fun onStartTrackingTouch(sb: SeekBar?) {}
                override fun onStopTrackingTouch(sb: SeekBar?) {}
            })
        }
        val applyAllBtn = Button(this).apply {
            text = "Apply This Height to ALL Rows"
            setOnClickListener {
                val h = row.heightCm
                tableView.document.rows.forEach { it.heightCm = h }
                tableView.requestLayout()
                tableView.invalidate()
                Toast.makeText(
                    this@MainActivity,
                    "Updated all rows to ${String.format(Locale.US, "%.2f", h)} cm",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

        layout.addView(label)
        layout.addView(seekBar)
        layout.addView(applyAllBtn)

        AlertDialog.Builder(this)
            .setTitle("Resize Row ${r + 1} Height (cm)")
            .setView(layout)
            .setPositiveButton("Done", null)
            .show()
    }

    private fun addColumn() {
        val doc = tableView.document
        val newColNum = doc.columns.size + 1
        doc.columns.add(ColumnConfig("Col $newColNum", 2.5f))
        doc.rows.forEach { it.cells.add(CellData("")) }
        tableView.requestLayout()
        tableView.invalidate()
    }

    private fun removeColumn() {
        val doc = tableView.document
        if (doc.columns.size <= 1) return
        val idx = tableView.selectedCol.coerceIn(0, doc.columns.lastIndex)
        doc.columns.removeAt(idx)
        doc.rows.forEach { if (idx < it.cells.size) it.cells.removeAt(idx) }
        tableView.selectedCol = (idx - 1).coerceAtLeast(0)
        tableView.requestLayout()
        tableView.invalidate()
        updateStatus()
    }

    private fun addRow() {
        val doc = tableView.document
        val cells = MutableList(doc.columns.size) { CellData("") }
        doc.rows.add(RowConfig(0.68f, cells))
        tableView.requestLayout()
        tableView.invalidate()
    }

    private fun removeRow() {
        val doc = tableView.document
        if (doc.rows.size <= 1) return
        val idx = tableView.selectedRow.coerceIn(0, doc.rows.lastIndex)
        doc.rows.removeAt(idx)
        tableView.selectedRow = (idx - 1).coerceAtLeast(0)
        tableView.requestLayout()
        tableView.invalidate()
        updateStatus()
    }

    private fun showTopHeaderDialog() {
        val doc = tableView.document
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 24, 48, 12)
        }

        val showTitleCb = CheckBox(this).apply {
            text = "Show Big Top Title"
            isChecked = doc.showMainTitle
        }
        val titleInput = EditText(this).apply {
            setText(doc.mainTitle)
            hint = "Big Top Header Text"
        }
        val showSubCb = CheckBox(this).apply {
            text = "Show Sub-Header Bar"
            isChecked = doc.showSubHeader
        }
        val leftInput = EditText(this).apply {
            setText(doc.subHeaderLeft)
            hint = "Left Label"
        }
        val midInput = EditText(this).apply {
            setText(doc.subHeaderMiddle)
            hint = "Middle Label"
        }
        val rightInput = EditText(this).apply {
            setText(doc.subHeaderRight)
            hint = "Right Label"
        }

        layout.addView(showTitleCb)
        layout.addView(titleInput)
        layout.addView(showSubCb)
        layout.addView(leftInput)
        layout.addView(midInput)
        layout.addView(rightInput)

        AlertDialog.Builder(this)
            .setTitle("Top Big Header & Info Bar")
            .setView(layout)
            .setPositiveButton("Save") { _, _ ->
                doc.showMainTitle = showTitleCb.isChecked
                doc.mainTitle = titleInput.text.toString()
                doc.showSubHeader = showSubCb.isChecked
                doc.subHeaderLeft = leftInput.text.toString()
                doc.subHeaderMiddle = midInput.text.toString()
                doc.subHeaderRight = rightInput.text.toString()
                tableView.requestLayout()
                tableView.invalidate()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showBorderStyleDialog() {
        val doc = tableView.document
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 24, 48, 12)
        }

        val widthLabel = TextView(this).apply {
            text = String.format(Locale.US, "Border Thickness: %.1f dp", doc.borderWidthDp)
        }
        val widthSeek = SeekBar(this).apply {
            max = 45
            progress = ((doc.borderWidthDp - 0.5f) * 10).toInt().coerceIn(0, 45)
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(sb: SeekBar?, p: Int, fromUser: Boolean) {
                    doc.borderWidthDp = 0.5f + (p / 10f)
                    widthLabel.text = String.format(Locale.US, "Border Thickness: %.1f dp", doc.borderWidthDp)
                    tableView.invalidate()
                }
                override fun onStartTrackingTouch(sb: SeekBar?) {}
                override fun onStopTrackingTouch(sb: SeekBar?) {}
            })
        }

        val curveLabel = TextView(this).apply {
            text = String.format(Locale.US, "Table Corner Curve: %.0f dp", doc.tableCornerRadiusDp)
        }
        val curveSeek = SeekBar(this).apply {
            max = 36
            progress = doc.tableCornerRadiusDp.toInt().coerceIn(0, 36)
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(sb: SeekBar?, p: Int, fromUser: Boolean) {
                    doc.tableCornerRadiusDp = p.toFloat()
                    doc.subHeaderRadiusDp = (p * 1.2f).coerceAtLeast(4f)
                    curveLabel.text = String.format(Locale.US, "Table Corner Curve: %.0f dp", doc.tableCornerRadiusDp)
                    tableView.invalidate()
                }
                override fun onStartTrackingTouch(sb: SeekBar?) {}
                override fun onStopTrackingTouch(sb: SeekBar?) {}
            })
        }

        val fontLabel = TextView(this).apply {
            text = String.format(Locale.US, "Table Font Size: %.0f sp", doc.fontSizeSp)
        }
        val fontSeek = SeekBar(this).apply {
            max = 16
            progress = (doc.fontSizeSp - 8f).toInt().coerceIn(0, 16)
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(sb: SeekBar?, p: Int, fromUser: Boolean) {
                    doc.fontSizeSp = 8f + p
                    fontLabel.text = String.format(Locale.US, "Table Font Size: %.0f sp", doc.fontSizeSp)
                    tableView.invalidate()
                }
                override fun onStartTrackingTouch(sb: SeekBar?) {}
                override fun onStopTrackingTouch(sb: SeekBar?) {}
            })
        }

        layout.addView(widthLabel)
        layout.addView(widthSeek)
        layout.addView(curveLabel)
        layout.addView(curveSeek)
        layout.addView(fontLabel)
        layout.addView(fontSeek)

        AlertDialog.Builder(this)
            .setTitle("Table Borders, Corner Curve & Font")
            .setView(layout)
            .setPositiveButton("Done", null)
            .show()
    }

    private fun exportTableToPdf() {
        tableView.isExporting = true
        tableView.invalidate()

        val pdfDocument = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(tableView.width, tableView.height, 1).create()
        val page = pdfDocument.startPage(pageInfo)
        val canvas: Canvas = page.canvas
        tableView.draw(canvas)
        pdfDocument.finishPage(page)

        tableView.isExporting = false
        tableView.invalidate()

        val fileName = "Table_${System.currentTimeMillis()}.pdf"
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val values = ContentValues().apply {
                    put(MediaStore.Downloads.DISPLAY_NAME, fileName)
                    put(MediaStore.Downloads.MIME_TYPE, "application/pdf")
                    put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                }
                val uri = contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                uri?.let {
                    contentResolver.openOutputStream(it)?.use { out ->
                        pdfDocument.writeTo(out)
                    }
                }
            } else {
                val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                val file = File(downloadsDir, fileName)
                FileOutputStream(file).use { out ->
                    pdfDocument.writeTo(out)
                }
            }
            Toast.makeText(this, "Saved PDF to Downloads/$fileName", Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            Toast.makeText(this, "Export error: ${e.message}", Toast.LENGTH_LONG).show()
        } finally {
            pdfDocument.close()
        }
    }
}
