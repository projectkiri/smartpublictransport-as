Smart Public Transport
======================

Smart Public Transport is the official KIRI App for Android, now open sourced! Do experiment or add new features by yourself.

Installation
------------

It is recommended to fork to a new Github repo before cloning into your computer. Content of this repo can be opened with Android Studio. However, prior to that you need to create a credential configuration file and place it in `/app/src/main/res/values/credentials.xml`. The content of this file is as follow:

```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <string name="com.google.android.maps.v2.API_KEY" translatable="false"><!--TODO--></string>
    <string name="KIRI_APIKEY" translatable="false"><!--TODO--></string>
</resources>
```

Fill the `<--TODO-->`s with your [Google Server Key](https://developers.google.com/maps/documentation/android/start#step_4_get_a_google_maps_api_key) and [KIRI API](https://kiri.travel/dev), respectively.

Contribution
------------

Contact @pascalalfadian prior to implementation if you want to improve this app, to avoid disappointment. Generally I prefer ...

* bugfixes
* code cleanup (more object oriented)
* new features in line with the website https://kiri.travel

... to ...

* new features not related to main function (i.e. navigation)
* new features that cost memory or performance
* notable changes in user experience
