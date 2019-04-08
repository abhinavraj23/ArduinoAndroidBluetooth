package com.developer.abhinavraj.envapp;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.ParcelUuid;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.util.Set;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {

    private static final UUID MY_UUID = UUID.fromString("00000000-0000-1000-8000-00805f9b34fb");
    TextView pressure, humidity, intensity, temperature;
    ImageButton bluetooth;
    BluetoothAdapter mBluetoothAdapter;
    BluetoothDevice mdevice;
    BluetoothSocket mSocket;
    InputStream mmInputStream;
    Thread workerThread;
    byte[] readBuffer;
    int readBufferPosition;
    int counter;
    String string;
    volatile boolean stopWorker;
    TextView heading;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        pressure = findViewById(R.id.pressure);
        temperature = findViewById(R.id.temperature);
        humidity = findViewById(R.id.humidity);
        intensity = findViewById(R.id.intensity);
        bluetooth = findViewById(R.id.bluetooth);

        bluetooth.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
                if (!mBluetoothAdapter.isEnabled()) {
                    int REQUEST_ENABLE_BT = 1;
                    Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                    startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
                }
                Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();
                Toast.makeText(MainActivity.this, pairedDevices.toString(), Toast.LENGTH_SHORT).show();

                if (pairedDevices.size() > 0) {
                    // There are paired devices. Get the name and address of each paired device.
                    for (BluetoothDevice device : pairedDevices) {
                        if (device.getName().equals("Rag")) {
                            view.setVisibility(View.GONE);
                            Toast.makeText(MainActivity.this, "Device found" , Toast.LENGTH_SHORT).show();
                            mdevice = device;
                            ParcelUuid[] uuids = device.getUuids();

                            if(uuids != null) {
                                for (ParcelUuid uuid : uuids) {
                                    Log.d("Hey", "UUID: " + uuid.getUuid().toString());
                                }
                            }else{
                                Log.d("Hey", "Uuids not found, be sure to enable Bluetooth!");
                            }

                            break;
                        }
                    }
                }
                if (mdevice != null) {
                    try {
                        mSocket = mdevice.createRfcommSocketToServiceRecord(MY_UUID);
                        mSocket.connect();
                        mmInputStream = mSocket.getInputStream();
                      //  mmInputStream.read();
                        Toast.makeText(getApplicationContext(), "You are Connected" , Toast.LENGTH_SHORT).show();
//                        beginListenForData();
                    } catch (IOException connectException) {
                        Toast.makeText(getApplicationContext(), "You are not Connected", Toast.LENGTH_SHORT).show();
                    }
                }
            }
        });
    }

//    void fetchDataAndUpdate() {
//
//        final Handler handler = new Handler();
//        final byte delimiter = 10;
//        int byteCount = 0;
//
//        try {
//            inputStream = mSocket.getInputStream();
//            byteCount = inputStream.available();
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//
//        if (byteCount > 0) {
//            byte[] rawBytes = new byte[byteCount];
//            try {
//                inputStream.read(rawBytes);
//            } catch (IOException e) {
//                e.printStackTrace();
//            }
//
//            string = "Nil";
//            try {
//                string = new String(rawBytes, "UTF-8");
//            } catch (UnsupportedEncodingException e) {
//                e.printStackTrace();
//            }
//            handler.post(new Runnable() {
//                public void run() {
//                    temperature.append(string);
//                }
//            });
//        }
//    }

    void beginListenForData() {
        final Handler handler = new Handler();
        final byte delimiter = 10; //This is the ASCII code for a newline character

        stopWorker = false;
        readBufferPosition = 0;
        readBuffer = new byte[1024];
        workerThread = new Thread(new Runnable() {
            public void run() {
                while (!Thread.currentThread().isInterrupted() && !stopWorker) {
                    try {
                       // Toast.makeText(MainActivity.this, "Yep", Toast.LENGTH_SHORT).show();
//                        mmInputStream = mSocket.getInputStream();
                        int bytesAvailable = mmInputStream.available();
                        if (bytesAvailable > 0) {
                            byte[] packetBytes = new byte[bytesAvailable];
                            mmInputStream.read(packetBytes);
                            for (int i = 0; i < bytesAvailable; i++) {
                                byte b = packetBytes[i];
                                if (b == delimiter) {
                                    byte[] encodedBytes = new byte[readBufferPosition];
                                    System.arraycopy(readBuffer, 0, encodedBytes, 0, encodedBytes.length);
                                    final String data = new String(encodedBytes, "US-ASCII");
                                    readBufferPosition = 0;

                                    handler.post(new Runnable() {
                                        public void run() {
                                            Toast.makeText(getApplicationContext(),data + "HI",Toast.LENGTH_SHORT).show();
                                            temperature.setText(data);
                                        }
                                    });
                                } else {
                                    readBuffer[readBufferPosition++] = b;
                                }
                            }
                        }
                    } catch (IOException ex) {
                        stopWorker = true;
                    }
                }
            }
        });

        workerThread.start();
    }


}
