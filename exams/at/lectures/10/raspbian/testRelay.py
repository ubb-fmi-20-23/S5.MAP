import RPi.GPIO as GPIO 
from time import sleep

pin=14

GPIO.setwarnings(False)
GPIO.setmode(GPIO.BCM)
GPIO.setup(pin, GPIO.OUT)
GPIO.output(pin, GPIO.HIGH)

# main loop
try:
  GPIO.output(pin, GPIO.LOW)
  sleep(2)
  GPIO.output(pin, GPIO.HIGH)
  sleep(2)
  print("Works")
  GPIO.cleanup()
  print("Good bye!")

except KeyboardInterrupt:
 print("  Bye!")
 GPIO.cleanup()
 exit()
