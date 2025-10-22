package com.example.shesafe

object Metrics {
    data class Scores(
        val accuracy: Double,
        val precision: Double,
        val recall: Double,
        val f1: Double
    )

    // For binary classification where positiveLabel is considered the positive class
    fun computeBinary(yTrue: List<String>, yPred: List<String>, positiveLabel: String): Scores {
        require(yTrue.size == yPred.size) { "Mismatched lengths" }
        var tp = 0
        var tn = 0
        var fp = 0
        var fn = 0
        for (i in yTrue.indices) {
            val t = yTrue[i] == positiveLabel
            val p = yPred[i] == positiveLabel
            if (p && t) tp++ else if (p && !t) fp++ else if (!p && t) fn++ else tn++
        }
        val acc = (tp + tn).toDouble() / (tp + tn + fp + fn).coerceAtLeast(1)
        val prec = if (tp + fp == 0) 0.0 else tp.toDouble() / (tp + fp)
        val rec = if (tp + fn == 0) 0.0 else tp.toDouble() / (tp + fn)
        val f1 = if (prec + rec == 0.0) 0.0 else 2 * prec * rec / (prec + rec)
        return Scores(acc, prec, rec, f1)
    }

    // Macro-averaged metrics for multi-class classification
    fun computeMulticlass(yTrue: List<String>, yPred: List<String>, labels: List<String>): Scores {
        require(yTrue.size == yPred.size) { "Mismatched lengths" }
        var sumPrec = 0.0
        var sumRec = 0.0
        var sumF1 = 0.0
        val acc = yTrue.zip(yPred).count { it.first == it.second }.toDouble() / yTrue.size.coerceAtLeast(1)
        for (label in labels) {
            val oneVsAll = computeBinary(yTrue, yPred, label)
            sumPrec += oneVsAll.precision
            sumRec += oneVsAll.recall
            sumF1 += oneVsAll.f1
        }
        val k = labels.size.coerceAtLeast(1)
        return Scores(
            accuracy = acc,
            precision = sumPrec / k,
            recall = sumRec / k,
            f1 = sumF1 / k
        )
    }
}


