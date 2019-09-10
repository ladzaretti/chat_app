import java.awt.Dimension;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.*;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.concurrent.LinkedBlockingQueue;

import javax.swing.JFrame;

/*the following class implements a chat server.
 * the server has three sub threads:
 *      1)  handles new clients
 *      2)  sends messages to clients
 *      3)  receives new messages from clients, each client has its own reading thread.
 *          each new msg enqueues in msgs ArrayList
 *   */
public class ChatServer {
    private final LinkedList<ClientHandler> clients; /* list containing the active clients */
    private final LinkedBlockingQueue<Object> msgs; /*
     * queue containing the incoming msgs, thread safe, blocks when
     * empty
     */
    private ServerSocket srv; /* server socket */
    private final ArrayList<String> participants; /* list containing the clients names */

    private ChatServer(int port) {
        clients = new LinkedList<>();
        msgs = new LinkedBlockingQueue<>();
        participants = new ArrayList<>();
        try {
            srv = new ServerSocket(port); /* open server on given port */
            new ServerUI(srv);

        } catch (IOException e) {
            System.out.println("server error.");
            System.exit(1);
        }

        /*
         * client accepting daemon thread, will wait for new clients. adds new clients
         * to the client list, when client accepted
         */
        new Thread(() -> {
            while (true)
                try {
                    clients.add(new ClientHandler(srv.accept()));
                } catch (IOException e) {
                    e.printStackTrace();
                }
        }).start();

        /* sending thread, dequeue and sends messages to all clients */
        Thread sendingDaemon = new Thread(() -> {
            while (true)
                try {
                    ChatServer.this.sendAll(msgs.take());
                } catch (InterruptedException ignored) {
                }
        });
        sendingDaemon.setDaemon(true);
        sendingDaemon.start();
    }

    private class ServerUI extends JFrame {
        public ServerUI(ServerSocket srv) throws UnknownHostException {
            super("Server is running at " + InetAddress.getLocalHost() + ":" + srv.getLocalPort());
            this.setVisible(true);
            this.setSize(new Dimension(500, 55));
            this.setResizable(false);
            this.addWindowListener(new WindowAdapter() {
                @Override
                public void windowClosing(WindowEvent e) {
                    while (!clients.isEmpty()) {
                        ClientHandler client = clients.pop();
                        System.out.println("closing");
                        client.close();
                    }
                    System.exit(0);
                }
            });
        }
    }

    /* the following class handles individual client */
    private class ClientHandler {
        private final Socket socket;
        private ObjectOutputStream out;
        private ObjectInputStream in;
        private String username; /* username as passed from client */
        private boolean hasUserName = false; /* username flag */
        private volatile Boolean run = true; /* reader running flag, false when client closes */

        public ClientHandler(Socket socket) throws IOException {
            this.socket = socket;
            out = new ObjectOutputStream(socket.getOutputStream()); /* open output stream */
            in = new ObjectInputStream(socket.getInputStream()); /* open input stream */
            /* reading daemon */
            Thread readDaemon = new Thread(() -> {
                Object input;
                try {
                    while (run) {
                        input = in.readObject();/* read object from client */
                        if (!hasUserName) {/* if client has no name, ie new client */
                            setUsername((String) input);
                            continue;
                        }
                        msgs.put(username + ": " + input); /* enqueue msg */
                    }
                } catch (ClassNotFoundException e) {
                    e.printStackTrace();
                } catch (EOFException e) {
                    this.close();
                } catch (IOException | InterruptedException ignored) {
                }
            });
            readDaemon.setDaemon(true);
            readDaemon.start();

        }

        /* reader thread closing procedure */
        private void close() {
            try {
                /* close streams */
                clients.remove(this);/* remove from clients list */
                in.close();
                out.close();
                socket.close(); /* close socket */
                run = false; /* set running flag false */
                participants.remove(username); /* remove username from participants */
                sendAll(username + " disconnected\n"); /* notify all clients */
                System.out.println("client removed");
                sendAll(participants); /* send new participants list to all users */
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        /* send object to client */
        public void sendObject(Object obj) throws IOException {
            if (!(obj instanceof String))
                out.reset();
            out.writeObject(obj);
        }

        /* set username */
        private void setUsername(String username) {
            this.username = username;
            hasUserName = true;
            participants.add(username);
            sendAll(username + " connected\n");
            System.out.println("client added");
            sendAll(participants);
        }
    }

    /* iterate over clients list and send obj */
    private void sendAll(Object obj) {
        for (ClientHandler i : clients) {
            try {
                i.sendObject(obj);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /* create new server, command line arguments are server name and port */
    public static void main(String[] args) {
        new ChatServer(Integer.parseInt(args[0]));
    }
}