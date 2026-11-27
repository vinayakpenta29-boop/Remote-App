package com.tablecreator.app

import android.graphics.Color
import org.json.JSONArray
import org.json.JSONObject

enum class AssignmentMode {
    NONE,
    SALESMAN,
    HELPER
}

data class CellData(
    var text: String = "",
    var isBold: Boolean = false,
    var bgColor: Int = Color.WHITE,
    var textColor: Int = Color.BLACK,
    var alignCenter: Boolean = false
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("text", text)
        put("isBold", isBold)
        put("bgColor", bgColor)
        put("textColor", textColor)
        put("alignCenter", alignCenter)
    }

    companion object {
        fun fromJson(obj: JSONObject): CellData = CellData(
            text = obj.optString("text", ""),
            isBold = obj.optBoolean("isBold", false),
            bgColor = obj.optInt("bgColor", Color.WHITE),
            textColor = obj.optInt("textColor", Color.BLACK),
            alignCenter = obj.optBoolean("alignCenter", false)
        )
    }
}

data class ColumnConfig(
    var headerText: String,
    var widthCm: Float = 2.5f,
    var salesmanChecked: Boolean = false,
    var helperChecked: Boolean = false
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("headerText", headerText)
        put("widthCm", widthCm.toDouble())
        put("salesmanChecked", salesmanChecked)
        put("helperChecked", helperChecked)
    }

    companion object {
        fun fromJson(obj: JSONObject): ColumnConfig = ColumnConfig(
            headerText = obj.optString("headerText", ""),
            widthCm = obj.optDouble("widthCm", 2.5).toFloat(),
            salesmanChecked = obj.optBoolean("salesmanChecked", false),
            helperChecked = obj.optBoolean("helperChecked", false)
        )
    }
}

data class RowConfig(
    var heightCm: Float = 0.7f,
    val cells: MutableList<CellData>
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("heightCm", heightCm.toDouble())
        put("cells", JSONArray().apply {
            cells.forEach { put(it.toJson()) }
        })
    }

    companion object {
        fun fromJson(obj: JSONObject): RowConfig {
            val cellList = mutableListOf<CellData>()
            val arr = obj.optJSONArray("cells")
            if (arr != null) {
                for (i in 0 until arr.length()) {
                    arr.optJSONObject(i)?.let { cellList.add(CellData.fromJson(it)) }
                }
            }
            return RowConfig(
                heightCm = obj.optDouble("heightCm", 0.7).toFloat(),
                cells = cellList
            )
        }
    }
}

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
    val salesmanNames: MutableList<String> = mutableListOf(),
    val helperNames: MutableList<String> = mutableListOf(),
    val columns: MutableList<ColumnConfig> = mutableListOf(),
    val rows: MutableList<RowConfig> = mutableListOf()
) {
    fun isColumnChecked(colIdx: Int, mode: AssignmentMode): Boolean {
        val col = columns.getOrNull(colIdx) ?: return false
        return when (mode) {
            AssignmentMode.SALESMAN -> col.salesmanChecked
            AssignmentMode.HELPER -> col.helperChecked
            AssignmentMode.NONE -> false
        }
    }

    fun setColumnChecked(colIdx: Int, mode: AssignmentMode, checked: Boolean) {
        val col = columns.getOrNull(colIdx) ?: return
        when (mode) {
            AssignmentMode.SALESMAN -> col.salesmanChecked = checked
            AssignmentMode.HELPER -> col.helperChecked = checked
            AssignmentMode.NONE -> Unit
        }
    }

    fun getUsedNamesInCheckedColumns(mode: AssignmentMode, excludeRow: Int, excludeCol: Int): Set<String> {
        val used = mutableSetOf<String>()
        for (r in rows.indices) {
            for (c in columns.indices) {
                if (r == excludeRow && c == excludeCol) continue
                if (isColumnChecked(c, mode)) {
                    val value = rows[r].cells.getOrNull(c)?.text?.trim().orEmpty()
                    if (value.isNotEmpty()) {
                        used.add(value)
                    }
                }
            }
        }
        return used
    }

    fun toJsonString(): String {
        val root = JSONObject().apply {
            put("mainTitle", mainTitle)
            put("showMainTitle", showMainTitle)
            put("subHeaderLeft", subHeaderLeft)
            put("subHeaderMiddle", subHeaderMiddle)
            put("subHeaderRight", subHeaderRight)
            put("showSubHeader", showSubHeader)
            put("subHeaderRadiusDp", subHeaderRadiusDp.toDouble())
            put("borderWidthDp", borderWidthDp.toDouble())
            put("borderColor", borderColor)
            put("tableCornerRadiusDp", tableCornerRadiusDp.toDouble())
            put("fontSizeSp", fontSizeSp.toDouble())
            put("salesmanNames", JSONArray().apply { salesmanNames.forEach { put(it) } })
            put("helperNames", JSONArray().apply { helperNames.forEach { put(it) } })
            put("columns", JSONArray().apply { columns.forEach { put(it.toJson()) } })
            put("rows", JSONArray().apply { rows.forEach { put(it.toJson()) } })
        }
        return root.toString()
    }

    companion object {
        fun fromJsonString(json: String): TableDocument? {
            return try {
                val obj = JSONObject(json)
                val doc = TableDocument(
                    mainTitle = obj.optString("mainTitle", "व्हि. आर. पवार सारीज"),
                    showMainTitle = obj.optBoolean("showMainTitle", true),
                    subHeaderLeft = obj.optString("subHeaderLeft", "सेनापतीचे नाव :"),
                    subHeaderMiddle = obj.optString("subHeaderMiddle", "तारीख :"),
                    subHeaderRight = obj.optString("subHeaderRight", "/ वार :"),
                    showSubHeader = obj.optBoolean("showSubHeader", true),
                    subHeaderRadiusDp = obj.optDouble("subHeaderRadiusDp", 12.0).toFloat(),
                    borderWidthDp = obj.optDouble("borderWidthDp", 1.2).toFloat(),
                    borderColor = obj.optInt("borderColor", Color.BLACK),
                    tableCornerRadiusDp = obj.optDouble("tableCornerRadiusDp", 8.0).toFloat(),
                    fontSizeSp = obj.optDouble("fontSizeSp", 12.0).toFloat()
                )

                obj.optJSONArray("salesmanNames")?.let { arr ->
                    for (i in 0 until arr.length()) {
                        val s = arr.optString(i, "").trim()
                        if (s.isNotEmpty()) doc.salesmanNames.add(s)
                    }
                }

                obj.optJSONArray("helperNames")?.let { arr ->
                    for (i in 0 until arr.length()) {
                        val s = arr.optString(i, "").trim()
                        if (s.isNotEmpty()) doc.helperNames.add(s)
                    }
                }

                obj.optJSONArray("columns")?.let { arr ->
                    for (i in 0 until arr.length()) {
                        arr.optJSONObject(i)?.let { doc.columns.add(ColumnConfig.fromJson(it)) }
                    }
                }

                obj.optJSONArray("rows")?.let { arr ->
                    for (i in 0 until arr.length()) {
                        arr.optJSONObject(i)?.let { doc.rows.add(RowConfig.fromJson(it)) }
                    }
                }

                if (doc.columns.isEmpty() || doc.rows.isEmpty()) null else doc
            } catch (_: Exception) {
                null
            }
        }

        fun createSampleLikeImage(): TableDocument {
            val doc = TableDocument()
            val defaultCols = listOf(
                ColumnConfig("क्र.", 1.1f),
                ColumnConfig("सेल्समन", 3.6f, salesmanChecked = true),
                ColumnConfig("वेळ", 1.8f),
                ColumnConfig("क्र.", 1.1f),
                ColumnConfig("हेल्पर", 3.8f, helperChecked = true),
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
