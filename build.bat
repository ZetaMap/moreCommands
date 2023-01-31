@echo off
powershell ./gradlew :build
move /y .\build\libs\!moreCommands.jar .\
rd /s /q .\build