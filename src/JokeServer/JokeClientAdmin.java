package JokeServer;
/*--------------------------------------------------------
Brandon Markwalder / 01/21/2018
CSC 435 Winter 2018, Clark Elliott
Java 8 Update 111 (build 1.8.0_111-b14)

File is: JokeClientAdmin.java
To compile: javac JokeClientAdmin.java
To run: In separate shell windows
    >java JokeClient
    >java JokeClientAdmin
    >java JokeServer

Files needed to run the program:
    JockClient.java
    JokeClientAdmin.java
    JokeServer.java

----------------------------------------------------------*/
import java.io.*;
import java.net.*;

public class JokeClientAdmin { ;

    //Main method where we declare the server name variable and
    //we default to localhost if there are no args supplied.
    public static void main (String args[]){
        String serverName;
        if (args.length < 1) serverName = "localhost";
        else serverName = args[0];

        System.out.println("Brandon Markwalder's Joke Client Administrator 1.0.\n");
        System.out.println("Using server: " + serverName + ", Port: 5050");
        System.out.println("Please make sure the JokeServer is running");

        //Instantiate a new Input Stream Reader wrapped in a Buffer Reader
        BufferedReader in = new BufferedReader(new InputStreamReader(System.in));

        //Hard coded stop word = "quit". While stop word not entered, loop forever
        //Each loop assigns user input to String name.
        try{
            String userInput;

            do {
                System.out.println("Press Enter to toggle the joke server between joke or proverb mode");
                System.out.flush();
                userInput = in.readLine();  //Assign user input to userName

                //If user input is and empty string from hitting Enter
                //Print changing mode and call the toggle method which takes
                //the serverNAme and userInput as arguments
                if (userInput.equals("")){
                    System.out.println("Changing JokeServer mode");
                    toggle(serverName, userInput);

                }
                else if(userInput.equals("quit")){
                    System.out.println("Cancelled by user request");
                }
                else{
                    System.out.println("Invalid input, Press Enter to get a joke or proverb from the JokeServer");
                }
            }
            while(!userInput.equals("quit"));
        }
        //Catch the bad stuff
        catch (IOException x) {
        x.printStackTrace();
        }
    }

    private static void toggle(String serverName, String userInput) {

        //Delcare our variables for the socket, reader and sender
        Socket sock;
        BufferedReader fromServer;
        PrintStream toServer;

        try{
            //Instantiate a new Socket instance and assign it to a port
            sock = new Socket(serverName, 5050);

            //Instantiate and assign are instances for sending to the server
            toServer = new PrintStream(sock.getOutputStream());
            //Send the toggle command to the JokeServer
            toServer.println(userInput);
            toServer.flush();
            sock.close();
        }
        //Catch the bad stuff
        catch (IOException x) {
            System.out.println("Socket error.");
            x.printStackTrace();
        }
    }
}
