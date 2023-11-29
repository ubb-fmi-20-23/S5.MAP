#include <Arduino.h>
#include <SimpleDHT.h>

#define PIN_DHT11 7

SimpleDHT11 dht11;
char buff[8];

void setup() { 
	Serial.begin(115200);	
}

void writeTemp() {
	byte temperature = 0;
	byte humidity = 0;
	if (dht11.read(PIN_DHT11, &temperature, &humidity, NULL)) {
		Serial.write("0");
	} else {
		sprintf(buff, "%2d", temperature);
		Serial.write(buff);
	}
}

void writeHumidity() {
	byte temperature = 0;
	byte humidity = 0;
	if (dht11.read(PIN_DHT11, &temperature, &humidity, NULL)) {
		Serial.write("0");
	} else {
		sprintf(buff, "%2d", humidity);
		Serial.write(buff);
	}
}

void processSerialCommand() {
	if (Serial.available() > 0) {
		char command = (char) Serial.read();
		switch (command) {
			case 'T':
				writeTemp();
				break;
			case 'H':
				writeHumidity();
				break;
		}
		Serial.flush();
	}
}

void loop() {
	processSerialCommand();
}
