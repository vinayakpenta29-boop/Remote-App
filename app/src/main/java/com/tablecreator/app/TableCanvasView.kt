package com.tablecreator.app

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import kotlin.math.roundToInt

class TableCanvasView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    var document: TableDocument = TableDocument.createSampleLikeImage()
        set(value) {
            field = value
            requestLayout()
            invalidate()
        }

    var assignmentMode: AssignmentMode = AssignmentMode.NONE
        set(value) {
            field = value
            if (value != AssignmentMode.NONE) {
                clearMultiSelection()
            }
            invalidate()
        }

    var selectedRow: Int = 0
    var selectedCol: Int = 1
    var isExporting: Boolean = false

    // Multi-cell selection support (activated on long-press)
    var isMultiSelectMode: Boolean = false
        private set
    val multiSelectedCells: MutableSet<Pair<Int, Int>> = mutableSetOf()

    var onCellSelectedListener: ((row: Int, col: Int) -> Unit)? = null
    var onHeaderSelectedListener: ((col: Int) -> Unit)? = null
    var onSelectionChangedListener: (() -> Unit)? = null

    private val pxPerCm: Float
        get() = (resources.displayMetrics.density * 160f) / 2.54f

    private val density: Float
        get() = resources.displayMetrics.density

    private val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
    }
    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.BLACK
    }
    private val selectionPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        color = Color.parseColor("#2563EB")
    }
    private val multiSelectTintPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.argb(42, 37, 99, 235)
    }
    private val checkboxBoxPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 2f
        color = Color.parseColor("#1E3A8A")
    }
    private val checkboxFillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.parseColor("#2563EB")
    }
    private val checkmarkPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 3f
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
        color = Color.WHITE
    }

    private var headerTopY = 0f
    private var headerBottomY = 0f
    private val colLefts = mutableListOf<Float>()
    private val colRights = mutableListOf<Float>()
    private val rowTops = mutableListOf<Float>()
    private val rowBottoms = mutableListOf<Float>()

    private val activePaddingPx: Float
        get() = if (isExporting) 0f else 16f * density

    private val headerHeightPx get() = 1.05f * pxPerCm

    private val mainTitleBlockHeightPx: Float
        get() = if (document.showMainTitle) (document.mainTitleSizeSp * 1.45f + 12f) * density else 0f

    private val subHeaderBarHeightPx: Float
        get() = (document.subHeaderSizeSp * 1.6f + 16f) * density

    // Exact 0.5 cm gap between Top Info Bar (सेनापतीचे नाव) and Main Table
    private val subHeaderBlockHeightPx: Float
        get() = if (document.showSubHeader) subHeaderBarHeightPx + (0.5f * pxPerCm) else 0f

    fun getContentWidthPx(): Float =
        document.columns.sumOf { (it.widthCm * pxPerCm).toDouble() }.toFloat()

    fun getContentHeightPx(): Float {
        val totalRowsHeight = document.rows.sumOf { (it.heightCm * pxPerCm).toDouble() }.toFloat()
        return mainTitleBlockHeightPx + subHeaderBlockHeightPx + headerHeightPx + totalRowsHeight
    }

    fun clearMultiSelection() {
        isMultiSelectMode = false
        multiSelectedCells.clear()
        onSelectionChangedListener?.invoke()
        invalidate()
    }

    private val gestureDetector = GestureDetector(context, object : GestureDetector.SimpleOnGestureListener() {
        override fun onDown(e: MotionEvent): Boolean = true

        override fun onLongPress(e: MotionEvent) {
            if (assignmentMode != AssignmentMode.NONE) return
            val cellPos = findCellAt(e.x, e.y) ?: return
            performHapticFeedback(android.view.HapticFeedbackConstants.LONG_PRESS)
            isMultiSelectMode = true
            selectedRow = cellPos.first
            selectedCol = cellPos.second
            if (!multiSelectedCells.add(cellPos)) {
                multiSelectedCells.remove(cellPos)
                if (multiSelectedCells.isEmpty()) {
                    isMultiSelectMode = false
                }
            }
            onSelectionChangedListener?.invoke()
            invalidate()
        }

        override fun onSingleTapUp(e: MotionEvent): Boolean {
            val colIdx = colLefts.indices.firstOrNull { e.x in colLefts[it]..colRights[it] } ?: return true

            if (e.y in headerTopY..headerBottomY) {
                selectedCol = colIdx
                if (assignmentMode != AssignmentMode.NONE) {
                    val current = document.isColumnChecked(colIdx, assignmentMode)
                    document.setColumnChecked(colIdx, assignmentMode, !current)
                    onHeaderSelectedListener?.invoke(colIdx)
                    invalidate()
                } else {
                    onHeaderSelectedListener?.invoke(colIdx)
                    invalidate()
                }
                return true
            }

            val cellPos = findCellAt(e.x, e.y) ?: return true
            selectedRow = cellPos.first
            selectedCol = cellPos.second

            if (isMultiSelectMode && assignmentMode == AssignmentMode.NONE) {
                if (!multiSelectedCells.add(cellPos)) {
                    multiSelectedCells.remove(cellPos)
                    if (multiSelectedCells.isEmpty()) {
                        isMultiSelectMode = false
                    }
                }
                onSelectionChangedListener?.invoke()
                invalidate()
                return true
            }

            onCellSelectedListener?.invoke(cellPos.first, cellPos.second)
            invalidate()
            return true
        }
    })

    private fun findCellAt(x: Float, y: Float): Pair<Int, Int>? {
        val colIdx = colLefts.indices.firstOrNull { x in colLefts[it]..colRights[it] } ?: return null
        val rowIdx = rowTops.indices.firstOrNull { y in rowTops[it]..rowBottoms[it] } ?: return null
        return rowIdx to colIdx
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val pad = 16f * density
        val desiredW = (getContentWidthPx() + pad * 2).roundToInt().coerceAtLeast(300)
        val desiredH = (getContentHeightPx() + pad * 2).roundToInt().coerceAtLeast(300)
        setMeasuredDimension(desiredW, desiredH)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawColor(Color.WHITE)

        val borderPx = document.borderWidthDp * density
        borderPaint.strokeWidth = borderPx
        borderPaint.color = document.borderColor
        checkboxBoxPaint.strokeWidth = 1.8f * density
        checkmarkPaint.strokeWidth = 2.4f * density

        val startX = activePaddingPx
        val totalTableWidth = getContentWidthPx()
        val endX = startX + totalTableWidth
        var currentY = activePaddingPx

        // 1. Main Big Header Text
        if (document.showMainTitle) {
            textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textPaint.textSize = document.mainTitleSizeSp * density
            textPaint.color = Color.BLACK
            textPaint.textAlign = Paint.Align.CENTER
            val titleRect = RectF(startX, currentY, endX, currentY + mainTitleBlockHeightPx - 6f * density)
            val fm = textPaint.fontMetrics
            val baseline = titleRect.centerY() - (fm.ascent + fm.descent) / 2f
            canvas.drawText(
                document.mainTitle,
                startX + totalTableWidth / 2f,
                baseline,
                textPaint
            )
            currentY += mainTitleBlockHeightPx
        }

        // 2. Top Info Sub-Header Bar (सेनापतीचे नाव :, तारीख :, / वार :)
        if (document.showSubHeader) {
            val barH = subHeaderBarHeightPx
            val subRect = RectF(startX, currentY, endX, currentY + barH)
            val subRadius = document.subHeaderRadiusDp * density
            canvas.drawRoundRect(subRect, subRadius, subRadius, borderPaint)

            textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textPaint.textSize = document.subHeaderSizeSp * density
            textPaint.color = Color.BLACK
            textPaint.textAlign = Paint.Align.LEFT
            val fm = textPaint.fontMetrics
            val textY = subRect.centerY() - (fm.ascent + fm.descent) / 2f

            canvas.drawText(document.subHeaderLeft, startX + 12f * density, textY, textPaint)
            canvas.drawText(document.subHeaderMiddle, startX + totalTableWidth * 0.68f, textY, textPaint)
            canvas.drawText(document.subHeaderRight, startX + totalTableWidth * 0.84f, textY, textPaint)

            currentY += subHeaderBlockHeightPx
        }

        colLefts.clear()
        colRights.clear()
        rowTops.clear()
        rowBottoms.clear()

        var xCursor = startX
        for (col in document.columns) {
            val w = col.widthCm * pxPerCm
            colLefts.add(xCursor)
            colRights.add(xCursor + w)
            xCursor += w
        }

        headerTopY = currentY
        headerBottomY = headerTopY + headerHeightPx
        var yCursor = headerBottomY

        for (row in document.rows) {
            val h = row.heightCm * pxPerCm
            rowTops.add(yCursor)
            rowBottoms.add(yCursor + h)
            yCursor += h
        }
        val gridBottomY = yCursor

        val tableRect = RectF(startX, headerTopY, endX, gridBottomY)
        val cornerPx = document.tableCornerRadiusDp * density
        val clipPath = Path().apply {
            addRoundRect(tableRect, cornerPx, cornerPx, Path.Direction.CW)
        }

        canvas.save()
        canvas.clipPath(clipPath)

        // 3. Column Headers + Checkboxes
        val showCheckboxes = !isExporting && assignmentMode != AssignmentMode.NONE
        val boxSize = 16f * density

        for (c in document.columns.indices) {
            val rect = RectF(colLefts[c], headerTopY, colRights[c], headerBottomY)
            val isChecked = document.isColumnChecked(c, assignmentMode)

            fillPaint.color = if (showCheckboxes && isChecked) {
                Color.parseColor("#E3F2FD")
            } else {
                Color.parseColor("#F4F6F8")
            }
            canvas.drawRect(rect, fillPaint)
            canvas.drawRect(rect, borderPaint)

            textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textPaint.textSize = document.fontSizeSp * density
            textPaint.color = Color.BLACK
            textPaint.textAlign = Paint.Align.CENTER

            val textRect = if (showCheckboxes) {
                RectF(rect.left, rect.top, rect.right, rect.bottom - boxSize - 4f * density)
            } else {
                rect
            }
            drawCenteredText(canvas, document.columns[c].headerText, textRect, textPaint, true)

            if (showCheckboxes) {
                val boxLeft = rect.centerX() - boxSize / 2f
                val boxTop = rect.bottom - boxSize - 3f * density
                val boxRect = RectF(boxLeft, boxTop, boxLeft + boxSize, boxTop + boxSize)

                if (isChecked) {
                    canvas.drawRoundRect(boxRect, 3f * density, 3f * density, checkboxFillPaint)
                    val tickPath = Path().apply {
                        moveTo(boxRect.left + boxSize * 0.22f, boxRect.centerY())
                        lineTo(boxRect.left + boxSize * 0.45f, boxRect.bottom - boxSize * 0.25f)
                        lineTo(boxRect.right - boxSize * 0.20f, boxRect.top + boxSize * 0.25f)
                    }
                    canvas.drawPath(tickPath, checkmarkPaint)
                } else {
                    canvas.drawRoundRect(boxRect, 3f * density, 3f * density, checkboxBoxPaint)
                }
            }
        }

        // 4. Data Cells
        for (r in document.rows.indices) {
            val row = document.rows[r]
            for (c in document.columns.indices) {
                val cell = row.cells.getOrNull(c) ?: continue
                val rect = RectF(colLefts[c], rowTops[r], colRights[c], rowBottoms[r])

                fillPaint.color = cell.bgColor
                canvas.drawRect(rect, fillPaint)
                canvas.drawRect(rect, borderPaint)

                if (cell.text.isNotEmpty()) {
                    textPaint.typeface = Typeface.create(
                        Typeface.DEFAULT,
                        if (cell.isBold) Typeface.BOLD else Typeface.NORMAL
                    )
                    textPaint.textSize = document.fontSizeSp * density
                    textPaint.color = cell.textColor
                    drawCenteredText(canvas, cell.text, rect, textPaint, cell.alignCenter)
                }

                if (!isExporting) {
                    val isPartOfMulti = isMultiSelectMode && (r to c) in multiSelectedCells
                    val isSingleActive = !isMultiSelectMode && r == selectedRow && c == selectedCol

                    if (isPartOfMulti) {
                        canvas.drawRect(rect, multiSelectTintPaint)
                        selectionPaint.strokeWidth = 2.5f * density
                        canvas.drawRect(
                            rect.left + 2f,
                            rect.top + 2f,
                            rect.right - 2f,
                            rect.bottom - 2f,
                            selectionPaint
                        )
                    } else if (isSingleActive) {
                        selectionPaint.strokeWidth = 2.5f * density
                        canvas.drawRect(
                            rect.left + 2f,
                            rect.top + 2f,
                            rect.right - 2f,
                            rect.bottom - 2f,
                            selectionPaint
                        )
                    }
                }
            }
        }
        canvas.restore()

        canvas.drawRoundRect(tableRect, cornerPx, cornerPx, borderPaint)
    }

    private fun drawCenteredText(
        canvas: Canvas,
        text: String,
        rect: RectF,
        paint: Paint,
        centerHorizontally: Boolean
    ) {
        val fm = paint.fontMetrics
        val baseline = rect.centerY() - (fm.ascent + fm.descent) / 2f
        if (centerHorizontally) {
            paint.textAlign = Paint.Align.CENTER
            canvas.drawText(text, rect.centerX(), baseline, paint)
        } else {
            paint.textAlign = Paint.Align.LEFT
            canvas.drawText(text, rect.left + 6f * density, baseline, paint)
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        return gestureDetector.onTouchEvent(event) || super.onTouchEvent(event)
    }
}
