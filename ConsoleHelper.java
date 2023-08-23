package com.javarush.task.task30.task3008;

//вспомогательный класс, для чтения или записи в консоль.

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

//вся работа должна проходить через этот класс
public class ConsoleHelper {
    private static BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));

    public static void writeMessage(String message) { //выводим message в консоль
        System.out.println(message);
    }

    public static String readString() { //считываем строку с консоли
        try {
            return reader.readLine();
        } catch (IOException e) {
            System.out.println("Произошла ошибка при попытке ввода текста. Попробуйте еще раз.");
            return readString();
        }

    }

    public static int readInt() { //считываем число из консоли
        try {
            return Integer.parseInt(readString());
        } catch (NumberFormatException e) {
            System.out.println("Произошла ошибка при попытке ввода числа. Попробуйте еще раз.");
            return readInt();
        }
    }

}
