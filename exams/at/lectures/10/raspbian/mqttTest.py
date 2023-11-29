import paho.mqtt.client as mqtt # Import the MQTT library
import time 
import sys

def messageFunction (client, userdata, message):
	topic = str(message.topic)
	message = str(message.payload.decode("utf-8"))
	print(topic +" -> *"+ message+"* ")
	global status
	if message == "turn on":
		status = 1
	elif message == "turn off":
		status = 0

# Create a MQTT client object
ourClient = mqtt.Client("myAC") 
# Connect to the test MQTT broker
print("Connecting to ",sys.argv[1])
ourClient.connect(sys.argv[1], 1883) 
# Subscribe to the topic AC_unit
ourClient.subscribe("AC_unit") 
# Attach the messageFunction to subscription
ourClient.on_message = messageFunction 
# Start the MQTT client
ourClient.loop_start() 

status = 0
# Main program loop
while(1):
	if status == 1:
		print("AC is on")	
	else: 
		print("AC is off")	
	time.sleep(5) # Sleep for a second
