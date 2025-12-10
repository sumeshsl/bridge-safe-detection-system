#!/usr/bin/env python3
"""
Municipal Clearance Detector with 1602 I2C LCD Display
Displays violation alerts on LCD when height exceeds clearance limit
"""
import RPi.GPIO as GPIO
import time
import paho.mqtt.client as mqtt
import json
from datetime import datetime
import smbus2

# ==================== Configuration ====================
MQTT_BROKER = "192.168.1.152"  # Change to your laptop IP
MQTT_PORT = 1883
DEVICE_ID = "TEST_001"
LOCATION = "Main Street Bridge"
CLEARANCE_HEIGHT = 13.5  # feet

# GPIO Pin Configuration
TRIG_PIN = 17  # HC-SR04 Trigger
ECHO_PIN = 24  # HC-SR04 Echo (via level shifter)

# LCD I2C Configuration
I2C_ADDR = 0x27  # Change if your address is different (0x3F)
LCD_BACKLIGHT = 0x08
ENABLE = 0b00000100

# Measurement Settings
MEASUREMENT_INTERVAL = 2  # seconds
SPEED_OF_SOUND = 34300    # cm/s
CM_TO_FEET = 0.0328084

# Sensor mounting height from ground
SENSOR_HEIGHT_CM = 450    # 4.5 meters = ~14.76 feet
SENSOR_HEIGHT_FEET = SENSOR_HEIGHT_CM * CM_TO_FEET

# ==================== Initialize Hardware ====================
# GPIO Setup
GPIO.setmode(GPIO.BCM)
GPIO.setwarnings(False)
GPIO.setup(TRIG_PIN, GPIO.OUT)
GPIO.setup(ECHO_PIN, GPIO.IN)
GPIO.output(TRIG_PIN, False)

# I2C Bus Setup
try:
    bus = smbus2.SMBus(1)
    print("âœ“ I2C bus initialized")
except Exception as e:
    print(f"âœ— I2C initialization failed: {e}")
    bus = None

# MQTT Setup
client = mqtt.Client()

def on_connect(client, userdata, flags, rc):
    if rc == 0:
        print(f"âœ“ Connected to MQTT broker at {MQTT_BROKER}")
        lcd_string("MQTT Connected", 2)
    else:
        print(f"âœ— Connection failed with code {rc}")

client.on_connect = on_connect

try:
    client.connect(MQTT_BROKER, MQTT_PORT, 60)
    client.loop_start()
except Exception as e:
    print(f"âœ— Failed to connect to MQTT broker: {e}")

# ==================== LCD Display Functions ====================
def lcd_write_byte(data, mode):
    """Write a byte to LCD in 4-bit mode"""
    if bus is None:
        return
    
    try:
        high = mode | (data & 0xF0) | LCD_BACKLIGHT
        low = mode | ((data << 4) & 0xF0) | LCD_BACKLIGHT
        
        # High nibble
        bus.write_byte(I2C_ADDR, high)
        bus.write_byte(I2C_ADDR, high | ENABLE)
        time.sleep(0.0005)
        bus.write_byte(I2C_ADDR, high & ~ENABLE)
        
        # Low nibble
        bus.write_byte(I2C_ADDR, low)
        bus.write_byte(I2C_ADDR, low | ENABLE)
        time.sleep(0.0005)
        bus.write_byte(I2C_ADDR, low & ~ENABLE)
    except Exception as e:
        print(f"LCD write error: {e}")

def lcd_init():
    """Initialize the LCD"""
    if bus is None:
        return
    
    try:
        lcd_write_byte(0x33, 0)  # Initialize
        lcd_write_byte(0x32, 0)  # Set to 4-bit mode
        lcd_write_byte(0x06, 0)  # Cursor move direction
        lcd_write_byte(0x0C, 0)  # Display on, cursor off
        lcd_write_byte(0x28, 0)  # 2 line, 5x7 matrix
        lcd_write_byte(0x01, 0)  # Clear display
        time.sleep(0.005)
        print("âœ“ LCD initialized")
    except Exception as e:
        print(f"LCD init error: {e}")

def lcd_string(message, line):
    """Display string on specified line (1 or 2)"""
    if bus is None:
        return
    
    try:
        line_addr = 0x80 if line == 1 else 0xC0
        lcd_write_byte(line_addr, 0)
        
        for char in message.ljust(16)[:16]:
            lcd_write_byte(ord(char), 1)
    except Exception as e:
        print(f"LCD string error: {e}")

def lcd_clear():
    """Clear LCD display"""
    if bus is None:
        return
    
    try:
        lcd_write_byte(0x01, 0)
        time.sleep(0.005)
    except Exception as e:
        print(f"LCD clear error: {e}")

def display_status(message):
    """Display status message on LCD"""
    lcd_string(f"Det: {DEVICE_ID}", 1)
    lcd_string(message[:16], 2)

def display_measurement(vehicle_height_feet, status):
    """Display measurement on LCD"""
    if status == "VIOLATION":
        # Line 1: ** VIOLATION **
        lcd_string("** VIOLATION **", 1)
        # Line 2: Height: XX.XX ft
        lcd_string(f"Ht: {vehicle_height_feet:.2f}ft", 2)
    elif status == "CLEAR":
        # Line 1: Status: CLEAR
        lcd_string("Status: CLEAR", 1)
        # Line 2: Height: XX.XX ft
        lcd_string(f"Ht: {vehicle_height_feet:.2f}ft", 2)

def display_violation_details(vehicle_height_feet, excess_height):
    """Display violation details with flashing effect"""
    for i in range(3):  # Flash 3 times
        # Show violation
        lcd_string("** VIOLATION **", 1)
        lcd_string(f"H:{vehicle_height_feet:.2f} +{excess_height:.2f}", 2)
        time.sleep(0.5)
        
        # Clear for flash effect
        lcd_clear()
        time.sleep(0.3)
    
    # Final display
    lcd_string("** VIOLATION **", 1)
    lcd_string(f"Height:{vehicle_height_feet:.1f}ft", 2)

# ==================== Sensor Functions ====================
def measure_distance():
    """Measure distance using HC-SR04 ultrasonic sensor"""
    try:
        # Send trigger pulse
        GPIO.output(TRIG_PIN, True)
        time.sleep(0.00001)  # 10 microseconds
        GPIO.output(TRIG_PIN, False)
        
        # Wait for echo
        pulse_start = time.time()
        timeout = pulse_start + 0.1  # 100ms timeout
        
        while GPIO.input(ECHO_PIN) == 0:
            pulse_start = time.time()
            if pulse_start > timeout:
                return None
        
        pulse_end = time.time()
        timeout = pulse_end + 0.1
        
        while GPIO.input(ECHO_PIN) == 1:
            pulse_end = time.time()
            if pulse_end > timeout:
                return None
        
        # Calculate distance
        pulse_duration = pulse_end - pulse_start
        distance_cm = (pulse_duration * SPEED_OF_SOUND) / 2
        
        # Validate measurement (2cm to 400cm is valid range)
        if 2 <= distance_cm <= 400:
            return distance_cm
        return None
        
    except Exception as e:
        print(f"Sensor error: {e}")
        return None

def calculate_vehicle_height(distance_cm):
    """Calculate vehicle height from sensor distance"""
    if distance_cm is None:
        return None
    
    # Vehicle height = Sensor height from ground - Measured distance
    vehicle_height_cm = SENSOR_HEIGHT_CM - distance_cm
    vehicle_height_feet = vehicle_height_cm * CM_TO_FEET
    
    return max(0, vehicle_height_feet)

# ==================== MQTT Publishing ====================
def publish_height_measurement(distance_cm, vehicle_height_feet):
    """Publish height measurement to MQTT broker"""
    try:
        topic = f"detector/{DEVICE_ID}/height"
        payload = {
            "device_id": DEVICE_ID,
            "height": round(vehicle_height_feet, 2),
            "distance_cm": round(distance_cm, 1),
            "timestamp": datetime.now().isoformat(),
            "sensor_status": "OK"
        }
        
        client.publish(topic, json.dumps(payload), qos=1)
        print(f"ðŸ“¤ Published: {vehicle_height_feet:.2f} ft")
        
    except Exception as e:
        print(f"MQTT publish error: {e}")

def publish_violation(distance_cm, vehicle_height_feet):
    """Publish violation to MQTT broker"""
    try:
        topic = f"detector/{DEVICE_ID}/violation"
        excess_height = vehicle_height_feet - CLEARANCE_HEIGHT
        
        payload = {
            "device_id": DEVICE_ID,
            "height": round(vehicle_height_feet, 2),
            "clearance_height": CLEARANCE_HEIGHT,
            "excess_height": round(excess_height, 2),
            "distance_cm": round(distance_cm, 1),
            "timestamp": datetime.now().isoformat(),
            "location": LOCATION,
            "severity": "CRITICAL" if excess_height > 2 else "HIGH" if excess_height > 1 else "MEDIUM",
            "sensor_status": "OK"  # ← ADDED FOR CONSISTENCY
        }
        
        client.publish(topic, json.dumps(payload), qos=1)
        print(f"📤 Violation published: {vehicle_height_feet:.2f} ft (excess: {excess_height:.2f} ft)")
        
    except Exception as e:
        print(f"MQTT publish error: {e}")

def publish_heartbeat():
    """Publish heartbeat to indicate detector is online"""
    try:
        topic = f"detector/{DEVICE_ID}/heartbeat"
        payload = {
            "device_id": DEVICE_ID,
            "timestamp": datetime.now().isoformat(),
            "status": "online"
        }
        
        client.publish(topic, json.dumps(payload), qos=1)
        
    except Exception as e:
        print(f"Heartbeat error: {e}")

# ==================== Main Loop ====================
def main():
    print(f"\n{'='*50}")
    print(f"Municipal Clearance Detector - {DEVICE_ID}")
    print(f"Location: {LOCATION}")
    print(f"Clearance Height: {CLEARANCE_HEIGHT} feet")
    print(f"Sensor Height: {SENSOR_HEIGHT_FEET:.2f} feet")
    print(f"{'='*50}\n")
    
    # Initialize LCD
    lcd_init()
    time.sleep(0.5)
    
    # Display startup message
    display_status("Initializing...")
    time.sleep(2)
    
    display_status("Ready!")
    time.sleep(1)
    
    heartbeat_counter = 0
    
    try:
        while True:
            # Measure distance
            distance_cm = measure_distance()
            
            if distance_cm is not None:
                # Calculate vehicle height
                vehicle_height_feet = calculate_vehicle_height(distance_cm)
                
                # Determine status and take action
                if vehicle_height_feet > CLEARANCE_HEIGHT:
                    status = "VIOLATION"
                    excess_height = vehicle_height_feet - CLEARANCE_HEIGHT
                    
                    print(f"âš ï¸  VIOLATION DETECTED!")
                    print(f"   Height: {vehicle_height_feet:.2f} ft")
                    print(f"   Exceeds limit by: {excess_height:.2f} ft")
                    
                    # Display violation with flashing effect
                    display_violation_details(vehicle_height_feet, excess_height)
                    
                    # Publish violation
                    publish_violation(distance_cm, vehicle_height_feet)
                    
                    # Keep violation displayed for longer
                    time.sleep(3)
                    
                else:
                    status = "CLEAR"
                    print(f"âœ“  Height: {vehicle_height_feet:.2f} ft (within limit)")
                    
                    # Display normal measurement
                    display_measurement(vehicle_height_feet, status)
                    
                    # Publish to MQTT
                    publish_height_measurement(distance_cm, vehicle_height_feet)
                
            else:
                print("âœ— Measurement failed - out of range")
                lcd_string("Sensor Error", 1)
                lcd_string("Out of range", 2)
            
            # Publish heartbeat every 10 measurements
            heartbeat_counter += 1
            if heartbeat_counter >= 10:
                publish_heartbeat()
                heartbeat_counter = 0
            
            time.sleep(MEASUREMENT_INTERVAL)
            
    except KeyboardInterrupt:
        print("\n\nâš ï¸  Detector stopped by user")
        lcd_string("System", 1)
        lcd_string("Stopped", 2)
        time.sleep(2)
        
    finally:
        cleanup()

def cleanup():
    """Clean up GPIO, MQTT, and LCD"""
    print("\nCleaning up...")
    
    # Clear LCD
    lcd_clear()
    
    # Cleanup GPIO
    GPIO.cleanup()
    
    # Stop MQTT
    client.loop_stop()
    client.disconnect()
    
    print("âœ“ Cleanup complete")

if __name__ == "__main__":
    main()
