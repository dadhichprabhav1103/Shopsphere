@echo off
REM Build first:  mvn clean package
REM Optional DB overrides: set JAVA_OPTS=-Ddb.user=root -Ddb.password=secret
cd /d "%~dp0.."
java %JAVA_OPTS% -cp network\target\shopsphere-network-1.0.0.jar com.shopsphere.network.server.InventoryServer %*
