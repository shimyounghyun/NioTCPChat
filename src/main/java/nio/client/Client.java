package nio.client;

import com.google.gson.Gson;
import nio.model.User;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.SocketChannel;
import java.nio.channels.WritableByteChannel;
import java.nio.charset.Charset;
import java.util.Random;

public class Client {
    private static final String IP = "";
    public static void main(String[] args) {
        Thread systemIn;
        Gson gson = new Gson();
        // 서버 IP와 포트로 연결되는 소켓채널 생성
        try (SocketChannel socket = SocketChannel.open
                (new InetSocketAddress(IP, 8000))) {

            // 모니터 출력에 출력할 채널 생성
            WritableByteChannel out = Channels.newChannel(System.out);

            // 버퍼 생성
            ByteBuffer buf = ByteBuffer.allocate(1024);
            // 유저 아이디 랜덤 생성
            Random random = new Random();
            random.setSeed(System.currentTimeMillis());
            int idx = random.nextInt(100);
            // 출력을 담당할 스레드 생성 및 실행
            systemIn = new Thread(new SystemIn(socket, new User(0,idx,"","", 1)));
            systemIn.start();
            Charset charset = Charset.forName("UTF-8");

            while (true) {
                socket.read(buf); // 읽어서 버퍼에 넣고
                buf.flip();

                String data = charset.decode(buf).toString();
                User user = gson.fromJson(data, User.class);
                System.out.println(user.getMessage());
//                out.write(user.getMessage()); // 모니터에 출력
                buf.clear();
            }

        } catch (IOException e) {

            System.out.println("서버와 연결이 종료되었습니다.");
        }
    }
}

// 입력을 담당하는 클래스
class SystemIn implements Runnable {

    SocketChannel socket;
    Gson gson = new Gson();
    User user;

    // 연결된 소켓 채널과 모니터 출력용 채널을 생성자로 받음
    SystemIn(SocketChannel socket, User user) {
        this.socket = socket;
        this.user = user;
    }

    @Override
    public void run() {

        // 키보드 입력받을 채널과 저장할 버퍼 생성
        ReadableByteChannel in = Channels.newChannel(System.in);
        ByteBuffer buf = ByteBuffer.allocate(1024);
        Charset charset = Charset.forName("UTF-8");
        try {
            socket.write(charset.encode(gson.toJson(this.user)));
            this.user.setMessageType(2);
            while (true) {
                in.read(buf); // 읽어올때까지 블로킹되어 대기상태
                buf.flip();

                String message = charset.decode(buf).toString();
                this.user.setMessage(message);
//                User user = gson.fromJson(data, User.class);

//                System.out.println("유저 idx"+idx);
                String sendData = gson.toJson(this.user);
                socket.write(charset.encode(sendData)); // 입력한 내용을 서버로 출력
                buf.clear();
            }

        } catch (IOException e) {
            System.out.println("채팅 불가.");
        }
    }
}
