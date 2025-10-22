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

# -----------------------------
# 1. Download Dataset (RAVDESS subset)
# -----------------------------
DATASET_URL = "https://zenodo.org/record/1188976/files/Audio_Speech_Actors_01-24.zip"
DATASET_PATH = "ravdess.zip"
EXTRACT_PATH = "ravdess_data"

if not os.path.exists(DATASET_PATH):
    print("Downloading dataset...")
    r = requests.get(DATASET_URL, stream=True)
    with open(DATASET_PATH, "wb") as f:
        f.write(r.content)

if not os.path.exists(EXTRACT_PATH):
    print("Extracting dataset...")
    with zipfile.ZipFile(DATASET_PATH, "r") as zip_ref:
        zip_ref.extractall(EXTRACT_PATH)

# -----------------------------
# 2. Feature Extraction (Optimized for Android)
# -----------------------------
def extract_features(file_path, max_pad_len=40):
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
        print("Error encountered while parsing file: ", file_path)
        return None

X, Y = [], []
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

print("Extracting features...")
for root, dirs, files in os.walk(EXTRACT_PATH):
    for file in files:
        if file.endswith(".wav"):
            file_path = os.path.join(root, file)
            emotion = emotion_map[file.split("-")[2]]
            data = extract_features(file_path)
            if data is not None:
                X.append(data)
                Y.append(emotion)

X = np.array(X)
X = X.reshape(X.shape[0], 40, 40, 1)  # reshape for CNN/LSTM
Y = np.array(Y)

# Encode labels
lb = LabelEncoder()
Y_encoded = lb.fit_transform(Y)
Y = to_categorical(Y_encoded)

print(f"Classes: {lb.classes_}")
print(f"Shape: {X.shape}, Labels: {Y.shape}")

# Train-test split
X_train, X_test, y_train, y_test = train_test_split(X, Y, test_size=0.2, random_state=42)

# -----------------------------
# 3. Optimized Model for Mobile
# -----------------------------
model = Sequential()
model.add(Reshape((40, 40), input_shape=(40, 40, 1)))
model.add(LSTM(64, return_sequences=True))  # Reduced from 128
model.add(Dropout(0.2))  # Reduced dropout
model.add(LSTM(32))      # Reduced from 64
model.add(Dense(16, activation='relu'))  # Reduced from 32
model.add(Dense(Y.shape[1], activation='softmax'))

model.compile(loss='categorical_crossentropy', optimizer='adam', metrics=['accuracy'])

print("Training model...")
model.fit(X_train, y_train, epochs=15, batch_size=32, validation_data=(X_test, y_test))

# -----------------------------
# 4. Evaluate Model
# -----------------------------
test_loss, test_acc = model.evaluate(X_test, y_test)
print(f"Test accuracy: {test_acc:.4f}")

# -----------------------------
# 5. Save Model
# -----------------------------
model.save("emotion_model.h5")
print("Model saved as emotion_model.h5")

# -----------------------------
# 6. Convert to TFLite (Optimized)
# -----------------------------
converter = tf.lite.TFLiteConverter.from_keras_model(model)
# Optimize for mobile deployment
converter.optimizations = [tf.lite.Optimize.DEFAULT]
converter.target_spec.supported_types = [tf.float16]  # Use float16 for smaller size
tflite_model = converter.convert()

with open("emotion_model.tflite", "wb") as f:
    f.write(tflite_model)

print("Optimized TFLite model saved as emotion_model.tflite")
print(f"Model size: {len(tflite_model) / 1024:.2f} KB")

# -----------------------------
# 7. Test Model with Sample Audio
# -----------------------------
def test_model_with_audio(audio_file):
    features = extract_features(audio_file)
    if features is not None:
        features = features.reshape(1, 40, 40, 1)
        prediction = model.predict(features)
        emotion_idx = np.argmax(prediction[0])
        emotion = lb.classes_[emotion_idx]
        confidence = prediction[0][emotion_idx]
        print(f"Predicted emotion: {emotion} (confidence: {confidence:.3f})")
        return emotion, confidence
    return None, 0

print("\nModel ready for Android integration!")
print("Copy emotion_model.tflite to: sheapp/app/src/main/ml/")
