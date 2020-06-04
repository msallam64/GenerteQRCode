#include <SoftwareSerial.h>
SoftwareSerial BTserial(10, 11); // RX | TX
int sensorPin = A0;
int sensorValue = 0;
int irPin = 7;
int count = 0;
char inbyte = 0;
boolean state = true;


void setup() {
  BTserial.begin(9600);
  pinMode(irPin, INPUT);
}

void loop() {
  if (BTserial.available() > 0)
  {
    inbyte = BTserial.read();
    if (inbyte == '0')
    {
      count = 0;
    }
    if (inbyte == '1')
    {
      BTserial.print(count);
      BTserial.print(";");
    }
  }
  if (!digitalRead(irPin) && state) {
    count++;
    state = false;
    Serial.print("Count: ");
    Serial.println(count);
    delay(100);
  }
  if (digitalRead(irPin)) {
    state = true;
    delay(100);
  }

  BTserial.print(count);
  BTserial.print(";");
  delay(20);



}
