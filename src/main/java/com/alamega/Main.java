package com.alamega;

import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Date;
import java.util.Properties;
import java.util.Timer;
import java.util.TimerTask;

public class Main {
    public static void main(String[] args) {
        Properties props = new Properties();
        try {
            props.load(new FileInputStream("config.ini"));
        } catch (IOException e) {
            System.out.println("Не удалось загрузить настройки, так как не обнаружен файл config.ini, используются значения по умолчанию.");
        }

        HttpClient client = HttpClient.newHttpClient();

        final String SERVER_URL  = props.getProperty("SERVER_URL", "https://alamegawatcher.onrender.com");
        final String PASSWORD  = props.getProperty("PASSWORD", "1111");
        final int LOG_TO_FILE  = Integer.parseInt(props.getProperty("LOG_TO_FILE", "1"));
        final int PRINT_JSON_TO_CONSOLE  = Integer.parseInt(props.getProperty("PRINT_JSON_TO_CONSOLE", "1"));

        int SEND_PERIOD  = Integer.parseInt(props.getProperty("SEND_PERIOD", "5"));

        //Период должен быть от 2 секунд до 24часов, а не то это кринж
        SEND_PERIOD = Math.max(SEND_PERIOD, 2);
        SEND_PERIOD = Math.min(SEND_PERIOD, 24 * 60);

        JSONObject json = new JSONObject();
        json.put("password", PASSWORD);
        json.put("mac", SystemScanner.getMACAddress());
        json.put("username", System.getProperty("user.name"));
        json.put("cores", SystemScanner.getAvailableProcessors());
        json.put("os", System.getProperty("os.name"));
        new Timer().scheduleAtFixedRate(new TimerTask(){
            @Override
            public void run(){
                json.put("discs", SystemScanner.getAllDiscUsage());
                json.put("discsphysical", SystemScanner.getAllPhysicalDiscUsage());
                json.put("ram", SystemScanner.getMemoryUsage());
                json.put("cpuusage", SystemScanner.getCPUUsage());
                json.put("time", new Date().getTime());
                json.put("cpuinfo", SystemScanner.getAllCPUInfo());
                json.put("gpuinfo", SystemScanner.getAllGPUInfo());

                if (LOG_TO_FILE != 0) {
                  try {
                    boolean result = new File("log.txt").createNewFile();
                    FileWriter writer = new FileWriter("log.txt", true);
                    writer.write(json + "\n");
                    writer.close();
                    } catch (IOException e) {
                      System.out.println("Ошибка записи файла логов: " + e.getMessage());
                    }
                }

                if (PRINT_JSON_TO_CONSOLE != 0) {
                    System.out.println(json);
                }

                try {
                    HttpRequest httpRequest = HttpRequest.newBuilder()
                            .uri(new URI(SERVER_URL + "/data"))
                            .POST(HttpRequest.BodyPublishers.ofString(json.toString()))
                            .build();
                    client.send(httpRequest, HttpResponse.BodyHandlers.ofString());
                } catch (URISyntaxException | IOException | InterruptedException e) {
                    System.out.println("Ошибка отправки данных на сервер: " + e.getMessage());
                }
            }
        },0,SEND_PERIOD * 1000L);
    }
}