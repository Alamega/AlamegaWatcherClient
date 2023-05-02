package com.alamega;

import com.profesorfalken.jsensors.JSensors;
import com.profesorfalken.jsensors.model.components.Components;
import com.profesorfalken.jsensors.model.components.Cpu;
import com.profesorfalken.jsensors.model.components.Disk;
import com.profesorfalken.jsensors.model.components.Gpu;
import com.sun.management.OperatingSystemMXBean;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.management.ManagementFactory;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;

public class SystemScanner {
    final static int GB = 1024*1024*1024;
    static OperatingSystemMXBean osMBean = null;

    static {
        try {
            osMBean = ManagementFactory.newPlatformMXBeanProxy(ManagementFactory.getPlatformMBeanServer(), ManagementFactory.OPERATING_SYSTEM_MXBEAN_NAME, OperatingSystemMXBean.class);
        } catch (IOException e) {
            System.out.println("Ошибка при получении данных о процессоре: " + e.getMessage());
        }
    }

    public static String getMACAddress(){
        //Получение MAC адреса устройства
        try {
            InetAddress localHost = InetAddress.getLocalHost();
            NetworkInterface ni = NetworkInterface.getByInetAddress(localHost);
            byte[] hardwareAddress = ni.getHardwareAddress();
            String[] hexadecimal = new String[hardwareAddress.length];
            for (int i = 0; i < hardwareAddress.length; i++) {
                hexadecimal[i] = String.format("%02X", hardwareAddress[i]);
            }
            return String.join(":", hexadecimal);
        } catch (UnknownHostException | SocketException e) {
            System.out.println("Ошибка при получении MAC-адреса: " + e.getMessage());
        }
        return null;
    }

    public static JSONArray getAllDiscUsage(){
        JSONArray resultJson = new JSONArray();
        for(File driver:File.listRoots())
        {
            if (!((double)driver.getTotalSpace() <= 0)) {
                JSONObject tempJson = new JSONObject();
                tempJson.put("name", driver.toString());
                tempJson.put("usage", (double)driver.getTotalSpace()/GB-(double)driver.getUsableSpace()/GB);
                tempJson.put("free", (double)driver.getUsableSpace()/GB);
                tempJson.put("total", (double)driver.getTotalSpace()/GB);
                resultJson.put(tempJson);
            }
        }
        return resultJson;
    }

    public static JSONArray getAllPhysicalDiscUsage(){
        Components components = JSensors.get.components();
        JSONArray resultJson = new JSONArray();
        for (int i = 0; i < components.disks.size(); i++) {
            Disk tempDisc = components.disks.get(i);
            JSONObject tempJson = new JSONObject();
            tempJson.put("name", tempDisc.name);
            tempJson.put("load", tempDisc.sensors.loads.get(0).value);
            resultJson.put(tempJson);
        }
        return resultJson;
    }

    public static String getMemoryUsage() {
        return String.format("%.2f", (double)osMBean.getTotalMemorySize()/GB-(double)osMBean.getFreeMemorySize()/GB) + "/" + String.format("%.2f", (double)osMBean.getTotalMemorySize()/GB);
    }

    public static int getAvailableProcessors() {
        return osMBean.getAvailableProcessors();
    }

    public static String getCPUUsage() {
        try {
            Process process = Runtime.getRuntime().exec(new String[]{"wmic", "cpu", "get", "loadpercentage"});
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String result = "";
            try {
                result = reader.lines().toArray()[2].toString().trim();
            } catch (Exception ignored) { }
            if (result.equals("")) { result = "0"; }
            return result;
        } catch (IOException e) {
            return "0";
        }
    }

    public static JSONArray getAllCPUInfo(){
        Components components = JSensors.get.components();
        JSONArray resultJson = new JSONArray();
        for (int i = 0; i < components.cpus.size(); i++) {
            JSONObject cpuJson = new JSONObject();
            JSONArray coresJson = new JSONArray();
            Cpu temp = components.cpus.get(i);
            if (!temp.sensors.loads.isEmpty()) {
                for (int j = 0; j < temp.sensors.loads.size() - 1; j++) {
                    if (!temp.sensors.loads.get(j).name.contains("Total")) {
                        JSONObject tempJson = new JSONObject();
                        tempJson.put("name", temp.sensors.loads.get(j).name.substring(5));
                        tempJson.put("load", temp.sensors.loads.get(j).value);
                        if (!temp.sensors.temperatures.isEmpty() && temp.sensors.temperatures.get(j) != null) {
                            tempJson.put("temperature", temp.sensors.temperatures.get(j).value);
                        } else {
                            tempJson.put("temperature", 0);
                        }
                        coresJson.put(tempJson);
                    }
                }
            }
            cpuJson.put("name", temp.name);
            cpuJson.put("cores", coresJson);
            resultJson.put(cpuJson);
        }
        return resultJson;
    }

    public static JSONArray getAllGPUInfo(){
        Components components = JSensors.get.components();
        JSONArray resultJson = new JSONArray();
        for (int i = 0; i < components.gpus.size(); i++) {
            JSONObject cpuJson = new JSONObject();
            Gpu temp = components.gpus.get(i);
            if (!temp.sensors.loads.isEmpty()) {
                for (int j = 0; j < temp.sensors.loads.size() - 1; j++) {
                    if (temp.sensors.loads.get(j).name.contains("Core")) {
                        cpuJson.put("load", temp.sensors.loads.get(j).value);
                        if (!temp.sensors.temperatures.isEmpty()) {
                            cpuJson.put("temperature", temp.sensors.temperatures.get(0));
                        } else {
                            cpuJson.put("temperature", 0);
                        }
                    }
                }
            }
            cpuJson.put("name", temp.name);
            resultJson.put(cpuJson);
        }
        return resultJson;
    }

}
