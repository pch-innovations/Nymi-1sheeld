/**
 *   Copyright 2016 Nymi Inc.
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */

package com.nymi.nymireferenceapp;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.view.Menu;
import android.view.MenuInflater;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.google.gson.Gson;
import com.nymi.api.NymiAdapter;
import com.nymi.api.NymiConfigurationInfo;
import com.nymi.api.NymiInfo;
import com.nymi.api.NymiProvision;
import com.nymi.api.NymiDeviceInfo;
import com.nymi.api.NymiRandomNumber;
import com.nymi.api.NymiPublicKey;
import com.nymi.api.NymiSymmetricKey;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.BitSet;

import com.integreight.onesheeld.sdk.*;


public class MainActivity extends Activity {

    private final String LOG_TAG = getClass().getSimpleName();

    private static final int LEDS_NUMBER = 5;

    private NymiAdapter mNymiAdapter;

    private AdapterProvisions mAdapterProvisions;
    private Switch mSwitchDiscovery;
    private ListView mListViewProvisions;

    private RadioButton mLeds[];
    private RadioButton NymiAuthenticated;

    private Button mButtonAccept;
    private Button mButtonReject;

    private static final String TOTP_SAMPLE_KEY = "48656c6c6f21deadbeef";
    private static final String NEA_NAME = "NymiDoorControl";

    private Handler uiThreadHandler = new Handler();
    private final static int MY_PERMISSIONS_REQUEST_ACCESS_COARSE_LOCATION = 1234;
    private boolean hasBluetoothPermission = false;
    private OneSheeldManager oneSheeldManager;
    OneSheeldDevice oneSheeldDevice;
    private boolean pin13value = true;

    private byte pushButtonShieldId = OneSheeldSdk.getKnownShields().PUSH_BUTTON_SHIELD.getId();
    private byte pushButtonFunctionId = (byte) 0x01;
    private byte keyPadShieldid = OneSheeldSdk.getKnownShields().KEYPAD_SHIELD.getId();
    private byte keyPadFunctionId = (byte) 0x01;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mSwitchDiscovery = (Switch) findViewById(R.id.layout_main_discovery_switch);

        mLeds = new RadioButton[LEDS_NUMBER];
        mLeds[0] = (RadioButton) findViewById(R.id.layout_main_led0);
        mLeds[1] = (RadioButton) findViewById(R.id.layout_main_led1);
        mLeds[2] = (RadioButton) findViewById(R.id.layout_main_led2);
        mLeds[3] = (RadioButton) findViewById(R.id.layout_main_led3);
        mLeds[4] = (RadioButton) findViewById(R.id.layout_main_led4);

        mButtonAccept = (Button) findViewById(R.id.layout_main_button_accept);
        mButtonReject = (Button) findViewById(R.id.layout_main_button_reject);
        mListViewProvisions = (ListView) findViewById(R.id.layout_main_provision_list);

        NymiAuthenticated = (RadioButton) findViewById(R.id.nymiFoundStatusAutheticated);


        // -------------------- 1Sheeld setup ------------------------



        Button btnSendToOneSheeld = (Button) findViewById(R.id.btnSendToOneSheeld);
        btnSendToOneSheeld.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // oneSheeldDevice.digitalWrite(13, pin13value);
                //

                ShieldFrame sf = new ShieldFrame(pushButtonShieldId, pushButtonFunctionId);
                sf.addArgument(pin13value);
                pin13value = !pin13value;
                oneSheeldDevice.sendShieldFrame(sf);


            }
        });

        //toggle.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
        //    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {

        ToggleButton btnkeypadtry = (ToggleButton) findViewById(R.id.btnkeypadtry);
        btnkeypadtry.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {

                byte rowByte = 0, columnByte = 0;
                int row =0;
                int column =1;

                ShieldFrame kp = new ShieldFrame(keyPadShieldid, keyPadFunctionId);
                if (isChecked) {

                    rowByte = BitsUtils.setBit(rowByte, row);
                    columnByte = BitsUtils.setBit(columnByte,column);
                    kp.addArgument(rowByte);
                    kp.addArgument(columnByte);
                } else {
                    rowByte = BitsUtils.resetBit(rowByte, row);
                    columnByte = BitsUtils.resetBit(columnByte,column);
                    kp.addArgument(rowByte);
                    kp.addArgument(columnByte);
                }
                oneSheeldDevice.sendShieldFrame(kp);
            }
        });

        ToggleButton btnkeypadtry2 = (ToggleButton) findViewById(R.id.btnkeypadtry2);
        btnkeypadtry2.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {

                byte rowByte = 0, columnByte = 0;
                int row =1;
                int column =0;

                ShieldFrame kp = new ShieldFrame(keyPadShieldid, keyPadFunctionId);
                if (isChecked) {

                    rowByte = BitsUtils.setBit(rowByte, row);
                    columnByte = BitsUtils.setBit(columnByte,column);
                    kp.addArgument(rowByte);
                    kp.addArgument(columnByte);
                } else {
                    rowByte = BitsUtils.resetBit(rowByte, row);
                    columnByte = BitsUtils.resetBit(columnByte,column);
                    kp.addArgument(rowByte);
                    kp.addArgument(columnByte);
                }
                oneSheeldDevice.sendShieldFrame(kp);
            }
        });


        // Init the SDK with context
        OneSheeldSdk.init(this);
        // Optional, enable debugging messages.
        OneSheeldSdk.setDebugging(true);

        // Get the manager instance
        oneSheeldManager = OneSheeldSdk.getManager();
        oneSheeldManager.setConnectionRetryCount(1);
        oneSheeldManager.setScanningTimeOut(20);
        oneSheeldManager.setAutomaticConnectingRetriesForClassicConnections(true);

        // Construct a new OneSheeldScanningCallback callback and override onDeviceFind method
        OneSheeldScanningCallback scanningCallback = new OneSheeldScanningCallback() {
            @Override
            public void onDeviceFind(OneSheeldDevice device) {
                // Cancel scanning before connecting
                OneSheeldSdk.getManager().cancelScanning();
                // Connect to the found device
                oneSheeldDevice = device;
                oneSheeldDevice.connect();
            }
        };

        // Construct a new OneSheeldConnectionCallback callback and override onConnect method
        OneSheeldConnectionCallback connectionCallback = new OneSheeldConnectionCallback() {
            @Override
            public void onConnect(OneSheeldDevice device) {
                uiThreadHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(MainActivity.this, "1sheeld connected", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        };

        OneSheeldErrorCallback errorCallback = new OneSheeldErrorCallback() {
            @Override
            public void onError(final OneSheeldDevice device, final OneSheeldError error) {
                uiThreadHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(MainActivity.this, "Error: " + error.toString() + (device != null ? " in " + device.getName() : ""), Toast.LENGTH_SHORT).show();
                    }
                });
            }
        };

        // Add the connection and scanning callbacks
        oneSheeldManager.addConnectionCallback(connectionCallback);
        oneSheeldManager.addScanningCallback(scanningCallback);
        oneSheeldManager.addErrorCallback(errorCallback);

        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},
                    MY_PERMISSIONS_REQUEST_ACCESS_COARSE_LOCATION);
        } else {
            hasBluetoothPermission = true;
        }


        if (hasBluetoothPermission) {
            oneSheeldManager.scan();
        } else {
            uiThreadHandler.post(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(MainActivity.this, "Error: Permission not granted", Toast.LENGTH_SHORT).show();
                }
            });
        }

        // -------------------- Nymi setup -------------------------

        mAdapterProvisions = new AdapterProvisions(this);

        /** Step 1 - Get instance of the Nymi Adapter*/
        mNymiAdapter = NymiAdapter.getDefaultAdapter();

        if (!mNymiAdapter.isInitialized()) {

            //NOTE: we cannot always use BuildConfig.LOCAL_IP to set the IP since the host can have
            //a few network interfaces, and the interface could be bound to more than one IP address.
            // in typical dev environments, the nymulator will be run on the
            // same machine as your build machine, so this is a sensible default.
            // If that's not the case for you, update this host field.

            /** Step 2 - Set the Nymulator ip */
            String nymulatorHost = BuildConfig.LOCAL_IP;
            mNymiAdapter.setNymulator(nymulatorHost);

            /** Step 3 - Initialize the Nymi adapter */
            //Context supplied to NymiAdapter here will be preserved throughout the lifetime of the app
            mNymiAdapter.init(ApplicationContextProvider.getContext(),
                    NEA_NAME, new NymiAdapter.InitCallback() {
                        @Override
                        public void onInitResult(int status, String message) {
                            Toast.makeText(MainActivity.this, message, Toast.LENGTH_SHORT).show();
                            if (status == NymiAdapter.InitCallback.INIT_SUCCESS) {

                                /** Step 4 - Get the provisions (Provisions are available only if the Adapter has been initialized) */
                                updateProvisions();

                                /** Step 5 - Set callbacks to receive agreement and new provision events */
                                setAreementAndProvisionCallbacks();

                                /** Step 6 - Start provisioning Nymi bands nearby*/
                                mNymiAdapter.startProvision();

                                //discovery enable state can be changed only if NymiAdapter has been initialized
                                mSwitchDiscovery.setEnabled(true);

                            } else if (status == NymiAdapter.InitCallback.INIT_FAIL) {
                                Toast.makeText(MainActivity.this, "Init failed", Toast.LENGTH_SHORT).show();
                                finish();
                            }
                        }
                    });

        } else {
            /** Step 4 - Get the provisions (Provisions are available only if the Adapter has been initialized) */
            updateProvisions();

            /** Step 5 - Set callbacks to receive agreement and new provision events */
            setAreementAndProvisionCallbacks();

            //discovery enable state can be changed only if NymiAdapter has been initialized
            mSwitchDiscovery.setEnabled(true);
        }

        /** Step 7 - Update the UI with the provisions */
        mListViewProvisions.setAdapter(mAdapterProvisions);

        /** Step 8 - Set notifications callbacks */
        setNotifications();

        /** Step 9 - Init UI  */
        mSwitchDiscovery.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean isChecked) {
                mSwitchDiscovery.setEnabled(false);
                mNymiAdapter.setDiscoveryEnabled(isChecked, new NymiAdapter.DiscoveryModeChangeCallback() {
                    @Override
                    public void onDiscoveryModeChange(int status) {
                        mSwitchDiscovery.setEnabled(true);
                        if (status == NYMI_DISCOVERY_CHANGE_ERROR) {
                            Toast.makeText(MainActivity.this, "Error changing discovery mode", Toast.LENGTH_LONG).show();
                        } else {
                            mSwitchDiscovery.setChecked(status == NYMI_DISCOVERY_ON);
                        }
                    }
                });
            }
        });

        mButtonAccept.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                BitSet bitSet = new BitSet(LEDS_NUMBER);
                bitSet.clear();
                for (int i = 0; i < LEDS_NUMBER; i++) {
                    if (mLeds[i].isChecked()) {
                        bitSet.set(i);
                    }
                    mLeds[i].setChecked(false);
                    mLeds[i].setEnabled(false);
                }

                // Calling setPattern is your application's way of saying
                // "Yes, I really want to provision with this band."
                // After setPattern, you'll get a callback with a NymiProvision
                // instance with which your application can interact.
                mNymiAdapter.setPattern(bitSet);
                mButtonAccept.setEnabled(false);
                mButtonReject.setEnabled(false);
            }
        });

        mButtonReject.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                for (int i = 0; i < LEDS_NUMBER; i++) {
                    mLeds[i].setChecked(false);
                    mLeds[i].setEnabled(false);
                }
                mButtonAccept.setEnabled(false);
                mButtonReject.setEnabled(false);
                mNymiAdapter.rejectAgreement();
                Toast.makeText(MainActivity.this, "Device rejected", Toast.LENGTH_SHORT).show();
            }
        });

        mListViewProvisions.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, final int position, long id) {
                PopupMenu popup = new PopupMenu(MainActivity.this, view);
                popup.getMenuInflater().inflate(R.menu.popup_menu, popup.getMenu());
                popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                    public boolean onMenuItemClick(MenuItem item) {
                        switch (item.getItemId()) {
                            case R.id.popup_menu_notify_positive:
                                ((NymiProvision) mAdapterProvisions.getItem(position)).sendNotification(NymiProvision.Notification.NOTIFICATION_POSITIVE, new NymiProvision.NotificationCallback() {
                                    @Override
                                    public void onNotificationResult(int status, NymiProvision.Notification nymiNotification) {
                                        if (status == NymiProvision.NotificationCallback.NOTIFICATION_SUCCESS) {
                                            Toast.makeText(MainActivity.this, nymiNotification.toString() + " notification completed", Toast.LENGTH_SHORT).show();
                                        } else {
                                            Toast.makeText(MainActivity.this, "Notification failed", Toast.LENGTH_SHORT).show();
                                        }
                                    }
                                });
                                break;
                            case R.id.popup_menu_notify_negative:
                                ((NymiProvision) mAdapterProvisions.getItem(position)).sendNotification(NymiProvision.Notification.NOTIFICATION_NEGATIVE, new NymiProvision.NotificationCallback() {
                                    @Override
                                    public void onNotificationResult(int status, NymiProvision.Notification nymiNotification) {
                                        if (status == NymiProvision.NotificationCallback.NOTIFICATION_SUCCESS) {
                                            Toast.makeText(MainActivity.this, nymiNotification.toString() + " notification completed", Toast.LENGTH_SHORT).show();
                                        } else {
                                            Toast.makeText(MainActivity.this, "Notification failed", Toast.LENGTH_SHORT).show();
                                        }
                                    }
                                });
                                break;
                            case R.id.popup_menu_get_random:
                                ((NymiProvision) mAdapterProvisions.getItem(position)).getRandom(new NymiProvision.RandomCallback() {
                                    @Override
                                    public void onRandomResult(int status, NymiRandomNumber nymiRandomNumber) {
                                        if (status == NymiProvision.RandomCallback.RANDOM_SUCCESS) {
                                            // Of course a real application will want to make use of this value otherwise,
                                            // but this is to demonstrate flow.
                                            Toast.makeText(MainActivity.this, "Obtained random: " + nymiRandomNumber.toString(), Toast.LENGTH_SHORT).show();
                                        } else {
                                            Toast.makeText(MainActivity.this, "Random failed", Toast.LENGTH_SHORT).show();
                                        }
                                    }
                                });
                                break;
                            case R.id.popup_menu_sign:
                                if (null != mAdapterProvisions.getItem(position)) {
                                    ((NymiProvision) mAdapterProvisions.getItem(position)).sign("Message to be signed",
                                            new NymiProvision.SignCallback() {
                                                @Override
                                                public void onMessageSigned(int status, String algorithm, String sig, String key) {
                                                    if (status == NymiProvision.SignCallback.SIGN_LOCAL_SUCCESS) {
                                                        // Of course your code will want to make use of this value otherwise.
                                                        // This code intends to demonstrate flow.
                                                        Toast.makeText(MainActivity.this, "Sign (on a dummy message) returned: " + sig, Toast.LENGTH_SHORT).show();
                                                    } else {
                                                        Toast.makeText(MainActivity.this, "Sign failed", Toast.LENGTH_SHORT).show();
                                                    }
                                                }
                                            });

                                } else {
                                    Toast.makeText(MainActivity.this, "Error retrieving keys", Toast.LENGTH_SHORT).show();
                                }
                                break;

                            case R.id.popup_menu_pair_with_partner:
                                ((NymiProvision) mAdapterProvisions.getItem(position)).addPartner(Constants.PARTNER_PUBLIC_KEY,
                                        new NymiProvision.PartnerAddedCallback() {
                                            @Override
                                            public void onPartnerAdded(int status, final NymiPublicKey key) {
                                                Toast.makeText(MainActivity.this, "partner added keyId=" + key.getId(), Toast.LENGTH_SHORT).show();
                                                new Thread() {
                                                    @Override
                                                    public void run() {
                                                        try {
                                                            URL url = new URL("http://" + Constants.PARTNER_HOST + ":" + Constants.PARTNER_PORT +
                                                                    "/signup/" + key.getId() + "/" + key.getKey() + "/" + Constants.USER_ID + "/" + Constants.WIEGAND);

                                                            URLConnection urlConnection = url.openConnection();
                                                            InputStream in = new BufferedInputStream(urlConnection.getInputStream());
                                                            try {
                                                                BufferedReader reader = new BufferedReader(
                                                                        new InputStreamReader(in));

                                                                Gson gson = new Gson();
                                                                GServerSignupResponse response = gson.fromJson(reader, GServerSignupResponse.class);

                                                                reader.close();
                                                                if (response != null &&
                                                                        response.Ok != null &&
                                                                        response.Ok.equals("yes")) {
                                                                    Toast.makeText(MainActivity.this, "Registered user " + Constants.USER_ID, Toast.LENGTH_SHORT).show();
                                                                } else {
                                                                    Toast.makeText(MainActivity.this, "Failed to register user " + Constants.USER_ID, Toast.LENGTH_SHORT).show();
                                                                }
                                                            } finally {
                                                                in.close();
                                                            }
                                                        } catch (Exception e) {
                                                            Log.e(LOG_TAG, e.toString());
                                                        }
                                                    }
                                                }.start();
                                            }
                                        });
                                break;

                            case R.id.popup_menu_get_symmetric_key:
                                ((NymiProvision) mAdapterProvisions.getItem(position)).getSymmetricKey(true, new NymiProvision.SymmetricKeyCallback() {
                                    @Override
                                    public void onNymiSymmetricKeyResult(int status, NymiSymmetricKey nymiSymmetricKey) {
                                        if (status == SYMMETRIC_KEY_SUCCESS) {
                                            Toast.makeText(MainActivity.this, "Got symmetric key id: "
                                                    + nymiSymmetricKey.getId() + " value: " + nymiSymmetricKey.getKey(), Toast.LENGTH_SHORT).show();
                                        } else {
                                            Toast.makeText(MainActivity.this, "Symmetric key failed", Toast.LENGTH_SHORT).show();
                                        }
                                    }
                                });

                                break;
                            case R.id.popup_menu_set_totp_key:
                                ((NymiProvision) mAdapterProvisions.getItem(position)).setTotpKey(TOTP_SAMPLE_KEY, true, new NymiProvision.TotpSetKeyCallback() {
                                    @Override
                                    public void onTotpKeySet(int status) {
                                        if (status == TOTP_SET_KEY_SUCCESS) {
                                            Toast.makeText(MainActivity.this, "Set TOTP key succeeded", Toast.LENGTH_SHORT).show();
                                        } else {
                                            Toast.makeText(MainActivity.this, "Get TOTP failed", Toast.LENGTH_SHORT).show();
                                        }
                                    }
                                });

                                break;
                            case R.id.popup_menu_get_totp:
                                ((NymiProvision) mAdapterProvisions.getItem(position)).getTotp(new NymiProvision.TotpGetCallback() {
                                    @Override
                                    public void onTotpGet(int status, String totp) {
                                        if (status == TOTP_GET_SUCCESS) {
                                            Toast.makeText(MainActivity.this, "Got TOTP: " + totp, Toast.LENGTH_SHORT).show();
                                        } else if (status == TOTP_GET_REFUSED) {
                                            Toast.makeText(MainActivity.this, "Get TOTP failed. Have you set TOTP key?", Toast.LENGTH_LONG).show();
                                        } else {
                                            Toast.makeText(MainActivity.this, "Get TOTP failed", Toast.LENGTH_LONG).show();
                                        }
                                    }
                                });

                                break;
                            case R.id.popup_menu_device_info:
                                ((NymiProvision) mAdapterProvisions.getItem(position)).getDeviceInfo(
                                        new NymiProvision.DeviceInfoCallback() {
                                            @Override
                                            public void onDeviceInfo(int status, final NymiDeviceInfo info) {
                                                if (status == DEVICE_INFO_SUCCESS) {
                                                    View infoView = inflateNymiDeviceInfo(info);
                                                    AlertDialog alertDialog = new AlertDialog.Builder(
                                                            MainActivity.this)
                                                            .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                                                                @Override
                                                                public void onClick(DialogInterface dialog, int i) {
                                                                    dialog.dismiss();
                                                                }
                                                            })
                                                            .setView(infoView)
                                                            .create();
                                                    alertDialog.show();
                                                } else {
                                                    Toast.makeText(MainActivity.this, "Get device info failed", Toast.LENGTH_LONG).show();
                                                }
                                            }
                                        });
                                break;

                            default:
                                break;
                        }
                        return true;
                    }
                });
                popup.show();
            }
        });
    }


    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_ACCESS_COARSE_LOCATION: {
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // permission was granted, yay!
                    hasBluetoothPermission = true;
                } else {
                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                }
                return;
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        /** Step 10 - Clear all callbacks: some might contain references to Views, this step is needed to avoid memory leaks */
        mNymiAdapter.clearCallbacks();
        oneSheeldManager.cancelScanning();
        oneSheeldManager.disconnectAll();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            // action with ID action_refresh was selected
            case R.id.action_info:
                if (mNymiAdapter.isInitialized()) {
                    mNymiAdapter.getInfo(new NymiAdapter.InfoCallback() {
                        @Override
                        public void onInfo(int status, NymiInfo info) {
                            if (status == INFO_SUCCESS) {
                                View infoView = inflateNymiInfo(info);
                                AlertDialog alertDialog = new AlertDialog.Builder(
                                        MainActivity.this)
                                        .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                                            @Override
                                            public void onClick(DialogInterface dialog, int i) {
                                                dialog.dismiss();
                                            }
                                        })
                                        .setView(infoView)
                                        .create();
                                alertDialog.show();
                            } else {
                                Toast.makeText(MainActivity.this, "error retrieving info", Toast.LENGTH_SHORT).show();
                            }

                        }
                    });
                }
                break;
            case R.id.action_1sheeld: {
                if (hasBluetoothPermission) {
                    oneSheeldManager.scan();
                } else {
                    uiThreadHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(MainActivity.this, "Error: Permission not granted", Toast.LENGTH_SHORT).show();
                        }
                    });
                }
                break;
            }
        }
        return true;
    }

    private void setNotifications() {
        mNymiAdapter.setDeviceApproachedCallback(new NymiAdapter.DeviceApproachedCallback() {
            @Override
            public void onDeviceApproached(String pid) {
                Log.d(LOG_TAG, "onDeviceApproached pid=" + pid);
            }
        });

        mNymiAdapter.setDeviceDetectedCallback(new NymiAdapter.DeviceDetectedCallback() {
            @Override
            public void onDeviceDetected(String pid,
                                         String nymibandNonce,
                                         boolean partnerVerified,
                                         boolean connectable,
                                         int RSSI_last,
                                         int RSSI_smoothed) {
                Log.d(LOG_TAG, "onDeviceDetected pid=" + pid +
                        " nonce=" + nymibandNonce +
                        " connectable=" + connectable +
                        " RSSI_last=" + RSSI_last +
                        " RSSI_smoothed=" + RSSI_smoothed);
            }
        });

        mNymiAdapter.setDeviceFoundCallback(new NymiAdapter.DeviceFoundCallback() {
            @Override
            public void onDeviceFound(String pid,
                                      String nymibandNonce,
                                      boolean connectable,
                                      int RSSI_last,
                                      int RSSI_smoothed,
                                      boolean strong) {
                Log.d(LOG_TAG, "onDeviceFound pid=" + pid +
                        " nonce=" + nymibandNonce +
                        " connectable=" + connectable +
                        " RSSI_last=" + RSSI_last +
                        " RSSI_smoothed=" + RSSI_smoothed +
                        " strong=" + strong);
            }
        });

        mNymiAdapter.setDeviceFoundStatusChangeCallback(new NymiAdapter.DeviceFoundStatusChangeCallback() {
            @Override
            public void onDeviceFoundStatusChange(String pid,
                                                  NymiDeviceInfo.FoundStatus before,
                                                  NymiDeviceInfo.FoundStatus after,
                                                  boolean partnerVerified
            ) {
                /// insert code here
                /*
                if (after == NymiDeviceInfo.FoundStatus.AUTHENTICATED) {
                    NymiAuthenticated.setEnabled(true);
                    NymiAuthenticated.setChecked(true);

                    byte rowByte = 0, columnByte = 0;
                    int column =0;
                    for(int row =0; row<2;row++) {

                        ShieldFrame kp = new ShieldFrame(keyPadShieldid, keyPadFunctionId);

                            rowByte = BitsUtils.setBit(rowByte, row);
                            columnByte = BitsUtils.setBit(columnByte, column);
                            kp.addArgument(rowByte);
                            kp.addArgument(columnByte);

                        oneSheeldDevice.sendShieldFrame(kp);
                    }

                } else {

                    byte rowByte = 0, columnByte = 0;
                    int column =0;
                    for(int row =0; row<2;row++) {

                        ShieldFrame kp = new ShieldFrame(keyPadShieldid, keyPadFunctionId);

                        rowByte = BitsUtils.resetBit(rowByte, row);
                        columnByte = BitsUtils.resetBit(columnByte, column);
                        kp.addArgument(rowByte);
                        kp.addArgument(columnByte);

                        oneSheeldDevice.sendShieldFrame(kp);
                    }

                    NymiAuthenticated.setEnabled(false);
                    NymiAuthenticated.setChecked(false);
                }
*/

                Log.d(LOG_TAG, "onDeviceFoundStatusChange pid=" + pid +
                        " before=" + before +
                        " after=" + after +
                        " partnerVerified=" + partnerVerified);
            }
        });

       // public static NymiDeviceInfo.FoundStatus valueOf (String name)

        mNymiAdapter.setDevicePresenceChangeCallback(new NymiAdapter.DevicePresenceChangeCallback() {
            @Override
            public void onDevicePresenceChange(String pid,
                                               NymiDeviceInfo.PresenceState before,
                                               NymiDeviceInfo.PresenceState after,
                                               boolean partnerVerified) {

                mAdapterProvisions.updateProvisionPresenceState(pid, after);

                if (after == NymiDeviceInfo.PresenceState.DEVICE_PRESENCE_YES) {

/*
                private void addNymiDeviceInfo(NymiDeviceInfo info) {

                    private NymiDeviceInfo.FoundStatus mFoundStatus;


                public NymiDeviceInfo.FoundStatus getFoundStatus () {
                    return this.mFoundStatus;
                }

                public String.valueOf(NymiDeviceInfo.getFoundStatus())


                public static NymiDeviceInfo.FoundStatus valueOf (String mNymiAdapter)
                if (valueOf == NymiDeviceInfo.FoundStatus.AUTHENTICATED) {
*/
                    NymiAuthenticated.setEnabled(true);
                    NymiAuthenticated.setChecked(true);

                    byte rowByte = 0, columnByte = 0;
                    int column = 0;
                    for (int row = 0; row < 2; row++) {

                        ShieldFrame kp = new ShieldFrame(keyPadShieldid, keyPadFunctionId);

                        rowByte = BitsUtils.setBit(rowByte, row);
                        columnByte = BitsUtils.setBit(columnByte, column);
                        kp.addArgument(rowByte);
                        kp.addArgument(columnByte);

                        oneSheeldDevice.sendShieldFrame(kp);
                    }

                } else {

                    byte rowByte = 0, columnByte = 0;
                    int column = 0;
                    for (int row = 0; row < 2; row++) {

                        ShieldFrame kp = new ShieldFrame(keyPadShieldid, keyPadFunctionId);

                        rowByte = BitsUtils.resetBit(rowByte, row);
                        columnByte = BitsUtils.resetBit(columnByte, column);
                        kp.addArgument(rowByte);
                        kp.addArgument(columnByte);

                        oneSheeldDevice.sendShieldFrame(kp);
                    }

                    NymiAuthenticated.setEnabled(false);
                    NymiAuthenticated.setChecked(false);
                }

                Log.d(LOG_TAG, "onDevicePresenceChange pid=" + pid +
                        " before=" + before +
                        " after=" + after +
                        " partnerVerified=" + partnerVerified);
                }


        });

        mNymiAdapter.setProximityEstimateChangeCallback(new NymiAdapter.ProximityEstimateChangeCallback() {
            @Override
            public void onDeviceProximityEstimateChange(String pid, NymiProvision.ProximityState before, NymiProvision.ProximityState after) {
                Log.d(LOG_TAG, "onDeviceProximityEstimateChange pid=" + pid + " before=" + before +
                        " after=" + after);
            }
        });

        mNymiAdapter.setFirmwareVersionCallback(new NymiAdapter.FirmwareVersionCallback() {
            @Override
            public void onFirmwareVersion(String pid, String fwVersion, int basicVersionCode, int imageCompatibilityCode, int nymibandVersion) {
                Log.d(LOG_TAG, "onFirmwareVersion pid=" + pid +
                        " fwVersion=" + fwVersion +
                        " basicVersionCode=" + basicVersionCode +
                        " imageCompatibilityCode=" + imageCompatibilityCode +
                        " nymiBandVersio=" + nymibandVersion);
            }
        });
    }

    private void setAreementAndProvisionCallbacks() {
        mNymiAdapter.setAgreementCallback(new NymiAdapter.AgreementCallback() {
            @Override
            public void onAgreement(BitSet pattern) {
                for (int i = 0; i < LEDS_NUMBER; i++) {
                    mLeds[i].setChecked(pattern.get(i));
                    mLeds[i].setEnabled(true);
                }
                mButtonAccept.setEnabled(true);
                mButtonReject.setEnabled(true);
            }
        });

        mNymiAdapter.setNewProvisionCallback(new NymiAdapter.NewProvisionCallback() {
            @Override
            public void onNewProvision(int status, NymiProvision provision) {
                if (status == NymiAdapter.NewProvisionCallback.PROVISION_SUCCESS) {
                    mAdapterProvisions.addProvision(provision);
                } else {
                    // Provisioning can fail due to connectivity problems.
                    // Unfortunately, your applications only recovery is to
                    // start the provisioning process over. You'll need to
                    // instruct the user to put their band back into provisioning mode.
                    Toast.makeText(MainActivity.this, "Error completing provision.", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void updateProvisions() {
        NymiAdapter.getDefaultAdapter().getProvisions(new NymiAdapter.GetProvisionsCallback() {
            @Override
            public void onGetProvisions(int status, ArrayList<NymiProvision> provisions) {
                if (status == GET_PROVISIONS_SUCCESS) {
                    mAdapterProvisions.setProvisions(provisions);
                    //Get presence state of each provision to update UI
                    for (final NymiProvision provision : provisions) {
                        provision.getDeviceInfo(new NymiProvision.DeviceInfoCallback() {
                            @Override
                            public void onDeviceInfo(int status, NymiDeviceInfo info) {
                                if (status == DEVICE_INFO_SUCCESS) {
                                    mAdapterProvisions.updateProvisionPresenceState(provision.getPid(), info.getPresenceState());
                                }
                            }
                        });
                    }
                }
            }
        });
    }

    private View inflateNymiInfo(NymiInfo nymiInfo) {
        View root = getLayoutInflater().inflate(R.layout.layout_dialog, null);
        LinearLayout container = (LinearLayout) root.findViewById(R.id.layout_dialog_main_container);

        addInfoHeader(container, "Devices");
        for (NymiDeviceInfo nymiDeviceInfo : nymiInfo.getDevicesInfo()) {
            addNymiDeviceInfo(container, nymiDeviceInfo);
        }

        addInfoHeader(container, "Provisions");

        if (nymiInfo.getProvisions() != null) {
            for (String provision : nymiInfo.getProvisions()) {
                addNymiProvisionInfo(container, provision, nymiInfo.getProvisionsPresent());
            }
        }

        addInfoHeader(container, "config");
        addNymiConfigurationInfo(container, nymiInfo.getConfigurationInfo());

        return root;
    }

    private View inflateNymiDeviceInfo(NymiDeviceInfo info) {
        View root = getLayoutInflater().inflate(R.layout.layout_dialog, null);
        LinearLayout container = (LinearLayout) root.findViewById(R.id.layout_dialog_main_container);

        addNymiDeviceInfo(container, info);

        return root;
    }

    private void addNymiConfigurationInfo(LinearLayout root, NymiConfigurationInfo info) {
        addInfoRow(root, "Version", info.getVersion(), false, false);
        addInfoRow(root, "Ecodaemon", info.getEcodaemon(), false, false);
        addInfoRow(root, "Flavor", info.getFlavor(), false, false);
        addInfoRow(root, "Commit", info.getCommit(), false, false);
    }

    private void addInfoHeader(LinearLayout root, String header) {
        View row = getLayoutInflater().inflate(R.layout.layout_info_row_header, null);
        ((TextView) row.findViewById(R.id.layout_info_row_header_value)).setText(header);
        root.addView(row);
    }

    private void addNymiProvisionInfo(LinearLayout root, String provision, ArrayList<String> provisionsPresent) {
        View row = getLayoutInflater().inflate(R.layout.layout_info_row_content, null);
        ((TextView) row.findViewById(R.id.layout_info_row_content_key)).setText(provision);
        boolean isPresent = provisionsPresent != null && provisionsPresent.contains(provision);
        ((TextView) row.findViewById(R.id.layout_info_row_content_value)).setText(isPresent ? "present" : "-");
        root.addView(row);
    }

    private void addInfoRow(LinearLayout root, String key, String value, boolean showSeparatorTop, boolean showSeparatorBottom) {
        View row = getLayoutInflater().inflate(R.layout.layout_info_row_content, null);
        ((TextView) row.findViewById(R.id.layout_info_row_content_key)).setText(key);
        ((TextView) row.findViewById(R.id.layout_info_row_content_value)).setText(value);
        row.findViewById(R.id.layout_info_row_separator_top).setVisibility(showSeparatorTop ? View.VISIBLE : View.INVISIBLE);
        row.findViewById(R.id.layout_info_row_separator_bottom).setVisibility(showSeparatorBottom ? View.VISIBLE : View.INVISIBLE);
        root.addView(row);
    }

    private void addNymiDeviceInfo(LinearLayout root, NymiDeviceInfo info) {
        addInfoRow(root, "Nonce", info.getNonce(), false, false);
        addInfoRow(root, "Is Provisioned", String.valueOf(info.isProvisioned()), false, false);
        addInfoRow(root, "Presence", info.getPresenceState().toString(), false, false);
        addInfoRow(root, "Firmware Version", info.getFirmwareVersion(), false, false);
        addInfoRow(root, "Proximity", info.getProximityState().toString(), false, false);
        addInfoRow(root, "Since Last Contact", String.valueOf(info.getSinceLastContact()), false, false);
        addInfoRow(root, "Has Approached", String.valueOf(info.hasApproached()), false, false);
        addInfoRow(root, "Found Status", String.valueOf(info.getFoundStatus()), false, false);
        addInfoRow(root, "Last RSSI", String.valueOf(info.getRSSI_last()), false, false);

        if (info.isProvisioned()) {
            addInfoRow(root, "Smoothed RSSI", String.valueOf(info.getRSSI_smoothed()), false, false);
            addInfoRow(root, "Provision id", info.getPid(), false, false);
            addInfoRow(root, "Auth.Remaining", String.valueOf(info.getAuthenticationWindowRemaining()), false, false);
            addInfoRow(root, "Commands Queued", String.valueOf(info.getCommandsQueued()), false, false);
            addInfoRow(root, "Has Totp", String.valueOf(info.hasTotp()), false, true);
        } else {
            addInfoRow(root, "Smoothed RSSI", String.valueOf(info.getRSSI_smoothed()), false, true);
        }
    }

    private class GServerSignupResponse {
        public String Ok;
    }



   private static class BitsUtils {

       public static byte setBit(byte b, int bit) {
           if (bit < 0 || bit >= 8) return b;
           return (byte) (b | (1 << bit));
       }

       public static byte resetBit(byte b, int bit) {
           if (bit < 0 || bit >= 8) return b;
           return (byte) (b & (~(1 << bit)));
       }

   }
}
