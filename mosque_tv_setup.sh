#!/bin/bash
# mosque_tv_setup.sh - Complete Android TV setup for Mosque Prayer Clock

echo "🕌 Setting up Android TV for Mosque Prayer Clock..."

# Check ADB connection
if ! adb devices | grep -q "device$"; then
    echo "❌ No ADB device connected. Please connect your Android TV device first."
    exit 1
fi

echo "📱 Connected to Android TV device"

# Install APK if provided
if [ -f "app-debug.apk" ]; then
    echo "📦 Installing Mosque Prayer Clock APK..."
    adb install -r app-debug.apk
fi

# Essential permissions
echo "🔐 Granting essential permissions..."
adb shell pm grant com.mosque.prayerclock android.permission.SYSTEM_ALERT_WINDOW
adb shell pm grant com.mosque.prayerclock android.permission.WAKE_LOCK
adb shell pm grant com.mosque.prayerclock android.permission.INTERNET
adb shell pm grant com.mosque.prayerclock android.permission.ACCESS_NETWORK_STATE
adb shell pm grant com.mosque.prayerclock android.permission.RECEIVE_BOOT_COMPLETED

# Battery optimization
echo "🔋 Removing battery optimization..."
adb shell dumpsys deviceidle whitelist +com.mosque.prayerclock
adb shell cmd appops set com.mosque.prayerclock RUN_IN_BACKGROUND allow
adb shell am set-inactive com.mosque.prayerclock false

# Display settings
echo "📺 Configuring display settings..."
adb shell settings put system screen_off_timeout 2147483647
adb shell settings put system screen_brightness 255
adb shell settings put secure screensaver_enabled 0
adb shell settings put global stay_on_while_plugged_in 7

# Performance optimization
echo "⚡ Optimizing performance..."
adb shell settings put global animator_duration_scale 0.5
adb shell settings put global transition_animation_scale 0.5
adb shell settings put global window_animation_scale 0.5

# Debloat common apps (optional - uncomment if needed)
echo "🧹 Debloating unnecessary apps..."
adb shell pm disable-user --user 0 com.google.android.youtube.tv
adb shell pm disable-user --user 0 com.netflix.ninja
adb shell pm disable-user --user 0 com.google.android.play.games

# Set as launcher (optional - uncomment for kiosk mode)
echo "🏠 Setting up launcher (optional)..."
adb shell pm clear-default-launcher
adb shell cmd package set-home-activity com.mosque.prayerclock/.MainActivity

# Start the app
echo "🚀 Starting Mosque Prayer Clock..."
adb shell am start -n com.mosque.prayerclock/.MainActivity

echo "✅ Setup complete! Your Android TV is now optimized for mosque display."
echo "📋 Next steps:"
echo "   1. Configure prayer times in the app settings"
echo "   2. Set up weather (if desired)"
echo "   3. Customize display preferences"
echo "   4. Test the always-on display functionality"