#include <Wire.h>  
  
#include <Max3421e.h>  
#include <Usb.h>  
#include <AndroidAccessory.h>  
  
#define pwmM1Pin 9
#define pwmM2Pin 10

#define inM1Pin1 28
#define inM1Pin2 29

#define inM2Pin1 22
#define inM2Pin2 23
  
AndroidAccessory acc("minea",  
       "ADKMotor",  
       "DemoKit Arduino Board",  
       "1.0",  
       "http://www.android.com",  
       "0000000012345678");  
  
void setup();  
void loop();  
  
void init_moters()  
{  
  pinMode(pwmM1Pin, OUTPUT);  // 速度調節(モータ1)
  pinMode(pwmM2Pin, OUTPUT);  // 速度調節(モータ2)
  // Pin1,Pin2 を両方共0の値にしないように！
  pinMode(inM1Pin1, OUTPUT);  // 前進
  pinMode(inM1Pin2, OUTPUT);
  pinMode(inM2Pin1, OUTPUT);
  pinMode(inM2Pin2, OUTPUT); 
}  
  
void setup()  
{  
    Serial.begin(115200);  
    Serial.print("\r\nStart");  
  
    init_moters();  
  
    acc.powerOn();  
}  
  
void loop()  
{  
    byte msg[2];  
  
    if (acc.isConnected()) {  
        int len = acc.read(msg, sizeof(msg), 1);  

        if (len > 0) {  
            if (msg[0] == 0x1) {
              // Advance
                if(msg[1] == 0x1) {
                  digitalWrite(inM1Pin2, LOW);
                  digitalWrite(inM2Pin2, LOW);
                  digitalWrite(inM1Pin1, HIGH);
                  digitalWrite(inM2Pin1, HIGH);
                  digitalWrite(pwmM1Pin, HIGH);
                  digitalWrite(pwmM2Pin, HIGH);
                    msg[0] = 0x1;  
                    msg[1] = 0x1;  
                    acc.write(msg, 2);  
                } 
                // Back
                else if(msg[1] == 0x2) {
                  digitalWrite(inM1Pin1, LOW);
                  digitalWrite(inM2Pin1, LOW);
                  digitalWrite(inM1Pin2, HIGH);
                  digitalWrite(inM2Pin2, HIGH);
                  digitalWrite(pwmM1Pin, HIGH);
                  digitalWrite(pwmM2Pin, HIGH);
                    msg[0] = 0x1;  
                    msg[1] = 0x1;  
                    acc.write(msg, 2);  
                } 
                // 右旋回
                else if(msg[1] == 0x3) {
                  digitalWrite(inM1Pin2, LOW);
                  digitalWrite(inM2Pin2, LOW);
                  digitalWrite(inM1Pin1, HIGH);  
                  digitalWrite(pwmM1Pin, HIGH);  
                  digitalWrite(pwmM2Pin, LOW);
                    msg[0] = 0x1;  
                    msg[1] = 0x1;  
                    acc.write(msg, 2);  
                }
                // 左旋回
                else if(msg[1] == 0x4) {
                  digitalWrite(inM1Pin1, LOW); 
                  digitalWrite(inM2Pin1, HIGH);  
                  digitalWrite(pwmM1Pin, LOW);  
                  digitalWrite(pwmM2Pin, HIGH);
                  msg[0] = 0x1;  
                  msg[1] = 0x1;  
                  acc.write(msg, 2); 
                }
                else {  
                    digitalWrite(pwmM1Pin, LOW);
                    digitalWrite(pwmM2Pin, LOW);  
                    msg[0] = 0x1;  
                    msg[1] = 0x2;  
                    acc.write(msg, 2);  
                }  
            }  
        }
    }   
    else {  
        (pwmM1Pin, LOW);
        (pwmM2Pin, LOW);
    }  
  
    delay(10);  
}  
