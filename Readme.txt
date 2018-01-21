Budowanie AndroidApp - z poziomu Android Studio:
- otworzy� folder AndroidApp jako projekt Android Studio (open an existing Android Studio project)
- pod��czy� do komputera telefon z w��czonym trybem debugowania
- utworzy� konfiguracj� Run -> Edit Configurations.. -> Defaults -> Android App -> wybra� "app" jako modu�
- uruchomi� konfiguracj� i wybra� pod��czony telefon jako cel
- alternatywnie mo�na zbudowa� plik .apk wybieraj�c Build -> Build APK
Do zbudowania AndroidApp konieczne jest zainstalowanie AndroidSDK 24

Budowanie Convergence - z poziomu IntelliJ:
- otworzy� w IntelliJ folder Convergence
- oznaczy� folder src jako Sources Root prawy klik na src -> Mark Directory as -> Sources Root
- doda� do projektu pliki .jar z folderu libs File -> Project Structure -> Libraries -> zielony plus -> Java -> wskaza� folder libs
- udstawi� foldery wyj�ciowe File -> Project Structure -> Modules -> zaznaczy� "Use module compile output path" i wybra� �cie�ki
- doda� artefakt .jar w konfiguracji projektu File -> Project Structure -> Artifacts -> zielony plus -> Jar from modules with dependencies -> wybra� Convergence.java jako Main Class
- zbudowa� .jar klikaj�c w Build -> Build Artifacts -> Convergence:jar -> Build
- plik Convergence.jar b�dzie si� znajdowa� w folderze ustawionym jako wyj�ciowy pod artifacts/Convergence_jar/
- uruchomi� z konsoli poleceniem java -jar Convergence.jar

Uruchamianie start_cameras.sh:
- otworzy� plik w wybranym edytorze tesktowym
- w miar� potrzeby zmieni� adresy ip/kamer/parametry
- uruchomi� na PandaBoard przy pomocy komendy ./start_cameras.sh
- UWAGA: u�ykownik, kt�ry uruchamia skrypt musi posiada� dost�p do kamer a wi�c nale�e� do odpowiedniej grupy u�ytkownik�w w systemie. Alternatywnie mo�na wykona� z u�ytkownika root polecenie chmod 777 /dev/videoX