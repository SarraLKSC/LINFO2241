import java.util.concurrent.ThreadLocalRandom;

public class TestRuns {


    public static void main(String[] args) throws InterruptedException {
        int n=5; // n is number of clients
        for (int i=0;i<n;i++) {
            /** Setting a random interval time before client arrival **/
            int t = ThreadLocalRandom.current().nextInt(0, (int) 10000);
            Thread.sleep(t);

            Client_Multithreaded cm = new Client_Multithreaded();
            new Thread(cm).start(); // we create a new thread to run the client

        }
    }
}
