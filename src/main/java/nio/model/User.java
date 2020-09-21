package nio.model;

public class User {
    int room;
    int idx;
    String message;
    int messageType;
    String img;

    public User() {}

    public User(int room, int idx, String message, String img, int messageType) {
        this.room = room;
        this.idx = idx;
        this.message = message;
        this.img = img;
        this.messageType = messageType;
    }

    public int getRoom() {
        return room;
    }

    public void setRoom(int room) {
        this.room = room;
    }

    public int getIdx() {
        return idx;
    }

    public void setIdx(int idx) {
        this.idx = idx;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getImg() {
        return img;
    }

    public void setImg(String img) {
        this.img = img;
    }

    public int getMessageType() {
        return messageType;
    }

    public void setMessageType(int messageType) {
        this.messageType = messageType;
    }
}
