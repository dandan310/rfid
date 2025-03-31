package com.uhf.scanlable;

import android.hardware.usb.UsbConstants;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbEndpoint;
import android.hardware.usb.UsbInterface;
import android.hardware.usb.UsbManager;
import android.util.Log;

import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;

public class UsbPrinter {
    private UsbManager usbManager;
    private UsbDevice usbDevice;
    private UsbDeviceConnection connection;
    private UsbEndpoint endpointOut;
    private UsbEndpoint endpointIn;

    public UsbPrinter(UsbManager manager, UsbDevice device) {
        this.usbManager = manager;
        this.usbDevice = device;
        initialize();
    }

    private void initialize() {
        UsbInterface usbInterface = usbDevice.getInterface(0);
        Log.d("USB_DEVICE", "Vendor ID: " + usbDevice.getVendorId());
        Log.d("USB_DEVICE", "Product ID: " + usbDevice.getProductId());
        Log.d("USB_DEVICE", "Device Class: " + usbDevice.getDeviceClass());
        Log.d("USB_DEVICE", "Device Subclass: " + usbDevice.getDeviceSubclass());
        Log.d("USB_DEVICE", "Device Protocol: " + usbDevice.getDeviceProtocol());
        Log.d("USB_DEBUG", "endpointCount:" + usbInterface.getEndpointCount());

        for (int i = 0; i < usbInterface.getEndpointCount(); i++) {
            UsbEndpoint ep = usbInterface.getEndpoint(i);
            if (ep.getDirection() == UsbConstants.USB_DIR_OUT) {
                endpointOut = ep;
            }
            if (ep.getDirection() == UsbConstants.USB_DIR_IN) {
                endpointIn = ep;
            }
        }

        connection = usbManager.openDevice(usbDevice);
        if (!connection.claimInterface(usbInterface, true)) {
            Log.e("USB_PRINTER", "Failed to claim interface");
            connection.close();
        }

    }

    public boolean print(String barCode, String tag) {
        String encodedTag;
        try {
            encodedTag = new String(tag.getBytes("GB2312"), "GB2312");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            encodedTag = tag; // 回退到默认值
        }
        if (connection != null && endpointOut != null) {
            // TSPL 指令示例：打印 Code 128 条码
            String tsplCommand = "SIZE 40 mm, 30 mm\n" + // 设置标签尺寸
                    "GAP 2 mm, 0 mm\n" + // 设置间距
                    "CLS\n" + // 清除缓冲区
                    "CODEPAGE 54936\n" +  // 设置 GBK/GB2312 编码
                    "BARCODE 50, 50, \"128\", 80, 0, 0, 2, 2,\"" + barCode + "\"\n" + // 打印条码
                    "TEXT 50, 140, \"TSS24.BF2\", 0, 1, 1, \"" + tag + "\"\n" + // 在条码下方打印文本
                    "PRINT 1\n"; // 打印一张

            byte[] tsplCommandBytes = null;
            try {
                tsplCommandBytes = tsplCommand.getBytes("GB2312");
            } catch (UnsupportedEncodingException e) {
                tsplCommandBytes = tsplCommand.getBytes();
            }
            int i = connection.bulkTransfer(endpointOut, tsplCommandBytes, tsplCommandBytes.length, 5000);
            Log.d("USB_DEBUG", "bulkTransfer:" + i);
            return i != -1;
        }
        return false;
    }

    public void print(String barCode) {
        if (connection != null && endpointOut != null) {
            // TSPL 指令示例：打印 Code 128 条码
            String tsplCommand = "SIZE 40 mm, 30 mm\n" + // 设置标签尺寸
                    "GAP 2 mm, 0 mm\n" + // 设置间距
                    "CLS\n" + // 清除缓冲区
                    "BARCODE 50, 50, \"128\", 80, 0, 0, 2, 2, \"" + barCode + "\"\n" + // 打印条码
                    "PRINT 1\n"; // 打印一张

            byte[] tsplCommandBytes = tsplCommand.getBytes();

            int i = connection.bulkTransfer(endpointOut, tsplCommandBytes, tsplCommandBytes.length, 5000);
            Log.d("USB_DEBUG", "bulkTransfer:" + i);


        }
    }
}
