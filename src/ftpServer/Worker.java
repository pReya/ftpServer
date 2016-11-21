package ftpServer;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.StringTokenizer;

public class Worker extends Thread
{
    private enum userStatus {
        NOTLOGGEDIN, ENTEREDUSERNAME, LOGGEDIN
    }
    
    // Path information
    private String root = "/Users/preya/Dropbox/Coding/Eclipse/ftpServer";
    private String currDirectory;
    private String fileSeparator = "/";


    // control connection
    private Socket client;
    private PrintWriter out;
    private BufferedReader in;


    // data Connection
    private Socket dataConnection;
    private OutputStream dataOut;


    // Is anyone logged in?
    private userStatus currentUserStatus = userStatus.NOTLOGGEDIN;
    private String validUser = "anonymous";
    
    private boolean quitCommandLoop = false;
    
    /**
     * Create new worker with given client socket
     * @param client the socket for the current client
     */
    public Worker(Socket client)
    {
        super();
        this.client = client;
        root = "/Users/preya";
        currDirectory = "/Users/preya";
    }
    
    

    /**
     * run method required by Java thread model
     */
    public void run()
    {

        try
        {
            // Input from client
            in = new BufferedReader(new InputStreamReader(client.getInputStream()));
            
            // Output to client, automatically flushed after each print
            out = new PrintWriter(client.getOutputStream(), true);
            
            sendMsgToClient("220 Welcome to the COMP4621 FTP-Server");
            
            
            while (true)
            {
                executeCommand(in.readLine());
                
                if (quitCommandLoop)
                {
                    break;
                }
            }
            
            client.close();
            System.out.println("Server shut down");
            
        }
        catch (Exception e)
        {
            
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
        // Split command and arguments
        int index = c.indexOf(' ');
        String command = ((index == -1)? c.toUpperCase() : (c.substring(0, index)).toUpperCase());
        String args = ((index == -1)? null : c.substring(index+1, c.length()));


        System.out.println("Command: " + command + " Args: " + args);

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
            case "NLST":
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
            case "SYST":
                handleSyst();
                break;
            case "FEAT":
                handleFeat();
                break;
            case "PORT":
                handlePort(args);
                break;
            default:
                sendMsgToClient("501 Unknown command");
                break;
            
        }

    return true;
    }

    //
    // Dealing with the CWD command.
    //
    // Acceptable arguments: .. OR . OR relative path name not including .. or .
    //
    private void handleCwd(String args)
    {
        String filename = currDirectory;
    
        //
        // First the case where we need to go back up a directory.
        //
        if (args.equals(".."))
        {
            int ind = filename.lastIndexOf(fileSeparator);
            if (ind > 0)
            {
                filename = filename.substring(0, ind);
            }
        }

        //
        // Don't do anything if the user did "cd .". In the other cases,
        // append the argument to the current directory.
        //
        else if ((args != null) && (!args.equals(".")))
        {
            filename = filename + fileSeparator + args;
        }
    
        //
        // Now make sure that the specified directory exists, and doesn't
        // attempt to go to the FTP root's parent directory.  Note how we
        // use a "File" object to test if a file exists, is a directory, etc.
        //
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
    
    
    private void handleNlist(String args) throws Exception
    {
        String[] dirContent = nlstHelper(args);
        for (int i = 0; i < dirContent.length; i++)
        {
            sendMsgToClient(dirContent[i]);
        }
        
        
    }

    //
    // A helper for the NLST command. The directory name is obtained by 
    // appending "args" to the current directory. 
    //
    // Return an array containing names of files in a directory. If the given
    // name is that of a file, then return an array containing only one element
    // (this name). If the file or directory does not exist, return nul.
    //
    private String[]  nlstHelper(String args) throws IOException
    {
        //
        // Construct the name of the directory to list.
        //
        String filename = currDirectory;
        if (args != null)
        {
            filename = filename + fileSeparator + args;
        }
    
        //
        // Now get a File object, and see if the name we got exists and is a
        // directory.
        //
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
    
    private void handlePort(String args)
    {
        //
        // Extract the host name (well, really its IP address) and the port number
        // from the arguments.
        //
        StringTokenizer st = new StringTokenizer(args, ",");
        String hostName = st.nextToken() + "." + st.nextToken() + "." + 
                          st.nextToken() + "." + st.nextToken();
    
        int p1 = Integer.parseInt(st.nextToken());
        int p2 = Integer.parseInt(st.nextToken());
        int p = p1*256 + p2;
        
        openDataConnection(hostName, p);
    }

    
    /**
     * Handler for PWD ftp command. Lists content of current directory.
     * PWD = Print Working Directory
     */
    private void handlePwd()
    {
        out.println("257 \"" + currDirectory + "\"");
    }
    
    /**
     * Sends a message to the connected client. Flushing is automatically performed by the stream.
     * @param msg The message that will be sent
     */
    private void sendMsgToClient(String msg)
    {
        out.println(msg);
    }
    
    private void openDataConnection(String host, int port)
    {
        try
        {
            dataConnection = new Socket(host, port);
            dataOut = dataConnection.getOutputStream();
        } catch (IOException e)
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
    
    private void handlePasv()
    {
        String myIp = client.getLocalAddress().getHostAddress();
        StringTokenizer st = new StringTokenizer(myIp, ".");
        int port = 1026;
        int p1 = port/256;
        int p2 = port - p1;
        
        receiveDataConnection(port);
        
        sendMsgToClient("227 Entering Passive Mode ("+ st.nextToken() +"," + st.nextToken() + "," + st.nextToken() + "," + st.nextToken() + "," + p1 + "," + p2 +")");

    }
    
    private void receiveDataConnection(int port)
    {
        try
        {
            ServerSocket data = new ServerSocket(port);
            dataConnection = data.accept();
            dataOut = dataConnection.getOutputStream();
        } catch (IOException e)
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
    
    private void handleUser(String username)
    {
        if (username.toLowerCase().equals(validUser))
        {
            sendMsgToClient("331 Anonymous access allowed");
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
        if (currentUserStatus == userStatus.ENTEREDUSERNAME)
        {
            // no password is needed
            currentUserStatus = userStatus.LOGGEDIN;
            sendMsgToClient("230 Welcome to HKUST");
            sendMsgToClient("230 Anonymous user logged in");
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

}
