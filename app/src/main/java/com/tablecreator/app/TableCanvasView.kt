package com.tablecreator.app

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
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

    var selectedRow: Int = 0
    var selectedCol: Int = 1
    var onCellSelectedListener: ((row: Int, col: Int) -> Unit)? = null
    var onHeaderSelectedListener: ((col: Int) -> Unit)? = null

    // Standard conversion: 1 cm = 37.795 px at 96 DPI baseline, scaled by display density
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
        color = Color.parseColor("#1976D2")
    }

    private var headerTopY = 0f
    private var headerBottomY = 0f
    private var gridTopY = 0f
    private val colLefts = mutableListOf<Float>()
    private val colRights = mutableListOf<Float>()
    private val rowTops = mutableListOf<Float>()
    private val rowBottoms = mutableListOf<Float>()

    private val paddingPx get() = 16f * density
    private val headerHeightPx get() = 0.85f * pxPerCm

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val totalColsWidth = document.columns.sumOf { (it.widthCm * pxPerCm).toDouble() }.toFloat()
        val totalRowsHeight = document.rows.sumOf { (it.heightCm * pxPerCm).toDouble() }.toFloat()

        var extraTop = paddingPx * 2
        if (document.showMainTitle) extraTop += 44f * density
        if (document.showSubHeader) extraTop += 46f * density

        val desiredW = (totalColsWidth + paddingPx * 2).roundToInt().coerceAtLeast(300)
        val desiredH = (totalRowsHeight + headerHeightPx + extraTop).roundToInt().coerceAtLeast(300)
        setMeasuredDimension(desiredW, desiredH)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawColor(Color.WHITE)

        val borderPx = document.borderWidthDp * density
        borderPaint.strokeWidth = borderPx
        borderPaint.color = document.borderColor

        val startX = paddingPx
        val totalTableWidth = document.columns.sumOf { (it.widthCm * pxPerCm).toDouble() }.toFloat()
        val endX = startX + totalTableWidth
        var currentY = paddingPx

        // 1. Top Big Header Text
        if (document.showMainTitle) {
            textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textPaint.textSize = 20f * density
            textPaint.color = Color.BLACK
            textPaint.textAlign = Paint.Align.CENTER
            canvas.drawText(
                document.mainTitle,
                startX + totalTableWidth / 2f,
                currentY + 24f * density,
                textPaint
            )
            currentY += 38f * density
        }

        // 2. Sub-header Rounded Bar (Name, Date, Day)
        if (document.showSubHeader) {
            val subRect = RectF(startX, currentY, endX, currentY + 34f * density)
            val subRadius = document.subHeaderRadiusDp * density
            canvas.drawRoundRect(subRect, subRadius, subRadius, borderPaint)

            textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textPaint.textSize = 12f * density
            textPaint.textAlign = Paint.Align.LEFT
            val textY = currentY + 22f * density

            canvas.drawText(document.subHeaderLeft, startX + 12f * density, textY, textPaint)
            canvas.drawText(document.subHeaderMiddle, startX + totalTableWidth * 0.68f, textY, textPaint)
            canvas.drawText(document.subHeaderRight, startX + totalTableWidth * 0.84f, textY, textPaint)

            currentY += 44f * density
        }

        // Calculate Grid Coordinates
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

        // Clip outer table with corner curve
        val tableRect = RectF(startX, headerTopY, endX, gridBottomY)
        val cornerPx = document.tableCornerRadiusDp * density
        val clipPath = Path().apply {
            addRoundRect(tableRect, cornerPx, cornerPx, Path.Direction.CW)
        }

        canvas.save()
        canvas.clipPath(clipPath)

        // 3. Draw Table Column Headers
        for (c in document.columns.indices) {
            val rect = RectF(colLefts[c], headerTopY, colRights[c], headerBottomY)
            fillPaint.color = Color.parseColor("#F4F6F8")
            canvas.drawRect(rect, fillPaint)
            canvas.drawRect(rect, borderPaint)

            textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textPaint.textSize = document.fontSizeSp * density
            textPaint.color = Color.BLACK
            textPaint.textAlign = Paint.Align.CENTER
            drawCenteredText(canvas, document.columns[c].headerText, rect, textPaint, true)
        }

        // 4. Draw Data Cells
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

                // Highlight selected cell (only during interactive view, not PDF export)
                if (!isExporting && r == selectedRow && c == selectedCol) {
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
        canvas.restore()

        // 5. Outer Rounded Border Stroke
        canvas.drawRoundRect(tableRect, cornerPx, cornerPx, borderPaint)
    }

    var isExporting: Boolean = false

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
        if (event.action == MotionEvent.ACTION_UP) {
            val x = event.x
            val y = event.y

            val colIdx = colLefts.indices.firstOrNull { x in colLefts[it]..colRights[it] } ?: return true

            if (y in headerTopY..headerBottomY) {
                selectedCol = colIdx
                onHeaderSelectedListener?.invoke(colIdx)
                invalidate()
                return true
            }

            val rowIdx = rowTops.indices.firstOrNull { y in rowTops[it]..rowBottoms[it] } ?: return true
            selectedRow = rowIdx
            selectedCol = colIdx
            onCellSelectedListener?.invoke(rowIdx, colIdx)
            invalidate()
        }
        return true
    }
}
