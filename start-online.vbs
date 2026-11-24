' 无窗口启动器：让计划任务以脱离控制台的方式运行 start-online.bat
' 直接 cmd /c 启动的话，任务进程一退出会连带把子进程一起终止（0xC000013A）
Set shell = CreateObject("WScript.Shell")
shell.Run "cmd.exe /c ""D:\BackEnd\library-system\start-online.bat""", 0, False
