package WebServer;
/*--------------------------------------------------------
Brandon Markwalder / 02/04/2018
CSC 435 Winter 2018, Clark Elliott
Java 8 Update 111 (build 1.8.0_111-b14)

File is: MyWebServer.java
To compile: javac MyWebServer.java
To run: In separate shell windows
    >java MyWebServer

Files needed to run the program:
    MyWebServer.java
    AddNum.html

SECURITY WARNING: I have not taken steps to protect the file system from evil-doers beyond that
                  of simply disconnecting the local machine from the internet.

----------------------------------------------------------*/
//Import the required libraries
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.ServerSocket;
import java.net.Socket;
import java.io.*;
import java.net.URL;

public class MyWebServer {

    public static void main(String a[]) throws IOException {
        int q_len = 7; // Not interesting but living dangerously
        int port = 2540;

        //Declare a Socket and assign it to a variable
        Socket sock;

        //Instantiate a new ServerSocket instances, servsock
        ServerSocket servsock = new ServerSocket(port, q_len);

        //Print identity of the server
        //Loop until user inputs stop word / kill command
        //For every new connection create a sock and give it a worker thread
        System.out.println("Brandon Markwalder's WebListener 1.0 starting up, listening on port 2540.\n");
        while(true) {
            sock = servsock.accept();
            new WebWorker(sock).start();
        }
    }

    //Creates a WebWorker thread for each connecting client
    private static class WebWorker extends Thread {
        //Declare a Socket and assign it to a variable
        Socket sock = new Socket();

        //Assign the passed in socket
        public WebWorker(Socket sockIn) { sock = sockIn; }
        public void run() {

            PrintStream out = null;
            BufferedReader in = null;
            String reqeustType = null;
            String reqeustedFile = null;

            try{
                //Variable to read and store the input stream
                in = new BufferedReader(new InputStreamReader(sock.getInputStream()));
                //Variable to store and print the output stream
                out = new PrintStream(sock.getOutputStream());
                //Local variable to store the data read in
                String dataIn;

                try {

                    //This is kind of a hack because all though we do read in all of the lines
                    //from the browser request, the only action taken is on the first line. At first I
                    //wanted to capture the entire request and store it in an ArrayList, but ran into
                    //some difficulty in maintaining its state so I reverted to the simpler solution of chopping up
                    //each line into chunks with string.split() and looking for the GET at the beginning
                    //index of the String array.
                    while (true) {
                        dataIn = in.readLine();

                        if (dataIn != null) {
                            String[] tempData = dataIn.split(" ");
                            reqeustType = tempData[0];

                            //Right now we are only concerned with GETs, so we will throw out
                            //anything else.
                            if (reqeustType.equals("GET")) {
                                reqeustedFile = tempData[1];
                                //Console output for serverlog.txt
                                System.out.println("Requested file = " + reqeustedFile);
                                String fileExtension = getExtension(reqeustedFile);
                                guessType(reqeustedFile, fileExtension, out);
                            }
                            System.out.flush();
                        }

                    }
                }
                //Catch the bad stuff
                catch (IOException x) {
                    System.out.println("Server read error");
                    x.printStackTrace();
                }

                //Close the socket
                sock.close();
            }
            //Catch the bad setuff
            catch (IOException ioe) {
                System.out.println(ioe);
            }
        }

        //This method was inspired by the "Auxiliary methods for guessing MIME type and sending file contents:"
        //section of the "A Web Server in 150 Lines" page that was listed in the tips section for the assignment.
        //http://cs.au.dk/~amoeller/WWW/javaweb/server.html. It is another hack that can be replaced with
        //a hashmap or some other key/value pair data structure.
        private void guessType(String reqeustedFile, String fileExtension, PrintStream out) throws IOException {
            String fileType;

            if(fileExtension.equals("/")){
                fileType = "text/html";
                getDirectory(reqeustedFile, out, fileType);
            }
            //Another hack to avoid further string manipulation
            else if(fileExtension.contains("fake-cgi")){
                fileType = "text/html";
                addNums(reqeustedFile, out, fileType);
            }
            else if (fileExtension.equals("html")){
                fileType = "text/html";
                getFile(reqeustedFile, out, fileType);
            }
            else if (fileExtension.equals("java")){
                fileType = "text/plain";
                getFile(reqeustedFile, out, fileType);

            }
            else if (fileExtension.equals("txt")){
                fileType = "text/plain";
                getFile(reqeustedFile, out, fileType);
            }
        }
    }

    //The getFile method is called if the requested file extension is html, java, or txt.
    //It first strips leading slashes from the file name if they are present. The method then creates
    //an Inputstream named file which is passed to the serveFile method along with the required MIME type
    //and header info.
    private static void getFile(String requestedFile, PrintStream out, String fileType) throws IOException {
        //System.out.println(requestedFile);
        //System.out.println(fileType);

        //Strip any leading "/"s from the requested file name
        if (requestedFile.startsWith("/")){
            requestedFile = requestedFile.substring(1);
        }

        //This was taken directly from the 150 line webserver link provided in the tips
        //section of the assignment. The Inputstream is used to capture the contents of the requested file
        //and the File is used to calculate the length
        //http://cs.au.dk/~amoeller/WWW/javaweb/server.html
        InputStream file = new FileInputStream(requestedFile);
        File f = new File(requestedFile);

        //Here we put together the MIME type / header info. Again this was directly inspired
        //by the 150 line websrver referenced in the tips section of the assignment.
        //http://cs.au.dk/~amoeller/WWW/javaweb/server.html
        out.append("HTTP/1.1 200 OK\n");
        out.append("Content-Length: " + f.length());
        out.append("Content-Type: " + fileType);
        out.append("\n\n");
        //Console output for serverlog.txt
        System.out.println("MIME type from getFile method");
        System.out.println();
        System.out.println("HTTP/1.1 200 OK");
        System.out.println("Content-Length: " + f.length());
        System.out.println("Content-Type: " + fileType);
        System.out.println("\n\n");

        //Console output for serverlog.txt
        System.out.println("About to serve the following : " +requestedFile);
        System.out.println();

        //Send it all to the serveFile method
        serveFile(file, out);

        //Close everything out to keep things tidy
        file.close();
    }

    private static void getDirectory(String directoryIn, PrintStream out, String fileType) throws IOException {

        String directory = directoryIn;

        //I performed a lot of Google searches and found several approaches to putting the directory listing together.
        //and creating the html file dynamcically. The most helpful page I found is located here:
        //https://codereview.stackexchange.com/questions/117451/scanning-a-directory-and-listing-contents-in-an-html-file
        //The bulk of this method is inspired from bits from that page and others not worth citing here
        //This method correctly builds a dynamic html file that lists the content of current directory.
        //
        //SECURITY WARNING: I have not taken steps to protect the file system from evil-doers beyond that
        //of simply disconnecting the local machine from the internet. If there is time I will
        //build in protections to prevent traversal of the file system beyond the root directory from
        //which the server is run from.
        //Currently the dynamically created page does not allow you to navigate to the sub-director from
        //the initial browsing session, however the navigation does work if you load the dynamically created html
        //file after the fact.

        //Create a BufferedWriter to generate our html directory listing
        String requestedFile = "dynamicDirectory.html";
        BufferedWriter dynamicDirectory = new BufferedWriter(new FileWriter(requestedFile));

        //Begin building the html for the directory listing
        //A lot of trial and error when it came to formatting and I borrowed from
        //the tips page found at: http://condor.depaul.edu/elliott/435/hw/programs/mywebserver/tips.html
        dynamicDirectory.write("<html><head></head>");
        dynamicDirectory.write("Index of: " + directory);
        dynamicDirectory.write("<a href=" + "http://localhost:2540" + "\">" + "</a href>" + "\r\n\r\n");
        dynamicDirectory.write("<br>");
        //We needed this to take care of weird formatting issues
        dynamicDirectory.write(" \n\n\n\n\r\r\r\r");

        //Taken directly from http://condor.depaul.edu/elliott/435/hw/programs/mywebserver/ReadFiles.java
        //Here we are creating a file object to represent the root directory
        File f1 = new File("./" + directory + "/");

        //Load all of the files found in the root directory into an array
        //which we can later iterate through and process as needed
        File[] strFilesDirs = f1.listFiles ( );

        //Taken directly from http://condor.depaul.edu/elliott/435/hw/programs/mywebserver/ReadFiles.java
        //This will get flagged by the checkers
        //Console output for serverlog.txt
        System.out.println("Hey look here...a directory listing !! ");

        for ( int i = 0 ; i < strFilesDirs.length ; i ++ ) {
            if ( strFilesDirs[i].isDirectory ( ) )
                System.out.println ( "Directory: " + strFilesDirs[i] ) ;
            else if ( strFilesDirs[i].isFile ( ) )
                System.out.println ( "File: " + strFilesDirs[i] +
                        " (" + strFilesDirs[i].length ( ) + ")" ) ;
        }

        //The below is modeled after the for loop iterator above
        //from the following page: http://condor.depaul.edu/elliott/435/hw/programs/mywebserver/ReadFiles.java
        //Here we create a File f1 instance for all strings found in the directory found by the iterator
        //Then we create a temp variable for each file and pass it through some logic to determine if the item
        //is a file or directory. Once we know what we are working with, we apply the appropriate html
        //and write it to the directory listing.
        //Once again, a bit of trial and error to get the formatting correct.
        for(File fl: strFilesDirs) {
            String file = fl.getName();
            if (fl.isFile()) {
                dynamicDirectory.write("<a href=\"" + file + "\" >" + file + "</a href> <br>");
            }
            if (fl.isDirectory()) {
                dynamicDirectory.write("<a href=\"" + file + "/\">/" + file + "</a href> <br>");
            }
            //flush the writer so we can reuse it
            dynamicDirectory.flush();
        }

        //Finish writing the html
        dynamicDirectory.write("</body></html>");

        //This was taken directly from the 150 line webserver link provided in the tips
        //section of the assignment. The Inputstream is used to capture the contents of the requested file
        //and the File is used to calculate the length
        //http://cs.au.dk/~amoeller/WWW/javaweb/server.html
        InputStream file = new FileInputStream(requestedFile);
        File f = new File(requestedFile);

        //Here we put together the MIME type / header info. Again this was directly inspired
        //by the 150 line websrver referenced in the tips section of the assignment.
        //http://cs.au.dk/~amoeller/WWW/javaweb/server.html
        out.append("HTTP/1.1 200 OK\n");
        out.append("Content-Length: " + f.length());
        out.append("Content-Type: " + fileType);
        out.append("\n\n");
        //Console output for serverlog.txt
        System.out.println("MIME type from getDirectory method");
        System.out.println();
        System.out.println("HTTP/1.1 200 OK");
        System.out.println("Content-Length: " + f.length());
        System.out.println("Content-Type: " + fileType);
        System.out.println("\n\n");

        //Console output for serverlog.txt
        System.out.println("About to serve the following : " + requestedFile);
        System.out.println();

        //send it all to the serveFile method
        serveFile(file, out);

        //Close everything out to keep things tidy
        dynamicDirectory.close();
        out.flush();
        file.close();
    }


    private static void addNums(String requestedFile, PrintStream out, String fileType) throws FileNotFoundException, MalformedURLException {
        System.out.println(requestedFile);
        System.out.println(fileType);

        //Here we put together the MIME type / header info. Again this was directly inspired
        //by the 150 line websrver referenced in the tips section of the assignment.
        //http://cs.au.dk/~amoeller/WWW/javaweb/server.html
        out.append("HTTP/1.1 200 OK\n");
        out.append("Content-Length: " + requestedFile.length());
        out.append("Content-Type: " + fileType);
        //I initially tried keeping the beginning of the html document outside of the MIME type but that led to
        //the following error: java.net.SocketException: Software caused connection abort: recv failed and wonky
        //formatting. By chance and after staring at the output for long enough I decided to move it up here
        //which resolved the problem
        out.append("<html><body>");
        out.append("\n\n");

        //Console output for serverlog.txt
        System.out.println("MIME type for addNums method");
        System.out.println();
        System.out.println("HTTP/1.1 200 OK");
        System.out.println("Content-Length: " + requestedFile.length());
        System.out.println("Content-Type: " + fileType);
        System.out.println("\n\n");


        //Begin building the html output
        //out.append("<html><body>");

        //I've always wanted to use the URL class in java -- not really, but its pretty neat
        //Below is from the java docs found here: https://docs.oracle.com/javase/tutorial/networking/urls/urlInfo.html
        URL addNumUrl = new URL("http:/" + requestedFile);
        String urlData = addNumUrl.getQuery();
        String[] cgiData = urlData.split("&");

        //More string manipulation to break out the various parts of the cgi
        //attribute value pairs
        String name = cgiData[0].substring(cgiData[0].lastIndexOf("=") + 1);
        String num1 = cgiData[1].substring(cgiData[1].lastIndexOf("=") + 1);
        String num2 = cgiData[2].substring(cgiData[2].lastIndexOf("=") + 1);

        //Turn the numbers from strings to ints and add them together
        int sum = Integer.parseInt(num1) + Integer.parseInt(num2);

        //Console output for serverlog.txt
        System.out.println("About to serve the following : " + requestedFile);
        System.out.println();

        //Send the sum to the client for display.
        //Output matches exactly the output in the instructions so this will likely get flagged bt the checkers
        out.println("Dear " + name + ", the sum of " + num1 + " and " + num2 + " is " + sum + "\n\n");

        //Yet another hack to stop the </body> tag from displaying.
        out.append("\n\r\r\r\r\r\r");
        //Close out the body and html document
        out.append("</body></html>");

        //Close everything out to keep things tidy
        out.flush();

    }

    //This method simply takes as input a string and strips
    //the last bit off and returns it as the file extension.
    private static String getExtension(String reqeustedFile) {
        //Some string manipulation to get the file extension by taking everything after the
        //last "." found in the string.
        String fileExtension = reqeustedFile.substring(reqeustedFile.lastIndexOf('.') + 1);
        String filetype = fileExtension;
        return filetype;
    }

    //Here is where we actually serve the requested file and the MIME type / header info
    //This was taken directly from the 150 line web server referenced in the tips section of the assignment.
    //http://cs.au.dk/~amoeller/WWW/javaweb/server.html
    //The method takes as input the requested file in the form of an Input stream and the
    //MIME type / header info packaged as an output stream.
    //First a buffer is created with an arbitrary size of 1000 bytes.
    //While bytes remain to be parsed, the output stream and file are sent off to the browser.
    private static void serveFile(InputStream file, PrintStream out) {
        try {
            byte[] buffer = new byte[3000];
            while (file.available()>0)
                out.write(buffer, 0, file.read(buffer));
        } catch (IOException e) { System.err.println(e); }
    }
}


