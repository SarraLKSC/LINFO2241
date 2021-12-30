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
import java.util.ArrayList;
import java.util.Arrays;
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

    static void allKLengthStrings( int k)
    {
        char[] set = {'a','b','c','d','e','f','g','h','i','j','k','l','m','n','o','p','q','r','s','t','u','v','w','x','y','z'};
        int n = set.length;
        allKLengthRec(set, "", n, k);
    }

    static void allKLengthRec(char[] set,String prefix,int n, int k)
    {   // Base case: k is 0, print prefix
        if (k == 0)    { allStrings.add(prefix);   return;  }
        for (int i = 0; i < n; ++i)
        {   String newPrefix = prefix + set[i];
            allKLengthRec(set, newPrefix,
                    n, k - 1);
        }
    }
    static void getKLength(int k) throws IOException {
        File file =new File("D:/SINF2M/LINFO-2241/Project_Part1/10k-most-common_filered.txt");

        BufferedReader br = new BufferedReader(new FileReader(file));

        for (String line = br.readLine(); line != null; line = br.readLine()) {
            if (line.length()==k){ allStrings.add(line);}
        }
    }
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

    public static void process_file(Socket socket) throws IOException, NoSuchAlgorithmException,
            InvalidKeySpecException, NoSuchPaddingException,
            IllegalBlockSizeException, BadPaddingException, InvalidKeyException {
        File decryptedFile = new File("test_file-decrypted-server.pdf");
        File networkFile = new File("temp-server.pdf");

        // ServerSocket ss = new ServerSocket(3333);
        System.out.println("Waiting connection");
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
        /*
        int readFromFile = 0;
        int bytesRead = 0;
        byte[] readBuffer = new byte[64];

        System.out.println("[Server] File length: "+ fileLength);
        while((readFromFile < fileLength)){
            bytesRead = inputStream.read(readBuffer);
            readFromFile += bytesRead;
            outFile.write(readBuffer, 0, bytesRead);
        }*/

        System.out.println("File length: " + networkFile.length());

        // HERE THE PASSWORD IS HARDCODED, YOU MUST REPLACE THAT WITH THE BRUTEFORCE PROCESS
        String password = bruteForce(request.getHashPassword(), request.getLengthPwd());
        System.out.println(" found password from brute force :" + password);
        SecretKey serverKey = CryptoUtils.getKeyFromPassword(password);
        CryptoUtils.decryptFile(serverKey, networkFile, decryptedFile);

        // Send the decryptedFile
        InputStream inDecrypted = new FileInputStream(decryptedFile);
        outSocket.writeLong(decryptedFile.length());
        outSocket.flush();
        FileManagement.sendFile(inDecrypted, outSocket);
        /*
        int readCount;
        byte[] buffer = new byte[64];
        //read from the file and send it in the socket
        while ((readCount = inDecrypted.read(buffer)) > 0){
            outSocket.write(buffer, 0, readCount);
        }*/

        dataInputStream.close();
        inputStream.close();
        inDecrypted.close();
        outFile.close();
        socket.close();

    }

    public static void main(String[] args){
        int number_of_threads = 2;
        if(args.length==1){
            number_of_threads = Integer.getInteger(args[0]);
        }
        int port = 3333;

        ThreadPoolExecutor executor = (ThreadPoolExecutor) Executors.newFixedThreadPool(number_of_threads);
        try {
            ServerSocket serverSocket = new ServerSocket(port);
            serverSocket.setReuseAddress(true);
            Socket un_client;
            System.out.println("Waiting connection");
            int id = 0;
            while ((un_client = serverSocket.accept()) != null ){
                Socket actuel_client = un_client;

                executor.submit(()-> {
                    try {
                        process_file(actuel_client);
                    } catch (IOException | NoSuchAlgorithmException | InvalidKeySpecException | NoSuchPaddingException | IllegalBlockSizeException | BadPaddingException | InvalidKeyException e) {
                        e.printStackTrace();
                    }
                });
                id++;
            }
        }catch (IOException e){
            e.printStackTrace();
        }finally {
            executor.shutdown();
        }
    }
}