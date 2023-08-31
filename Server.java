package com.javarush.task.task30.task3008;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketAddress;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

//основной класс сервера.
/*- Сервер создает серверное сокетное соединение.
- В цикле ожидает, когда какой-то клиент подключится к сокету.
- Создает новый поток обработчик Handler, в котором будет происходить обмен сообщениями с клиентом.
- Ожидает следующее соединение. */

public class Server {
    private static Map<String, Connection> connectionMap = new ConcurrentHashMap<>();

    public static void main(String[] args) {
        ConsoleHelper.writeMessage("Введите порт сервера:");
        int port = ConsoleHelper.readInt();

        try (ServerSocket serverSocket = new ServerSocket(port)) {
            ConsoleHelper.writeMessage("Чат сервер запущен.");
            while (true) {
                // Ожидаем входящее соединение и запускаем отдельный поток при его принятии
                Socket socket = serverSocket.accept();
                new Handler(socket).start();
            }
        } catch (Exception e) {
            ConsoleHelper.writeMessage("Произошла ошибка при запуске или работе сервера.");
        }
    }


    public static void sendBroadcastMessage(Message message) { //отправляем сообщения сразу всем клиентам
        for (Connection connection : connectionMap.values()) {
            try {
                connection.send(message);
            } catch (IOException e) {
                ConsoleHelper.writeMessage("Сообщение не отправлено" + connection.getRemoteSocketAddress());
            }
        }
    }

    private static class Handler extends Thread { //реализует протокол общения с клиентом
        private Socket socket;

        public Handler(Socket socket) {
            this.socket = socket;
        }

        @Override
        public void run() {
            String userName = null;
            try {
                ConsoleHelper.writeMessage("Установлено новое соединение с удаленным адресом: " + socket.getRemoteSocketAddress());
                Connection connection = new Connection(socket);
                userName = serverHandshake(connection); //получаем имя пользователя чтобы передавать его в другие методы
                sendBroadcastMessage(new Message(MessageType.USER_ADDED, userName)); //отправляем всем участникам уведомление что добавлен новый пользователь
                notifyUsers(connection, userName); //передаем список участников чата
                serverMainLoop(connection, userName); //запускаем бесконечный цикл обработки сообщений
            } catch (IOException | ClassNotFoundException e) {
                ConsoleHelper.writeMessage("Произошла ошибка при обмене данными с удаленным адресом");
            }
            if (userName != null) { //отправляем информацию о пользователе который покинул чат
                connectionMap.remove(userName);
                sendBroadcastMessage(new Message(MessageType.USER_REMOVED,userName));
            }
            ConsoleHelper.writeMessage("Соединение с удаленным адресом закрыто");

        }

        /*
        правильно ли я понимаю, что когда мы прописываем throws в методе1, то мы прокидываем его дальше, обозначая что текущий метод1 при
        получении исключения продолжает работу, отсылая его в метод2, который его вызывает, где есть try catch для отлавливания
        исключений из вызываемого метода и обработки их в кэтч
         */
        private String serverHandshake(Connection connection) throws IOException, ClassNotFoundException { //этап "рукопожатия", знакомство клиента с сервером
            //принимает соединение connection, возвращает имя нового клиента
            while (true) {
                connection.send(new Message(MessageType.NAME_REQUEST, "Введите имя: ")); //запрос имени клиента
                Message message = connection.receive(); //получаем ответ
                if (message.getType() == MessageType.USER_NAME) { //проверяем совпадает ли с именем пользователя
                    if (!message.getData().isEmpty() && !connectionMap.containsKey(message.getData())) { //проверка что не пустое имя и такой пользователь не подключен
                        connectionMap.put(message.getData(), connection); //добавляем имя пользоватея и соединение
                        connection.send(new Message(MessageType.NAME_ACCEPTED, " имя принято")); //информируем что имя принято
                        return message.getData();
                    }

                }
            }
        }

        private void notifyUsers(Connection connection, String userName) throws IOException { //отправка новому пользователю информации об остальных участниках чата
            //connection - соединение с участником кому шлем информацию, userName - его имя
            for (Map.Entry<String, Connection> entry : connectionMap.entrySet()) {
                if (entry.getKey() != userName) { //если имя не совпадает с переданным параметром
                    connection.send(new Message(MessageType.USER_ADDED, entry.getKey())); //отправляем информацию об уже добавленных пользователях
                }
            }
        }

        private void serverMainLoop(Connection connection, String userName) throws IOException, ClassNotFoundException {
            //цикл обработки сообщений сервером
            while (true) {
                Message message = connection.receive(); //принимаем сообщение клиента
                if (message.getType() == MessageType.TEXT) { //если принятое сообщение соответствует типу TEXT, формируем новое сообщение
                    String data = message.getData();
                    sendBroadcastMessage(new Message(MessageType.TEXT, userName + ": " + data)); //отправляем сообщение всем клиентам
                } else { //если сообщение не типа TEXT
                    ConsoleHelper.writeMessage(String.format("Ошибка! Сообщение не является текстом. %s, проверь вводимый текст.", userName));
                }
            }
        }
    }
}

