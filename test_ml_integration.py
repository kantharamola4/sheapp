#!/usr/bin/env python3
"""
Test script to verify ML model integration with Android app
"""

import os
import tensorflow as tf
import numpy as np

def test_model_compatibility():
    """Test if the TFLite model can be loaded and run"""
    print("🔍 Testing ML Model Integration...")
    
    # Check if model file exists
    model_path = "app/src/main/assets/emotion_model.tflite"
    if not os.path.exists(model_path):
        print(f"❌ Model file not found at {model_path}")
        return False
    
    print(f"✅ Model file found: {model_path}")
    
    # Test model loading
    try:
        interpreter = tf.lite.Interpreter(model_path=model_path)
        interpreter.allocate_tensors()
        print("✅ Model loaded successfully")
        
        # Get input/output details
        input_details = interpreter.get_input_details()
        output_details = interpreter.get_output_details()
        
        print(f"📊 Input shape: {input_details[0]['shape']}")
        print(f"📊 Output shape: {output_details[0]['shape']}")
        
        # Test with dummy data
        input_shape = input_details[0]['shape']
        dummy_input = np.random.random(input_shape).astype(np.float32)
        
        interpreter.set_tensor(input_details[0]['index'], dummy_input)
        interpreter.invoke()
        
        output_data = interpreter.get_tensor(output_details[0]['index'])
        print(f"✅ Model inference successful")
        print(f"📊 Output: {output_data}")
        
        return True
        
    except Exception as e:
        print(f"❌ Model loading failed: {e}")
        return False

def check_android_integration():
    """Check Android integration files"""
    print("\n🔍 Checking Android Integration...")
    
    # Check EmotionAnalyzer.kt
    analyzer_path = "app/src/main/java/com/example/shesafe/EmotionAnalyzer.kt"
    if os.path.exists(analyzer_path):
        print("✅ EmotionAnalyzer.kt found")
        
        with open(analyzer_path, 'r') as f:
            content = f.read()
            if "org.tensorflow.lite.Interpreter" in content:
                print("✅ TensorFlow Lite integration found")
            if "emotion_model.tflite" in content:
                print("✅ Model file reference found")
            if "detectEmotion" in content:
                print("✅ Emotion detection function found")
    else:
        print("❌ EmotionAnalyzer.kt not found")
    
    # Check build.gradle.kts
    build_path = "app/build.gradle.kts"
    if os.path.exists(build_path):
        print("✅ build.gradle.kts found")
        
        with open(build_path, 'r') as f:
            content = f.read()
            if "tensorflow-lite" in content:
                print("✅ TensorFlow Lite dependency found")
            if "mlModelBinding = true" in content:
                print("✅ ML Model Binding enabled")
    else:
        print("❌ build.gradle.kts not found")

def main():
    print("🚀 SheSafe ML Integration Test")
    print("=" * 40)
    
    model_ok = test_model_compatibility()
    check_android_integration()
    
    print("\n" + "=" * 40)
    if model_ok:
        print("🎉 ML Integration Status: READY")
        print("📱 Your Android app should be able to use emotion detection!")
    else:
        print("⚠️  ML Integration Status: NEEDS ATTENTION")
        print("🔧 Please check the model file and dependencies")

if __name__ == "__main__":
    main()
