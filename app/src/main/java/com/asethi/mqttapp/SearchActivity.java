package com.asethi.mqttapp;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.StrictMode;
import android.support.v7.app.AppCompatActivity;
import android.text.format.Formatter;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.concurrent.TimeoutException;

import static android.R.attr.value;
import static android.net.ConnectivityManager.TYPE_MOBILE;
import static android.net.ConnectivityManager.TYPE_WIFI;

import org.xbill.DNS.DClass;
import org.xbill.DNS.Lookup;
import org.xbill.DNS.Record;
import org.xbill.DNS.Resolver;
import org.xbill.DNS.SRVRecord;
import org.xbill.DNS.SimpleResolver;
import org.xbill.DNS.TXTRecord;
import org.xbill.DNS.TextParseException;
import org.xbill.DNS.Type;

/**
 * Created by ASethi on 20-08-2017.
 */

public class SearchActivity extends AppCompatActivity {
    TextView ipaddressF, hostsF;
    private ProgressBar progress;
    String selfIP = "0.0.0.0";
    String subnetScan = "192.168.1.";
    ArrayList<String> subnetList;
    private Handler mHandler = new Handler();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Use the following 2 lines - else use async threads
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);

        setContentView(R.layout.search_activity);
        ipaddressF = (TextView) findViewById(R.id.ipaddress);
        hostsF = (TextView) findViewById(R.id.hosts);
        ipaddressF.setText(null);
        progress = (ProgressBar) findViewById(R.id.progressBar1);
        //make the progress bar visible
        progress.setVisibility(View.VISIBLE);

        // Initialize the self-ip and subnet scan to relevant ip addresses.
        getIP(getApplicationContext());
        EditText inputTxt = (EditText) findViewById(R.id.subnetScanUI);
        inputTxt.setText(selfIP.substring(0, selfIP.lastIndexOf('.')+1));
        System.out.println("Aseem:" + selfIP.substring(0, selfIP.lastIndexOf('.')+1));
        subnetScan = selfIP.substring(0, selfIP.lastIndexOf('.')+1);

        //dnsLookup();

        Button search = (Button) findViewById(R.id.searchStartB);
        search.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                InputMethodManager inputManager = (InputMethodManager)
                        getSystemService(Context.INPUT_METHOD_SERVICE);
                inputManager.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(),
                        InputMethodManager.HIDE_NOT_ALWAYS);

                EditText inputTxt = (EditText) findViewById(R.id.subnetScanUI);
                String typedText = inputTxt.getText().toString();
                subnetScan = typedText;
                EditText inputTxt1 = (EditText) findViewById(R.id.subnetRangeUI);
                final int subnetRange = Integer.parseInt( inputTxt1.getText().toString() );

                boolean cont = getIP(getApplicationContext());
                if (cont == false) return;
                Runnable runnable = new Runnable() {
                    @Override public void run() {
                        subnetList = scanSubNet(subnetScan, subnetRange);
                    }
                };
                new Thread(runnable).start();
            }
        });
        // Wait till thread above completes
        // hostsF.setText(subnetList.toString());
    }

    @Override
    public boolean onCreateOptionsMenu (Menu menu){
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    public boolean getIP(Context context) {
        int[] networkTypes = {TYPE_MOBILE,
                TYPE_WIFI};
        try {
            ConnectivityManager connectivityManager =
                    (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            for (int networkType : networkTypes) {
                NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
                if (activeNetworkInfo != null &&
                        activeNetworkInfo.getType() == networkType) {
                    Toast.makeText(context, connectivityManager.getActiveNetworkInfo().getTypeName(), Toast.LENGTH_LONG).show();
                    System.out.println("Connection State: " + connectivityManager.getActiveNetworkInfo().getDetailedState()); // Connected ?
                    System.out.println("SSID: " + connectivityManager.getActiveNetworkInfo().getExtraInfo()); // SSID
                    System.out.println("TypeName: " + connectivityManager.getActiveNetworkInfo().getTypeName()); //WiFi
                }
            }
            if (connectivityManager.getActiveNetworkInfo().getTypeName().contains("WIFI")) {
                WifiManager myWifiManager = (WifiManager) context.getSystemService(WIFI_SERVICE);
                WifiInfo myWifiInfo = myWifiManager.getConnectionInfo();
                int ipAddress = myWifiInfo.getIpAddress();
                System.out.println("WiFi address: " + Formatter.formatIpAddress(ipAddress));
                String ip = String.format("%d.%d.%d.%d", (ipAddress & 0xff),(ipAddress >> 8 & 0xff),(ipAddress >> 16 & 0xff),(ipAddress >> 24 & 0xff));
                System.out.println("WiFi address: " + ip);
                ipaddressF.setText(ip);
                selfIP = ip;
                return true;
            } else {
                ipaddressF.setText("0.0.0.0");
                Toast.makeText(context, "Need WiFi Connection", Toast.LENGTH_LONG).show();
                return false;
            }
        } catch (Exception e) {
            return false;
        }
    }

    private static boolean isReachable(String addr, int openPort, int timeOutMillis) {
        // Any Open port on other machine
        // openPort =  22 - ssh, 80 or 443 - webserver, 25 - mailserver etc.
        try {
            Socket soc = new Socket();
            soc.connect(new InetSocketAddress(addr, openPort), timeOutMillis);
            System.out.println("Reachable addr: " + addr);
            return true;
        } catch (IOException ex) {
            return false;
        }
    }

    private ArrayList<String> scanSubNet(String subnet, int subnetRange){
        final ArrayList<String> hosts = new ArrayList<String>();
        InetAddress inetAddress = null;
        progress.setMax(subnetRange);
        for(int i=1; i<=subnetRange; i++) {
            final int val = i;
            System.out.println("Trying: " + subnet + String.valueOf(i));
            mHandler.post(new Runnable() {  // or progress.post
                @Override
                public void run() {
                    System.out.println("val: " + val);
                    progress.setProgress(val);
                    if (hosts != null)
                        hostsF.setText(hosts.toString());
                }
            });
            try {
                inetAddress = InetAddress.getByName(subnet + String.valueOf(i));
                if (inetAddress.isReachable(1000)) {
                    hosts.add(inetAddress.getHostName());
                    System.out.println("Found Device using Ping: " + inetAddress.getHostName());
                } else if (isReachable(subnet + String.valueOf(i), 80, 1000)) {
                    hosts.add(inetAddress.getHostName());
                    System.out.println("Found Device using Telnet:80: " + inetAddress.getHostName());
                } else if (isReachable(subnet + String.valueOf(i), 22, 1000)) {
                    hosts.add(inetAddress.getHostName());
                    System.out.println("Found Device using SSH:80: " + inetAddress.getHostName());
                }
            } catch (UnknownHostException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return hosts;
    }
        @Override
        public boolean onOptionsItemSelected (MenuItem item){
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

        // Not working
        public void dnsLookup() {
            String serviceName = "google.com";
            Lookup lookup = null;
            try {
                //lookup = new Lookup(serviceName, Type.CNAME, DClass.ANY);
                lookup = new Lookup(serviceName, Type.CNAME);
            } catch (TextParseException e) {
                e.printStackTrace();
            }
            Resolver resolver = null;
            try {
                resolver = new SimpleResolver("8.8.8.8");
            } catch (UnknownHostException e) {
                e.printStackTrace();
            }

            lookup.setResolver(resolver);
            lookup.setCache(null);
            Record[] records = lookup.run();
            if (lookup.getResult() == Lookup.SUCCESSFUL) {
                String responseMessage = null;
                String listingType = null;
                for (int i = 0; i < records.length; i++) {
                    if (records[i] instanceof SRVRecord) {
                        listingType = ((SRVRecord) records[i]).toString();
                    }
                }
                System.out.println("Found!");
                System.out.println("Response Message: " + responseMessage);
                System.out.println("Listing type: " + listingType);
            } else if (lookup.getResult() == Lookup.HOST_NOT_FOUND) {
                System.out.println("Not found.");
            } else if (lookup.getResult() == Lookup.TRY_AGAIN) {
                System.out.println("Error!1" + lookup.getErrorString());
            }  else if (lookup.getResult() == Lookup.TYPE_NOT_FOUND) {
                System.out.println("Error!2");
            } else if (lookup.getResult() == Lookup.UNRECOVERABLE) {
                System.out.println("Error!3");
            }
        }

    }