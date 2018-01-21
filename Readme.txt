Budowanie AndroidApp - z poziomu Android Studio:
- otworzyæ folder AndroidApp jako projekt Android Studio (open an existing Android Studio project)
- pod³¹czyæ do komputera telefon z w³¹czonym trybem debugowania
- utworzyæ konfiguracjê Run -> Edit Configurations.. -> Defaults -> Android App -> wybraæ "app" jako modu³
- uruchomiæ konfiguracjê i wybraæ pod³¹czony telefon jako cel
- alternatywnie mo¿na zbudowaæ plik .apk wybieraj¹c Build -> Build APK
Do zbudowania AndroidApp konieczne jest zainstalowanie AndroidSDK 24

Budowanie Convergence - z poziomu IntelliJ:
- otworzyæ w IntelliJ folder Convergence
- oznaczyæ folder src jako Sources Root prawy klik na src -> Mark Directory as -> Sources Root
- dodaæ do projektu pliki .jar z folderu libs File -> Project Structure -> Libraries -> zielony plus -> Java -> wskazaæ folder libs
- udstawiæ foldery wyjœciowe File -> Project Structure -> Modules -> zaznaczyæ "Use module compile output path" i wybraæ œcie¿ki
- dodaæ artefakt .jar w konfiguracji projektu File -> Project Structure -> Artifacts -> zielony plus -> Jar from modules with dependencies -> wybraæ Convergence.java jako Main Class
- zbudowaæ .jar klikaj¹c w Build -> Build Artifacts -> Convergence:jar -> Build
- plik Convergence.jar bêdzie siê znajdowa³ w folderze ustawionym jako wyjœciowy pod artifacts/Convergence_jar/
- uruchomiæ z konsoli poleceniem java -jar Convergence.jar

Uruchamianie start_cameras.sh:
- otworzyæ plik w wybranym edytorze tesktowym
- w miarê potrzeby zmieniæ adresy ip/kamer/parametry
- uruchomiæ na PandaBoard przy pomocy komendy ./start_cameras.sh
- UWAGA: u¿ykownik, który uruchamia skrypt musi posiadaæ dostêp do kamer a wiêc nale¿eæ do odpowiedniej grupy u¿ytkowników w systemie. Alternatywnie mo¿na wykonaæ z u¿ytkownika root polecenie chmod 777 /dev/videoX