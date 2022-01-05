package com.example.usb_sample1

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.ContentValues.TAG
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.usb.UsbAccessory
import android.hardware.usb.UsbManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.ParcelFileDescriptor
import android.util.Log
import android.widget.TextView
import android.widget.Toast
import java.io.FileInputStream
import java.io.FileOutputStream

import com.github.mjdev.libaums.UsbMassStorageDevice
import com.github.mjdev.libaums.fs.FileSystem
import com.github.mjdev.libaums.fs.UsbFile
import com.github.mjdev.libaums.fs.UsbFileInputStream
import com.github.mjdev.libaums.partition.Partition

class MainActivity : AppCompatActivity() {
    private lateinit var accessory: UsbAccessory
    private var usbManager: UsbManager? = null
    private var usbDeviceStateFilter: IntentFilter? = null
    private var fileDescriptor: ParcelFileDescriptor? = null
    private var inputStream: FileInputStream? = null
    private var outputStream: FileOutputStream? = null
    private var device: UsbMassStorageDevice? = null
    private val ACTION_USB_PERMISSION = "com.android.example.USB_PERMISSION"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        var textview = findViewById<TextView>(R.id.textView)
        //textview.text = "USB接続せれておりません！"

        val usbManager = getSystemService(Context.USB_SERVICE) as UsbManager

        usbDeviceStateFilter?.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED)
        usbDeviceStateFilter?.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED)
        //registerReceiver(usbReceiver,usbDeviceStateFilter)

        val filter = IntentFilter(ACTION_USB_PERMISSION)
        registerReceiver(usbReceiver, filter)

    }

    /**
     * アクセサリとの通信の権限を取得する
     */
    private val usbReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val action = intent?.action
            when (action) {
                ACTION_USB_PERMISSION -> {
                    //接続されたアクセサリを表す UsbAccessory をインテントから取得できます
                    val usbAccessory =
                        intent?.getParcelableArrayExtra(UsbManager.EXTRA_DEVICE) as UsbAccessory
                    if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                        if (usbAccessory != null) {
                            readDevice()
                        } else {
                            var textview = findViewById<TextView>(R.id.textView)
                            textview.text = "USB接続せれておりません！"
                            Toast.makeText(this@MainActivity, "USB接続せれておりません", Toast.LENGTH_LONG)
                                .show()
                        }
                    } else {
                        Toast.makeText(this@MainActivity, "権限がありません！", Toast.LENGTH_LONG).show()
                    }
                }
                UsbManager.ACTION_USB_ACCESSORY_ATTACHED -> {
                    val device_add = intent.getParcelableArrayExtra(UsbManager.EXTRA_DEVICE)
                    if (device_add != null) {
                        redUDiskDevsList();
                    }
                }
                UsbManager.ACTION_USB_ACCESSORY_DETACHED -> {
                    Toast.makeText(this@MainActivity, "USBを抜き出しました", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    /**
     * ??????
     */
    private fun readDevice() {
        //Log.d(TAG, "openAccessory: $mAccessory")
        fileDescriptor = usbManager?.openAccessory(accessory)
        fileDescriptor?.fileDescriptor?.also { fd ->
            inputStream = FileInputStream(fd)
            outputStream = FileOutputStream(fd)
            val thread = Thread(null, null, "AccessoryThread")
            thread.start()
        }
    }

    /**
     *USBを読み込み
     */
    private fun redUDiskDevsList() {
        val usbManager = getSystemService(Context.USB_SERVICE) as UsbManager
        //usbを取得する
        val storageDevice = UsbMassStorageDevice.getMassStorageDevices(this)
        val pendingIntent = PendingIntent.getBroadcast(this, 0, Intent(ACTION_USB_PERMISSION), 0)
        for (device: UsbMassStorageDevice in storageDevice) {
            if (usbManager.hasPermission(device.usbDevice)) {
                readDevice()
            } else {
                //権限がありません
                usbManager.requestPermission(device.usbDevice, pendingIntent);
            }
        }
        if (storageDevice.size == 0) {
            Toast.makeText(this@MainActivity, "USBを挿入してください", Toast.LENGTH_LONG).show()
        }
    }
}