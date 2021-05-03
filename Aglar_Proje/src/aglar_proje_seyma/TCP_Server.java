package aglar_proje_v1;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

public class TCP_Server {

    private ServerSocket serverSocket;
    private Thread serverThread;
    private ObjectOutputStream clientOutput;
    private HashSet<ObjectOutputStream> allClients = new HashSet<>();

    private javax.swing.JTextPane historyJTextPane;

    public String turn;

    final static int WINPOINT = 100;

    static Map<Integer, Integer> snake = new HashMap<Integer, Integer>();
    static Map<Integer, Integer> ladder = new HashMap<Integer, Integer>();

    {
        snake.put(97, 78);
        snake.put(95, 56);
        snake.put(88, 24);
        snake.put(62, 18);
        snake.put(48, 26);
        snake.put(36, 6);
        snake.put(32, 10);

        ladder.put(1, 38);
        ladder.put(4, 14);
        ladder.put(8, 30);
        ladder.put(28, 74);
        ladder.put(21, 42);
        ladder.put(50, 67);
        ladder.put(71, 92);
        ladder.put(88, 99);
    }

    protected void start(int port, javax.swing.JTextPane jTextPaneHistory) throws IOException {
        // server soketi oluşturma (sadece port numarası)
        serverSocket = new ServerSocket(port);
        System.out.println("Server başlatıldı ..");

        // server arayüzündeki history alanı, bütün olaylar buraya yazılacak
        this.historyJTextPane = jTextPaneHistory;
        // arayüzü kitlememek için, server yeni client bağlantılarını ayrı Thread'de beklemeli
        serverThread = new Thread(() -> {
            while (!serverSocket.isClosed()) {
                try {
                    // blocking call, yeni bir client bağlantısı bekler
                    Socket clientSocket = serverSocket.accept();
                    clientOutput = new ObjectOutputStream(clientSocket.getOutputStream());
                    System.out.println("Yeni bir client bağlandı : " + clientSocket);
                    allClients.add(clientOutput);
                    // bağlanan her client için bir thread oluşturup dinlemeyi başlat
                    new ListenThread(clientSocket).start();
                } catch (IOException ex) {
                    System.out.println("Hata - new Thread() : " + ex);
                    break;
                }
            }
        });
        serverThread.start();
    }

    protected void writeToHistory(String message) {
        // server arayüzündeki history alanına mesajı yaz
        historyJTextPane.setText(historyJTextPane.getText() + "\n" + message);
    }

    protected void sendBroadcast(String message) throws IOException {
        // bütün bağlı client'lara mesaj gönder
        for (ObjectOutputStream output : allClients) {
            output.writeObject(message);
        }
    }

    protected void stop() throws IOException {
        // bütün streamleri ve soketleri kapat
        if (serverSocket != null) {
            serverSocket.close();
        }

        //sendBroadcast("kapat");
    }

    class ListenThread extends Thread {

        // dinleyeceğimiz client'ın soket nesnesi, input ve output stream'leri
        private final Socket clientSocket;
        private ObjectInputStream clientInput;
        private ObjectOutputStream clientOutput;

        private ListenThread(Socket clientSocket) {
            this.clientSocket = clientSocket;
        }

        @Override
        public void run() {
            //writeToHistory("Bağlanan client için thread oluşturuldu : " + this.getName());

            try {
                // input  : client'dan gelen mesajları okumak için
                // output : server'a bağlı olan client'a mesaj göndermek için
                clientInput = new ObjectInputStream(clientSocket.getInputStream());
                //  clientOutput = new ObjectOutputStream(clientSocket.getOutputStream());
                String name = ((String) clientInput.readObject().toString()); //ilk geleni(ismini aldık)
                turn = name;
                // Bütün client'lara yeni katılan client bilgisini gönderir
                for (ObjectOutputStream out : allClients) {
                    out.writeObject(name + " server'a katıldı.");
                }

                Object mesaj;
                int temp = 0;
                // client mesaj gönderdiği sürece mesajı al
                while ((mesaj = clientInput.readObject()) != null) {
                    // client'in gönderdiği mesajı server ekranına yaz

                    System.out.println(mesaj);
                    String[] mesajlar = ((String) mesaj).split(" ");
                    turn = mesajlar[0];

                    int dice = Integer.parseInt(mesajlar[1]);
                    temp = calculatePlayerValue(temp, dice);
                    System.out.println("turn : " + turn);
                    //writeToHistory(name + ": " + temp);
                    sendBroadcast(turn + " : " + temp);

                    if (mesaj.equals("100")) {
                        clientSocket.close();
                        break;
                    }
                }

            } catch (IOException | ClassNotFoundException ex) {
                System.out.println("Hata - ListenThread : " + ex);
            } finally {
                try {
                    // client'ların tutulduğu listeden çıkart
                    allClients.remove(clientOutput);

                    // bütün client'lara ayrılma mesajı gönder
                    for (ObjectOutputStream out : allClients) {
                        out.writeObject(turn + " server'dan ayrıldı.");
                    }

                    // bütün streamleri ve soketleri kapat
                    if (clientInput != null) {
                        clientInput.close();
                    }
                    if (clientOutput != null) {
                        clientOutput.close();
                    }
                    if (clientSocket != null) {
                        clientSocket.close();
                    }

                } catch (IOException ex) {
                    System.out.println("Hata - Soket kapatılamadı : " + ex);
                }
            }
        }
    }

    public int calculatePlayerValue(int player, int diceValue) throws IOException {

        if (WINPOINT != player) {
            player = player + diceValue;

            if (player > WINPOINT) {
                player = player - diceValue;
                return player;
            }

            if (null != snake.get(player)) {
                System.out.println("swallowed by snake");
                //writeToHistory("swallowed by snake");
                sendBroadcast(turn + " : swallowed by snake");
                player = snake.get(player);
            }

            if (null != ladder.get(player)) {
                System.out.println("climb up the ladder");
                //writeToHistory("climb up the ladder");
                sendBroadcast(turn + " : climb up the ladder");
                player = ladder.get(player);
            }
        } else {
            writeToHistory("wins");
            if (turn != null) {
                sendBroadcast(turn + " KAZANDI !!!!");
            }
            stop();
        }
        return player;
    }

}
