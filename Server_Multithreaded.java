import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import java.io.*;
import java.net.Socket;
import java.net.ServerSocket;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Scanner;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

public class Server_Multithreaded {

    static ArrayList<String> allStrings = new ArrayList<>();

    public static byte[] hashSHA1(String data) throws NoSuchAlgorithmException {
        MessageDigest md = MessageDigest.getInstance("SHA-1");
        return md.digest(data.getBytes());
    }

    /**
     * @param in Stream from which to read the request
     * @return Request with information required by the server to process encrypted file
     */
    public static Request readRequest(DataInputStream in) throws IOException {
        byte[] hashPwd = new byte[20];
        int count = in.read(hashPwd, 0, 20);
        if (count < 0) {
            throw new IOException("Server could not read from the stream");
        }
        int pwdLength = in.readInt();
        long fileLength = in.readLong();

        return new Request(hashPwd, pwdLength, fileLength);
    }

    /**
     * This function generates of strings of length k in a recursive way, the strings are stored in the allStrings structure
     * @param k size of the string we want to generate
     */
    static void allKLengthStrings( int k)
    {
        char[] set = {'a','b','c','d','e','f','g','h','i','j','k','l','m','n','o','p','q','r','s','t','u','v','w','x','y','z'};
        int n = set.length;
        allKLengthRec(set, "", n, k);
    }

    /**
     * This function is the recursive body of previous function
     * @param set the alphabet from which we generate the string
     * @param prefix  result of previous recurcive call
     * @param n length of the alphabet
     * @param k number of remaining characters to fill in the string
     */
    static void allKLengthRec(char[] set,String prefix,int n, int k)
    {   // Base case: k is 0, print prefix
        if (k == 0)    { allStrings.add(prefix);   return;  }
        for (int i = 0; i < n; ++i)
        {   String newPrefix = prefix + set[i];
            allKLengthRec(set, newPrefix,
                    n, k - 1);
        }
    }
    /**
     * this function extracts all strings from 10k-most-common file with a length of k
     * @param k length of targeted strings
     */
    static void getKLength(int k) throws IOException {
        //File file =new File("D:/SINF2M/LINFO-2241/Project_Part1/10k-most-common_filered.txt");

         File file =new File("C:/Users/celia/OneDrive/Bureau/Projet-LINFO2241-master/Projet-LINFO2241-master/10k-most-common_filered.txt");

        BufferedReader br = new BufferedReader(new FileReader(file));

        for (String line = br.readLine(); line != null; line = br.readLine()) {
            if (line.length()==k){ allStrings.add(line);}
        }
    }
    /**
     * This function brute forces the SHA-1 hash using a look-up attack to extract the password
     * @param hashPwd SHA-1 hash of the password
     * @param pwdlength length of original password before hash
     * @return cracked password
     */
    public static String bruteForce(byte [] hashPwd,int pwdlength) throws NoSuchAlgorithmException, IOException {
        if(pwdlength<6){
            allKLengthStrings(pwdlength);
        }
        else{
            getKLength(pwdlength);
        }

        String real_pass=Arrays.toString(hashPwd);
        for (int i=0;i< allStrings.size();i++){
            if( real_pass.equals(Arrays.toString(hashSHA1(allStrings.get(i))))){
                return allStrings.get(i);
            }
        }
        return "";
    }

    public static void thread_body(Socket socket) throws IOException, NoSuchAlgorithmException,
            InvalidKeySpecException, NoSuchPaddingException,
            IllegalBlockSizeException, BadPaddingException, InvalidKeyException {
        long current_time_t = System.currentTimeMillis();
        long pid=Thread.currentThread().getId();
        String decryptedFile_name="test_file-decrypted-server-"+pid+current_time_t+".pdf";
        File decryptedFile = new File(decryptedFile_name);
        String tempFile_name="temp-server"+pid+current_time_t+".pdf";
        File networkFile = new File(tempFile_name);

        // ServerSocket ss = new ServerSocket(3333);
        // System.out.println("Waiting connection");
        // Socket socket = ss.accept();
        // System.out.println("Connection from: " + socket);

        // Stream to read request from socket
        InputStream inputStream = socket.getInputStream();
        DataInputStream dataInputStream = new DataInputStream(inputStream);
        // Stream to write response to socket
        DataOutputStream outSocket = new DataOutputStream(socket.getOutputStream());


        // Stream to write the file to decrypt
        OutputStream outFile = new FileOutputStream(networkFile);

        Request request = readRequest(dataInputStream);
        long fileLength = request.getLengthFile();

        FileManagement.receiveFile(inputStream, outFile, fileLength);

        System.out.println("File length: " + networkFile.length());

        // HERE THE PASSWORD IS REPLACED WITH THE BRUTEFORCE PROCESS
        String password= bruteForce(request.getHashPassword(), request.getLengthPwd());
        System.out.println(" found password from brute force :" + password);
        SecretKey serverKey = CryptoUtils.getKeyFromPassword(password);
        CryptoUtils.decryptFile(serverKey, networkFile, decryptedFile);

        // Send the decryptedFile
        InputStream inDecrypted = new FileInputStream(decryptedFile);
        outSocket.writeLong(decryptedFile.length());
        outSocket.flush();
        FileManagement.sendFile(inDecrypted, outSocket);
        System.out.println("returned file for password :"+password);


        dataInputStream.close();
        inputStream.close();
        inDecrypted.close();
        outFile.close();
        decryptedFile.delete();
        networkFile.delete();
        socket.close();


    }

    public static void main(String[] args){



        int port = 3333;

        ThreadPoolExecutor executor = (ThreadPoolExecutor) Executors.newFixedThreadPool(4);
        try {
            ServerSocket ss = new ServerSocket(port);
            ss.setReuseAddress(true);
            Socket socket;
            System.out.println("Waiting connection");

            while ((socket = ss.accept()) != null ){

                Socket waiting_request = socket;

                executor.submit(()-> {
                    try {
                        thread_body(waiting_request);
                    } catch (IOException | NoSuchAlgorithmException | InvalidKeySpecException | NoSuchPaddingException | IllegalBlockSizeException | BadPaddingException | InvalidKeyException e) {
                        e.printStackTrace();
                    }
                });

            }
        }catch (IOException e){
            e.printStackTrace();
        }finally {
            executor.shutdown();
        }
    }
}