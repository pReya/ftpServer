package ftpServer;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.StringTokenizer;

public class Worker extends Thread
{
    //
    // Path information
    //
    private String root;
    private String currDirectory;
    private String fileSeparator = "/";

    //
    // TELNET Connection
    //
    private Socket client;
    private PrintWriter out;
    private BufferedReader in;

    //
    // Data Connection
    //
    private Socket dataConnection;
    private OutputStream dataOut;

    //
    // Is anyone logged in?
    //
    private boolean hasCurrentUser;
    
    

    //
    // The run method...
    // 
    public void run()
    {

        try
        {
            in = new BufferedReader(new InputStreamReader(client.getInputStream()));
            out = new PrintWriter(client.getOutputStream(), true);
            handleLogin();
            
            executeCommand(in.readLine());
        }
        catch (Exception e)
        {
            
        }
        
    }

    //
    // Execute a single command. This function should return "false" if the
    // command was "quit", and true in every other case.
    //
    private boolean executeCommand(String c) throws IOException
    {
        int index = c.indexOf(' ');
        String command = ((index == -1)? c.toUpperCase() : (c.substring(0, index)).toUpperCase());
        String args = ((index == -1)? null : c.substring(index+1, c.length()));

        //
        // For debugging purposes...
        //
        System.out.println("Command: " + command + " Args: " + args);

        //
        // Deal with each command in its own method, please.
        //
        switch(command) {
            case "CWD":
                handleCwd(args);
                break;
            case "NLST":
                break;
            case "PWD":
                handlePwd();
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
            out.println("250 The current directory has been changed to " + currDirectory);
        }
        else
        {
            out.println("550 Requested action not taken. File unavailable.");
        }
    }
    
    
    private void handleNlist(String args) throws Exception
    {
        String[] dirContent = nlstHelper(args);
        for (int i = 0; i < dirContent.length; i++)
        {
            out.println(dirContent[i]);
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

    public Worker(Socket client)
    {
        super();
        this.client = client;
        root = "/Users/preya";
        currDirectory = "/Users/preya";
    }
    
    private void handleLogin() throws IOException
    {
        String validUser = "anonymous";
        // send welcome message
        out.println("220 Welcome to the COMP4621 FTP-Server");
        
        // receive string from server in this form: "USER xxxx"
        // split it on whitespace to get just the user name
        String user = in.readLine().split("\\s+")[1];
        
        if (user.toLowerCase().equals(validUser))
        {
            out.println("331 Anonymous access allowed");
            hasCurrentUser = true;
            String password = in.readLine().split("\\s+")[1];
            out.println("230 Welcome to HKUST");
            out.println("230 Anonymous user logged in");
            
            // handle SYST
            if (in.readLine().toLowerCase().equals("syst"))
            {
                out.println("215 COMP4621 FTP Server Homebrew");
            }
            
            // handle FEAT
            if (in.readLine().toLowerCase().equals("feat"))
            {
                out.println("211-Extensions supported:");
                out.println("211 END");
            }
            //System.out.println(in.readLine());
        }
        else
        {
            out.println("530 Not logged in");
        }
        
    }
    
    private void handlePwd()
    {
        out.println(currDirectory);
    }

}
