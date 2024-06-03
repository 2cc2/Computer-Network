// Client.java
package chat.client;

import javax.swing.*;
import javax.swing.text.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.Socket;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class Client extends JFrame {
    private JTextArea messageArea;
    private JTextField inputField;
    private JButton sendButton;
    private String userName;
    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;
    private DefaultListModel<String> userListModel;
    private JList<String> userList;
    private JScrollPane userListScrollPane;
    private JScrollPane scrollPane;

    // 客户端构造函数
    public Client(String title) {
        super(title);
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());
        initUI();
        inputField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER && sendButton.isEnabled()) {
                    sendMessage();
                }
            }
        });
        sendButton.addActionListener(e -> sendMessage());
        String serverAddress = JOptionPane.showInputDialog(this, "请输入服务器地址：", "127.0.0.1");
        String userName = JOptionPane.showInputDialog(this, "请输入Username：", "User" + (int) (Math.random() * 1000));
        connect(serverAddress, userName);
    }

    // 初始化UI界面
    private void initUI() {
        messageArea = new JTextArea();
        messageArea.setEditable(false);

        JPanel chatPanel = new JPanel(new BorderLayout());
        chatPanel.add(messageArea, BorderLayout.CENTER);
        chatPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        scrollPane = new JScrollPane(chatPanel, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);

        add(scrollPane, BorderLayout.CENTER);

        SimpleAttributeSet selfStyle = new SimpleAttributeSet();
        StyleConstants.setForeground(selfStyle, Color.BLUE);
        StyleConstants.setBold(selfStyle, true);

        JPanel bottomPanel = new JPanel();
        bottomPanel.setLayout(new BorderLayout());
        inputField = new JTextField();
        sendButton = new JButton("发送<<<");
        sendButton.setEnabled(false);
        bottomPanel.add(inputField, BorderLayout.CENTER);
        bottomPanel.add(sendButton, BorderLayout.EAST);
        add(bottomPanel, BorderLayout.SOUTH);

        userListModel = new DefaultListModel<>();
        userList = new JList<>(userListModel);
        userListScrollPane = new JScrollPane(userList);
        add(createUserPanel(), BorderLayout.EAST);
    }

    // 创建用户面板
    private JPanel createUserPanel() {
        JPanel userPanel = new JPanel(new BorderLayout());
        JLabel userListTitleLabel = new JLabel("Client-List");
        JPanel userListPanel = new JPanel(new BorderLayout());
        userListPanel.add(userListTitleLabel, BorderLayout.NORTH);
        userListPanel.add(userListScrollPane, BorderLayout.CENTER);
        userPanel.add(userListPanel, BorderLayout.CENTER);
        return userPanel;
    }

    // 连接服务器
    private void connect(String serverAddress, String userName) {
        try {
            socket = new Socket(serverAddress, 5000);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()), true);
            out.println(userName);
            this.userName = userName;
            setTitle("Chatroom- " + userName);
            sendButton.setEnabled(true);
            inputField.requestFocus();
            new Thread(this::receiveMessage).start();
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "连接服务器失败！");
            System.exit(1);
        }
    }

    // 发送消息
    private void sendMessage() {
        String message = inputField.getText().trim();
        if (!message.isEmpty()) {
            if (message.startsWith("/private")) {
                // 格式化私聊消息并发送给服务器
                out.println(message);
                // 在消息区域显示私聊消息
                messageArea.append("[" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm")) + "] " + userName + ": " + message.substring(message.indexOf("::") + 2) + "\n");
            } else {
                // 发送群聊消息
                out.println(message);
                // 在消息区域显示群聊消息
                messageArea.append("[" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm")) + "] " + userName + ": " + message + "\n");
            }
            inputField.setText(""); // 清空群聊消息的输入栏
            inputField.requestFocus();
            JScrollBar verticalScrollBar = scrollPane.getVerticalScrollBar();
            verticalScrollBar.setValue(verticalScrollBar.getMaximum());
        }
    }

    // 接收消息
    private void receiveMessage() {
        try {
            while (true) {
                String message = in.readLine();
                if (message.startsWith("JOIN::")) {
                    // 当用户加入或离开时更新用户列表
                    updateUserList(message);
                } else if (message.startsWith("PRIVATE::")) {
                    // 处理私聊消息
                    handlePrivateMessage(message);
                }else if (message.startsWith("QUIT::")) {
                    userListModel.clear();
                    message = message.replace("QUIT::", "");
                    ArrayList<String> arrayList = new ArrayList<>(Arrays.asList(message.substring(1, message.length() - 1).split(", ")));
                    for (String user : arrayList) {
                        userListModel.addElement(user);
                    }
                }else {
                    // 在消息区域显示群聊消息
                    messageArea.append(message + "\n");
                    JScrollBar verticalScrollBar = scrollPane.getVerticalScrollBar();
                    verticalScrollBar.setValue(verticalScrollBar.getMaximum());
                }
            }
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "与服务器连接断开！");
            System.exit(1);
        }
    }

    // 更新用户列表
    private void updateUserList(String message) {
        userListModel.clear();
        message = message.replace("JOIN::", "");
        ArrayList<String> arrayList = new ArrayList<>(Arrays.asList(message.substring(1, message.length() - 1).split(", ")));
        for (String user : arrayList) {
            userListModel.addElement(userName.equals(user) ? "*"+user: user);
        }
    }

    // 处理私聊消息
    private void handlePrivateMessage(String message) {
        // 在消息区域显示私聊消息
        messageArea.append(message.substring(message.indexOf("::") + 2) + "\n");
        JScrollBar verticalScrollBar = scrollPane.getVerticalScrollBar();
        verticalScrollBar.setValue(verticalScrollBar.getMaximum());
    }

    // 主函数
    public static void main(String[] args) {
        // 设置CMD运行的编码,终端乱码问题得以解决
        System.setProperty("file.encoding", "UTF-8");
        Client client = new Client("ChatRoom");
        client.setSize(600, 400);
        client.setLocationRelativeTo(null);
        client.setVisible(true);
    }
}
