# Ellipse-Android-Bluetooth-SDK-Public



Application usage:
Authorization Token: Please enter the authorization token provided by Lattis.
Ellipse Mac Address: If you know the Ellipse's mac address, please enter here. If you dont know, please click on 'Scan Ellipse(s)' button to start scanning the Ellipse(s). Once app show Ellipse(s), click on it to use it further for connection.
Once Ellipse is selected, click 'Connect Lock' to connect it.


Ellipse connection methods:

In order to scan and find Ellipse(s), please use 'startScan()' method defined in ScanEllipseFragment
In order to connect particular Ellipse, please use 'connectToLock()' method defined in HomeActivityFragment
Once Ellipse is connected, Ellipse can be lock unlock using 'setPosition(BluetoothLock lock, boolean locked)' defined in HomeActivityFragment. Put 'locked=true' if want to lock Ellipse.


Ellipse SDK can be found at Ellipse-Android-Bluetooth-SDK/Ellipse-Android-Bluetooth-SDK.aar


In order to build the application, please see the app's build.gradle to include necessary dependencies like rxbinding2, retrofit2, okhttp3, rxjava2 etc

