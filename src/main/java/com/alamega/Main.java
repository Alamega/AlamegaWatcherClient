package com.alamega;

import org.json.JSONObject;
import oshi.SystemInfo;
import oshi.hardware.CentralProcessor;
import oshi.hardware.HardwareAbstractionLayer;

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

        final String SERVER_URL  = props.getProperty("SERVER_URL", "https://alamegawatcher.fly.dev/data");
        final String PASSWORD  = props.getProperty("PASSWORD", "1111");
        final int LOG_TO_FILE  = Integer.parseInt(props.getProperty("LOG_TO_FILE", "1"));
        int SEND_PERIOD  = Integer.parseInt(props.getProperty("SEND_PERIOD", "5"));

        //Период должен быть от 2 минут до 24часов, а не то это кринж
        SEND_PERIOD = Math.max(SEND_PERIOD, 2);
        SEND_PERIOD = Math.min(SEND_PERIOD, 24 * 60);

        SystemInfo systemInfo = new SystemInfo();
        HardwareAbstractionLayer hardware = systemInfo.getHardware();
        CentralProcessor processor = hardware.getProcessor();
        CentralProcessor.ProcessorIdentifier processorIdentifier = processor.getProcessorIdentifier();

        JSONObject json = new JSONObject();
        json.put("password", PASSWORD);
        json.put("mac", SystemScanner.getMACAddress());
        json.put("username", System.getProperty("user.name"));
        json.put("cpuname", processorIdentifier.getName());
        json.put("cores", SystemScanner.getAvailableProcessors());
        json.put("os", System.getProperty("os.name"));
        new Timer().scheduleAtFixedRate(new TimerTask(){
            @Override
            public void run(){
                JSONObject drivers = new JSONObject();
                SystemScanner.getAllDiscUsage().forEach(drivers::put);
                json.put("drivers", drivers);
                json.put("ram", SystemScanner.getMemoryUsage());
                json.put("cpuusage", SystemScanner.getCPUUsage());
                json.put("time", new Date().getTime());
                json.put("cputemp", SystemScanner.getAllCPUTemp());

                if (LOG_TO_FILE != 0) {
                  try {
                    new File("log.txt").createNewFile();
                    FileWriter writer = new FileWriter("log.txt", true);
                    writer.write(json + "\n");
                    writer.close();
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }

                try {
                    HttpRequest httpRequest = HttpRequest.newBuilder()
                            .uri(new URI(SERVER_URL))
                            .POST(HttpRequest.BodyPublishers.ofString(json.toString()))
                            .build();
                    client.send(httpRequest, HttpResponse.BodyHandlers.ofString());
                } catch (URISyntaxException | IOException | InterruptedException ignored) { }
            }
        },0,SEND_PERIOD * 1000L);
    }
}