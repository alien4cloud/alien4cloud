@echo off

rem args:
rem %1 Number or requests
rem %2 Concurrency
rem %3 URL


set requests=%1
set concurrency=%2
set currentUrl=%3

install\bin\ab.exe -v INFO -n %requests% -c %concurrency% %currentUrl%