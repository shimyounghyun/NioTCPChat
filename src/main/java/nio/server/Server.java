package nio.server;

import com.google.gson.Gson;
import nio.model.User;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.Vector;

public class Server {
    Selector selector;
    ServerSocketChannel serverSocketChannel;
    List<Client> connections = new Vector<Client>();
    Gson gson = new Gson();

    public static void main(String[] args){
        Server server = new Server();
        server.start();
    }

    public void start(){

        try {
            selector = Selector.open();
            serverSocketChannel = ServerSocketChannel.open();
            serverSocketChannel.configureBlocking(false);
            serverSocketChannel.bind(new InetSocketAddress(8000));
            serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);
        } catch (IOException e) {
            e.printStackTrace();
        }

        Thread thread = new Thread() {
            @Override
            public void run() {
                super.run();
                while (true) {
                    try {
                        int keyCount = selector.select();

                        if (keyCount == 0){
                            continue;
                        }
                        Set<SelectionKey> selectedKeys = selector.selectedKeys();
                        Iterator<SelectionKey> iterator = selectedKeys.iterator();
                        while (iterator.hasNext()){
                            SelectionKey selectionKey = iterator.next();

                            if (selectionKey.isAcceptable()) {
                                accept(selectionKey);
                            }else if (selectionKey.isReadable()) {
                                Client client = (Client) selectionKey.attachment();
                                client.receive(selectionKey);
                            }else if (selectionKey.isWritable()) {
                                Client client = (Client) selectionKey.attachment();
                                client.send(selectionKey);
                            }
                            iterator.remove();
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                        if (serverSocketChannel.isOpen()) {
                            stopServer();
                        }
                        break;
                    }
                }
            }
        };
        thread.start();
    }

    public void stopServer() {
        Iterator<Client> iterator = connections.iterator();

        try {
            while (iterator.hasNext()) {
                Client client = iterator.next();
                client.socketChannel.close();
                iterator.remove();
            }
            if (serverSocketChannel != null && serverSocketChannel.isOpen()) {
                serverSocketChannel.close();
            }

            if (selector != null && selector.isOpen()) {
                selector.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void accept(SelectionKey selectionKey) {
        ServerSocketChannel serverSocketChannel = (ServerSocketChannel) selectionKey.channel();
        try {
            SocketChannel socketChannel = serverSocketChannel.accept();

            String msg = "연결 수락 : " +socketChannel.getRemoteAddress() + ": "+Thread.currentThread().getName();

            Client client = new Client(socketChannel);
            connections.add(client);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    class Client {

        SocketChannel socketChannel;
        String sendData;
        User user;

        public Client (SocketChannel socketChannel) throws IOException{
            this.socketChannel = socketChannel;
            this.socketChannel.configureBlocking(false);
            SelectionKey selectionKey = socketChannel.register(selector, SelectionKey.OP_READ);
            selectionKey.attach(this);
        }

        public void receive(SelectionKey selectionKey) {
            ByteBuffer byteBuffer = ByteBuffer.allocate(1024);

            try {
                int byteCount = socketChannel.read(byteBuffer);
                if (byteCount == -1) {
                    throw new IOException();
                }
                String msg = "요청 처리:"+socketChannel.getRemoteAddress()+" : "+Thread.currentThread().getName();
                byteBuffer.flip();
                Charset charset = Charset.forName("UTF-8");
                String data = charset.decode(byteBuffer).toString();
                User user = gson.fromJson(data, User.class);
                this.user = user;
                if (this.user.getMessageType() == 2) {
                    for (Client client : connections) {
                        if (this.user.getIdx() == client.user.getIdx()) continue;
                        client.user.setMessage(user.getMessage() + " | " + user.getIdx() + " " + user.getRoom());
                        SelectionKey key = client.socketChannel.keyFor(selector);
                        key.interestOps(SelectionKey.OP_WRITE);
                    }
                }
                selector.wakeup();
            } catch (IOException e) {
                e.printStackTrace();
                try {
                    socketChannel.close();
                } catch (IOException ioException) {
                    ioException.printStackTrace();
                }
            }
        }

        public void send(SelectionKey selectionKey) {
            Charset charset = Charset.forName("UTF-8");
            ByteBuffer byteBuffer = charset.encode(gson.toJson(this.user));

            try {
                socketChannel.write(byteBuffer);
                selectionKey.interestOps(SelectionKey.OP_READ);
                selector.wakeup();
            } catch (IOException e) {
                e.printStackTrace();
                String msg = "클라이언트 통신 안됨";
                connections.remove(this);
                try {
                    socketChannel.close();
                } catch (IOException ioException) {
                    ioException.printStackTrace();
                }
            }
        }
    }
}
