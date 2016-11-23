package ftpServer;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class Server
{
    private int controlPort = 1025;
    private ServerSocket welcomeSocket;
    boolean serverRunning = true;

    public static void main(String[] args)
    {
        new Server();
    }

    public Server()
    {
        try
        {
            welcomeSocket = new ServerSocket(controlPort);
        }
        catch (IOException e)
        {
            System.out.println("Could not create server socket"); 
            System.exit(-1);
        }
        
        System.out.println("FTP Server started listening on port " + controlPort);


        while (serverRunning)
        {

            try
            {
                Socket client = welcomeSocket.accept();
                Worker w = new Worker(client);
                System.out.println("New connection received. Worker was created.");
                w.start();
            }
            catch (IOException e)
            {
                System.out.println("Exception encountered on accept");  
                e.printStackTrace();
            }
            
        }
        try
        {
            welcomeSocket.close();
            System.out.println("Server was stopped");
            
        } catch (IOException e)
        {
            System.out.println("Problem stopping server"); 
            System.exit(-1);
        }

    }
    
    

}
