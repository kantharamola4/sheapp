# SheSafe - AI-Powered Safety App

An intelligent mobile safety application that automatically detects distress through voice emotion recognition and camera-based face detection, providing instant emergency response without manual input.

## 🚀 Features

### Core Safety Features
- **Automatic Distress Detection**: Uses AI to detect panic, fear, or yelling in real-time
- **Voice Emotion Recognition**: TensorFlow Lite-powered emotion analysis from audio
- **Face Detection**: ML Kit integration for face presence verification
- **Background Monitoring**: Continuous safety monitoring via foreground service
- **Instant Emergency Response**: Automatic SMS alerts with location and evidence

### ML-Powered Detection
- **Emotion Analysis**: Detects 6 emotions (angry, fear, happy, neutral, sad, surprise)
- **Distress Detection**: Audio amplitude analysis for yelling/screaming detection
- **Hotword Detection**: Voice command recognition for "help me" phrases
- **Face Verification**: Camera-based face presence detection for emergency validation

### Emergency Response
- **Location Sharing**: GPS coordinates sent via SMS and Firebase
- **Evidence Capture**: Silent photo and audio recording during emergencies
- **Multi-Channel Alerts**: SMS fallback + Firebase cloud storage
- **Emergency Contacts**: Manage trusted contacts for emergency notifications

## 🛠 Technical Implementation

### Architecture
- **Language**: Kotlin
- **UI**: Material Design with custom glass morphism effects
- **ML Framework**: TensorFlow Lite + ML Kit
- **Backend**: Firebase (Firestore + Storage)
- **Background Processing**: WorkManager + Foreground Services

### ML Components
- **EmotionAnalyzer**: PCM-based audio emotion classification
- **DistressDetector**: Real-time audio amplitude monitoring
- **FaceVerifier**: ML Kit face detection integration
- **HotwordDetector**: Speech recognition for emergency phrases

### Performance Metrics
- **Accuracy Calculation**: Binary and multi-class classification metrics
- **Precision/Recall**: F1-score computation for model evaluation
- **Confidence Scoring**: Softmax probability outputs for predictions

## 📱 User Interface

### Main Screen
- Emergency SOS button
- Profile management
- Emergency contacts
- Emotion detection testing
- Real-time ML status indicator

### Settings
- Face verification requirement toggle
- Hotword detection enable/disable
- Emergency contact management

### Profile Management
- Personal information storage
- Medical information (blood type, age)
- Contact details

## 🔧 Setup Instructions

### Prerequisites
- Android Studio Arctic Fox or later
- Android SDK 26+ (API level 26)
- Google Play Services
- Firebase project setup

### Installation
1. Clone the repository
2. Open in Android Studio
3. Set up Firebase project and add `google-services.json`
4. Place emotion model at `app/src/main/ml/emotion_model.tflite`
5. Build and run on device/emulator

### Required Permissions
- `RECORD_AUDIO` - For voice emotion detection
- `CAMERA` - For face detection and evidence capture
- `ACCESS_FINE_LOCATION` - For emergency location sharing
- `SEND_SMS` - For emergency SMS alerts
- `FOREGROUND_SERVICE` - For background monitoring

## 🧠 ML Model Integration

### Emotion Detection Pipeline
1. **Audio Capture**: 16kHz mono PCM via AudioRecord
2. **Preprocessing**: Normalization and fixed-length windowing
3. **Inference**: TensorFlow Lite model execution
4. **Post-processing**: Softmax probability calculation
5. **Decision**: Confidence-based emotion classification

### Model Requirements
- Input: 16,000 samples (1 second at 16kHz)
- Output: 6 emotion probabilities
- Format: TensorFlow Lite (.tflite)
- Location: `app/src/main/ml/emotion_model.tflite`

### Performance Evaluation
```kotlin
// Binary classification (distress vs non-distress)
val scores = Metrics.computeBinary(yTrue, yPred, "distress")

// Multi-class emotion classification
val scores = Metrics.computeMulticlass(yTrue, yPred, emotions)
```

## 🔒 Privacy & Security

- **Local Processing**: All ML inference happens on-device
- **Encrypted Storage**: Sensitive data stored securely
- **Minimal Permissions**: Only essential permissions requested
- **User Control**: Full control over emergency contacts and settings

## 📊 Accuracy & Performance

### Emotion Recognition
- **Input**: 16kHz mono PCM audio
- **Window Size**: 1 second
- **Emotions**: 6 classes (angry, fear, happy, neutral, sad, surprise)
- **Confidence**: Softmax probability outputs

### Distress Detection
- **Method**: Audio amplitude analysis
- **Threshold**: 4x baseline or >10,000 amplitude
- **Calibration**: 50-frame baseline establishment

### Face Detection
- **Framework**: ML Kit Face Detection
- **Use Case**: Emergency validation
- **Timeout**: 2-second maximum wait

## 🚨 Emergency Response Flow

1. **Detection**: ML systems detect distress indicators
2. **Validation**: Optional face presence verification
3. **Evidence Capture**: Silent photo and audio recording
4. **Location Fetch**: GPS coordinates retrieval
5. **Alert Dispatch**: SMS + Firebase cloud upload
6. **Monitoring Resume**: Continue background safety monitoring

## 🔄 Background Services

### SafetyMonitorService
- Continuous emotion analysis every 5 seconds
- Distress detection monitoring
- Hotword detection
- Emergency response coordination

### EmergencyUploader
- WorkManager-based evidence upload
- Firebase Firestore metadata storage
- Firebase Storage file upload
- Offline queue management

## 📈 Future Enhancements

- Real-time safety data integration
- Offline GPS fallback improvements
- Enhanced emotion model training
- Multi-language hotword support
- Advanced threat assessment algorithms

## 🤝 Contributing

1. Fork the repository
2. Create feature branch
3. Implement changes
4. Add tests for new functionality
5. Submit pull request

## 📄 License

This project is licensed under the MIT License - see the LICENSE file for details.

## ⚠️ Disclaimer

This app is designed for emergency situations and should be used responsibly. Always ensure you have proper emergency contacts configured and test the app in safe environments before relying on it in actual emergency situations.

---

**Built with ❤️ for women's safety everywhere**
