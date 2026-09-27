#!/usr/bin/env python3
"""
Bluetooth Receiver for Steering Wheel Controller
Receives steering, throttle, and brake data from the Android app
Works on Windows, Mac, and Linux with appropriate Bluetooth adapters
"""

import socket
import threading
import sys
import time
from datetime import datetime

# For Windows - PyBluez
try:
    import bluetooth
    HAS_BLUETOOTH = True
except ImportError:
    HAS_BLUETOOTH = False
    print("PyBluez not installed. Install with: pip install pybluez")

class SteeringReceiver:
    def __init__(self, port=1, timeout=10):
        self.port = port
        self.timeout = timeout
        self.running = False
        self.client_socket = None
        self.last_values = {"steering": 0, "throttle": 0, "brake": 0}
        
    def start_server_windows(self):
        """Start Bluetooth server on Windows"""
        if not HAS_BLUETOOTH:
            print("Error: PyBluez required for Windows. Install: pip install pybluez")
            return
        
        print("Starting Bluetooth server on Windows...")
        print("Make sure your phone's Bluetooth is on and app is running")
        
        try:
            server_sock = bluetooth.BluetoothSocket(bluetooth.RFCOMM)
            server_sock.bind(("", bluetooth.PORT_ANY))
            server_sock.listen(1)
            
            port = server_sock.getsockname()[1]
            uuid = "00001101-0000-1000-8000-00805F9B34FB"
            
            bluetooth.advertise_service(
                server_sock, 
                "SteeringWheelReceiver",
                service_id=uuid,
                service_classes=[uuid, bluetooth.SERIAL_PORT_CLASS],
                profiles=[bluetooth.SERIAL_PORT_PROFILE]
            )
            
            print(f"Listening on port {port}")
            print("Waiting for connection from phone...\n")
            
            client_sock, client_info = server_sock.accept()
            print(f"✓ Connected from: {client_info}")
            
            self.receive_data(client_sock)
            
        except Exception as e:
            print(f"Error: {e}")
        finally:
            if server_sock:
                server_sock.close()
    
    def start_server_socket(self, host='0.0.0.0', port=8888):
        """Fallback TCP socket server (for local network testing)"""
        print(f"Starting TCP server on {host}:{port}")
        print("On Android, use IP address of this computer\n")
        
        try:
            server_sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
            server_sock.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
            server_sock.bind((host, port))
            server_sock.listen(1)
            
            print(f"Listening on {host}:{port}")
            print("Waiting for connection...\n")
            
            client_sock, client_info = server_sock.accept()
            print(f"✓ Connected from: {client_info}")
            
            self.receive_data(client_sock)
            
        except Exception as e:
            print(f"Error: {e}")
        finally:
            if server_sock:
                server_sock.close()
    
    def receive_data(self, socket_obj):
        """Receive and display steering data"""
        self.running = True
        socket_obj.settimeout(5)
        
        print("\n" + "="*60)
        print("Receiving data... (Press Ctrl+C to stop)")
        print("="*60)
        
        try:
            while self.running:
                try:
                    data = socket_obj.recv(1024)
                    
                    if not data:
                        print("Connection closed")
                        break
                    
                    # Parse data: STEERING|THROTTLE|BRAKE
                    message = data.decode('utf-8', errors='ignore').strip()
                    
                    if '|' in message:
                        parts = message.split('|')
                        if len(parts) >= 3:
                            try:
                                steering = int(parts[0])
                                throttle = int(parts[1])
                                brake = int(parts[2])
                                
                                self.last_values = {
                                    "steering": steering,
                                    "throttle": throttle,
                                    "brake": brake
                                }
                                
                                # Display values with visual indicators
                                self.display_values(steering, throttle, brake)
                                
                            except ValueError:
                                print(f"Invalid data: {message}")
                
                except socket.timeout:
                    continue
                except Exception as e:
                    print(f"Error receiving data: {e}")
                    break
                    
        except KeyboardInterrupt:
            print("\n\nShutdown requested")
        finally:
            self.running = False
            socket_obj.close()
    
    def display_values(self, steering, throttle, brake):
        """Display steering values in a nice format"""
        
        # Create steering bar
        steering_bar = self.create_bar(steering, -100, 100, 40)
        
        # Create throttle bar
        throttle_bar = self.create_bar(throttle, 0, 100, 20)
        
        # Create brake bar
        brake_bar = self.create_bar(brake, 0, 100, 20)
        
        # Clear screen (for nice updating)
        print("\033[2J\033[H", end="")
        
        # Print data
        print(f"Time: {datetime.now().strftime('%H:%M:%S.%f')[:-3]}")
        print("\n" + "="*60)
        print(f"STEERING: {steering:>4}° {steering_bar}")
        print("=" * 60)
        print(f"THROTTLE: {throttle:>4}% {throttle_bar}")
        print(f"BRAKE:    {brake:>4}% {brake_bar}")
        print("="*60)
        
        # Show current action
        if steering < -50:
            action = "🔄 TURNING LEFT"
        elif steering > 50:
            action = "🔄 TURNING RIGHT"
        elif throttle > 30:
            action = "🚗 ACCELERATING"
        elif brake > 30:
            action = "🛑 BRAKING"
        else:
            action = "⚙️  IDLE"
        
        print(f"Action: {action}")
        print("="*60 + "\n")
    
    def create_bar(self, value, min_val, max_val, width=40):
        """Create a visual bar for the value"""
        # Normalize value to 0-1 range
        normalized = (value - min_val) / (max_val - min_val)
        normalized = max(0, min(1, normalized))  # Clamp to 0-1
        
        # Calculate bar length
        bar_length = int(normalized * width)
        
        # Create bar
        bar = "█" * bar_length + "░" * (width - bar_length)
        
        return f"[{bar}]"

def main():
    print("""
╔════════════════════════════════════════════════════════════╗
║         Steering Wheel Controller - Bluetooth Receiver       ║
║                     v1.0                                     ║
╚════════════════════════════════════════════════════════════╝
    """)
    
    receiver = SteeringReceiver()
    
    print("Choose connection method:")
    print("1. Bluetooth (Windows with PyBluez - pip install pybluez)")
    print("2. TCP Socket (Local network - for testing/Android)")
    print("3. Exit")
    
    choice = input("\nEnter your choice (1-3): ").strip()
    
    if choice == "1":
        receiver.start_server_windows()
    elif choice == "2":
        host = input("Enter host IP (press Enter for 0.0.0.0): ").strip() or "0.0.0.0"
        port = input("Enter port (press Enter for 8888): ").strip()
        try:
            port = int(port) if port else 8888
            receiver.start_server_socket(host, port)
        except ValueError:
            print("Invalid port number")
    else:
        print("Exiting...")
        sys.exit(0)

if __name__ == "__main__":
    try:
        main()
    except KeyboardInterrupt:
        print("\n\nReceiver stopped.")
        sys.exit(0)
