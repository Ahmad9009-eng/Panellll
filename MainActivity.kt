package com.example.steeringwheel

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.Bundle
import android.view.MotionEvent
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import kotlin.math.abs

class MainActivity : AppCompatActivity(), SensorEventListener {

    private lateinit var sensorManager: SensorManager
    private var gyroscope: Sensor? = null
    private var accelerometer: Sensor? = null
    
    private lateinit var bluetoothManager: BluetoothManager
    private var bluetoothAdapter: BluetoothAdapter? = null
    private var bluetoothSocket: BluetoothSocket? = null
    
    private var steeringValue = 0f // -100 to +100
    private var throttleValue = 0f // 0 to 100
    private var brakeValue = 0f // 0 to 100
    
    private var lastGyroX = 0f
    private var lastGyroY = 0f
    private var lastGyroZ = 0f
    
    private lateinit var steeringView: SteeringWheelView
    private lateinit var steeringValueText: TextView
    private lateinit var throttleValueText: TextView
    private lateinit var brakeValueText: TextView
    private lateinit var statusText: TextView
    private lateinit var connectButton: Button
    
    private var isConnected = false
    private val PERMISSIONS = arrayOf(
        Manifest.permission.BLUETOOTH,
        Manifest.permission.BLUETOOTH_ADMIN,
        Manifest.permission.BLUETOOTH_CONNECT,
        Manifest.permission.BLUETOOTH_SCAN,
        Manifest.permission.ACCESS_FINE_LOCATION
    )
    
    private val PERMISSION_REQUEST_CODE = 100

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        
        // Request permissions
        if (!hasAllPermissions()) {
            ActivityCompat.requestPermissions(this, PERMISSIONS, PERMISSION_REQUEST_CODE)
        }
        
        // Initialize UI components
        initializeUI()
        
        // Initialize sensors
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        gyroscope = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        
        // Initialize Bluetooth
        bluetoothManager = getSystemService(BluetoothManager::class.java)
        bluetoothAdapter = bluetoothManager.adapter
        
        // Start listening to sensors
        sensorManager.registerListener(this, gyroscope, SensorManager.SENSOR_DELAY_GAME)
        sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_GAME)
    }
    
    private fun hasAllPermissions(): Boolean {
        return PERMISSIONS.all {
            ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
        }
    }
    
    private fun initializeUI() {
        steeringView = findViewById(R.id.steeringWheelView)
        steeringValueText = findViewById(R.id.steeringValueText)
        throttleValueText = findViewById(R.id.throttleValueText)
        brakeValueText = findViewById(R.id.brakeValueText)
        statusText = findViewById(R.id.statusText)
        connectButton = findViewById(R.id.connectButton)
        
        connectButton.setOnClickListener {
            if (isConnected) {
                disconnectBluetooth()
            } else {
                startBluetoothDiscovery()
            }
        }
        
        updateStatusUI()
    }
    
    override fun onSensorChanged(event: SensorEvent) {
        when (event.sensor.type) {
            Sensor.TYPE_GYROSCOPE -> {
                // Use gyroscope for fine steering adjustment
                lastGyroX = event.values[0]
                lastGyroY = event.values[1]
                lastGyroZ = event.values[2]
                
                // Adjust steering based on gyroscope rotation around Z axis
                steeringValue = (lastGyroZ * 50f).coerceIn(-100f, 100f)
            }
            Sensor.TYPE_ACCELEROMETER -> {
                // Use accelerometer for base steering
                val accelX = event.values[0]
                val accelY = event.values[1]
                
                // Map tilt angle to steering value
                val tiltAngle = (accelX / 9.8f * 100f).coerceIn(-100f, 100f)
                
                // Combine gyro and accelerometer for smooth steering
                if (abs(lastGyroZ) < 0.5f) {
                    steeringValue = tiltAngle
                } else {
                    steeringValue = (tiltAngle * 0.3f + steeringValue * 0.7f).coerceIn(-100f, 100f)
                }
            }
        }
        
        // Update UI
        updateSteering()
        
        // Send data via Bluetooth
        if (isConnected) {
            sendBluetoothData()
        }
    }
    
    override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {}
    
    private fun updateSteering() {
        steeringView.setSteering(steeringValue)
        steeringValueText.text = String.format("%.0f°", steeringValue)
    }
    
    private fun sendBluetoothData() {
        try {
            bluetoothSocket?.outputStream?.apply {
                // Create data packet: STEERING|THROTTLE|BRAKE
                val data = "${steeringValue.toInt()}|${throttleValue.toInt()}|${brakeValue.toInt()}\n"
                write(data.toByteArray())
                flush()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            disconnectBluetooth()
        }
    }
    
    private fun startBluetoothDiscovery() {
        try {
            if (ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.BLUETOOTH_SCAN
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                return
            }
            
            // Get paired devices and try to connect to first one
            val pairedDevices = bluetoothAdapter?.bondedDevices
            if (pairedDevices != null && pairedDevices.isNotEmpty()) {
                val device = pairedDevices.first()
                connectToDevice(device.address)
            } else {
                Toast.makeText(this, "No paired Bluetooth devices found", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Bluetooth error: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun connectToDevice(deviceAddress: String) {
        try {
            if (ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.BLUETOOTH_CONNECT
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                return
            }
            
            val device = bluetoothAdapter?.getRemoteDevice(deviceAddress)
            bluetoothSocket = device?.createRfcommSocketToServiceRecord(
                java.util.UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
            )
            
            bluetoothSocket?.connect()
            isConnected = true
            statusText.text = "Connected: ${device?.name}"
            connectButton.text = "Disconnect"
            
            Toast.makeText(this, "Connected to ${device?.name}", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            e.printStackTrace()
            isConnected = false
            statusText.text = "Failed to connect"
            Toast.makeText(this, "Connection failed: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun disconnectBluetooth() {
        try {
            bluetoothSocket?.close()
            bluetoothSocket = null
            isConnected = false
            statusText.text = "Disconnected"
            connectButton.text = "Connect"
            Toast.makeText(this, "Disconnected", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    private fun updateStatusUI() {
        throttleValueText.text = "${throttleValue.toInt()}%"
        brakeValueText.text = "${brakeValue.toInt()}%"
        statusText.text = if (isConnected) "Connected" else "Disconnected"
    }
    
    override fun onDestroy() {
        super.onDestroy()
        sensorManager.unregisterListener(this)
        disconnectBluetooth()
    }
    
    override fun onTouchEvent(event: MotionEvent): Boolean {
        val x = event.x
        val y = event.y
        val width = window.decorView.width.toFloat()
        val height = window.decorView.height.toFloat()
        
        // Right side - throttle (top to bottom: 0 to 100)
        if (x > width * 0.8f) {
            throttleValue = ((height - y) / height * 100f).coerceIn(0f, 100f)
            brakeValue = 0f
            throttleValueText.text = "${throttleValue.toInt()}%"
            brakeValueText.text = "0%"
        }
        // Left side - brake (top to bottom: 0 to 100)
        else if (x < width * 0.2f) {
            brakeValue = ((height - y) / height * 100f).coerceIn(0f, 100f)
            throttleValue = 0f
            throttleValueText.text = "0%"
            brakeValueText.text = "${brakeValue.toInt()}%"
        }
        
        return super.onTouchEvent(event)
    }
}
