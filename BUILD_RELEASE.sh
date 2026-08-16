#!/bin/bash

echo "============================================"
echo "  PIX GALLERY - AUTOMATIC BUILD SCRIPT"
echo "============================================"
echo ""

# Check if in correct directory
if [ ! -f "gradlew" ]; then
    echo "ERROR: This script must be run from PixGallery_Enhanced folder!"
    echo ""
    echo "Correct folder structure:"
    echo "  PixGallery_Enhanced/"
    echo "  ├── app/"
    echo "  ├── gradlew"
    echo "  └── build.gradle.kts"
    echo ""
    exit 1
fi

echo "Step 1: Checking if signing key exists..."
if [ -f "../my-release-key.jks" ]; then
    echo "✓ Signing key found!"
else
    echo "× Signing key NOT found!"
    echo ""
    echo "Creating signing key..."
    echo "Password: pixgallery@2024"
    echo ""
    keytool -genkey -v -keystore ../my-release-key.jks -keyalg RSA -keysize 2048 -validity 10000 -alias my-key-alias
    if [ $? -ne 0 ]; then
        echo "ERROR: Failed to create signing key!"
        exit 1
    fi
    echo "✓ Signing key created!"
fi

echo ""
echo "Step 2: Cleaning build..."
./gradlew clean
if [ $? -ne 0 ]; then
    echo "ERROR: Clean failed!"
    exit 1
fi
echo "✓ Clean complete!"

echo ""
echo "Step 3: Building Release APK (This will take 10-15 minutes)..."
./gradlew assembleRelease
if [ $? -ne 0 ]; then
    echo "ERROR: Build failed!"
    echo "Try running: ./gradlew clean"
    exit 1
fi
echo "✓ Build complete!"

echo ""
echo "============================================"
echo "  BUILD SUCCESSFUL! ✓"
echo "============================================"
echo ""
echo "APK Location:"
echo "  app/build/outputs/apk/release/app-release.apk"
echo ""
echo "Size: ~15-20 MB"
echo "Status: Ready for Uptodown!"
echo "Ads: REAL EARNING ADS CONFIGURED"
echo ""
echo "Next Steps:"
echo "  1. Connect Android phone via USB"
echo "  2. Run: adb uninstall com.pixgallery.app"
echo "  3. Run: adb install app/build/outputs/apk/release/app-release.apk"
echo "  4. Test on phone (banner ads, rewarded ads)"
echo "  5. Upload to Uptodown"
echo ""
