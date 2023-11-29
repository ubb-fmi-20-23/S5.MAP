import RPi.GPIO as GPIO 
import paho.mqtt.client as mqtt # Import the MQTT library
import time 
import sys

def messageFunction (client, userdata, message):
	topic = str(message.topic)
	message = str(message.payload.decode("utf-8"))
	print(topic +" -> *"+ message+"* ")
	global status
        global pin
	if message == "turn on":
		status = 1
  		GPIO.output(pin, GPIO.HIGH)
	elif message == "turn off":
		status = 0
  		GPIO.output(pin, GPIO.LOW)

pin=14

GPIO.setwarnings(False)
GPIO.setmode(GPIO.BCM)
GPIO.setup(pin, GPIO.OUT)
GPIO.output(pin, GPIO.HIGH)

# Create a MQTT client object
ourClient = mqtt.Client("myAC") 
# Connect to the test MQTT broker
print("Connecting to ",sys.argv[1])
ourClient.connect(sys.argv[1], 1883) 
# Subscribe to the topic AC_unit
ourClient.subscribe("AC_unit") 
ourClient.subscribe("topic/all") 
# Attach the messageFunction to subscription
ourClient.on_message = messageFunction 
# Start the MQTT client
ourClient.loop_start() 

status = 0
# Main program loop
try:
   while(1):
	if status == 1:
		print("AC is on")	
	else: 
		print("AC is off")	
	time.sleep(5) # Sleep for a second


except KeyboardInterrupt:
 print("  Bye!")
 GPIO.cleanup()
 exit()
