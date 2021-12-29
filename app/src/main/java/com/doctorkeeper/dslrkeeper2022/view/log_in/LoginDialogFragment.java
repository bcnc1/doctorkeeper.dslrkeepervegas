/**
 * Copyright 2013 Nils Assbeck, Guersel Ayaz and Michael Zoech
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.doctorkeeper.dslrkeeper2022.view.log_in;

import android.app.DialogFragment;
import android.app.FragmentTransaction;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.loopj.android.http.JsonHttpResponseHandler;
import com.doctorkeeper.dslrkeeper2022.R;
import com.doctorkeeper.dslrkeeper2022.API.BcncAPI;
import com.doctorkeeper.dslrkeeper2022.util.SmartFiPreference;
import com.doctorkeeper.dslrkeeper2022.view.patient.PatientDialogFragment;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;

import cz.msebera.android.httpclient.Header;

public class LoginDialogFragment extends DialogFragment {

    private final String TAG = LoginDialogFragment.class.getSimpleName();

    public static LoginDialogFragment newInstance() {
        Bundle args = new Bundle();

        LoginDialogFragment f = new LoginDialogFragment();
        f.setArguments(args);
        return f;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        getDialog().setTitle("Login");
        View view = inflater.inflate(R.layout.activity_login, container, false);

        LoginDialogFragment.this.getDialog().requestWindowFeature(Window.FEATURE_NO_TITLE);

        final TextView usernameTextView;
        final TextView passwordTextView;
        final TextView hospitalNumberTextView;

        usernameTextView = view.findViewById(R.id.input_email);
        passwordTextView = view.findViewById(R.id.input_password);
        hospitalNumberTextView = view.findViewById(R.id.input_hospital_number);

        String spId = SmartFiPreference.getTnhId(BcncAPI.getActivity());
        String spPwd = SmartFiPreference.getTnhPwd(BcncAPI.getActivity());
        String spHsptNum = SmartFiPreference.getHospitalId(BcncAPI.getActivity());

        Log.i(TAG, "spId:" + spId);
        Log.i(TAG, "spPwd:" + spPwd);
        Log.i(TAG, "spHsptNum:" + spHsptNum);
        if(!spId.isEmpty() && !spPwd.isEmpty() && !spHsptNum.isEmpty()){
            usernameTextView.setText(spId);
            passwordTextView.setText(spPwd);
            hospitalNumberTextView.setText(spHsptNum);
        }

        final Button loginButton = view.findViewById(R.id.btn_login);

        loginButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.v(TAG,"login btn clicked");
                loginButton.setEnabled(false);
                if(hospitalNumberTextView.getText().toString().isEmpty()){
                    Toast.makeText(BcncAPI.getContext(),"병원번호를 입력해 주세요",Toast.LENGTH_LONG).show();
                    loginButton.setEnabled(true);
                    return;
                }
                try {
                    BcncAPI.loginTNH(BcncAPI.getContext(),usernameTextView.getText().toString(),passwordTextView.getText().toString(),hospitalNumberTextView.getText().toString(),
                            new JsonHttpResponseHandler(){
                                @Override
                                public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                                    super.onSuccess(statusCode, headers, response);
                                    Log.w(TAG,"성공 = "+response);
                                    dismiss();
                                    for(Header h : headers){
                                        Log.w(TAG,"h = "+h);
                                        if(h.toString().contains("Authorization")){
                                            try {
                                                JSONObject j1 = new JSONObject(h.toString().replace("Authorization: ",""));
                                                Log.w(TAG,"j1 = "+j1);

                                                SmartFiPreference.setTnhId(BcncAPI.getContext(),usernameTextView.getText().toString());
                                                SmartFiPreference.setTnhPwd(BcncAPI.getContext(),passwordTextView.getText().toString());
                                                SmartFiPreference.setHospitalId(BcncAPI.getContext(),hospitalNumberTextView.getText().toString());

                                                Log.w(TAG,"access-token : "+j1.getString("access-token"));
                                                SmartFiPreference.setSfToken(BcncAPI.getContext(),j1.getString("access-token"));
                                                String accessTokenCreatedTime = System.currentTimeMillis()+"";

                                                SmartFiPreference.setSfTokenTime(BcncAPI.getContext(),accessTokenCreatedTime);
                                                Log.w(TAG,"refresh-token : "+j1.getString("refresh-token"));
                                                SmartFiPreference.setSfRefToken(BcncAPI.getContext(),j1.getString("refresh-token"));

                                            } catch (JSONException e) {
                                                e.printStackTrace();
                                                Log.w(TAG,"j1 error = "+e);
                                            }
                                        }
                                    }

                                    try {
                                        BcncAPI.vegasLoginCheck(usernameTextView.getText().toString(),hospitalNumberTextView.getText().toString());
                                    } catch (UnsupportedEncodingException e) {
                                        e.printStackTrace();
                                    }
                                    startSelectPatient();
                                }

                                @Override
                                public void onFailure(int statusCode, Header[] headers, Throwable throwable, JSONObject errorResponse) {
                                    super.onFailure(statusCode, headers, throwable, errorResponse);
                                    Log.w(TAG,"errorResponse"+errorResponse);
                                    Log.w(TAG,"statusCode"+statusCode);
                                    Toast.makeText(BcncAPI.getContext(),"입력정보를 확인해 주세요",Toast.LENGTH_LONG).show();
                                    loginButton.setEnabled(true);
                                }
                            });
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
            }
        });

        return view;
    }

    public void onResume() {
        super.onResume();
        Window window = getDialog().getWindow();
        DisplayMetrics metrics = getResources().getDisplayMetrics();
//        int width = (int) (metrics.widthPixels * .85);
//        int height = (int) (metrics.heightPixels * .60);
//        window.setLayout(width, height);
        window.setLayout(550, 1500);
        window.setGravity(Gravity.CENTER);

    }

    private void startSelectPatient() {

        if (getFragmentManager() != null) {
            FragmentTransaction changelogTx = getFragmentManager().beginTransaction();
            PatientDialogFragment patientDialogFragment = PatientDialogFragment.newInstance();
            changelogTx.add(patientDialogFragment, "환자검색");
            changelogTx.commit();
        }
    }
}
