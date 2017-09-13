# SunriseSunsetBot
[![Build Status](https://travis-ci.org/carlopantaleo/SunriseSunsetBot.svg?branch=master)](https://travis-ci.org/carlopantaleo/SunriseSunsetBot)

A Telegram bot that alerts you at sunrise and sunset, based on your location. Sunrise and sunset times are provided by the excellent API by [sunrise-sunset.org](http://sunrise-sunset.org).

#### How does it work?
1. Start the bot (the official registered bot on Telegram is `@SunriseSunset_bot`).
2. Send your location if you are using a mobile client, or send your coordinates if location messages are not supported by your client.
3. Every day at sunrise and sunset you will receive an alert message.

Got a problem? Found a bug? Send a `/support` message and get in touch with us!

#### Features
* Fast setup: send your location as a "location" message.
* Legacy clients support: send your coordinates as a simple text message.
* Easily change your location with a `/change_location` command.
* Straight-forward support: send a message to the developers with the `/support` command.

#### Upcoming features
The following are in order of priority, from higher to lower.
* Possibility to send the name of the city instead of the coordinates.
* More configurable alerts (civil and nautical twilight).
* Optional alerts also for moonrise and moonset.

...and many more!

#### Contributions
Contributions are welcome. Feel free to send a pull request, but please note that any pull request containing any bot token will be rejected.

Thank you!