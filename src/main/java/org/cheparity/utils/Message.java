package utils;

public final class Message<T> {
    public final String request;
    public T data;

    public Message(T data, String request) {
        this.data = data;
        this.request = request;
    }


    public boolean isEmpty() {
        return data == null;
    }

    public T get() {
        return data;
    }

}
