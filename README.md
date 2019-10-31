# Face-Detector

## Setting Up the Project

1. Clone the Project
2. Download *Open4Android 3.1.0* from this [Link](https://sourceforge.net/projects/opencvlibrary/files/opencv-android/3.1.0/OpenCV-3.1.0-android-sdk.zip/download)
3. Unzip the *Opencv4Android SDK*
4. Open the Project files from Android Studio
5. - After successfully opening on Android project, Click on the **File-> New-> Import Module** and 
    - Browse to the folder where you extracted the OpenCV Android library zip file contents. 
    - Select the `java` folder inside of the `sdk` folder
6.  - Click on Next to go to the next screen. 
    - On the next screen (the image below) you should leave the default options checked and click on Finish to complete the module import.
7. You should get a Gradle build error after you finish importing the OpenCV library. This happens because the library is using an old Android SDK that you probably don’t have installed yet.
    - To fix this open the `build.gradle` file for `opencv4android` and remove `compileSdkVersion` and `targetSdkVersion`
    - After changing the version you should click on the sync button so that Gradle can sync the project.
8. Add the OpenCV Dependency
  - click on **File -> Project Structure**.
  - click on the **Dependencies** tab.
  - Under the **App** module click the `+` in the *Decleared Dependencies* Section
  - Click on the **Module dependency** and add `Opencv4Android`
<<<<<<< HEAD
9. On the `OpenCv4Android SDK` Copy the `sdk/native/libs` folder into the app module main folder (Usually `ProjectName/app/src/main`) and rename the folder as `jniLibs`.
10. Your project is ready and Must `Sync` and `Build` properly
=======
9. Your project is ready and Must `Sync` and `Build` properly
>>>>>>> origin/master

## Contributers

- [Arkadip Bhattacharya](https://github.com/darkmatter18)
