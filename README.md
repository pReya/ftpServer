# ftpServer
Simple implementation of a FTP server in Java. This was a class project for COMP4621 (Computer Communication Networks) at
Hong Kong University of Science and Technology (HKUST).

## Features
* Works with the standard Linux/Mac command line ftp tool
* Not working with grahical clients, since the directory listings are not in /bin/ls format
* Supports both active and passive mode data connection
* Multi threaded (allowing multiple connections at the same time)

## Configuration
1. Set "controlPort" in Server.java (needs to be above 1024 if you're not running the JVM in sudo mode, e.g. within your IDE)
2. Set root, currFirectory and fileSeparator in Worker.java
(3. If client and server are not on the same host, you need to add the client IP address to myIp in handlePasv().)

