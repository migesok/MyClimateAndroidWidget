MyClimateAndroidWidget
======================

Simple android widget showing current temperature in Novosibirsk made just for fun.

Widget updates itself every two hours from http://weather.nsu.ru/weather_brief.xml if the network connectivity is up.

To build the apk-file ready to install on you phone you need to supply signing properties.
One way to do it is to create a "gradle.properties" file in the root project directory:
```INI
keyStorePath=<path to the key store file>
keyStorePassword=<key store password>
keyAlias=<key alias>
keyPassword=<key password>
```
Now you can assemble the project:
> gradlew assemble
