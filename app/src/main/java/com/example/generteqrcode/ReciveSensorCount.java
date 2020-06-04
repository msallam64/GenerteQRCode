package com.example.generteqrcode;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import android.view.Display;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.util.UUID;

import androidmads.library.qrgenearator.QRGContents;
import androidmads.library.qrgenearator.QRGEncoder;


public class ReciveSensorCount extends AppCompatActivity {
    private TextView counterUI;
    private Button checkBtn, resetBtn;
    private ImageView imageQR;
    QRGEncoder qrgEncoder;
    Bitmap bitmap;

    String canCount = "";
    Handler bluetoothIn;
    final int handlerState = 0;                         //used to identify handler message
    private BluetoothAdapter btAdapter = null;
    private BluetoothSocket btSocket = null;
    private StringBuilder recDataString = new StringBuilder();
    private ConnectedThread mConnectedThread;
    // SPP UUID service - this should work for most devices
    private static final UUID BTMODULEUUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    // String for MAC address
    private static String address = "00:13:EF:00:08:D8";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recive_sensor_count);
        counterUI = findViewById(R.id.trash_Counter_tv);
        checkBtn = findViewById(R.id.check_btn);
        resetBtn = findViewById(R.id.reset_btn);
        imageQR = findViewById(R.id.iv_QR);
        updateUI();
        btAdapter = BluetoothAdapter.getDefaultAdapter();       // get Bluetooth adapter
        checkBTState();
        checkBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int counter = Integer.parseInt(counterUI.getText().toString());
                if (counter > 0) {
                    CharSequence options[] = new CharSequence[]{"Yes", "NO"};
                    AlertDialog.Builder builder = new AlertDialog.Builder(ReciveSensorCount.this);
                    builder.setTitle("Is Your Cans Number :  " + counterUI.getText().toString() + " ?");
                    builder.setItems(options, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int i) {
                            if (i == 0) {
                                Toast.makeText(ReciveSensorCount.this, "Scan Your QRCode ", Toast.LENGTH_LONG).show();
                            } else {
                                mConnectedThread.write("1");
                                Toast.makeText(ReciveSensorCount.this, "QRCode Updated ", Toast.LENGTH_LONG).show();
                            }
                        }
                    });
                    builder.show();
                } else {
                    Toast.makeText(ReciveSensorCount.this, "No Cans Counted ", Toast.LENGTH_LONG).show();
                }
            }
        });
        resetBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mConnectedThread.write("0");
            }
        });
    }

    private void checkBTState() {

        if (btAdapter == null) {
            //Toast.makeText(getBaseContext(), "Device does not support bluetooth", Toast.LENGTH_LONG).show();
        } else {
            if (!btAdapter.isEnabled()) {
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBtIntent, 1);
                Toast.makeText(ReciveSensorCount.this, "Opening Connection Wait Seconds", Toast.LENGTH_LONG).show();

            } else {

            }
        }
    }

    public void updateUI() {
        bluetoothIn = new Handler() {
            public void handleMessage(android.os.Message msg) {
                String readMessage = (String) msg.obj;                             // msg.arg1 = bytes from connect thread
                recDataString.append(readMessage);
                //keep appending to string until ;
                int endOfLineIndex = recDataString.indexOf(";");
                if (endOfLineIndex > 0) {
                    String dataInPrint = recDataString.substring(0, endOfLineIndex);    // extract string
                    counterUI.setText("Counter : " + dataInPrint);
                    canCount = dataInPrint;
                    recDataString.delete(0, recDataString.length());                   //clear all string data
                    dataInPrint = "";
                    if (canCount.length() > 0) {
                        WindowManager windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
                        Display display = windowManager.getDefaultDisplay();
                        Point point = new Point();
                        display.getSize(point);
                        int width = point.x;
                        int high = point.y;
                        int demention = width < high ? width : high;
                        demention = demention * 3 / 4;
                        qrgEncoder = new QRGEncoder(canCount, null, QRGContents.Type.TEXT, demention);
                        try {
                            bitmap = qrgEncoder.encodeAsBitmap();
                            imageQR.setVisibility(View.VISIBLE);
                            imageQR.setImageBitmap(bitmap);

                        } catch (Exception e) {
                        }
                        counterUI.setText(canCount);
                    } else {
                        Toast.makeText(ReciveSensorCount.this, "Error Gitting Count Try again", Toast.LENGTH_LONG).show();
                    }
                }

            }
        };

    }

    private BluetoothSocket createBluetoothSocket(BluetoothDevice device) throws IOException {
        return device.createRfcommSocketToServiceRecord(BTMODULEUUID);
    }

    private class ConnectedThread extends Thread {
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;

        public ConnectedThread(BluetoothSocket socket) {
            InputStream tmpIn = null;
            OutputStream tmpOut = null;
            try {
                //Create I/O streams for connection
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e) {
            }

            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }

        public void run() {
            byte[] buffer = new byte[256];
            int bytes;
            // Keep  listen for received messages
            while (true) {
                try {
                    bytes = mmInStream.read(buffer);            //read bytes from input buffer
                    String readMessage = new String(buffer, 0, bytes);
                    // Send  to the UI  via handler
                    bluetoothIn.obtainMessage(handlerState, bytes, -1, readMessage).sendToTarget();
                } catch (IOException e) {
                    break;
                }
            }

        }

        //Send method
        public void write(String input) {
            byte[] msgBuffer = input.getBytes();           //converts entered String into bytes
            try {
                mmOutStream.write(msgBuffer);                //write bytes over BT connection via outstream
            } catch (IOException e) {
                //if you cannot write, close the application
                Log.e("ERRORX", e.getMessage().toString());
                Toast.makeText(getBaseContext(), "Connection Failure", Toast.LENGTH_LONG).show();
                // finish();

            }
        }

    }


    public void soketData() {
        BluetoothDevice device = btAdapter.getRemoteDevice(address);
        try {
            btSocket = createBluetoothSocket(device);
        } catch (IOException e) {
            //Toast.makeText(getBaseContext(), "Socket creation failed", Toast.LENGTH_LONG).show();
        }
        //  the Bluetooth socket connection.
        try {
            btSocket.connect();
        } catch (Exception e) {
            try {
                Log.e("Closed", e.getMessage().toString());
                if (btSocket != null) {
                    btSocket.close();
                }
            } catch (IOException e2) {
            }
        }
        if (mConnectedThread == null && btSocket != null) {
            mConnectedThread = new ConnectedThread(btSocket);
            mConnectedThread.start();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        soketData();
    }
}



