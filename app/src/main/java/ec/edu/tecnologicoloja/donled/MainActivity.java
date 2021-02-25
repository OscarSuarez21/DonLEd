package ec.edu.tecnologicoloja.donled;

import androidx.appcompat.app.AppCompatActivity;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity {

    Button btnOn, btnOff;
    TextView txtArduino, txtString, txtStringLength;
    TextView txtSendorLDR;
    Handler bluetoothIn;
    ImageView bloqueado, desbloqueado;

    final int handlerState = 0;        				 //utilizado para identificar el mensaje del controlador

    private BluetoothAdapter btAdapter = null;
    private BluetoothSocket btSocket = null;
    private StringBuilder recDataString = new StringBuilder();

    private ConnectedThread mConnectedThread;

    // Servicio SPP UUID:para hacerlo compatible con la mayoria de dispositivos
    private static final UUID BTMODULEUUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    //Cadena para la dirección MAC
    private static String address = null;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        //Vincular los botones a las respectivas vistas
        btnOn = (Button) findViewById(R.id.buttonOn);
        btnOff = (Button) findViewById(R.id.buttonOff);
        bloqueado=(ImageView) findViewById(R.id.imageLoked);
        desbloqueado=(ImageView) findViewById(R.id.imageLoked);


       // txtSendorLDR = (TextView) findViewById(R.id.tv_sendorldr);


        bluetoothIn = new Handler() {
            public void handleMessage(android.os.Message msg) {
                if (msg.what == handlerState) {										//si mensaje es lo que queremos

                    String readMessage = (String) msg.obj;                                 // msg.arg1 = bytes del hilo de conexión

                    recDataString.append(readMessage);      								//sigue agregando a la cadena hasta ~

                    int endOfLineIndex = recDataString.indexOf("~");                    // determine the end-of-line
                    if (endOfLineIndex > 0) {                                           // asegúrese de que haya datos antes ~

                        String dataInPrint = recDataString.substring(0, endOfLineIndex);    // extraer cadena

                        txtString.setText("Datos recibidos = " + dataInPrint);
                        int dataLength = dataInPrint.length();							//obtener la longitud de los datos recibidos

                        txtStringLength.setText("Tamaño del String = " + String.valueOf(dataLength));

                        if (recDataString.charAt(0) == '#')								//si comienza con # sabemos que es lo que estamos buscando

                        {

                        }
                        recDataString.delete(0, recDataString.length()); 					//borrar todos los datos de la cadena

                        // strIncom =" ";
                        dataInPrint = " ";
                    }
                }
            }
        };

        btAdapter = BluetoothAdapter.getDefaultAdapter();       // conseguir adaptador Bluetooth

        checkBTState();


        // Configure los oyentes de onClick para que los botones envíen 1 o 0 para abrir / cerrar la puerta
        btnOff.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                mConnectedThread.write("0");    // Enviar "0" a través de Bluetooth

                bloqueado.setImageResource(R.drawable.locked_icon);


                Toast.makeText(getBaseContext(), "Bloqueado", Toast.LENGTH_SHORT).show();
            }
        });

        btnOn.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                mConnectedThread.write("1");    //  Enviar "1" a través de Bluetooth
                bloqueado.setImageResource(R.drawable.unlocked_icon);
                Toast.makeText(getBaseContext(), "Desbloquado ", Toast.LENGTH_SHORT).show();
            }
        });
    }


    private BluetoothSocket createBluetoothSocket(BluetoothDevice device) throws IOException {

        return  device.createRfcommSocketToServiceRecord(BTMODULEUUID);
        //crea una conexión saliente segura con un dispositivo BT usando UUID
    }

    @Override
    public void onResume() {
        super.onResume();

        //Obtenga la dirección MAC de DeviceListActivity a través de la intención
        Intent intent = getIntent();

        //Obtenga la dirección MAC de DeviceListActivty a través de EXTRA
        address = intent.getStringExtra(DeviceListActivity.EXTRA_DEVICE_ADDRESS);

        // crear dispositivo y configurar la dirección MAC
        //Log.i("ramiro "," dirección: "+ dirección);
        BluetoothDevice device = btAdapter.getRemoteDevice(address);

        try {
            btSocket = createBluetoothSocket(device);
        } catch (IOException e) {
            Toast.makeText(getBaseContext(), "La creacción del Socket fallo", Toast.LENGTH_LONG).show();
        }
        // Establezca la conexión de la toma Bluetooth.
        try
        {
            btSocket.connect();
        } catch (IOException e) {
            try
            {
                btSocket.close();
            } catch (IOException e2)
            {
                //inserte el código para tratar con esto
            }
        }
        mConnectedThread = new ConnectedThread(btSocket);
        mConnectedThread.start();

        // Envío un carácter al reanudar la transmisión inicial para comprobar que el dispositivo está conectado
        // Si no es una excepción, se lanzará en el método de escritura y se llamará a finish ()
        mConnectedThread.write("x");
    }

    @Override
    public void onPause()
    {
        super.onPause();
        try
        {
            //No deje las tomas de Bluetooth abiertas cuando deje la actividad
            btSocket.close();
        } catch (IOException e2) {
            //inserte el código para tratar con esto
        }
    }

    //Comprueba que el Bluetooth del dispositivo Android esté disponible y solicita que se encienda si está apagado
    private void checkBTState() {

        if(btAdapter==null) {
            Toast.makeText(getBaseContext(), "El dispositivo no compatible con bluetooth", Toast.LENGTH_LONG).show();
        } else {
            if (btAdapter.isEnabled()) {
            } else {
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBtIntent, 1);
            }
        }
    }

    //crear una nueva clase para conectar el hilo
    private class ConnectedThread extends Thread {
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;

        //creación del hilo de conexión
        public ConnectedThread(BluetoothSocket socket) {
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            try {
                //Cree flujos de E / S para la conexión
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e) { }

            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }


        public void run() {
            byte[] buffer = new byte[256];
            int bytes;

            // Siga repitiendo para escuchar los mensajes recibidos
            while (true) {
                try {
                    bytes = mmInStream.read(buffer);        	//leer bytes del búfer de entrada

                    String readMessage = new String(buffer, 0, bytes);
                    // Envíe los bytes obtenidos a la actividad de la interfaz de usuario a través del controlador
                    bluetoothIn.obtainMessage(handlerState, bytes, -1, readMessage).sendToTarget();
                } catch (IOException e) {
                    break;
                }
            }
        }
        //método de escritura
        public void write(String input) {
            byte[] msgBuffer = input.getBytes();           //convierte la cadena ingresada en bytes
            try {
                mmOutStream.write(msgBuffer);                //escribir bytes sobre la conexión BT a través de la salida
            } catch (IOException e) {
                //si no puede escribir, cierre la aplicación
                Toast.makeText(getBaseContext(), "La Conexión fallo", Toast.LENGTH_LONG).show();
                finish();

            }
        }
    }

}