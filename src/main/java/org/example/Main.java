package org.example;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.logging.Logger;
import java.util.logging.Level;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import com.fastcgi.*;

public class Main {
    private static final String BASE_RESPONSE = """
            Access-Control-Allow-Origin: *
            Connection: keep-alive
            Content-Type: application/json
            Content-Length: %d
                        
            %s
            """;

    private static final Logger logger = Logger.getLogger(Main.class.getName());

    public static void main(String[] args) {
        logger.setLevel(Level.ALL);

        logger.info("Сервер запущен и ожидает подключений");

        FCGIInterface fcgi = new FCGIInterface();

        while (fcgi.FCGIaccept() >= 0) {
            long startTime = System.nanoTime();
            logger.info("Принято новое соединение");

            HashMap<String, String> params = parseRequestBody();

            logger.info("Получены параметры: " + params.toString());

            Checker check = new Checker(params);
            String response;

            if (check.validate()) {
                long endTime = System.nanoTime();
                long elapsedTime = (endTime - startTime)/1000;
                response = createJson(String.format("{\"result\": %b, \"time\": %d}", check.isHit(), elapsedTime));

                logger.info("Проверка завершена успешно. Результат: " + check.isHit() +
                        ", время выполнения: " + elapsedTime + " мкс");
            } else {
                response = createJson("{\"error\": \" incorrect data\"}");

                logger.warning("Ошибка валидации данных: " + params.toString());
            }

            System.out.println(response);
            logger.info("Ответ отправлен клиенту");
        }
    }

    private static HashMap<String, String> parseRequestBody() {
        HashMap<String, String> params = new HashMap<>();

        try {
            String contentLengthStr = FCGIInterface.request.params.getProperty("CONTENT_LENGTH");
            logger.fine("CONTENT_LENGTH: " + contentLengthStr);

            if (contentLengthStr == null || contentLengthStr.isEmpty()) {
                logger.warning("Заголовок CONTENT_LENGTH отсутствует или пуст");
                return params;
            }

            int contentLength = Integer.parseInt(contentLengthStr);
            logger.fine("Длина тела запроса: " + contentLength + " байт");

            if (contentLength <= 0) {
                logger.warning("Тело запроса пустое");
                return params;
            }

            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(System.in, StandardCharsets.UTF_8));
            char[] buffer = new char[contentLength];
            reader.read(buffer, 0, contentLength);
            String requestBody = new String(buffer);

            logger.fine("Тело запроса: " + requestBody);

            // Парсим JSON
            requestBody = requestBody.trim();
            if (requestBody.startsWith("{") && requestBody.endsWith("}")) {
                requestBody = requestBody.substring(1, requestBody.length() - 1);
                String[] pairs = requestBody.split(",");

                for (String pair : pairs) {
                    String[] keyValue = pair.split(":", 2);
                    if (keyValue.length == 2) {
                        String key = keyValue[0].trim().replace("\"", "");
                        String value = keyValue[1].trim().replace("\"", "");
                        params.put(key, value);
                    }
                }

                logger.info("Успешно распарсено " + params.size() + " параметров");
            } else {
                logger.warning("Неверный формат JSON в теле запроса: " + requestBody);
            }
        } catch (NumberFormatException e) {
            logger.severe("Ошибка преобразования CONTENT_LENGTH в число: " + e.getMessage());
        } catch (Exception e) {
            logger.severe("Ошибка при чтении тела запроса: " + e.getMessage());
            e.printStackTrace();
        }

        return params;
    }

    private static String createJson(String answer) {
        logger.fine("Формирование JSON ответа: " + answer);
        return String.format(BASE_RESPONSE, answer.getBytes(StandardCharsets.UTF_8).length, answer);
    }
}