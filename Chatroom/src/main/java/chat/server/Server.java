// Server.java
package chat.server;

import java.net.*;
import java.io.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

public class Server {
    private List<ClientHandler> clients = new ArrayList<>();

    public static void main(String[] args) {
        // 设置CMD运行的编码,终端乱码问题得以解决
        System.setProperty("file.encoding", "UTF-8");
        new Server().start();
    }

    // 服务端启动，监听客户端连接
    public void start() {
        try (ServerSocket serverSocket = new ServerSocket(5000)) {
            System.out.println("服务器已启动，端口为： 5000，IP地址为：" + InetAddress.getLocalHost().getHostAddress() + "(客户端请填写该IP地址或通过命令ipconfig查看本机IP地址)");
            while (true) {
                Socket socket = serverSocket.accept();
                ClientHandler clientHandler = new ClientHandler(socket);
                clients.add(clientHandler);
                clientHandler.start();
            }
        } catch (IOException ex) {
            System.out.println("错误: " + ex.getMessage());
        }
    }

    // 广播消息给所有客户端，排除发送者
    public void broadcast(String message, ClientHandler sender) {
        for (ClientHandler client : clients) {
            if (client != sender) {
                client.sendMessage(message);
            }
        }
    }

    // 广播在线用户列表
    public void broadcastOnlineUsers(String status, List<String> userList, ClientHandler sender) {
        for (ClientHandler client : clients) {
            client.sendMessage(status + "::" + userList.toString());
        }
    }

    // 移除客户端
    public void removeClient(ClientHandler clientHandler) {
        clients.remove(clientHandler);
    }

    // 客户端处理线程
    class ClientHandler extends Thread {
        private Socket socket;
        private String userName;
        private BufferedReader reader;
        private PrintWriter writer;

        // 获取用户名
        public String getUserName() {
            return userName;
        }

        // 设置用户名
        public void setUserName(String userName) {
            this.userName = userName;
        }

        @Override
        public String toString() {
            return "ClientHandler{" +
                    "socket=" + socket +
                    ", userName='" + userName + '\'' +
                    ", reader=" + reader +
                    ", writer=" + writer +
                    '}';
        }

        // 客户端处理
        public ClientHandler(Socket socket) {
            this.socket = socket;
        }

        // 运行
        public void run() {
            try {
                reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                writer = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()), true);
                // 用户登录
                userName = reader.readLine();
                writer.println("[" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm")) + "]  " + userName + " 加入聊天室!");
                writer.println("欢迎加入聊天室，当前在线人数：" + clients.size());
                broadcast(userName + " 加入聊天室!", this);
                List<String> list = clients.stream().map(l -> l.getUserName()).collect(Collectors.toList());
                broadcastOnlineUsers("JOIN", list, this);
                System.out.println("[" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm")) + "]  " + userName + " 加入聊天室!");

                // 开始读取客户端消息
                String message;
                while ((message = reader.readLine()) != null) {
                    if (message.startsWith("/private")) {
                        handlePrivateMessage(message);
                    } else {
                        broadcast("[" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm")) + "]  " + userName + ": " + message, this);
                        System.out.println("[" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm")) + "]  " + userName + ": " + message);
                    }
                }

            } catch (IOException ex) {
                System.out.println("Error: " + ex.getMessage());
            } finally {
                try {
                    socket.close();
                    removeClient(this);
                    broadcast("[" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm")) + "]  " + userName + " 已离开.当前聊天室在线人数：" + clients.size(), this);
                    List<String> list = clients.stream().map(l -> l.getUserName()).collect(Collectors.toList());
                    broadcastOnlineUsers("QUIT", list, this);
                    System.out.println(userName + " 离开聊天室!");
                } catch (IOException ex) {
                    System.out.println("错误: " + ex.getMessage());
                }
            }
        }

        // 发送消息
        public void sendMessage(String message) {
            writer.println(message);
        }

        // 处理私聊消息
        private void handlePrivateMessage(String message) {
            String[] parts = message.split(" ", 3);
            if (parts.length == 3) {
                String recipient = parts[1];
                String privateMessage = parts[2];
                for (ClientHandler client : clients) {
                    if (client.getUserName().equals(recipient)) {
                        client.sendMessage("[" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm")) + "]  [私聊] " + userName + ": " + privateMessage);
                        break; // 假设只有一个带有给定用户名的接收者
                    }
                }
            }
        }
    }
}
