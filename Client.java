package com.javarush.task.task30.task3008.client;

import com.javarush.task.task30.task3008.Connection;
import com.javarush.task.task30.task3008.ConsoleHelper;
import com.javarush.task.task30.task3008.Message;
import com.javarush.task.task30.task3008.MessageType;

import java.io.IOException;
import java.net.Socket;

/*Клиент, в начале своей работы, должен запросить у пользователя адрес и порт сервера, подсоединиться к указанному адресу, получить запрос имени от сервера, спросить имя у пользователя, отправить имя пользователя серверу, дождаться принятия имени сервером.
 После этого клиент может обмениваться текстовыми сообщениями с сервером.
 Обмен сообщениями будет происходить в двух параллельно работающих потоках.
 Один будет заниматься чтением из консоли и отправкой прочитанного серверу, а второй поток будет получать данные от сервера и выводить их в консоль.
*/
public class Client {
    public static void main(String[] args) throws IOException, ClassNotFoundException {
        Client client = new Client();
        client.run();
    }

    protected Connection connection;
    private volatile boolean clientConnected = false; //переход в true, если клиент подсоединен к серверу

    protected String getServerAddress() {
        // ввод адреса сервера у пользователя и вернуть введенное значение.
        ConsoleHelper.writeMessage("Введите адрес сервера:");
        return ConsoleHelper.readString();
    }

    protected int getServerPort() {
        // ввод порта сервера и возвращать его.
        ConsoleHelper.writeMessage("Введите порт сервера:");
        return ConsoleHelper.readInt();
    }

    protected String getUserName() {
        //должен запрашивать и возвращать имя пользователя.
        ConsoleHelper.writeMessage("Введите ваше имя:");
        return ConsoleHelper.readString();
    }

    protected boolean shouldSendTextFromConsole() {
        //в данной реализации клиента всегда должен возвращать true (мы всегда отправляем текст введенный в консоль).
        //Этот метод может быть переопределен, если мы будем писать какой-нибудь другой клиент, унаследованный от нашего,
        // который не должен отправлять введенный в консоль текст.
        return true;
    }

    protected SocketThread getSocketThread() {
        return new SocketThread();
    }

    protected void sendTextMessage(String text) {
        //создает новое текстовое сообщение, используя переданный текст
        // и отправляет его серверу через соединение connection.
        try {
            connection.send(new Message(MessageType.TEXT, text));
        } catch (IOException e) {
            ConsoleHelper.writeMessage("при отправке сообщения возникла ошибка");
            clientConnected = false;
        }
    }

    public void run() throws IOException, ClassNotFoundException {
        SocketThread socketThread = getSocketThread();
        socketThread.setDaemon(true);
        socketThread.start();
        synchronized (this) {
            try {
                this.wait(); //ждем пока не получим нотификацию из другого потока
            } catch (InterruptedException e) {
                ConsoleHelper.writeMessage("Ошибка при соединении с сервером");
                return;
            }
        }
        if (clientConnected) {
            ConsoleHelper.writeMessage("Соединение установлено. Для выхода наберите команду 'exit'.");
        } else {
            ConsoleHelper.writeMessage("Произошла ошибка во время работы клиента");
        }
        while (clientConnected) {
            String text = ConsoleHelper.readString();
            if (text.equals("exit")) { //если дана команда на выход, прерываем цикл
                clientConnected = false;
                break;
            }
            if (shouldSendTextFromConsole()) {
                sendTextMessage(text); //отправляем полученный текст
            }
        }
    }

    public class SocketThread extends Thread {
        //класс отвечает за поток, который устанавливает сокетное соединение и читает сообщения сервера

        @Override
        public void run() {
            try {
                //создаем соединение с сервером
                connection = new Connection(new Socket(getServerAddress(), getServerPort()));

                clientHandshake();
                clientMainLoop();
            } catch (IOException | ClassNotFoundException e) {
                notifyConnectionStatusChanged(false);
            }
        }

        protected void processIncomingMessage(String message) {
            //выводит текст message в консоль
            ConsoleHelper.writeMessage(message);
        }

        protected void informAboutAddingNewUser(String userName) {
            //выводит информацию, что участник с userName присоединился к чату
            ConsoleHelper.writeMessage("Участник с именем " + userName + " присоединился к чату");
        }

        protected void informAboutDeletingNewUser(String userName) {
            //выводит информацию, что участник с userName покинул чат
            ConsoleHelper.writeMessage("Участник с именем " + userName + " покинул чат");
        }

        protected void notifyConnectionStatusChanged(boolean clientConnected) {
            Client.this.clientConnected = clientConnected; //устанавливем значение поля внешнего объекта
            synchronized (Client.this) {
                Client.this.notify(); //оповещаем (пробуждаем) основной ожидающий поток в классе Client
            }
        }

        protected void clientHandshake() throws IOException, ClassNotFoundException {
            //метод "представляет" клиента серверу
            while (true) {
                Message message = connection.receive(); //получаем сообщение
                if (message.getType() == MessageType.NAME_REQUEST) { //если сервер запросил имя
                    String userName = getUserName(); //получаем имя
                    connection.send(new Message(MessageType.USER_NAME, userName)); //отправляем сообщение серверу об имени
                } else if (message.getType() == MessageType.NAME_ACCEPTED) { //если сервер указал что имя принято
                    notifyConnectionStatusChanged(true); //сообщаем главному потоку о готовности начать работу
                    return;

                } else { //если сообщение не двух объявленных типов
                    throw new IOException("Unexpected MessageType");
                }
            }
        }

        protected void clientMainLoop() throws IOException, ClassNotFoundException {
            //реализация главного цикла обработки сообщений сервера
            while (true) {
                //используем соответствующие методы
                Message message = connection.receive();
                if (message.getType() == MessageType.TEXT) { //если сообщение текст
                    processIncomingMessage(message.getData());
                } else if (message.getType() == MessageType.USER_ADDED) { //если информация о добавлении пользователя
                    informAboutAddingNewUser(message.getData());
                } else if (message.getType() == MessageType.USER_REMOVED) { // если информация об удалении пользователя
                    informAboutDeletingNewUser(message.getData());
                } else {
                    throw new IOException("Unexpected MessageType");
                }
            }
        }


    }

}
