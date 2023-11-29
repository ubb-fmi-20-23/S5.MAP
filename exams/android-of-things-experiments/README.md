# android-of-things-experiments

This project contains two parts: An Arduino program and an Android of Things app.

The Arduino program is located in the `arduino` folder and builds using `platformio`. It's in charge of reading low level Arduino sensors and communicating the responses to the Raspberry Pi running Android of Things.

On the Android of Things app, I connect to Firebase to store the read sensors every hour on the Firebase online database.
