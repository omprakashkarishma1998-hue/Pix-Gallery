@rem
@rem Gradle startup script for Windows
@rem
@echo off
set DIRNAME=%~dp0
set APP_HOME=%DIRNAME%

set CLASSPATH=%APP_HOME%gradle\wrapper\gradle-wrapper.jar

if not exist "%CLASSPATH%" (
    echo ERROR: gradle-wrapper.jar not found at %CLASSPATH%
    echo Open this project in Android Studio once and it will regenerate the
    echo wrapper jar automatically, or run "gradle wrapper" if Gradle is installed.
    exit /b 1
)

"%JAVA_HOME%\bin\java.exe" -classpath "%CLASSPATH%" org.gradle.wrapper.GradleWrapperMain %*
