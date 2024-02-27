mvn clean package & ^
rmdir %~dp0\bundle\jre /s /q & ^
del %~dp0\bundle\PSHE_Client-closedalpha.0.0.1.one-jar.jar & ^
echo "Creating Java runtime..." & ^
jlink --module-path javafx-jmods-21.0.2 --add-modules java.base,javafx.controls,javafx.base,javafx.graphics --output bundle/jre --compress=2 & ^
xcopy /s %~dp0\target\PSHE_Client-closedalpha.0.0.1.one-jar.jar %~dp0\bundle\ & ^
%~dp0\docs\warp-packer --arch windows-x64 --input_dir %~dp0\bundle --exec run.bat --output patcher_ui.exe