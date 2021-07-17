# FTP Project report

This is a simple implementation of a FTP server in Java as a class project for COMP4621 (Computer Communication Networks) at Hong Kong University of Science and Technology (HKUST) in fall semester 2016.

## The main features of this implementation are:

- Works with standard Windows/Linux/Mac terminal tools ftp, telnet and curl.
- Supports both IPv4 and IPv6 (thanks to @abdesamie).
- Support for both active and passive mode connections
- Supports binary/image and ASCII transfer mode
- Multi threaded (multiple users can transfer files at the same time)
- Standard control port is 1025, standard data port is 1026
- Standard user name is "comp4621" and password "network". User name and password are not case sensitive.
- Understands extended FTP arguments (EPSV instead of PASV and EPRT instead of EPRT).

## Problems/Todos with this implementation:

- Not working with GUI ftp clients, because they need to receive directory listings ins /bin/ls format. This implementation only prints names of files and folders, no additional information.
- File system access is not synchronized. Two clients writing to the same file will result in invalid results.
- If this is executed within an IDE (like Eclipse or IntelliJ IDEA), the control port likely needs to be a number larger than 1024. Java allows sockets on ports below 1024 only when the JVM is executed in super user mode (which IDEs normally don't do).
- The number of accepted connections is not limited. This could easily be exploited to crash the server by just opening several thousands of connections until the JVM crashes.
- This implementation can only run in Passive Mode on the same host as the client. If it needs to run on another host, the external server IP must be manually set as "myIp" in handlePasv(). This is because the Java socket implementation does not reliably return the external IP of the corresponding network device. To overcome this, one would probably have to use some kind of external API.
- No unit tests.
- No timeouts.

## General Architecture

This implementation consists of two classes: Server and Worker. They implement a basic worker scheme. The Server class is listening for new connections. As soon as it receives a new connection it will hand it of to the Worker class within a new thread. So for every new data connection a new thread will be created, making this a multi-threaded server.

The Worker class takes care of handling all FTP commands. Therefore it uses a central while-loop (line 95). Every iteration of the loop gets a new command from the input stream that is connected to the client socket.

The incoming command is sent of to a central mini-dispatcher function called "executeCommand", which splits the command from the arguments and then dispatches it with a big switch/case statement. Every FTP command has its own handler function.

For implementation details on the handler functions please refer to the comments/javadoc.

## Supported FTP commands
* USER
* PASS
* PASV
* EPASV
* PORT
* EPRT
* MKD
* XMKD
* RMD
* CWD
* PWD
* XPWD
* TYPE
* RETR
* STOR
* QUIT
* SYST
* FEAT


## Instructions

Before you start the server, you should create a folder named `test` in the folder, where you want to start the server. These examples are based on the Windows command line ftp client:

```
ftp> open localhost 1025
Connected to localhost.
220 Welcome to the COMP4621 FTP-Server
501 Unknown command
User (localhost:(none)): comp4621
331 User name okay, need password
Password:
230-Welcome to HKUST
230 User logged in successfully
```

## Ressources used
* https://tools.ietf.org/html/rfc959
* https://tools.ietf.org/html/rfc2428
* http://www.nsftools.com/tips/RawFTP.htm
* https://cr.yp.to/ftp.html
* http://www.mysamplecode.com/2011/12/java-multithreaded-socket-server.html
* http://www.ugrad.cs.ubc.ca/~cs219/Labs/Threads/lab-threads.html
