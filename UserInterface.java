import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.net.SocketException;
import java.util.ArrayList;

public class UserInterface extends JFrame implements ActionListener {
    private final JTextAreaWithTitle messages;
    private final JTextAreaWithTitle participants;
    private final JTextAreaWithTitle output;
    private JButton connect;
    private JButton disconnect;
    private JButton send;
    private Client client;
    private Boolean connected = false;

    private UserInterface() {
        messages = new JTextAreaWithTitle("Message Board", new Dimension(400, 200));
        participants = new JTextAreaWithTitle("Participants", new Dimension(200, 200));
        output = new JTextAreaWithTitle("Send message:", new Dimension(600, 50));
        output.setEditable(true);
        this.setTitle("Chat");
        this.setLayout(new BorderLayout());
        messages.setEditable(false);
        participants.setEditable(false);
        add(messages, BorderLayout.CENTER);
        add(participants, BorderLayout.WEST);
        add(new InterfacePanel(), BorderLayout.SOUTH);
        output.setEditable(true);
        this.pack();
        this.setResizable(true);
        this.setVisible(true);
        this.addWindowListener(new WindowAdapter() {
                                   @Override
                                   public void windowClosing(WindowEvent e) {
                                       if (client != null)
                                           client.stop();
                                       UserInterface.this.dispose();
                                       System.exit(0);
                                   }
                               }
        );
    }

    private class JTextAreaWithTitle extends JPanel {
        private final JTextArea textArea;

        public JTextAreaWithTitle(String title, Dimension dimension) {
            this.setLayout(new BorderLayout());
            this.add(new JLabel(title), BorderLayout.NORTH);
            JScrollPane scrollable = new JScrollPane(this.textArea = new JTextArea()); /*set note area as scrollable*/
            scrollable.setViewportView(this.textArea);
            scrollable.setPreferredSize(dimension);
            this.add(scrollable, BorderLayout.CENTER);
        }

        public void setEditable(Boolean bool) {
            this.textArea.setEditable(bool);
        }

        public String getText() {
            return this.textArea.getText();
        }

        public void setText(String str) {
            this.textArea.setText(str);
        }

        public void append(String str) {
            this.textArea.append(str);
        }
    }

    class ButtonPanel extends JPanel {

        /*panel constructor*/
        public ButtonPanel() {
            this.setLayout(new GridLayout(1, 3));
            JButton connect;
            JButton disconnect;
            JButton send;
            UserInterface.this.connect = connect = new JButton("Connect");
            UserInterface.this.disconnect = disconnect = new JButton("disconnect");
            UserInterface.this.send = send = new JButton("Send");
            this.setPreferredSize(new Dimension(600, 30));
            add(send);
            add(connect);
            add(disconnect);
            /*set as action listener.*/
            send.addActionListener(UserInterface.this);
            connect.addActionListener(UserInterface.this);
            disconnect.addActionListener(UserInterface.this);
        }
    }

    private class InterfacePanel extends JPanel {
        public InterfacePanel() {
            this.setLayout(new BorderLayout());
            this.add(output, BorderLayout.CENTER);
            this.add(new ButtonPanel(), BorderLayout.SOUTH);
        }
    }

    private static String getStringFromUser(String prompt, String initialText) {
        String input;
        while ((input = JOptionPane.showInputDialog(null, prompt, initialText)).length() == 0) {
            showPopup("required input must be longer then one character");
        }
        return input;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        Object obj = e.getSource();
        String port, serverIP;
        if (obj instanceof JButton) {
            if (obj.hashCode() == getConnectHash()) {
                if (!connected) {
                    if (((serverIP = getStringFromUser("enter server address", "localhost")) == null) ||
                            (port = getStringFromUser("enter server port", "7777")) == null)
                        return;
                    try {
                        client = new Client(serverIP, Integer.parseInt(port), this);
                    } catch (SocketException ex) {
                        showPopup("Server connection error");
                        updateMessageBoard("Server connection error!");
                        return;
                    } catch (IOException ex) {
                        ex.printStackTrace();
                    }
                    messages.setText("");
                    participants.setText("");
                    connected = true;
                    String name;
                    if ((name = getStringFromUser("enter desired user name", "")) != null) {
                        client.send(name);
                    }
                } else
                    showPopup("already connected to server");
            } else if (connected) {
                if (obj.hashCode() == getDisconnectHash()) {
                    showPopup("Disconnected from server.");
                    updateMessageBoard("Disconnected!");
                    client.stop();
                    connected = false;
                } else if (obj.hashCode() == getSendHash()) {
                    client.send(getMessage() + "\n");
                }
            } else
                showPopup("not connected to server");
        }
    }

    public static void showPopup(String str) {
        JOptionPane.showMessageDialog(null, str);

    }

    private int getConnectHash() {
        return connect.hashCode();
    }

    private int getDisconnectHash() {
        return disconnect.hashCode();
    }

    private int getSendHash() {
        return send.hashCode();
    }

    private String getMessage() {
        String msg = output.getText();
        output.setText("");
        return msg;
    }

    public void setParticipants(ArrayList<String> list) {
        participants.setText("");
        for (String i : list)
            participants.append(i + "\n");
    }

    public void updateMessageBoard(String str) {
        messages.append(str);
    }

    public void setConnectedStatus(boolean bool) {
        connected = bool;
    }

    public static void main(String[] args) {
        new UserInterface();
    }
}