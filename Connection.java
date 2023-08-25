package com.javarush.task.task30.task3008;

import java.io.Closeable;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.net.SocketAddress;

//класс соединения между клиентом и сервером.
// выполняет роль обертки над классом java.net.Socket,
//которая должна будет уметь сериализовать и десериализовать объекты типа Message в сокет.
public class Connection implements Closeable {
    private final Socket socket;
    private final ObjectOutputStream out;
    private final ObjectInputStream in;

    public Connection(Socket socket) throws IOException {
        this.socket = socket;
        this.out = new ObjectOutputStream(socket.getOutputStream());
        this.in = new ObjectInputStream(socket.getInputStream());
    }

    public void send(Message message) throws IOException { //записываем (сериализуем) сообщение message в outStream
        synchronized (out) {
            out.writeObject(message);

        }
    }

    public Message receive() throws IOException, ClassNotFoundException { //читаем (десериализуем) данные
        synchronized (in) {
            return (Message) in.readObject();
        }
    }

    public SocketAddress getRemoteSocketAddress() { //возвращает удаленный адрес сокетного соединения
        return socket.getRemoteSocketAddress();
    }

    @Override
    public void close() throws IOException { // закрываем все ресурсы класса
        out.close();
        in.close();
        socket.close();
    }
}
