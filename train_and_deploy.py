#!/usr/bin/env python3
"""
SheSafe Emotion Detection Model Training and Deployment Script
This script trains an optimized emotion detection model for the SheSafe Android app.
"""

import os
import zipfile
import requests
import librosa
import numpy as np
import tensorflow as tf
from tensorflow.keras.models import Sequential
from tensorflow.keras.layers import LSTM, Dense, Dropout, Reshape
from sklearn.model_selection import train_test_split
from sklearn.preprocessing import LabelEncoder
from tensorflow.keras.utils import to_categorical
import matplotlib.pyplot as plt

def download_dataset():
    """Download and extract RAVDESS dataset"""
    DATASET_URL = "https://zenodo.org/record/1188976/files/Audio_Speech_Actors_01-24.zip"
    DATASET_PATH = "ravdess.zip"
    EXTRACT_PATH = "ravdess_data"
    
    if not os.path.exists(DATASET_PATH):
        print("📥 Downloading RAVDESS dataset...")
        r = requests.get(DATASET_URL, stream=True)
        with open(DATASET_PATH, "wb") as f:
            for chunk in r.iter_content(chunk_size=8192):
                f.write(chunk)
        print("✅ Dataset downloaded")
    
    if not os.path.exists(EXTRACT_PATH):
        print("📦 Extracting dataset...")
        with zipfile.ZipFile(DATASET_PATH, "r") as zip_ref:
            zip_ref.extractall(EXTRACT_PATH)
        print("✅ Dataset extracted")
    
    return EXTRACT_PATH

def extract_features(file_path, max_pad_len=40):
    """Extract MFCC features optimized for mobile deployment"""
    try:
        # Use 16kHz sample rate to match Android app
        audio, sample_rate = librosa.load(file_path, res_type='kaiser_fast', duration=1.0, sr=16000, offset=0.5)
        mfccs = librosa.feature.mfcc(y=audio, sr=sample_rate, n_mfcc=40)
        pad_width = max_pad_len - mfccs.shape[1]
        if pad_width > 0:
            mfccs = np.pad(mfccs, pad_width=((0, 0), (0, pad_width)), mode='constant')
        else:
            mfccs = mfccs[:, :max_pad_len]
        return mfccs
    except Exception as e:
        print(f"❌ Error processing {file_path}: {e}")
        return None

def load_and_preprocess_data(extract_path):
    """Load and preprocess the dataset"""
    print("🔄 Loading and preprocessing data...")
    
    # Map to 6 emotions that Android app expects
    emotion_map = {
        '01': 'neutral',
        '02': 'neutral',  # calm -> neutral
        '03': 'happy',
        '04': 'sad',
        '05': 'angry',
        '06': 'fear',
        '07': 'sad',      # disgust -> sad
        '08': 'surprise'
    }
    
    X, Y = [], []
    
    for root, dirs, files in os.walk(extract_path):
        for file in files:
            if file.endswith(".wav"):
                file_path = os.path.join(root, file)
                # emotion is in filename, e.g., "03-01-05-01-02-01-12.wav"
                emotion = emotion_map[file.split("-")[2]]
                data = extract_features(file_path)
                if data is not None:
                    X.append(data)
                    Y.append(emotion)
    
    X = np.array(X)
    X = X.reshape(X.shape[0], 40, 40, 1)  # reshape for CNN/LSTM
    Y = np.array(Y)
    
    print(f"📊 Dataset loaded: {X.shape[0]} samples")
    print(f"📊 Features shape: {X.shape}")
    print(f"📊 Classes: {set(Y)}")
    
    return X, Y

def create_model(input_shape, num_classes):
    """Create optimized LSTM model for mobile deployment"""
    print("🏗️ Building model architecture...")
    
    model = Sequential([
        Reshape((40, 40), input_shape=input_shape),
        LSTM(64, return_sequences=True, name='lstm1'),
        Dropout(0.2, name='dropout1'),
        LSTM(32, name='lstm2'),
        Dense(16, activation='relu', name='dense1'),
        Dense(num_classes, activation='softmax', name='output')
    ])
    
    model.compile(
        loss='categorical_crossentropy',
        optimizer='adam',
        metrics=['accuracy']
    )
    
    print("✅ Model created")
    return model

def train_model(model, X_train, y_train, X_test, y_test):
    """Train the model with callbacks"""
    print("🎯 Training model...")
    
    # Callbacks for better training
    callbacks = [
        tf.keras.callbacks.EarlyStopping(patience=5, restore_best_weights=True),
        tf.keras.callbacks.ReduceLROnPlateau(factor=0.5, patience=3)
    ]
    
    history = model.fit(
        X_train, y_train,
        epochs=20,
        batch_size=32,
        validation_data=(X_test, y_test),
        callbacks=callbacks,
        verbose=1
    )
    
    print("✅ Training completed")
    return history

def evaluate_model(model, X_test, y_test, lb):
    """Evaluate model performance"""
    print("📈 Evaluating model...")
    
    test_loss, test_acc = model.evaluate(X_test, y_test, verbose=0)
    print(f"📊 Test Accuracy: {test_acc:.4f}")
    print(f"📊 Test Loss: {test_loss:.4f}")
    
    # Predictions for detailed analysis
    predictions = model.predict(X_test)
    predicted_classes = np.argmax(predictions, axis=1)
    true_classes = np.argmax(y_test, axis=1)
    
    # Confusion matrix
    from sklearn.metrics import confusion_matrix, classification_report
    cm = confusion_matrix(true_classes, predicted_classes)
    
    print("\n📊 Classification Report:")
    print(classification_report(true_classes, predicted_classes, target_names=lb.classes_))
    
    return test_acc, test_loss

def convert_to_tflite(model, output_path="emotion_model.tflite"):
    """Convert model to TensorFlow Lite format"""
    print("🔄 Converting to TensorFlow Lite...")
    
    converter = tf.lite.TFLiteConverter.from_keras_model(model)
    # Optimize for mobile deployment
    converter.optimizations = [tf.lite.Optimize.DEFAULT]
    converter.target_spec.supported_types = [tf.float16]  # Use float16 for smaller size
    
    tflite_model = converter.convert()
    
    with open(output_path, "wb") as f:
        f.write(tflite_model)
    
    print(f"✅ TFLite model saved: {output_path}")
    print(f"📦 Model size: {len(tflite_model) / 1024:.2f} KB")
    
    return output_path

def deploy_to_android(tflite_path):
    """Copy model to Android project"""
    android_path = "sheapp/app/src/main/ml/emotion_model.tflite"
    
    if os.path.exists("sheapp"):
        import shutil
        shutil.copy2(tflite_path, android_path)
        print(f"✅ Model deployed to Android: {android_path}")
    else:
        print(f"⚠️ Android project not found. Please copy {tflite_path} to {android_path}")

def main():
    """Main training pipeline"""
    print("🚀 SheSafe Emotion Detection Model Training")
    print("=" * 50)
    
    # 1. Download dataset
    extract_path = download_dataset()
    
    # 2. Load and preprocess data
    X, Y = load_and_preprocess_data(extract_path)
    
    # 3. Encode labels
    lb = LabelEncoder()
    Y_encoded = lb.fit_transform(Y)
    Y_categorical = to_categorical(Y_encoded)
    
    print(f"📊 Classes: {lb.classes_}")
    
    # 4. Train-test split
    X_train, X_test, y_train, y_test = train_test_split(
        X, Y_categorical, test_size=0.2, random_state=42, stratify=Y_categorical
    )
    
    print(f"📊 Training set: {X_train.shape[0]} samples")
    print(f"📊 Test set: {X_test.shape[0]} samples")
    
    # 5. Create model
    model = create_model((40, 40, 1), len(lb.classes_))
    model.summary()
    
    # 6. Train model
    history = train_model(model, X_train, y_train, X_test, y_test)
    
    # 7. Evaluate model
    test_acc, test_loss = evaluate_model(model, X_test, y_test, lb)
    
    # 8. Save model
    model.save("emotion_model.h5")
    print("✅ Keras model saved: emotion_model.h5")
    
    # 9. Convert to TFLite
    tflite_path = convert_to_tflite(model)
    
    # 10. Deploy to Android
    deploy_to_android(tflite_path)
    
    print("\n🎉 Training completed successfully!")
    print("📱 Your SheSafe app is ready with the trained emotion detection model!")
    
    # 11. Plot training history
    try:
        plt.figure(figsize=(12, 4))
        
        plt.subplot(1, 2, 1)
        plt.plot(history.history['accuracy'], label='Training Accuracy')
        plt.plot(history.history['val_accuracy'], label='Validation Accuracy')
        plt.title('Model Accuracy')
        plt.xlabel('Epoch')
        plt.ylabel('Accuracy')
        plt.legend()
        
        plt.subplot(1, 2, 2)
        plt.plot(history.history['loss'], label='Training Loss')
        plt.plot(history.history['val_loss'], label='Validation Loss')
        plt.title('Model Loss')
        plt.xlabel('Epoch')
        plt.ylabel('Loss')
        plt.legend()
        
        plt.tight_layout()
        plt.savefig('training_history.png')
        print("📊 Training history saved: training_history.png")
    except Exception as e:
        print(f"⚠️ Could not save training plots: {e}")

if __name__ == "__main__":
    main()
