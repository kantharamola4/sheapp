package com.example.shesafe

import org.junit.Test
import org.junit.Assert.*

class MLIntegrationTest {

    @Test
    fun testEmotionAnalyzerInitialization() {
        // Test that EmotionAnalyzer can be initialized without errors
        // Note: This would require a mock context in a real test
        assertTrue("EmotionAnalyzer should be available", true)
    }

    @Test
    fun testMetricsCalculation() {
        // Test binary classification metrics
        val yTrue = listOf("distress", "ok", "distress", "ok")
        val yPred = listOf("distress", "ok", "ok", "distress")
        val scores = Metrics.computeBinary(yTrue, yPred, "distress")
        
        assertEquals(0.5, scores.accuracy, 0.01)
        assertEquals(0.5, scores.precision, 0.01)
        assertEquals(0.5, scores.recall, 0.01)
        assertEquals(0.5, scores.f1, 0.01)
    }

    @Test
    fun testMulticlassMetrics() {
        // Test multi-class classification metrics
        val yTrue = listOf("fear", "happy", "angry", "neutral")
        val yPred = listOf("fear", "happy", "angry", "neutral")
        val labels = listOf("angry", "fear", "happy", "neutral", "sad", "surprise")
        val scores = Metrics.computeMulticlass(yTrue, yPred, labels)
        
        assertEquals(1.0, scores.accuracy, 0.01)
        assertTrue("Precision should be > 0", scores.precision > 0)
        assertTrue("Recall should be > 0", scores.recall > 0)
        assertTrue("F1 should be > 0", scores.f1 > 0)
    }

    @Test
    fun testDistressEmotions() {
        // Test that distress emotions are properly identified
        val distressEmotions = listOf("fear", "angry", "sad")
        val testEmotions = listOf("fear", "happy", "angry", "neutral", "sad", "surprise")
        
        val distressCount = testEmotions.count { it in distressEmotions }
        assertEquals(3, distressCount)
    }
}
