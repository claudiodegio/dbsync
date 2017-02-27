## DbSync

Small library for sync android SqlLite database to cloud storage (for now only GDrive)

## Motivation

There is not solution to sync SqLite database between device without the of custom
server application or use of paid online cloud service. My solution it's base
a json file on GDrive cloud account of user with no server involved, so all
base are private to user e non saved on server.

## How work

The library write a file on GDrive called cloud file like:
```json
{
  "name" : "db2.db",
  "formatVersion" : 1,
  "schemaVersion" : 1,
  "tableCount" : 2,
  "tables" : [ {
    "name" : "name",
    "recordsCount" : 1,
    "records" : [ {
      "NAME" : "myname",
      "SEND_TIME" : 1487887189513,
      "CLOUD_ID" : "c2bd71ee-3f1b-4dca-b478-ebecaaa1f835"
    } ]
  }, {
    "name" : "category",
    "recordsCount" : 2,
    "records" : [ {
      "NAME" : "cat1",
      "SEND_TIME" : 1488038770858,
      "CLOUD_ID" : "6f2a8bcc-f1ee-4f14-8987-9f4275205793"
    }, {
      "NAME" : "musica",
      "SEND_TIME" : 1487887376113,
      "CLOUD_ID" : "52a292b5-8f38-4f30-b13f-7f86bbff49a6"
    } ]
  } ]
}
```

The library download the file from cloud storage and update the local database using the cloud id used
to connect a record in 2 different device and and the send time use to understand is the record
it's to update.
The solution try to update only the record who are been update from last sync time, for
performance reason don't update all database, after the sync process it generates the cloud id (uuid)
and rewrite the json cloud file and upload to file storage.

Note: with GDrive you can share between more user

## Features

The library support:

* Sync of 1 o more table
* Sync join table (updating the correct id)
* Support more match rule (default use CLOUD_ID counted with uiid faction)
* Support for ignore selected column
* Support for filter on what to sync (usefull for multi account application but different sync file)


Not supported yet
* Other cloud storage
* Support for images and file

# Usage


# Installation
**Add the dependencies to your gradle file:**
```javascript
allprojects {
    repositories {
        ...
        maven { url 'https://jitpack.io' }
    }
}
dependencies {
        compile 'com.github.claudiodegio:dbsync:X.Y.Z'
}
```

## License

Copyright 2017 Claudio Degioanni

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.