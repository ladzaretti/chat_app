import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.concurrent.LinkedBlockingQueue;

public class Client {
    private final LinkedBlockingQueue<String> msgs;   /*queue containing the incoming msgs*/
    private ServerConnection srv;
    private Socket socket;
    private volatile Boolean run = true;
    private Thread messageHandler;
    private Thread reader;
    private final UserInterface clientUI;

    public Client(String ip, int port, UserInterface UI) throws IOException {
        msgs = new LinkedBlockingQueue<>();
        clientUI = UI;
        srv = new ServerConnection(socket = new Socket(ip, port));
        socket.setSoTimeout(1000);
        messageHandler = new Thread(() -> {
            while (run) {
                try {
                    String message = msgs.take();
                    clientUI.updateMessageBoard(message);
                } catch (InterruptedException e) {
                    run = false;
                }
            }
        });
        messageHandler.setDaemon(true);
        messageHandler.start();
    }

    private class ServerConnection {
        private ObjectOutputStream out;
        private ObjectInputStream in;

        ServerConnection(Socket socket) throws IOException {
            out = new ObjectOutputStream(socket.getOutputStream());
            in = new ObjectInputStream(socket.getInputStream());
            reader = new Thread(() -> {
                while (run)
                    try {
                        Object input;
                        if ((input = in.readObject()) instanceof String)
                            msgs.put((String) input);
                        else if (((ArrayList<?>) input).get(0) instanceof String)
                            clientUI.setParticipants((ArrayList<String>) input);
                    } catch (SocketException | EOFException e) {
                        run = false;
                        UserInterface.showPopup("connection lost");
                        clientUI.updateMessageBoard("Connection lost");
                        clientUI.setConnectedStatus(false);
                    } catch (SocketTimeoutException | ClassNotFoundException ignored) {
                    } catch (IOException e) {
                        e.printStackTrace();
                    } catch (InterruptedException e) {
                        run = false;
                        System.out.println("reader stopped");
                    }
            });
            reader.setDaemon(true);
            reader.start();
        }

        public void println(String str) {
            try {
                out.writeObject(str);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        public void close() throws IOException {
            out.close();
            in.close();
            socket.close();
            System.out.println("input/output streams closed");
        }
    }

    public void send(String str) {
        srv.println(str);
    }

    public void stop() {
        if (run) {
            run = false;
            messageHandler.interrupt();
            reader.interrupt();
            try {
                reader.join();
                messageHandler.join();
                srv.close();
            } catch (InterruptedException | IOException e) {
                e.printStackTrace();
            }
        }
    }
}