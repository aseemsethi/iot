package com.asethi.mqttapp;

import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.DisconnectedBufferOptions;
import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttMessageListener;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import java.text.SimpleDateFormat;
import java.util.ArrayList;

import static android.R.attr.button;
import static android.R.attr.color;

public class ToolActivity extends AppCompatActivity {
    //implements CompoundButton.OnCheckedChangeListener {

    private HistoryAdapter mAdapter;
    private MqttAndroidClient mqttAndroidClient;
    // final String serverUri = "test.mosquitto.org:1883";
    String serverUri = "tcp://iot.eclipse.org:1883";
    String clientId = "AndroidClient";
    String subscriptionTopic = "Topic1";
    String publishTopic = "Topic1";
    Boolean connected = false;
    final String publishMessage = "Hello World !";
    SimpleDateFormat simpleDateFormat = new SimpleDateFormat("dd-MM-yyyy-hh-mm-ss");
    RecyclerView mRecyclerView;
    CheckBox cl, rl;
    Boolean cleanVar = false;

    // Stores all Subscriptions here
    ArrayList<String> mysubsList = new ArrayList<String>();
/*
    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        if (buttonView.getId() == R.id.cleanBUI) {
            System.out.println("Aseem: isChecked: " + isChecked);
            if (isChecked) {
                System.out.println("Aseem: checked is true");
                cl.setChecked(true);
            } else {
                System.out.println("Aseem: checked is false");
                cl.setChecked(false);
            }
        }
        if (buttonView.getId() == R.id.retainBUI) {
        }
    }
    */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.tool_activity);

        final CheckBox cl = (CheckBox) findViewById(R.id.cleanBUI);
        cl.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (cl.isChecked()) {
                    System.out.println("Aseem: checked is true");
                    cleanVar = true;
                } else {
                    System.out.println("Aseem: checked is false");
                    cleanVar = false;
                }
            }
        });
        rl = (CheckBox) findViewById(R.id.retainBUI);


        final Button stat = (Button) findViewById(R.id.button1);
        stat.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (connected)
                    stat.setBackgroundColor(Color.GREEN);
                else
                    stat.setBackgroundColor(Color.RED);
            }
        });

        Button pub = (Button) findViewById(R.id.publishB);
        pub.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                InputMethodManager inputManager = (InputMethodManager)
                        getSystemService(Context.INPUT_METHOD_SERVICE);
                inputManager.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(),
                        InputMethodManager.HIDE_NOT_ALWAYS);
                publishMessage();
            }
        });

        final Button sysBT = (Button) findViewById(R.id.sysB);
        sysBT.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                InputMethodManager inputManager = (InputMethodManager)
                        getSystemService(Context.INPUT_METHOD_SERVICE);
                inputManager.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(),
                        InputMethodManager.HIDE_NOT_ALWAYS);
                sysBT.setTextColor( Color.BLUE );
                EditText inputTopicS = (EditText) findViewById(R.id.mqtt_topicS);
                inputTopicS.setText("$SYS/#");
                subscribeToTopic();
            }
        });

        Button conn = (Button) findViewById(R.id.connectB);
        conn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                InputMethodManager inputManager = (InputMethodManager)
                        getSystemService(Context.INPUT_METHOD_SERVICE);
                inputManager.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(),
                        InputMethodManager.HIDE_NOT_ALWAYS);
                EditText inputTxt = (EditText) findViewById(R.id.mqtt_server);
                String typedText = inputTxt.getText().toString();
                serverUri = typedText;
                connectToServer();
            }
        });

        Button disconn = (Button) findViewById(R.id.disconnectB);
        disconn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                InputMethodManager inputManager = (InputMethodManager)
                        getSystemService(Context.INPUT_METHOD_SERVICE);
                inputManager.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(),
                        InputMethodManager.HIDE_NOT_ALWAYS);
                try {
                    if(mqttAndroidClient != null) {
                        for (String s : mysubsList) {
                            mqttAndroidClient.unsubscribe(s);
                        }
                        mysubsList.clear();
                        mqttAndroidClient.disconnect();
                    }
                    mqttAndroidClient = null;
                    addToHistory("Disconnecting from : " + serverUri);
                } catch (MqttException e) {
                    e.printStackTrace();
                }
            }
        });

        Button subs = (Button) findViewById(R.id.subscribeB);
        subs.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                InputMethodManager inputManager = (InputMethodManager)
                        getSystemService(Context.INPUT_METHOD_SERVICE);
                inputManager.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(),
                        InputMethodManager.HIDE_NOT_ALWAYS);

                subscribeToTopic();
            }
        });

        Button mysubs = (Button) findViewById(R.id.mySubsB);
        mysubs.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                addToHistory("My subscriptions: ");
                InputMethodManager inputManager = (InputMethodManager)
                        getSystemService(Context.INPUT_METHOD_SERVICE);
                inputManager.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(),
                        InputMethodManager.HIDE_NOT_ALWAYS);
                for (String s : mysubsList){
                    addToHistory(s);
                }
            }
        });

        mRecyclerView = (RecyclerView) findViewById(R.id.history_recycler_view);
        RecyclerView.LayoutManager mLayoutManager = new LinearLayoutManager(this);
        mRecyclerView.setLayoutManager(mLayoutManager);

        mAdapter = new HistoryAdapter(new ArrayList<String>());
        mRecyclerView.setAdapter(mAdapter);
    }

    public void updateConnectionButton() {
        final Button stat = (Button) findViewById(R.id.button1);
        if (connected)
            stat.setBackgroundColor(Color.GREEN);
        else
            stat.setBackgroundColor(Color.RED);
    }

    public void connectToServer() {
        // If the client id is not unique, the earlier subscription in the DB file is used
        // and the client recvd messages not subscribed to in the callback below
        EditText cidT = (EditText) findViewById(R.id.clientIDUI);
        String cid = cidT.getText().toString();
        if (cid.toString().isEmpty()) {
            clientId = "AndroidClient" + System.currentTimeMillis();
            addToHistory("Client ID is null, Using: " + clientId);
        } else {
            // When the Client ID changes, we need to clear the subscriptions
            clientId = cid;
            addToHistory("Client ID is not null, Using: " + clientId);
            mysubsList.clear();
        }
        mqttAndroidClient = new MqttAndroidClient(getApplicationContext(), serverUri, clientId);
        mqttAndroidClient.setCallback(new MqttCallbackExtended() {
            @Override
            public void connectComplete(boolean reconnect, String s) {
                System.out.println("Connection completed: ");
                connected = true; updateConnectionButton();
                if (reconnect) {
                    addToHistory(clientId + " Reconnected to : " + serverUri);
                    // Because Clean Session is true, we need to re-subscribe
                    //subscribeToTopic();
                } else {
                    addToHistory(clientId + " Connected to: " + serverUri);
                }
            }

            @Override
            public void connectionLost(Throwable throwable) {
                System.out.println("Connection lost: ");
                addToHistory("The Connection was lost.");
                connected = false; updateConnectionButton();
                // We clear our DB. But, if clean was false, then, this would still be present at the server
                // side, which gives an incorrect picture at our side. Maybe, we clean only, if clean if true ?
                // TBD
                mysubsList.clear();
            }

            @Override
            public void messageArrived(String s, MqttMessage mqttMessage) throws Exception {
                System.out.println("Incoming Msg...: " + new String(mqttMessage.getPayload()));
                addToHistory("Incoming message...: " + new String(mqttMessage.getPayload()));
            }

            @Override
            public void deliveryComplete(IMqttDeliveryToken iMqttDeliveryToken) {

            }
        });

        MqttConnectOptions mqttConnectOptions = new MqttConnectOptions();
        mqttConnectOptions.setAutomaticReconnect(true);
        mqttConnectOptions.setCleanSession(cleanVar);

        try {
            mqttAndroidClient.connect(mqttConnectOptions, null, new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken iMqttToken) {
                    System.out.println("Connected to : " + serverUri);
                    addToHistory("Connected to: " + serverUri);
                    DisconnectedBufferOptions disconnectedBufferOptions = new DisconnectedBufferOptions();
                    disconnectedBufferOptions.setBufferEnabled(true);
                    disconnectedBufferOptions.setBufferSize(100);
                    disconnectedBufferOptions.setPersistBuffer(false);
                    disconnectedBufferOptions.setDeleteOldestMessages(false);
                    mqttAndroidClient.setBufferOpts(disconnectedBufferOptions);
                    connected = true; updateConnectionButton();
                    //subscribeToTopic();
                }

                @Override
                public void onFailure(IMqttToken iMqttToken, Throwable throwable) {
                    System.err.println("Failed to connect to : " + serverUri);
                    addToHistory("Failed to connect to: " + serverUri);
                    connected = false; updateConnectionButton();
                }
            });
        } catch (MqttException ex) {
            ex.printStackTrace();
        }
    }

    private void addToHistory(String mainText){
        String format = simpleDateFormat.format(System.currentTimeMillis());

        System.out.println("LOG: " + mainText);
        mAdapter.add(format + ": " + mainText);
        mRecyclerView.smoothScrollToPosition(mAdapter.getItemCount());
        /* Snackbar.make(findViewById(android.R.id.content), mainText, Snackbar.LENGTH_LONG)
                .setAction("Action", null).show();
        */
    }

    public void subscribeToTopic() {
        EditText inputTopicS = (EditText) findViewById(R.id.mqtt_topicS);
        String typedTopicS = inputTopicS.getText().toString();
        subscriptionTopic = typedTopicS;
        if (mqttAndroidClient == null) {
            addToHistory(clientId + " Not connected to server");
            return;
        }
        // Check if already subscribed to the same topic
        if (mysubsList.contains(subscriptionTopic)) {
            addToHistory("Already subscribed to: " + subscriptionTopic);
            return;
        }
        mysubsList.add(subscriptionTopic); //this adds an element to the list.

        try {
            mqttAndroidClient.subscribe(subscriptionTopic, 0, null, new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken iMqttToken) {
                    System.out.println(clientId + " Successfully subscribed to topic : " + subscriptionTopic);
                    addToHistory(clientId + " Subscribed to : " + subscriptionTopic);
                }

                @Override
                public void onFailure(IMqttToken iMqttToken, Throwable throwable) {
                    System.err.println(clientId + " Failed to subscribe to topic : " + subscriptionTopic);
                    addToHistory(clientId + " Subscription Failure: " + subscriptionTopic);
                }
            });

            mqttAndroidClient.subscribe(subscriptionTopic, 0, new IMqttMessageListener() {
                @Override
                public void messageArrived(String s, MqttMessage mqttMessage) throws Exception {
                    String msg = new String(mqttMessage.getPayload());
                    addToHistory("Message Recvd: " + s + ":" + msg);
                    System.out.println("Message Recvd on Topic: " + s + ":" + msg);
                }
            });
        } catch (MqttException ex) {
            System.err.println("Exception whilst subscribing");
            ex.printStackTrace();
        }
    }

    public void publishMessage() {
        try {
            if (mqttAndroidClient == null) {
                addToHistory("Not connected to server");
                return;
            }
            EditText inputTopicP = (EditText) findViewById(R.id.mqtt_topicP);
            String typedTopicP = inputTopicP.getText().toString();
            publishTopic = typedTopicP;
            System.out.println("Publishing Message: ");
            addToHistory(clientId + " Publishing Message: " + publishMessage + "on Topic: " + publishTopic);
            MqttMessage message = new MqttMessage();
            message.setPayload(publishMessage.getBytes());
            mqttAndroidClient.publish(publishTopic, message);

        } catch (MqttException e) {
            System.err.println("Error publishing: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        /*setContentView(R.layout.tool_activity);
        cl = (RadioButton) findViewById(R.id.cleanBUI);
        rl = (RadioButton) findViewById(R.id.retainBUI);
        cl.setOnCheckedChangeListener(this);
        rl.setOnCheckedChangeListener(this);
        */
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
