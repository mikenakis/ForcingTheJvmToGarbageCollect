@echo off
if not "%Platform%"=="x64" (
	echo ERROR: 'Platform' variable missing.
    echo Please run this from within the x64 Native Tools Command Prompt of Visual Studio.
	exit /b 1
)
cl /LD /I "C:\Program Files\Java\jdk-17\include" /I "C:\Program Files\Java\jdk-17\include\win32" ForceGarbageCollection.cpp
move ForceGarbageCollection.dll ..
del ForceGarbageCollection.obj
del ForceGarbageCollection.exp
del ForceGarbageCollection.lib
