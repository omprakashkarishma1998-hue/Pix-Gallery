@echo off
echo ============================================
echo   PIX GALLERY - AUTOMATIC BUILD SCRIPT
echo ============================================
echo.

REM Check if in correct directory
if not exist "gradlew.bat" (
    echo ERROR: This script must be run from PixGallery_Enhanced folder!
    echo.
    echo Correct folder structure:
    echo   PixGallery_Enhanced/
    echo   ├── app/
    echo   ├── gradlew.bat
    echo   └── build.gradle.kts
    echo.
    pause
    exit /b 1
)

echo Step 1: Checking if signing key exists...
if exist "..\my-release-key.jks" (
    echo ✓ Signing key found!
) else (
    echo × Signing key NOT found!
    echo.
    echo Creating signing key...
    echo Please enter password (remember it!): pixgallery@2024
    echo.
    keytool -genkey -v -keystore ..\my-release-key.jks -keyalg RSA -keysize 2048 -validity 10000 -alias my-key-alias
    if errorlevel 1 (
        echo ERROR: Failed to create signing key!
        pause
        exit /b 1
    )
    echo ✓ Signing key created!
)

echo.
echo Step 2: Cleaning build...
call gradlew.bat clean
if errorlevel 1 (
    echo ERROR: Clean failed!
    pause
    exit /b 1
)
echo ✓ Clean complete!

echo.
echo Step 3: Building Release APK (This will take 10-15 minutes)...
call gradlew.bat assembleRelease
if errorlevel 1 (
    echo ERROR: Build failed!
    echo Try running: gradlew.bat clean
    pause
    exit /b 1
)
echo ✓ Build complete!

echo.
echo ============================================
echo   BUILD SUCCESSFUL! ✓
echo ============================================
echo.
echo APK Location:
echo   app\build\outputs\apk\release\app-release.apk
echo.
echo Size: ~15-20 MB
echo Status: Ready for Uptodown!
echo Ads: REAL EARNING ADS CONFIGURED
echo.
echo Next Steps:
echo   1. Connect Android phone via USB
echo   2. Run: adb install app\build\outputs\apk\release\app-release.apk
echo   3. Test on phone (banner ads, rewarded ads)
echo   4. Upload to Uptodown
echo.
pause
