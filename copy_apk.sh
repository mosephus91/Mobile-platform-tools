#!/bin/bash
APK_PATH="app/build/outputs/apk/debug/app-debug.apk"
if [ -f "$APK_PATH" ]; then
    cp "$APK_PATH" app-debug.apk
    echo "Successfully copied app-debug.apk to root directory."
else
    echo "APK not found at $APK_PATH. Ensure you have run 'gradle assembleDebug'."
fi
