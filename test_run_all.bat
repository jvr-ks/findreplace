@echo off
@for %%i in ("test_*.frpl") do (findreplace %%i
if errorlevel 1 pause & echo A test failed!
)

testspezial_variablen.bat

@pause READY!



