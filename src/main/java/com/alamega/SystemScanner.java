package com.alamega;

import com.profesorfalken.jsensors.JSensors;
import com.profesorfalken.jsensors.model.components.Components;
import com.profesorfalken.jsensors.model.components.Cpu;
import com.profesorfalken.jsensors.model.sensors.Temperature;
import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.platform.win32.Kernel32;
import com.sun.jna.platform.win32.WinBase;
import com.sun.jna.platform.win32.WinDef;
import com.sun.jna.ptr.IntByReference;
import com.sun.jna.win32.StdCallLibrary;
import com.sun.jna.win32.W32APIFunctionMapper;
import com.sun.jna.win32.W32APITypeMapper;
import com.sun.management.OperatingSystemMXBean;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.management.ManagementFactory;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.*;

public class SystemScanner {
    final static int MB = 1024*1024;
    final static int GB = 1024*1024*1024;

    final static OperatingSystemMXBean osBean = (OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();
    static OperatingSystemMXBean osMBean = null;

    static {
        try {
            osMBean = ManagementFactory.newPlatformMXBeanProxy(ManagementFactory.getPlatformMBeanServer(), ManagementFactory.OPERATING_SYSTEM_MXBEAN_NAME, OperatingSystemMXBean.class);
        } catch (IOException e) {
            System.out.println("Ошибка при получении данных о процессоре.");
        }
    }

    public static String getMACAddress(){
        //Получение MAC адреса устройства
        InetAddress localHost = null;
        try {
            localHost = InetAddress.getLocalHost();
            NetworkInterface ni = NetworkInterface.getByInetAddress(localHost);
            byte[] hardwareAddress = ni.getHardwareAddress();
            String[] hexadecimal = new String[hardwareAddress.length];
            for (int i = 0; i < hardwareAddress.length; i++) {
                hexadecimal[i] = String.format("%02X", hardwareAddress[i]);
            }
            return String.join(":", hexadecimal);
        } catch (UnknownHostException | SocketException e) {
            System.out.println("Ошибка при получении MAC-адреса: " + e);;
        }
        return null;
    }

    public static Map<String, String> getAllDiscUsage(){
        Map<String, String> drivers = new LinkedHashMap<>();
        for(File driver:File.listRoots())
        {
            if (!((double)driver.getTotalSpace() <= 0)) {
                drivers.put(driver.toString(), String.format("%.2f", (double)driver.getTotalSpace()/GB - (double)driver.getUsableSpace()/GB) + "/" + String.format("%.2f", (double)driver.getTotalSpace()/GB));
            }
        }
        return drivers;
    }

    public static String getMemoryUsage() {
        return String.format("%.2f", (double)osBean.getTotalMemorySize()/GB-(double)osBean.getFreeMemorySize()/GB) + "/" + String.format("%.2f", (double)osBean.getTotalMemorySize()/GB);
    }

    public static int getAvailableProcessors() {
        return osMBean.getAvailableProcessors();
    }

    public static String getCPUUsage() {
        Process process = null;
        try {
            process = Runtime.getRuntime().exec(new String[]{"wmic", "cpu", "get", "loadpercentage"});
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String result = "";
            try {
                result = reader.lines().toArray()[2].toString().trim();
            } catch (Exception ignored) { }
            if (result.equals("")) { result = "0"; }
            return result;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static Map<String, Double> getAllCPUTemp(){
        Map<String, Double> tempMap = new LinkedHashMap<>();
        Components components = JSensors.get.components();
        if (!components.cpus.isEmpty()) {
            Cpu cpu = components.cpus.get(0);
            List<Temperature> temps = cpu.sensors.temperatures;
            for (final Temperature temp : temps) {
                tempMap.put(temp.name.substring(5), temp.value);
            }
        }
        return tempMap;
    }
}
