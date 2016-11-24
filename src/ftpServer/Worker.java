package ftpServer;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;

public class Worker extends Thread
{
    // Enable debugging output to console
    private boolean debugMode = true;
    
    private enum transferType {
        ASCII, BINARY
    }
    
    private enum userStatus {
        NOTLOGGEDIN, ENTEREDUSERNAME, LOGGEDIN
    }
    
    
    // Path information
    private String root = "/Users/preya";
    private String currDirectory = "/Users/preya/Dropbox";
    private String fileSeparator = "/";


    // control connection
    private Socket controlSocket;
    private PrintWriter controlOutWriter;
    private BufferedReader controlIn;


    // data Connection
    private ServerSocket dataSocket;
    private Socket dataConnection;
    private PrintWriter dataOutWriter;
    
    private int dataPort;
    private transferType transferMode = transferType.ASCII;


    // user properly logged in?
    private userStatus currentUserStatus = userStatus.NOTLOGGEDIN;
    private String validUser = "comp4621";
    private String validPassword = "network";
    
    private boolean quitCommandLoop = false;
    
    /**
     * Create new worker with given client socket
     * @param client the socket for the current client
     */
    public Worker(Socket client, int dataPort)
    {
        super();
        this.controlSocket = client;
        this.dataPort = dataPort;
    }
    
    

    /**
     * Run method required by Java thread model
     */
    public void run()
    {

        try
        {
            // Input from client
            controlIn = new BufferedReader(new InputStreamReader(controlSocket.getInputStream()));
            
            // Output to client, automatically flushed after each print
            controlOutWriter = new PrintWriter(controlSocket.getOutputStream(), true);
            
            // Greeting
            sendMsgToClient("220 Welcome to the COMP4621 FTP-Server");
            
            // Get new command from client
            while (!quitCommandLoop)
            {
                executeCommand(controlIn.readLine());
            }
                        
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        finally
        { 
            // Clean up 
            try
            {                    
                controlIn.close(); 
                controlOutWriter.close(); 
                controlSocket.close(); 
                debugOutput("Sockets closed and worker stopped"); 
            } 
            catch(IOException e) 
            { 
                e.printStackTrace();
                debugOutput("Could not close sockets");
            } 
        } 
        
    }
    
    /**
     * Main command dispatcher method
     * @param c
     * @return
     * @throws IOException
     */
    private boolean executeCommand(String c) throws IOException
    {
        // split command and arguments
        int index = c.indexOf(' ');
        String command = ((index == -1)? c.toUpperCase() : (c.substring(0, index)).toUpperCase());
        String args = ((index == -1)? null : c.substring(index+1, c.length()));


        debugOutput("Command: " + command + " Args: " + args);
        
        // dispatcher mechanism for different commands
        switch(command) {
            case "USER":                
                handleUser(args);
                break;
                
            case "PASS":
                handlePass(args);
                break;
                
            case "CWD":
                handleCwd(args);
                break;
                
            case "LIST":
                handleNlst(args);
                break;
                
            case "NLST":
                handleNlst(args);
                break;
                
            case "PWD":
                handlePwd();
                break;
                
            case "QUIT":
                handleQuit();
                break;
                
            case "PASV":
                handlePasv();
                break;
            
            case "EPSV":
                handleEpsv();
                break;
                
            case "SYST":
                handleSyst();
                break;
                
            case "FEAT":
                handleFeat();
                break;
                
            case "PORT":
                handlePort(args);
                break;
                
            case "EPRT":
                handlePort(parseExtendedArguments(args));
                break;
            
            case "RETR":
                handleRetr(args);
                break;
                
            case "MKD":
                handleMkd(args);
                break;
                
            case "RMD":
                handleRmd(args);
                break;
            
            case "TYPE":
                handleType(args);
                break;
                
            default:
                sendMsgToClient("501 Unknown command");
                break;
            
        }

    return true;
    }

    /**
     * Handler for CWD (change working directory) command.
     * @param new directory
     */
    private void handleCwd(String args)
    {
        String filename = currDirectory;
   
        // go one level up (cd ..)
        if (args.equals(".."))
        {
            int ind = filename.lastIndexOf(fileSeparator);
            if (ind > 0)
            {
                filename = filename.substring(0, ind);
            }
        }

        // if argument is anything else (cd . does nothing)
        else if ((args != null) && (!args.equals(".")))
        {
            filename = filename + fileSeparator + args;
        }
    
        // check if file exists, is directory and is not above root directory
        File f = new File(filename);
    
        if (f.exists() && f.isDirectory() && (filename.length() >= root.length()))
        {
            currDirectory = filename;
            sendMsgToClient("250 The current directory has been changed to " + currDirectory);
        }
        else
        {
            sendMsgToClient("550 Requested action not taken. File unavailable.");
        }
    }
    
    /**
     * Handler for NLST (Named List) command. Lists the directory content in a short format (names only)
     * @param args
     */
    private void handleNlst(String args)
    {
        if (dataConnection == null || dataConnection.isClosed())
        {
            sendMsgToClient("425 No data connection was established");
        }
        else
        {
            
            String[] dirContent = nlstHelper(args);
            
            if (dirContent == null)
            {
                sendMsgToClient("550 File does not exist.");
            }
            else
            {
                sendMsgToClient("125 Opening ASCII mode data connection for file list.");

                for (int i = 0; i < dirContent.length; i++)
                {
                    sendDataMsgToClient(dirContent[i]);
                }
            
                sendMsgToClient("226 Transfer complete.");
                closeDataConnection();
                
            }
            
        }
   
    }
    
    /**
     * A helper for the NLST command. The directory name is obtained by 
     * appending "args" to the current directory
     * @param args
     * @return an array containing names of files in a directory. If the given
     * name is that of a file, then return an array containing only one element
     * (this name). If the file or directory does not exist, return nul.
     */
    private String[] nlstHelper(String args)
    {
        // Construct the name of the directory to list.
        String filename = currDirectory;
        if (args != null)
        {
            filename = filename + fileSeparator + args;
        }
    

        // Now get a File object, and see if the name we got exists and is a
        // directory.
        File f = new File(filename);
            
        if (f.exists() && f.isDirectory())
        {
            return f.list();
        }
        else if (f.exists() && f.isFile())
        {
            String[] allFiles = new String[1];
            allFiles[0] = f.getName();
            return allFiles;
        }
        else
        {
            return null;
        }
    }
    
    /**
     * Handler for the PORT command.
     * The client issues a PORT command to the server in active mode, so the
     * server can open a data connection to the client through the given address
     * and port number.
     * @param The first four segments (separated by comma) are the IP address.
     *        The last two segments encode the port number (port = seg1*256 + seg2)
     */
    private void handlePort(String args)
    {
        // Extract IP address and port number from arguments
        String[] stringSplit = args.split(",");
        String hostName = stringSplit[0] + "." + stringSplit[1] + "." + 
                stringSplit[2] + "." + stringSplit[3];
    
        int p = Integer.parseInt(stringSplit[4])*256 + Integer.parseInt(stringSplit[5]);
        
        // Initiate data connection to client
        openDataConnectionActive(hostName, p);
        sendMsgToClient("200 Command OK");
        
    }

    
    /**
     * Handler for PWD (Print working directory) command.
     * Names the path of the current directory.
     */
    private void handlePwd()
    {
        sendMsgToClient("257 \"" + currDirectory + "\"");
    }
    
    /**
     * Sends a message to the connected client over the control connection.
     * Flushing is automatically performed by the stream.
     * @param msg The message that will be sent
     */
    private void sendMsgToClient(String msg)
    {
        controlOutWriter.println(msg);
    }
    
    /**
     * Send a message to the connected client over the data connection.
     * @param msg
     */
    private void sendDataMsgToClient(String msg)
    {
        if (dataConnection == null || dataConnection.isClosed())
        {
            sendMsgToClient("425 No data connection was established");
            debugOutput("Cannot send message, because no data connection is established");
        }
        else
        {
            dataOutWriter.println(msg);
        }
        
    }
    
    /**
     * Open a new data connection socket and wait for new incoming connection from client.
     * Used for passive mode.
     * @param Port on which to listen for new incoming connection
     */
    private void openDataConnectionPassive(int port)
    {

        try
        {
            dataSocket = new ServerSocket(port);
            dataConnection = dataSocket.accept();
            dataOutWriter = new PrintWriter(dataConnection.getOutputStream(), true);
            debugOutput("Data connection - Passive Mode - established");
            
        } catch (IOException e)
        {
            debugOutput("Could not create data connection.");
            e.printStackTrace();
        }
//        finally
//        {
//            try
//            {
//                dataOutWriter.close();
//                dataConnection.close();
//                dataSocket.close();
//                
//            } catch (IOException e)
//            {
//                debugOutput("Could not close sockets for data connection.");
//                e.printStackTrace();
//            }
//            dataOutWriter = null;
//            dataConnection = null;
//            dataSocket = null;
//            
//            
//        }
    }
    
    /**
     * Connect to client socket for data connection.
     * Used for active mode.
     * @param Client IP address to connect to
     * @param Client port to connect to
     */
    private void openDataConnectionActive(String ipAddress, int port)
    {
        try
        {
            dataConnection = new Socket(ipAddress, port);
            dataOutWriter = new PrintWriter(dataConnection.getOutputStream(), true);
            debugOutput("Data connection - Active Mode - established");
        } catch (IOException e)
        {
            debugOutput("Could not connect to client data socket");
            e.printStackTrace();
        }
//        finally
//        {
//            try
//            {
//                dataConnection.close();
//                dataOutWriter.close();
//            } catch (IOException e)
//            {
//                // TODO Auto-generated catch block
//                e.printStackTrace();
//            }
//            dataConnection = null;
//            dataOutWriter = null;
//        }
        
        
    }
    
    /**
     * Handler for PASV command which initiates the passive mode.
     * In passive mode the client initiates the data connection to the server.
     * In active mode the server initiates the data connection to the client.
     */
    private void handlePasv()
    {
        // Using fixed IP for connections on the same machine
        // For usage on separate hosts, we'd need to get the local IP address from somewhere
        // Java sockets did not offer a good method for this
        String myIp = "127.0.0.1";
        String myIpSplit[] = myIp.split("\\.");
        
        int p1 = dataPort/256;
        int p2 = dataPort%256;
        
        sendMsgToClient("227 Entering Passive Mode ("+ myIpSplit[0] +"," + myIpSplit[1] + "," + myIpSplit[2] + "," + myIpSplit[3] + "," + p1 + "," + p2 +")");
        
        openDataConnectionPassive(dataPort);

    }
    
    /**
     * Handler for EPSV command which initiates extended passive mode.
     * Similar to PASV but for newer clients (IPv6 support is possible but not implemented here).
     */
    private void handleEpsv()
    {
        sendMsgToClient("229 Entering Extended Passive Mode (|||" + dataPort + "|)");
        openDataConnectionPassive(dataPort);  
    }
    
    /**
     * Close previously established data connection sockets and streams
     */
    private void closeDataConnection()
    {
        try
        {
            dataOutWriter.close();
            dataConnection.close();
            if (dataSocket != null)
            {
                dataSocket.close();
            }
            
           
            debugOutput("Data connection was closed");
        } catch (IOException e)
        {
            debugOutput("Could not close data connection");
            e.printStackTrace();
        }
        dataOutWriter = null;
        dataConnection = null;
        dataSocket = null;          
    }
    
    /**
     * Handler for USER command.
     * User identifies the client.
     * @param username
     */
    private void handleUser(String username)
    {
        if (username.toLowerCase().equals(validUser))
        {
            sendMsgToClient("331 User name okay, need password");
            currentUserStatus = userStatus.ENTEREDUSERNAME;
        }
        else if (currentUserStatus == userStatus.LOGGEDIN)
        {
            sendMsgToClient("530 User already logged in");
        }
        else
        {
            sendMsgToClient("530 Not logged in");
        }
    }
    
    private void handlePass(String password)
    {
        // User has entered a valid username and password is correct
        if (currentUserStatus == userStatus.ENTEREDUSERNAME && password.equals(validPassword))
        {
            currentUserStatus = userStatus.LOGGEDIN;
            sendMsgToClient("230-Welcome to HKUST");
            sendMsgToClient("230 User logged in successfully");  
        }
        
        // User is already logged in
        else if (currentUserStatus == userStatus.LOGGEDIN)
        {
            sendMsgToClient("530 User already logged in");
        }
        
        // Wrong password
        else
        {
            sendMsgToClient("530 Not logged in");
        }
    }
    
    private void handleQuit()
    {
        sendMsgToClient("221 Closing connection");
        quitCommandLoop = true;
    }
    
    private void handleSyst()
    {
        sendMsgToClient("215 COMP4621 FTP Server Homebrew");
    }
    
    private void handleFeat()
    {
        sendMsgToClient("211-Extensions supported:");
        sendMsgToClient("211 END");
    }
    
    private void handleRetr()
    {
        sendMsgToClient("150 Opening ASCII mode data connection");
        
    }
    
    private void handleMkd(String args)
    {
        // Allow only alphanumeric characters
        if (args != null && args.matches("^[a-zA-Z0-9]+$"))
        {
            File dir = new File(currDirectory + fileSeparator + args);
            
            if(!dir.mkdir())
            {
                sendMsgToClient("550 Failed to create new directory");
                debugOutput("Failed to create new directory");
            }
            else
            {
                sendMsgToClient("250 Directory successfully created");
            }
        }
        else
        {
            sendMsgToClient("550 Invalid name");
        }
        
    }
    
    private void handleRmd(String args)
    {  
        String filename = currDirectory;
        
        if (args != null && args.matches("^[a-zA-Z0-9]+$"))
        {
            filename = filename + fileSeparator + args;
        }
    
        // check if file exists, is directory and is not above root directory
        File dir = new File(filename);
    
        if (dir.exists() && dir.isDirectory())
        {
            dir.delete();
            
            sendMsgToClient("250 Directory was successfully removed");
        }
        else
        {
            sendMsgToClient("550 Requested action not taken. File unavailable.");
        }
        
    }
    
    private void handleType(String mode)
    {
        if(mode.toUpperCase().equals("A"))
        {
            transferMode = transferType.ASCII;
            sendMsgToClient("200 OK");
        }
        else if(mode.toUpperCase().equals("I"))
        {
            transferMode = transferType.BINARY;
            sendMsgToClient("200 OK");
        }
        else
            sendMsgToClient("504 Not OK");;
            
    }
    
    private String parseExtendedArguments(String extArg)
    {
        String[] splitArgs = extArg.split("\\|");
        String ipAddress = splitArgs[2].replace('.', ',');
        int port = Integer.parseInt(splitArgs[3]);
        int p1 = port/256;
        int p2 = port%256;
         
        return ipAddress + "," + p1 + "," + p2;
        
    }
    
    private void handleRetr(String file)
    {
        File f =  new File(currDirectory + fileSeparator + file);
        if(!f.exists())
        {
            sendMsgToClient("550 Not ok");
        }

        else
        {
            BufferedOutputStream fout = null;
            BufferedInputStream fin = null;
            
            // Binary mode
            if (transferMode == transferType.BINARY)
            {
                sendMsgToClient("150 Opening binary mode data connection for requested file " + f.getName());
                
                try
                {
                    //create streams
                    fout = new BufferedOutputStream(dataConnection.getOutputStream());
                    fin = new BufferedInputStream(new FileInputStream(f));
                }
                catch (Exception e)
                {
                    debugOutput("Could not create file streams");
                }
                    
                debugOutput("Starting file transmission of " + f.getName());
                
                // write file with buffer
                byte[] buf = new byte[1024];
                int l = 0;
                try
                {
                    while ((l = fin.read(buf,0,1024)) != -1)
                    {
                        fout.write(buf,0,l);
                    }
                }
                catch (IOException e)
                {
                    debugOutput("Could not read from or write to file streams");
                    e.printStackTrace();
                }
                
                //close streams
                try
                {
                    fin.close();
                    fout.close();
                } catch (IOException e)
                {
                    debugOutput("Could not close file streams");
                    e.printStackTrace();
                }
                
                    
                debugOutput("Completed file transmission of " + f.getName());
  
                sendMsgToClient("226 File transfer successful. Closing data connection.");

            }
            
            // ASCII mode
            else
            {
                sendMsgToClient("150 Opening ASCII mode data connection for requested file " + f.getName());

                BufferedReader rin = null;
                PrintWriter rout = null;
                
                try
                {
                    rin = new BufferedReader(new FileReader(f));
                    rout = new PrintWriter(dataConnection.getOutputStream(),true);
                    
                }
                catch (IOException e)
                {
                    debugOutput("Could not create file streams");
                }
                
                String s;
                
                try
                {
                    while((s = rin.readLine()) != null)
                    {
                        rout.println(s);
                    }
                } catch (IOException e)
                {
                    debugOutput("Could not read from or write to file streams");
                    e.printStackTrace();
                }
                
                try
                {
                    rout.close();
                    rin.close();
                } catch (IOException e)
                {
                    debugOutput("Could not close file streams");
                    e.printStackTrace();
                }
                sendMsgToClient("226 File transfer successful. Closing data connection.");
            }

        }
        closeDataConnection();

    }
    
    private void debugOutput(String msg)
    {
        if (debugMode)
        {
            System.out.println("Thread " + this.getId() + ": " + msg);
        }
    }
    
    

}
