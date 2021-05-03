package aglar_proje_v1;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import javax.swing.JOptionPane;
import javax.swing.JTextPane;

public class TCP_Client {

    private Socket clientSocket;
    private ObjectInputStream clientInput;
    private ObjectOutputStream clientOutput;
    private Thread clientThread;
    private JTextPane historyJTextPane;
    private javax.swing.JButton buton_zar;

    protected void start(String host, int port, javax.swing.JTextPane jTextPaneHistory, javax.swing.JButton buton_zar) throws IOException {
        // client soketi oluşturma (ip + port numarası)
        clientSocket = new Socket(host, port);
        this.historyJTextPane = jTextPaneHistory;
        this.buton_zar = buton_zar;
        // client arayüzündeki history alanı, bütün olaylar buraya yazılacak

        // client arayüzündeki isim yazısı, client ismi server tarafından belirlenecek
        // input  : client'a gelen mesajları okumak için
        // output : client'dan bağlı olduğu server'a mesaj göndermek için
        clientOutput = new ObjectOutputStream(clientSocket.getOutputStream());
        clientInput = new ObjectInputStream(clientSocket.getInputStream());

        // server'ı sürekli dinlemek için Thread oluştur
        clientThread = new ListenThread();
        clientThread.start();
    }

    protected void writeToHistory(String message) {
        // server arayüzündeki history alanına mesajı yaz
        historyJTextPane.setText(historyJTextPane.getText() + "\n" + message);
    }

    protected void sendMessage(String message) throws IOException {
        // gelen mesajı server'a gönder
        try {
            if (Integer.parseInt(message) == 100) {
                disconnect();
            }
        } catch (NumberFormatException e) {

        }

        clientOutput.writeObject(message);
    }

    protected void sendObject(String message) throws IOException {
        // gelen nesneyi server'a gönder
        clientOutput.writeObject(message);
    }

    protected void disconnect() throws IOException {
        // bütün streamleri ve soketleri kapat
        if (clientInput != null) {
            clientInput.close();
        }
        if (clientOutput != null) {
            clientOutput.close();
        }
        if (clientThread != null) {
            clientThread.interrupt();
        }
        if (clientSocket != null) {
            clientSocket.close();
        }
    }

    class ListenThread extends Thread {

        // server'dan gelen mesajları dinle
        @Override
        public void run() {
            try {
                Object mesaj;
                // server mesaj gönderdiği sürece gelen mesajı al
                String name = (String) clientInput.readObject();
                while ((mesaj = clientInput.readObject()) != null) {

                    // serverdan gelen mesajı arayüze yaz
                    System.out.println(mesaj);
                    writeToHistory("" + mesaj);
                    String[] mesajlar = ((String) mesaj).split(" ");
                    String turn = mesajlar[0];

                    if (name.contains(turn)) {
                        buton_zar.setEnabled(false);
                    } else {
                        buton_zar.setEnabled(true);
                    }
                    if (((String) mesaj).contains("KAZANDI")) {
                        buton_zar.setEnabled(false);
                        return;
                    }
                    // 100 mesajı iletişimi sonlandırır
                    if (mesaj.equals("100")) {
                        break;
                    }
                }
            } catch (IOException | ClassNotFoundException ex) {
                System.out.println("Error - ListenThread : " + ex);
            }
        }
    }

}
