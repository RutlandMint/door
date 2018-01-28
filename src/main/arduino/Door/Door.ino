#define MAX_BITS 100                 // max number of bits 
#define WEIGAND_WAIT_TIME  3000      // time to wait for another weigand pulse.  

#define CardReadLED  11
#define CardValidLED  10
#define CardInvalidLED  9
#define Input1LED  8
#define Input2LED  7
#define DebugLED  6

//Strike State
#define STRIKE_TIME 5000
unsigned long striketimer = 0;

//Card Reader State
unsigned char cardReader_databits[MAX_BITS];    // stores all of the data bits
unsigned char cardReader_bitCount;              // number of bits currently captured
unsigned char cardReader_flagDone;              // goes low when data is currently being captured
unsigned int cardReader_weigand_counter;        // countdown until we assume there are no more bits

void setup() {
  cardReader_setup();

  pinMode(CardReadLED, OUTPUT);
  pinMode(CardValidLED, OUTPUT);
  pinMode(CardInvalidLED, OUTPUT);
  pinMode(Input1LED, OUTPUT);
  pinMode(Input2LED, OUTPUT);
  pinMode(DebugLED, OUTPUT);

  pinMode(A1, OUTPUT);

  Serial.begin(115200);

  Serial.println("#Starup");
}

long status = 0;
void loop() {
  if (striketimer && striketimer <= millis()) {
    lock();
    striketimer = 0;
  }
  char in = Serial.read();
  if (in == 'U' ) {
    unlock();
  }

  if (millis() == status ) {
    status = millis() + 10000;
    Serial.println("STATUS=OK");
  }
  cardReader_loop();
}

void lock() {
  Serial.println("#Locking Strike");
  digitalWrite(CardReadLED, LOW);
  digitalWrite(CardValidLED, LOW);
  digitalWrite(CardInvalidLED, LOW);
  digitalWrite(Input1LED, LOW);
  digitalWrite(Input2LED, LOW);
  digitalWrite(A1, LOW);
}

void unlock() {
  Serial.println("#Unlocking Strike");
  digitalWrite(CardValidLED, HIGH);
  digitalWrite(Input1LED, HIGH);
  digitalWrite(A1, HIGH);
  striketimer = STRIKE_TIME + millis();
}

void cardRead(unsigned long facilityCode, unsigned long cardCode) {
  digitalWrite(CardReadLED, HIGH);
  unsigned long int card = (facilityCode * 100000) + cardCode;

  //Hardcoded 24 hour access
  if ( false //
       || card == 10010001 //Pete
       || card == 10010100 //Dan
       || card == 10010084 //karen
       || card == 10010081 //Bill
       || card == 19233338 //forest
     ) {
    //Serial.print("#OVERRIDE=");
    //Serial.println(card);
    //unlock();
  }
  Serial.print("CARD=");
  Serial.println(card);
}

void cardReader_ISR_INT0() // interrupt that happens when INTO goes low (0 bit)
{
  //cardReader_databits[cardReader_bitCount] = 0;
  cardReader_bitCount++;
  cardReader_flagDone = 0;
  cardReader_weigand_counter = WEIGAND_WAIT_TIME;

}
void cardReader_ISR_INT1() // interrupt that happens when INT1 goes low (1 bit)
{
  cardReader_databits[cardReader_bitCount] = 1;
  cardReader_bitCount++;
  cardReader_flagDone = 0;
  cardReader_weigand_counter = WEIGAND_WAIT_TIME;
}

void cardReader_setup() {
  //Card Reader Setup
  //Pins 2&3 as input
  pinMode(2, INPUT);     // DATA0 (INT0)
  pinMode(3, INPUT);     // DATA1 (INT1)
  // binds the ISR functions to the falling edge of INTO and INT1
  attachInterrupt(0, cardReader_ISR_INT0, FALLING);
  attachInterrupt(1, cardReader_ISR_INT1, FALLING);
  cardReader_weigand_counter = WEIGAND_WAIT_TIME;
}

void cardReader_loop() {
  // This waits to make sure that there have been no more data pulses before processing data
  if (!cardReader_flagDone) {
    if (--cardReader_weigand_counter == 0) {
      cardReader_flagDone = 1;
    }
  }

  // if we have bits and the weigand counter went out
  if (cardReader_bitCount > 0 && cardReader_flagDone) {
    unsigned long facilityCode = 0;      // decoded facility code
    unsigned long cardCode = 0;          // decoded card code
    unsigned char i;
    // we will decode the bits differently depending on how many bits we have
    // see www.pagemac.com/azure/data_formats.php for mor info
    if (cardReader_bitCount == 35) {
      // 35 bit HID Corporate 1000 format
      // facility code = bits 2 to 14
      for (i = 2; i < 14; i++) {
        facilityCode <<= 1;
        facilityCode |= cardReader_databits[i];
      }
      // card code = bits 15 to 34
      for (i = 14; i < 34; i++) {
        cardCode <<= 1;
        cardCode |= cardReader_databits[i];
      }
      cardRead(facilityCode, cardCode);
    } else if (cardReader_bitCount == 26) {
      // standard 26 bit format
      // facility code = bits 2 to 9
      for (i = 1; i < 9; i++) {
        facilityCode <<= 1;
        facilityCode |= cardReader_databits[i];
      }
      // card code = bits 10 to 23
      for (i = 9; i < 25; i++) {
        cardCode <<= 1;
        cardCode |= cardReader_databits[i];
      }
      cardRead(facilityCode, cardCode);
    } else {
      // you can add other formats if you want!
      Serial.println("#Unable to decode.");
    }

    // cleanup and get ready for the next card
    cardReader_bitCount = 0;
    for (i = 0; i < MAX_BITS; i++) {
      cardReader_databits[i] = 0;
    }
  }
}

