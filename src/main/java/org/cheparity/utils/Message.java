package utils;

public final class Message<T> {
    public T data;

    private Message(T data) {
        this.data = data;
    }

    public static <T> Message<T> of(T data) {
        return new Message<>(data);
    }

    public static <T> Message<T> empty() {
        return new Message<>(null);
    }

    public boolean isEmpty() {
        return data == null;
    }

    public T get() {
        return data;
    }

    public void set(T data) {
        this.data = data;
    }
}
