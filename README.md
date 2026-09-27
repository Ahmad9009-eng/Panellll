# Steering Wheel Controller 🎮

A professional Android phone-based steering wheel controller with Bluetooth connectivity for car racing games.

## Features

✅ **Gyroscope-based steering** - Tilt your phone left/right to steer (-100° to +100°)  
✅ **Real-time Bluetooth connectivity** - Connect to any device via Bluetooth  
✅ **Throttle & Brake controls** - Touch left/right sides to control gas and brake  
✅ **Gaming UI** - Professional dark mode with live steering visualization  
✅ **Works with all car games** - Compatible with GTA, Racing games, etc.  
✅ **Connection status display** - Shows real-time Bluetooth connection status  

## Installation

### Requirements
- Android 7.0+ (API 24+)
- Phone with gyroscope and accelerometer sensors
- Bluetooth capability

### Setup Steps

1. **Open Android Studio**
2. **Create a new project** with these details:
   - Name: `SteeringWheelController`
   - Package: `com.example.steeringwheel`
   - Min SDK: 24
3. **Replace files:**
   - Copy `MainActivity.kt` → `app/src/main/java/com/example/steeringwheel/`
   - Copy `SteeringWheelView.kt` → `app/src/main/java/com/example/steeringwheel/`
   - Copy `activity_main.xml` → `app/src/main/res/layout/`
   - Copy `AndroidManifest.xml` → `app/src/main/`
   - Copy `build.gradle` → `app/`
   - Copy `strings.xml` → `app/src/main/res/values/`
   - Copy `styles.xml` → `app/src/main/res/values/`

4. **Build & Run** on your Android device

## How to Use

### On Your Phone (Steering Controller)

1. **Launch the app** - Opens in landscape mode
2. **Press "Connect"** - Scans for paired Bluetooth devices
3. **Select device** - Connects to the first paired device
4. **Tilt to steer** - Rotate phone left/right to control steering wheel
5. **Throttle** - Tap right side of screen and drag up/down (0-100%)
6. **Brake** - Tap left side of screen and drag up/down (0-100%)

### Control Layout

```
┌─────────────────────────────────────────┐
│ BRAKE │  STEERING WHEEL  │ THROTTLE    │
│ ↕     │  ●  ⌘  ↔      │ ↕             │
│ Value │  Visualization  │ Value        │
└─────────────────────────────────────────┘
```

- **Left side (BRAKE)**: Swipe up/down to apply brakes
- **Center (STEERING)**: Tilt phone to steer
- **Right side (THROTTLE)**: Swipe up/down for acceleration
- **Status bar**: Shows connection status and steering angle

## Data Protocol (Bluetooth)

The app sends steering data in this format via Bluetooth:

```
STEERING_VALUE|THROTTLE_VALUE|BRAKE_VALUE
Example: -45|75|0
```

### Values
- **STEERING**: -100 to +100 (0 = center)
- **THROTTLE**: 0 to 100 (0 = idle, 100 = full)
- **BRAKE**: 0 to 100 (0 = no brake, 100 = full)

## Receiver Examples

### Windows PC (Python)

```python
import socket
import threading

def receive_steering_data(port=8888):
    server = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    server.bind(('0.0.0.0', port))
    server.listen(1)
    
    print("Waiting for connection...")
    client, addr = server.accept()
    print(f"Connected from {addr}")
    
    while True:
        try:
            data = client.recv(1024).decode()
            if data:
                steering, throttle, brake = data.strip().split('|')
                print(f"Steering: {steering}° | Throttle: {throttle}% | Brake: {brake}%")
        except:
            break
    
    client.close()

if __name__ == "__main__":
    receive_steering_data()
```

### Another Android Device (Receiver)

```kotlin
// Listen for incoming Bluetooth data
private fun startBluetoothServer() {
    val serverSocket = bluetoothAdapter?.listenUsingRfcommWithServiceRecord(
        "SteeringWheel",
        UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
    )
    
    Thread {
        val clientSocket = serverSocket?.accept()
        val inputStream = clientSocket?.inputStream
        
        val buffer = ByteArray(1024)
        var bytes: Int
        
        while (inputStream?.read(buffer)?.also { bytes = it } != -1) {
            val data = String(buffer, 0, bytes)
            val (steering, throttle, brake) = data.trim().split("|")
            
            println("Steering: $steering | Throttle: $throttle | Brake: $brake")
            // Use values to control game
        }
    }.start()
}
```

## Pairing Bluetooth Devices

### On Android:

1. Go to **Settings** → **Bluetooth**
2. Turn on Bluetooth
3. Make target device discoverable
4. Tap device name to pair
5. Return to steering app and press "Connect"

### For Windows PC:

1. Go to **Settings** → **Devices** → **Bluetooth**
2. Enable Bluetooth
3. Make your Android phone discoverable
4. Add device and pair
5. Connect from the app

## Sensor Calibration

The steering uses:
- **Gyroscope** - For real-time rotation (Z-axis rotation = steering)
- **Accelerometer** - For tilt angle (X-axis tilt = steering)

The app automatically combines both for smooth, responsive steering.

## Troubleshooting

### Connection Fails
- ✓ Ensure device is paired first
- ✓ Check Bluetooth permissions in app settings
- ✓ Restart Bluetooth on both devices
- ✓ Keep devices within 10 meters

### Steering Not Responsive
- ✓ Tilt phone at least 30 degrees
- ✓ Ensure gyroscope is working (check Settings → Motion)
- ✓ Restart the app

### No Throttle/Brake
- ✓ Make sure you're touching the left/right edges
- ✓ Drag up and down on the control panels
- ✓ Try longer swipe gestures

## Customization

### Change Colors
Edit `SteeringWheelView.kt`:
```kotlin
private val wheelBorderPaint = Paint().apply {
    color = Color.parseColor("#YOUR_COLOR") // Change this
}
```

### Adjust Sensitivity
Edit `MainActivity.kt`:
```kotlin
steeringValue = (tiltAngle * 0.5f + steeringValue * 0.5f) // Change weights
```

### Change Orientation
In `AndroidManifest.xml`:
```xml
android:screenOrientation="landscape" <!-- Change to portrait -->
```

## Game Integration

### For GTA V / GTA Online:
1. Connect controller
2. In GTA settings: **Controller Configuration**
3. Select your Bluetooth device
4. Map steering to analog stick (should auto-detect)

### For Racing Games:
- Usually auto-detects as a standard gamepad
- Maps steering to X-axis
- Maps throttle to RT button
- Maps brake to LT button

## Performance Tips

- 🎮 Use landscape orientation for best steering experience
- 📱 Close other apps for optimal sensor responsiveness
- 🔋 Keep phone plugged in for extended sessions
- 📡 Minimize Bluetooth interference (keep away from WiFi/microwaves)

## License

Open source - Feel free to modify and share!

## Support

If you need help:
1. Check the Troubleshooting section
2. Verify Bluetooth permissions
3. Ensure sensors are working (check device Settings)
4. Test with multiple games

Enjoy racing! 🏎️⚡
