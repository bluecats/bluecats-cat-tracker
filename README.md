# bluecats-cat-tracker

Update AndroidManidfest.xml with your BlueCats app token.

1. Detect beacon sighting.
2. Create beacon payload.
3. Store payload to Local Storage for beacon identifier (each payload will only report on the latest beacon sighting, meaning that if you detect a beacon 8 times within a payload then only the latest location, rssi, etc will be reported in the payload).
4. Each sighting upload interval (currently every 60 seconds) will trigger the SightingsUploadHelper to generate a user upload (basically user location at time of upload) and will then call BeaconPayloadService to load any beacon payloads from Local Storage to send to the server.
5. If network is unavailable at the time of upload the payload will remain in Local Storage until the next upload time that the user has network access.

The beacons list is simply a visual list of all the beacons that currently have a payload waiting to be uploaded. 

