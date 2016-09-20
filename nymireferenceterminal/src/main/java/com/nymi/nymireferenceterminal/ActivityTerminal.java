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

package com.nymi.nymireferenceterminal;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.Gson;
import com.nymi.api.NymiAdapter;
import com.nymi.api.NymiDeviceInfo;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;

public class ActivityTerminal extends Activity {

    private final String LOG_TAG = getClass().getSimpleName();

    public static final int NOT_FOUND_RESET_INTERVAL = 5000;    //ms
    public static final String PARTNER_HOST = "10.0.1.21";
    public static final int PARTNER_PORT = 8080;
    private static final String NEA_NAME = "TerminalExample";

    private NymiAdapter mNymiAdapter;
    private Handler mHandler;

    private TextView mTvStatus;
    private TextView mTvNonce;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_terminal);
        mHandler = new Handler();

        mResetter = new Runnable() {
            @Override
            public void run() {
                mTvStatus.setText(getResources().getText(R.string.no_band_found));
                mTvNonce.setText("");
                mTvStatus.setTextColor(Color.GRAY);
            }
        };

        mTvStatus = (TextView) findViewById(R.id.activity_terminal_status);
        mTvNonce = (TextView) findViewById(R.id.activity_terminal_nonce);

        mNymiAdapter = NymiAdapter.getDefaultAdapter();

        if (!mNymiAdapter.isInitialized()) {
            String nymulatorHost = "10.0.1.21";
            mNymiAdapter.setNymulator(nymulatorHost);

            mNymiAdapter.init(this,
                    NEA_NAME, new NymiAdapter.InitCallback() {
                        @Override
                        public void onInitResult(int status, String message) {
                            Toast.makeText(ActivityTerminal.this, message, Toast.LENGTH_SHORT).show();
                            if (status == NymiAdapter.InitCallback.INIT_SUCCESS) {

                                setNotifications();
                            } else if (status == NymiAdapter.InitCallback.INIT_FAIL) {
                                finish();
                            }
                        }
                    });
        }
    }

    private void setNotifications() {
        mNymiAdapter.setNewNymiNonceCallback(new NymiAdapter.NewAdvNonceCallback() {
            @Override
            public void onNewNymibandNonce(String pid, int newAt, final String nymiNonce, boolean partnerVerified) {
                if (partnerVerified) {
                    //partner has been verified, no need to initiate the sequence again 
                    return;
                }

                mTvNonce.setText(nymiNonce + (partnerVerified ? ": verified" : " unverified"));

                mHandler.removeCallbacks(mResetter);
                mHandler.postDelayed(mResetter, NOT_FOUND_RESET_INTERVAL);

                /** Step 1 - send nonce to the partner server, get it signed */
                new Thread() {
                    @Override
                    public void run() {
                        try {
                            URL url = new URL("http://" + PARTNER_HOST + ":" + PARTNER_PORT +
                                    "/query/" + nymiNonce);

                            URLConnection urlConnection = url.openConnection();
                            InputStream in = new BufferedInputStream(urlConnection.getInputStream());
                            try {
                                BufferedReader reader = new BufferedReader(
                                        new InputStreamReader(in));

                                Gson gson = new Gson();
                                final GServerSignResponse response = gson.fromJson(reader, GServerSignResponse.class);

                                reader.close();
                                if (response != null &&
                                        response.Ok != null &&
                                        response.Ok.equals("yes") &&
                                        response.ServerNonce != null &&
                                        response.Signature != null &&
                                        response.ppk != null) {

                                    /** Step 2 - verify partner signature, sign partner nonce, get public key id */
                                    mNymiAdapter.verifyPartnerAndSign(
                                            nymiNonce,
                                            response.ServerNonce,
                                            response.Signature,
                                            response.ppk,
                                            new NymiAdapter.PartnerVerifiedCallback() {
                                                @Override
                                                public void onPartnerVerified(int status, final String sig, final String pkId) {
                                                    if (status == NymiAdapter.PartnerVerifiedCallback.PARTNER_VERIFICATION_SUCCESS) {
                                                        /** Step 3 - partner verified, key id present - send key id to partner */
                                                        new Thread() {
                                                            @Override
                                                            public void run() {
                                                                try {
                                                                    URL url2 = new URL("http://" + PARTNER_HOST + ":" + PARTNER_PORT +
                                                                            "/check_sig/" + pkId + "/" + sig + "/" + response.ServerNonce);
                                                                    URLConnection urlConnection = url2.openConnection();
                                                                    InputStream in = new BufferedInputStream(urlConnection.getInputStream());
                                                                    try {
                                                                        BufferedReader reader = new BufferedReader(
                                                                                new InputStreamReader(in));

                                                                        Gson gson = new Gson();
                                                                        final GServerVerifyResponse response = gson.fromJson(reader, GServerVerifyResponse.class);

                                                                        if (response != null &&
                                                                                response.Ok != null &&
                                                                                response.Ok.equals("yes") &&
                                                                                response.Username != null) {
                                                                            /** partner recognized this user */
                                                                            ActivityTerminal.this.runOnUiThread(new Runnable() {
                                                                                @Override
                                                                                public void run() {
                                                                                    mTvStatus.setTextColor(getResources().getColor(R.color.primary));
                                                                                    mTvStatus.setText("Hello, " + response.Username);
                                                                                }
                                                                            });
                                                                            mHandler.removeCallbacks(mResetter);
                                                                            mHandler.postDelayed(mResetter, NOT_FOUND_RESET_INTERVAL);
                                                                        }
                                                                        reader.close();
                                                                        Toast.makeText(ActivityTerminal.this, getString(R.string.roaming_authentication_verified), Toast.LENGTH_SHORT).show();
                                                                    } finally {
                                                                        in.close();
                                                                    }

                                                                } catch (Exception e) {
                                                                    Log.e(LOG_TAG, e.toString());
                                                                }
                                                            }
                                                        }.start();
                                                    } else if (status == NymiAdapter.PartnerVerifiedCallback.PARTNER_ALREADY_VERIFIED) {
                                                        Toast.makeText(ActivityTerminal.this, getString(R.string.partner_already_verified), Toast.LENGTH_SHORT).show();
                                                        mHandler.removeCallbacks(mResetter);
                                                        mHandler.postDelayed(mResetter, NOT_FOUND_RESET_INTERVAL);
                                                    }
                                                    else {
                                                        ActivityTerminal.this.runOnUiThread(new Runnable() {
                                                            @Override
                                                            public void run() {
                                                                mTvStatus.setTextColor(Color.GRAY);
                                                                mTvStatus.setText(ActivityTerminal.this.getResources().getText(R.string.activity_terminal_unauthorized));
                                                            }
                                                        });
                                                        mHandler.removeCallbacks(mResetter);
                                                        mHandler.postDelayed(mResetter, NOT_FOUND_RESET_INTERVAL);
                                                    }
                                                }
                                            }
                                    );
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

        mNymiAdapter.setDeviceFoundStatusChangeCallback(new NymiAdapter.DeviceFoundStatusChangeCallback() {
            @Override
            public void onDeviceFoundStatusChange(String pid,
                                                  NymiDeviceInfo.FoundStatus before,
                                                  NymiDeviceInfo.FoundStatus after,
                                                  boolean partnerVerified
            ) {
                Log.d(LOG_TAG, "onDeviceFoundStatusChange pid=" + pid +
                        " before=" + before +
                        " after=" + after +
                        " partnerVerified=" + partnerVerified);
            }
        });

        mNymiAdapter.setDevicePresenceChangeCallback(new NymiAdapter.DevicePresenceChangeCallback() {
            @Override
            public void onDevicePresenceChange(String pid,
                                               NymiDeviceInfo.PresenceState before,
                                               NymiDeviceInfo.PresenceState after,
                                               boolean partnerVerified) {
                Log.d(LOG_TAG, "onDevicePresenceChange pid=" + pid +
                        " before=" + before +
                        " after=" + after +
                        " partnerVerified=" + partnerVerified);
            }
        });
    }

    private class GServerSignResponse {
        public String Ok;
        public String ServerNonce;
        public String Signature;
        public String ppk;
    }

    private class GServerVerifyResponse {
        public String Ok;
        public String Username;
        public String wiegand;
    }

    private Runnable mResetter;
}
