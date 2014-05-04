Sprebble
==============
## cs4414-project

### Course project for UVA CS 4414 Operating Systems Spring 2014 with David Evans

### Team Members
Justin Ingram
Alex Abramson
Vikram Bhasin
Justin Dao

### Goal
Design a Spritz style app for Pebble for reading notifications. Make reading messages on the Pebble faster and more convenient while eliminating scrolling. The app should allow reading all notifications, including full length text messages and emails on the Pebble quickly, rather than having to scroll through snippets of the message.

### Strategy
Design a companion Android app that will send the notifications to the Pebble. This app will allow you to pick and choose which apps can send notifications using this service. Design a Pebble app for displaying these sent notifications in the Spritz style of one word at a time at a fixed position. To get the notification text, the Android app will install an Accessibility Service. Binding to the notification service is only supported in Android versions greater than 4.3, and we did not want to restrict this app to those newer phones. Future iterations may take advantage of the notification service features on newer devices and bypass installing the accessibility service.

### Build Instructions
#### Pebble App
Requirements: Pebble SDK > 2.0 setup and configured, Pebble App in Developer mode on Android device
1. cd sprebble
2. pebble build
3. pebble install --phone "<Phone IP>"

#### Android App
Requirements: Android SDK supporting Android 4.0 - 4.4, Eclipse with Android ADT plugin
1. Import existing project -> pick the android folder from our repo
2. Ensure your classpath is correct and that the Pebble-Kit Android Eclipse project was also included. This should happen automatically, and it has been included in this repository for convenience. If it doesn't happen automatically, Use Import Existing Android code into workspace, and then add PebbleKit Android as a required library from the project properties of our Android app
3. Install our app to your android device of choice using Eclipse and a device with USB debugging turned on.

Once both apps are installed, we recommend you:
1. Pick which apps should send notifications to Sprebble
2. Turn off notification sending in the Pebble app for those notification features you want to use with Sprebble. For example, if you are sending notifications in the Sprebble app from GMail, you would want to turn this off in the Pebble apps
3. Make sure the Sprebble Accessibility Service is running on the phone. The Android app *should* prompt you for this.

#### Other cool things in our project
- Acknowledgements: Press the Down Pebble button when done reading a message to receive the next one. No notification will interrupt your current message reading
- Ability to send Toast notifications in addition to standard notifications, configurable in android app settings
- The length of time a word is displayed on the Pebble screen is a function of length for increased readability
- Words per minute is settable with the UP button on the Pebble
- Pebble vibrates on message received
- Setting to ignore ongoing style Android notifications