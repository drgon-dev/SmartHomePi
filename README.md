# SmartHomePi
Умный дом под управлением Raspberry Pi с интеграцией андройд приложения

Андройд приложение представляет из себя две активности, написанных на языке Java(сборка gradle), в которых настраиваются различные части системы "умный дом". 
Данные о настройке всех устройств передаются в raspberry Pi через технологию Bluetooth, сами устройства управляются пинами raspberry Pi через специальную программу на языке Python


## Основная активность

[Main](MainActivity.png)

## Активность настройки

[Settings](DeviceSettingsActivity.png)

## Python программа для Raspberry Pi

[Script](RaspbScript/Script.py)