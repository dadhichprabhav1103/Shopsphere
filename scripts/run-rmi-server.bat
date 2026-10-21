@echo off
REM Build first:  mvn clean package
REM Optional DB overrides: set JAVA_OPTS=-Ddb.user=root -Ddb.password=secret
cd /d "%~dp0.."
java %JAVA_OPTS% -cp rmi\target\shopsphere-rmi-1.0.0.jar com.shopsphere.rmi.server.RmiServer %*
