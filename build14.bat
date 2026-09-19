@echo off
setlocal EnableDelayedExpansion
cd /d "%~dp0"
set LOG=%~dp0build-log14.txt
echo === build started %DATE% %TIME%>"%LOG%"

echo Removing old extracted android folder if present ...>>"%LOG%"
if exist "android" rmdir /s /q "android">>"%LOG%" 2>&1

REM --- Pick the newest vin-android-v*.zip present in this folder ---
set "ZIPFILE="
for /f "delims=" %%F in ('dir /b /o-d "vin-android*.zip" 2^>nul') do (
  if not defined ZIPFILE set "ZIPFILE=%%F"
)

if not defined ZIPFILE (
  echo NO_ZIP_FOUND>>"%LOG%"
  echo Aucun fichier vin-android*.zip trouve dans ce dossier.>>"%LOG%"
  echo EXIT_CODE=2>>"%LOG%"
  echo BUILD_SCRIPT_COMPLETE_9f3a1>>"%LOG%"
  exit /b 2
)

echo Extracting !ZIPFILE! ...>>"%LOG%"
powershell -NoProfile -ExecutionPolicy Bypass -Command "Expand-Archive -Path '!ZIPFILE!' -DestinationPath '.' -Force">>"%LOG%" 2>&1

cd android

set "SDKDIR=%USERPROFILE%\android-sdk"
if not exist "%SDKDIR%" set "SDKDIR=%LOCALAPPDATA%\Android\Sdk"
set "ANDROID_HOME=%SDKDIR%"
set "ANDROID_SDK_ROOT=%SDKDIR%"

echo sdk.dir=!SDKDIR:\=/!>local.properties
echo local.properties:>>"%LOG%"
type local.properties>>"%LOG%"

REM =====================================================================
REM JDK detection. IMPORTANT: Gradle 8.9's embedded Kotlin DSL compiler
REM cannot parse very new JDK version strings (e.g. JDK 25.x, which is
REM what recent Android Studio releases bundle as their JBR). So we
REM must PREFER a real JDK 17 install over Android Studio's bundled
REM jbr/jre, even though the jbr is usually a convenient fallback.
REM Search order: (1) explicit JDK 17 installs, (2) Android Studio's
REM bundled jbr/jre as a lower-priority fallback, (3) recursive scan,
REM (4) whatever "java" resolves to on PATH.
REM =====================================================================
set "JAVA_HOME="

REM --- Pass 1: explicit JDK 17 installs (preferred) ---
for %%D in (
  "%USERPROFILE%\jdk17"
  "C:\Program Files\Eclipse Adoptium\jdk-17"
  "C:\Program Files\Java\jdk-17"
  "C:\Program Files\Microsoft\jdk-17"
) do (
  if exist "%%~D\bin\java.exe" if not defined JAVA_HOME set "JAVA_HOME=%%~D"
)

if not defined JAVA_HOME (
  for /f "delims=" %%P in ('dir /b /ad "C:\Program Files\Eclipse Adoptium\jdk-17*" 2^>nul') do (
    if not defined JAVA_HOME set "JAVA_HOME=C:\Program Files\Eclipse Adoptium\%%P"
  )
)
if not defined JAVA_HOME (
  for /f "delims=" %%P in ('dir /b /ad "C:\Program Files\Java\jdk-17*" 2^>nul') do (
    if not defined JAVA_HOME set "JAVA_HOME=C:\Program Files\Java\%%P"
  )
)
if not defined JAVA_HOME (
  for /f "delims=" %%P in ('dir /b /ad "C:\Program Files\Microsoft\jdk-17*" 2^>nul') do (
    if not defined JAVA_HOME set "JAVA_HOME=C:\Program Files\Microsoft\%%P"
  )
)

REM --- Pass 2: Android Studio's bundled jbr/jre (fallback only, may be too new) ---
if not defined JAVA_HOME (
  for %%D in (
    "C:\Program Files\Android\Android Studio\jbr"
    "C:\Program Files\Android\Android Studio\jre"
    "C:\Program Files (x86)\Android\Android Studio\jbr"
    "%LOCALAPPDATA%\Android Studio\jbr"
    "%LOCALAPPDATA%\Programs\Android Studio\jbr"
    "%LOCALAPPDATA%\JetBrains\Toolbox\apps\Android Studio\ch-0\jbr"
  ) do (
    if exist "%%~D\bin\java.exe" if not defined JAVA_HOME set "JAVA_HOME=%%~D"
  )
)

REM --- Pass 3: scoped recursive search for any java.exe ---
if not defined JAVA_HOME (
  for %%R in (
    "%LOCALAPPDATA%\Android Studio"
    "%LOCALAPPDATA%\Programs\Android Studio"
    "%LOCALAPPDATA%\Programs"
    "C:\Program Files\Android"
  ) do (
    if not defined JAVA_HOME if exist "%%~R" (
      set "VIN_JDK_SEARCH_ROOT=%%~R"
      for /f "delims=" %%J in ('powershell -NoProfile -Command "Get-ChildItem -Path $env:VIN_JDK_SEARCH_ROOT -Filter java.exe -Recurse -ErrorAction SilentlyContinue -Depth 6 | Select-Object -First 1 -ExpandProperty FullName" 2^>nul') do (
        if not defined JAVA_HOME set "JAVA_HOME=%%~dpJ.."
      )
    )
  )
)

REM --- Pass 4: whatever "java" already resolves to on PATH ---
if not defined JAVA_HOME (
  for /f "delims=" %%J in ('where java 2^>nul') do (
    if not defined JAVA_HOME set "JAVA_HOME=%%~dpJ.."
  )
)

if defined JAVA_HOME (
  set "PATH=%JAVA_HOME%\bin;%PATH%"
) else (
  echo NO_JDK_FOUND>>"%LOG%"
  echo Checked: %USERPROFILE%\jdk17, Eclipse Adoptium, Program Files\Java, Microsoft Build of OpenJDK, Android Studio jbr/jre ^(Program Files and LOCALAPPDATA^), PATH>>"%LOG%"
)

echo ANDROID_HOME=[%ANDROID_HOME%]>>"%LOG%"
echo JAVA_HOME=[%JAVA_HOME%]>>"%LOG%"

if not defined JAVA_HOME (
  echo EXIT_CODE=9009>>"%LOG%"
  echo BUILD_SCRIPT_COMPLETE_9f3a1>>"%LOG%"
  exit /b 9009
)

copy /Y "%~dp0google-services-real.json" "app\google-services.json">>"%LOG%" 2>&1
call gradlew.bat --stop>>"%LOG%" 2>&1
call gradlew.bat assembleDebug --stacktrace --no-daemon>>"%LOG%" 2>&1
set "BUILD_RESULT=%ERRORLEVEL%"
echo EXIT_CODE=%BUILD_RESULT%>>"%LOG%"

REM --- On success: copy+rename the APK to the root "vin" folder as ---
REM --- "vin X.YY.apk", auto-incrementing X.YY on every successful build. ---
if "%BUILD_RESULT%"=="0" (
  set "APKSRC=app\build\outputs\apk\debug\app-debug.apk"
  if exist "!APKSRC!" (
    set "VERFILE=%~dp0vin-version.txt"
    set "VERNUM=100"
    if exist "!VERFILE!" set /p VERNUM=<"!VERFILE!"
    set /a VERNUM=VERNUM+1
    echo !VERNUM!>"!VERFILE!"
    set /a VMAJOR=VERNUM/100
    set /a VMINOR=VERNUM%%100
    if !VMINOR! LSS 10 set "VMINOR=0!VMINOR!"
    set "APKVERSION=!VMAJOR!.!VMINOR!"
    set "APKDEST=%~dp0vin !APKVERSION!.apk"
    copy /y "!APKSRC!" "!APKDEST!">>"%LOG%" 2>&1
    echo APK_VERSION=!APKVERSION!>>"%LOG%"
    echo APK_COPIED_TO=!APKDEST!>>"%LOG%"
    echo.
    echo Build reussi -^> !APKDEST!
  ) else (
    echo APK_NOT_FOUND=!APKSRC!>>"%LOG%"
    echo Build signale comme reussi mais app-debug.apk introuvable, voir "%LOG%".
  )
) else (
  echo Build echoue ^(code %BUILD_RESULT%^), voir "%LOG%".
)

cd /d "%~dp0"
echo BUILD_SCRIPT_COMPLETE_9f3a1>>"%LOG%"