import java.text.SimpleDateFormat
/**
 *  Current temperature and humidity
 *
 *  Summary:
 *  Retrieve current temperature and humidity and battery level data from a LHT65
 *  (http://www.dragino.com/products/lora-lorawan-end-node/item/151-lht65.html)
 *
 *  registered at thethingsnetwork.com lorawan network
 *
 *
 * ------------------------------------------------------------------------------------------------------------------------------
 * MIT License
 *
 * Copyright (c) 2021 Erik Billerby
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 *
 * ------------------------------------------------------------------------------------------------------------------------------
 *
 *  Changes:
 *
 * v0.1.0 - 2021-01-31 - First version
 * v0.1.1 - 2021-07-10 - Updated to use TTN version 3
 *
 */

metadata {
    definition (
            name: "Current Temperature and Humidity",
            author: "Erik Billerby",
            namespace: "billerby",
            importUrl: "https://raw.githubusercontent.com/billerby/hubitat-lht65/main/lht65TheThingsNetworkDriver.groovy"
    ) {
        capability "Sensor"
        capability "Polling"
        capability "Battery"
        capability "TemperatureMeasurement"
        capability "RelativeHumidityMeasurement"

    }

    preferences {
        input name: "ttnRegion", type: "text", title: "The ttn region to use (server)", required: false, defaultValue: 'eu1'
        input name: "appName", type: "text", title: "Name of TTN-application to poll", required: true, defaultValue: ''
        input name: "deviceId", type: "text", title: "Device id of the LHT65 to fetch data from", required: true, defaultValue: ''
        input name: "accessKey", type: "text", title: "TTN Access key for the App", required: true, defaultValue: ''
        input "logEnable", "bool", title: "Enable debug logging", required: true, defaultValue: true
    }
}


def updated()  {
    poll()
}

def configure() {
    poll()
}

def uninstalled()  {
    unschedule()
}

def installed()  {
    runEvery30Minutes(poll)
    poll()
}

def poll()  {

    if(logEnable){
        log.debug "In poll method..."
    }

    if (appName != null) {
        if(logEnable){
            log.debug "AppName '${appName}' found in device configuration. Device id: ${deviceId}, Access key: ${accessKey}"
        }
    }

    try {

        def params = [
                uri: "https://${ttnRegion}.cloud.thethings.network/api/v3/as/applications/${appName}/devices/${deviceId}/packages/storage/uplink_message?limit=1&order=-received_at",
                headers: ['Accept': 'text/event-stream', 'Authorization': " Bearer ${accessKey}"],
                ignoreSSLIssues: true
        ]

        if(logEnable){
            log.debug "calling: ${params.uri} with HTTP GET"
        }
        
        httpGet(params) {resp ->
            // read from inputstream into a String
            int n = resp.getData().available()
            log.debug(n)
            byte[] bytes = new byte[n]
            resp.getData().read(bytes, 0, n)
            String payload = new String(bytes)
            def parser = new groovy.json.JsonSlurper()
            def json = parser.parseText(payload)
            log.debug(json)
            
            if(logEnable){
                log.debug("data from temp SHT: " + json.result.uplink_message.decoded_payload.TempC_SHT)
                log.debug("data from temp DS: " + json.result.uplink_message.decoded_payload.TempC_DS)
                log.debug("data from humidity SHT: "+ json.result.uplink_message.decoded_payload.Hum_SHT)
                log.debug("data from Battery (volt): " + json.result.uplink_message.decoded_payload.BatV)
                log.debug("Timestamp when data was submitted: " + json.result.received_at)
            }

            sendEvent([
                    name: 'humidity',
                    value: json.result.uplink_message.decoded_payload.Hum_SHT,
                    unit: "%",
                    descriptionText: "Humidity is $resp.data[0].Hum_SHT%"
            ]);

            sendEvent([
                    name: 'temperature',
                    value: json.result.uplink_message.decoded_payload.TempC_DS,
                    unit: "C",
                    descriptionText: "Temperature (external sensor) is $resp.data[0].TempC_DS C"
            ]);

            sendEvent([
                    name: 'temperatureInternal',
                    value: json.result.uplink_message.decoded_payload.TempC_SHT,
                    unit: "C",
                    descriptionText: "Temperature (internal sensor) is $resp.data[0].TempC_SHT C"
            ]);

            sendEvent([
                    name: 'battery',
                    value: json.result.uplink_message.decoded_payload.BatV,
                    unit: "V",
                    descriptionText: "Temperature level is $resp.data[0].BatV Volts"
            ]);
            String time = json.result.received_at;

            // Truncate microseconds from the incoming time
            int length = time.length()
            int endIndex = length-7
            time = time.substring(0, endIndex)
            SimpleDateFormat sdfOriginal = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS");
            SimpleDateFormat displayFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

            String utcDateTime = displayFormat.format(sdfOriginal.parse(time)) + " UTC";

            sendEvent(
                    name: 'lastCheckingTime',
                    value: utcDateTime
            );

        }

    }
    catch (SocketTimeoutException e) {
        log.error("Connection to ${params.uri} API timed out.", e)
        sendEvent(name: "error", value: "Connection timed out while retrieving data from API", displayed: true)
    }
    catch (e) {
        log.error ("Could not retrieve temperature and humidity data:", e)
        sendEvent(name: "error", value: "Could not retrieve data from API", displayed: true)
    }
}
