@echo off
set "program=Berkeley.jar"
set "num_instancias=3"
set "parametros1=1 localhost 8001 10000 950 25"
set "parametros2=2 localhost 8002 10000 950 25"
set "parametros3=0 localhost 8000 10000 950 25"

for /l %%i in (1,1,%num_instancias%) do (
    if %%i==1 (
        start "Instancia%%i" cmd /c "java -jar "%program%" %parametros1%"
    ) else if %%i==2 (
        start "Instancia%%i" cmd /c "java -jar "%program%" %parametros2%"
    ) else if %%i==3 (
        start "Instancia%%i" cmd /c "java -jar "%program%" %parametros3%"
    )
)
d