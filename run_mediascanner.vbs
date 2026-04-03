' run_mediascanner.vbs
' This script launches MediaScanner without any visible command prompt window

Set WshShell = CreateObject("WScript.Shell")

' Set the working directory to your nested Maven project folder
WshShell.CurrentDirectory = "C:\Users\theda\Desktop\VS Projects\mediascanner\mediascanner"

' Run the maven command. The '0' argument completely hides the command window. 
' The 'False' argument lets the script close immediately while the app stays open.
WshShell.Run "cmd.exe /c mvn javafx:run", 0, False