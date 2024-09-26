# NoiseBlock 🔇

Android application for blocking unwanted noise by playing relaxing sounds. You can also select your own sound. For your privacy, the application does not require Internet access.

![Icon](/app/src/main/res/mipmap-xhdpi/ic_launcher.webp)

# Latest release 

[https://github.com/otecfura/NoiseBlock/releases/latest/download/app-release.apk](https://github.com/otecfura/NoiseBlock/releases/latest/download/app-release.apk)


![Download](/screenshots/qrcode.png)

## How it works
Upon starting the service, listening is activated, and if the application detects the selected noise, relaxing music or a pre-selected sound will begin play. If the selected noise does not occur again within 30 seconds, the music will turn off. Each detected selected noise extends the music duration by an additional 30 seconds.

## Noises?
There is 521 types of [noises](app/src/main/java/com/aquasoup/barking/yamnet/CsvData.kt) just select one :)


## Requirements
Minimum Android version is Android 13

## Screenshot
![Icon](/screenshots/main-activity.png)

## Developed with ❤️ by
- [otecfura](https://otecfura.cz)
- [AquaSoup](https://www.aquasoup.com)
