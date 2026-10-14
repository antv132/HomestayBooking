@echo off
chcp 65001 >nul
setlocal EnableDelayedExpansion

echo ============================================
echo  Homestay Backend - Huong dan chay
echo ============================================
echo.
echo SQL Server phai dang chay (port 1433).
echo Frontend da chay o http://localhost:3000
echo Backend can chay o http://localhost:8080
echo.

set "JAVA_HOME="
if exist "C:\Program Files\Eclipse Adoptium\jdk-17.0.19.10-hotspot" (
  set "JAVA_HOME=C:\Program Files\Eclipse Adoptium\jdk-17.0.19.10-hotspot"
)
if exist "C:\Program Files\Java\jdk-17" (
  set "JAVA_HOME=C:\Program Files\Java\jdk-17"
)
if exist "C:\Program Files\Java\jdk-17.0.1" (
  set "JAVA_HOME=C:\Program Files\Java\jdk-17.0.1"
)

if defined JAVA_HOME (
  echo JDK: !JAVA_HOME!
) else (
  echo [CANH BAO] Chua tim thay JDK 17. Cai Temurin 17 hoac gan SDK trong IntelliJ.
)

echo.
echo CACH 1 - IntelliJ (khuyen dung):
echo   1. Build -^> Rebuild Project  (Ctrl+Shift+F9)
echo   2. Stop backend dang chay (nut Stop do)
echo   3. Run HomestayApplication lai
echo   4. Kiem tra: http://localhost:8080/api/rooms?page=0^&size=1
echo      Copy id -^> http://localhost:8080/api/rooms/{id}
echo      Phai thay JSON (KHONG phai loi 500)
echo   5. Refresh trang web: http://localhost:3000/rooms
echo.
echo CACH 2 - Maven (neu da cai mvn):
echo   cd /d "%~dp0"
echo   mvn spring-boot:run
echo.

set "MVN="
where mvn >nul 2>&1 && set "MVN=mvn"

if not defined MVN (
  for /d %%I in ("%ProgramFiles%\JetBrains\IntelliJ*") do (
    if exist "%%I\plugins\maven\lib\maven3\bin\mvn.cmd" (
      set "MVN=%%I\plugins\maven\lib\maven3\bin\mvn.cmd"
    )
  )
)

if defined MVN if defined JAVA_HOME (
  echo Tim thay Maven. Dang khoi dong backend...
  cd /d "%~dp0"
  set "PATH=!JAVA_HOME!\bin;!PATH!"
  "!MVN!" spring-boot:run
  goto :done
)

echo Chua chay duoc tu dong. Hay dung CACH 1 trong IntelliJ.
echo.
pause

:done
endlocal
