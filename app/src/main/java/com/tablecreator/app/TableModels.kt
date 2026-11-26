package com.tablecreator.app

import android.graphics.Color

data class CellData(
    var text: String = "",
    var isBold: Boolean = false,
    var bgColor: Int = Color.WHITE,
    var textColor: Int = Color.BLACK,
    var alignCenter: Boolean = false
)

data class ColumnConfig(
    var headerText: String,
    var widthCm: Float = 2.5f
)

data class RowConfig(
    var heightCm: Float = 0.7f,
    val cells: MutableList<CellData>
)

data class TableDocument(
    var mainTitle: String = "व्हि. आर. पवार सारीज",
    var showMainTitle: Boolean = true,
    var subHeaderLeft: String = "सेनापतीचे नाव :",
    var subHeaderMiddle: String = "तारीख :",
    var subHeaderRight: String = "/ वार :",
    var showSubHeader: Boolean = true,
    var subHeaderRadiusDp: Float = 12f,
    var borderWidthDp: Float = 1.2f,
    var borderColor: Int = Color.BLACK,
    var tableCornerRadiusDp: Float = 8f,
    var fontSizeSp: Float = 12f,
    val columns: MutableList<ColumnConfig> = mutableListOf(),
    val rows: MutableList<RowConfig> = mutableListOf()
) {
    companion object {
        fun createSampleLikeImage(): TableDocument {
            val doc = TableDocument()
            val defaultCols = listOf(
                ColumnConfig("क्र.", 1.1f),
                ColumnConfig("सेल्समन", 3.6f),
                ColumnConfig("वेळ", 1.8f),
                ColumnConfig("क्र.", 1.1f),
                ColumnConfig("हेल्पर", 3.8f),
                ColumnConfig("वेळ", 1.8f),
                ColumnConfig("डबा मागवणे", 4.5f),
                ColumnConfig("सेल्समन सुट्टी", 4.5f),
                ColumnConfig("सेल्समन अर्धी सुट्टी", 4.5f)
            )
            doc.columns.addAll(defaultCols)

            for (r in 1..25) {
                val cells = MutableList(defaultCols.size) { colIdx ->
                    when (colIdx) {
                        0 -> CellData(if (r <= 21) r.toString() else "", alignCenter = true)
                        3 -> CellData(r.toString(), alignCenter = true)
                        else -> CellData("")
                    }
                }
                doc.rows.add(RowConfig(0.68f, cells))
            }

            // Sample highlighted cells matching your image
            doc.rows[9].cells[7].apply {
                text = "हेल्पर सुट्टी"
                isBold = true
                bgColor = Color.parseColor("#FFF9DB")
                alignCenter = true
            }
            doc.rows[14].cells[8].apply {
                text = "हेल्पर अर्धी सुट्टी"
                isBold = true
                alignCenter = true
            }
            doc.rows[16].cells[6].apply {
                text = "हेल्पर"
                isBold = true
                bgColor = Color.parseColor("#FFF9DB")
                alignCenter = true
            }
            doc.rows[18].cells[7].apply {
                text = "बाहेर जाणारे"
                isBold = true
                alignCenter = true
            }
            return doc
        }
    }
}
