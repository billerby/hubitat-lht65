# Hubitat driver for the getting lorawan LHT65 device data from thethingsnetwork

This thethingsnetwork integration allows one to interact with the The Things Network. This community-driven and open network supports LoRaWAN for long range (~5 to 15 km) communication with a low bandwidth (51 bytes/message). Gateways transfers the received data from the sensors to the The Things Network.

The Things network support various integrations to make the data available:

## Setup
Visit the [The Things Network Console website](https://console.thethingsnetwork.org/), log in with your The Things Network credentials, choose your application from Applications.

The Application ID is used to identify the scope of your data.

## Configuration
Install the groovy-script as a new driver in hubitat. Then add a virutal device of this new driver type. In the Advanced preferences of the device add your application (from TTN), the device-id and finally the access key for authorization.
