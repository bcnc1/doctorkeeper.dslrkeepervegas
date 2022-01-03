package com.doctorkeeper.dslrkeepervegas.API;

import android.app.Activity;
import android.app.FragmentTransaction;
import android.content.Context;
import android.graphics.Bitmap;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.Gravity;
import android.webkit.MimeTypeMap;
import android.widget.Toast;

import com.android.volley.AuthFailureError;
import com.android.volley.Cache;
import com.android.volley.DefaultRetryPolicy;
import com.android.volley.NetworkResponse;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;

import com.android.volley.toolbox.JsonObjectRequest;
import com.doctorkeeper.dslrkeepervegas.R;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.JsonHttpResponseHandler;
import com.doctorkeeper.dslrkeepervegas.Constants;
import com.doctorkeeper.dslrkeepervegas.models.PhotoModel;
import com.doctorkeeper.dslrkeepervegas.services.PhotoModelService;
import com.doctorkeeper.dslrkeepervegas.util.SSLConnect;
import com.doctorkeeper.dslrkeepervegas.util.SmartFiPreference;

import org.apache.http.protocol.HTTP;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import static android.content.Context.CONNECTIVITY_SERVICE;
import static com.doctorkeeper.dslrkeepervegas.MainActivity.countDownTimer;
import static com.loopj.android.http.AsyncHttpClient.log;

import com.doctorkeeper.dslrkeepervegas.view.log_in.LoginDialogFragment;
import com.loopj.android.http.RequestParams;
import com.loopj.android.http.ResponseHandlerInterface;
import com.loopj.android.http.SyncHttpClient;

import cz.msebera.android.httpclient.Header;
import cz.msebera.android.httpclient.HttpHeaders;
import cz.msebera.android.httpclient.entity.StringEntity;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.RequestBody;

public class BcncAPI {

    private static final String TAG = BcncAPI.class.getSimpleName();
    private static AsyncHttpClient client = new AsyncHttpClient();
    private static SyncHttpClient client2 = new SyncHttpClient();
    private static Cache mCache;

    private static Activity mActivity;
    private static Context mContext;

//    private static Network mNetwork;
    public static HashMap<String,String> selectedDoctor;

//    private static final String BASE_URL = "http://api.doctorkeeper.com:7818/v1";
    private static final String mAPIKey = "NTlFUG5qdkNBV1VJWDRjL0tBMU5TMlZOY1UvaTBVQVVVU3h2eW5aRlkwND0K.gXttoBDWfyPc3z92HxRurTXo56s4NBT2khGTsBskfYM=";

    private static String boardId = null;
    private static String mAcccessToken = null;

    public static boolean isCameraOn = false;

    public static String getAccessToken() {
        mAcccessToken = SmartFiPreference.getSfToken(getActivity());
        return mAcccessToken;
    }

    public static String getBoardId() {
        String boardId = SmartFiPreference.getHospitalId(getActivity());
        return boardId;
    }

    private static String getAbsoluteUrl(String relativeUrl) {
        return Constants.m5.BASE_URL + "/v1/" + relativeUrl;
    }

    private static String getAbsoluteUrl2(String relativeUrl) {
        return Constants.bcnc.BASE_URL + "/api/v1" + relativeUrl;
    }

    public static Activity getActivity() {
        return mActivity;
    }

    public static Context getContext() {
        return mContext;
    }

    public static void setContext(Activity activity, Context context) {
        mActivity = activity;
        mContext = context;
    }

    public static void postToastMessage(final String message) {
        Handler handler = new Handler(Looper.getMainLooper());
        handler.post(() -> {
//                Toast.makeText(getContext(), message, Toast.LENGTH_LONG).show();
            Toast toast = Toast.makeText(mActivity.getBaseContext(), message, Toast.LENGTH_LONG);
            toast.setGravity(Gravity.CENTER, 0, 0);
            toast.show();
        });
    }

    private static String getMimeType(String path) {
        String extension = MimeTypeMap.getFileExtensionFromUrl(path);
        return MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension.toLowerCase());
    }

    public static void uploadImage(final String filePath, final ResponseHandlerInterface handler) throws UnsupportedEncodingException {
        Log.v(TAG,"filePath UploadImage : " + filePath);
//        postToastMessage("hi");
        if (!BcncAPI.isNetworkConnected()) {
            postToastMessage(BlabAPI.getContext().getString(R.string.check_network));
            return;
        }

        // login for token
        if(!checkTokenValidity()) {
            BcncAPI.SyncLoginTNH(getContext(),
                    SmartFiPreference.getTnhId(getContext()),
                    SmartFiPreference.getTnhPwd(getContext()),
                    SmartFiPreference.getHospitalId(getContext()),
                    new JsonHttpResponseHandler() {
                        @Override
                        public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                            super.onSuccess(statusCode, headers, response);
                            Log.w(TAG, "성공 = " + response);
                            for (Header h : headers) {
                                Log.w(TAG, "h = " + h);
                                if (h.toString().contains("Authorization")) {
                                    try {
                                        JSONObject j1 = new JSONObject(h.toString().replace("Authorization: ", ""));
                                        Log.w(TAG, "j1 = " + j1);

                                        Log.w(TAG, "access-token" + j1.getString("access-token"));
                                        SmartFiPreference.setSfToken(getActivity(), j1.getString("access-token"));
                                        String accessTokenCreatedTime = System.currentTimeMillis() + "";

                                        SmartFiPreference.setSfTokenTime(getActivity(), accessTokenCreatedTime);
                                        Log.w(TAG, "refresh-token" + j1.getString("refresh-token"));
                                        SmartFiPreference.setSfRefToken(getActivity(), j1.getString("refresh-token"));

                                    } catch (JSONException e) {
                                        e.printStackTrace();
                                        Log.w(TAG, "j1 error = " + e);
                                    }
                                }
                            }

                        }

                        @Override
                        public void onFailure(int statusCode, Header[] headers, Throwable throwable, JSONObject errorResponse) {
                            super.onFailure(statusCode, headers, throwable, errorResponse);
                            Log.w(TAG, "errorResponse" + errorResponse);
                            Log.w(TAG, "statusCode" + statusCode);
                        }
                    });
        }

        Thread t = new Thread(() -> {

            // Extract file name
            String filename=filePath.substring(filePath.lastIndexOf("/")+1);
            Log.v(TAG,"filename : " + filename);

            // Extract CUSTOMERID in file name
            String[] names = filename.split("_");
            String customerId = names[0];
            Log.v(TAG,"customerId : " + customerId);

            String originalPath = filePath.replace("thumbnail/", "");
            String content_type = getMimeType(filePath);

            OkHttpClient client = new OkHttpClient();
            Log.v(TAG,"origin:"+originalPath);
            Log.v(TAG,"thumbnail:"+filePath);

            String url = Constants.vegas.BASE_URL + "/upload";
            RequestBody requestBody = new MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart("CUSTOMERID",customerId)
                    .addFormDataPart("IMAGE",originalPath,RequestBody.create(new File(originalPath), MediaType.parse(content_type)))
                    .addFormDataPart("THUMBNAIL",filePath,RequestBody.create(new File(filePath), MediaType.parse(content_type)))
                    .build();

            okhttp3.Request request = new okhttp3.Request.Builder()
                    .url(url)
                    .post(requestBody)
                    .addHeader("Authorization", "Bearer " + getAccessToken())
                    .build();

            try {
                okhttp3.Response response = client.newCall(request).execute();
                Log.v(TAG,">>><<<"+response.toString());
                //response.body()
                if (!response.isSuccessful()) {
                    throw new IOException("Error : "+response);
                } else {
                    getActivity().runOnUiThread(() -> {
                        Toast toast = Toast.makeText(mActivity.getBaseContext(), "이미지 업로드 성공!", Toast.LENGTH_LONG);
                        toast.setGravity(Gravity.CENTER, 0, 0);
                        toast.show();

                        String CheckFolderPath = mContext.getExternalFilesDir(null).getAbsolutePath() + "/uploadCheck";
                        Log.v(TAG,"CheckFolderPath : "+ CheckFolderPath);
                        File dir = new File(CheckFolderPath);
                        if(!dir.exists()){
                            Log.v(TAG,"CheckFolder not exists");
                            dir.mkdirs();
                        }
                        Log.v(TAG,"filename : " + filename);

                        // write empty file
                        Log.v(TAG,"check name : "+ CheckFolderPath + "/" + filename);
                        try {
                            FileOutputStream fos = new FileOutputStream(CheckFolderPath + "/" + filename, true);
                        } catch (FileNotFoundException e) {
                            e.printStackTrace();
                        }

                    });
                }

                countDownTimer.cancel();
                countDownTimer.start();

            } catch (IOException e) {
                postToastMessage("업로드  실패");
                e.printStackTrace();
            }
        });
        t.start();
    }

    public static void SyncLoginTNH(Context con, String id, String pw, String hospitalNumber, ResponseHandlerInterface handler){
        String url = Constants.vegas.BASE_URL+"/login";
//        StringEntity jsonEntity = null;
        StringEntity jsonEntityUTF8 = null;
        String EncodedId = URLEncoder.encode(id);
        Log.w(TAG,id+"::"+pw+"::"+hospitalNumber);
        JSONObject jsonParams = new JSONObject();
        try {
            jsonParams.put("USERID", id);
            jsonParams.put("PW", pw);
            jsonParams.put("ORGNO", hospitalNumber);


        } catch (JSONException e) {
            e.printStackTrace();
        }

        //            jsonEntity = new StringEntity(jsonParams.toString());

        //set json to StringEntity
        jsonEntityUTF8 = new StringEntity(jsonParams.toString(), HTTP.UTF_8);

        Log.w(TAG,jsonParams.toString());

        client2.addHeader("Accept", "application/json");
        client2.addHeader("charset", "utf-8");
        client2.post(con, url,jsonEntityUTF8,"application/json",handler);
    }

    public static void loginTNH(Context con, String id, String pw, String hospitalNumber, ResponseHandlerInterface handler) throws UnsupportedEncodingException {
        String url = Constants.vegas.BASE_URL+"/login";
        StringEntity jsonEntityUTF8 = null;
        String EncodedId = URLEncoder.encode(id, "UTF-8");

        Log.w(TAG,id+"::"+pw+"::"+hospitalNumber);
        JSONObject jsonParams = new JSONObject();
        try {
            jsonParams.put("USERID", id);
            jsonParams.put("PW", pw);
            jsonParams.put("ORGNO", hospitalNumber);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        jsonEntityUTF8 = new StringEntity(jsonParams.toString(), HTTP.UTF_8);

        Log.w(TAG,jsonParams.toString());

        client.addHeader("Accept", "application/json");
        client.addHeader("charset", "utf-8");
        client.post(con, url,jsonEntityUTF8,"application/json",handler);
    }

    public static void vegasLoginCheck(String id, String orgno) throws UnsupportedEncodingException {
//        Log.w(TAG,"Vegas Login Check");

        loginCheck(BlabAPI.getContext(),id,orgno,new JsonHttpResponseHandler(){
            @Override
            public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                super.onSuccess(statusCode, headers, response);
                Log.w(TAG,response.toString());
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, Throwable throwable, JSONObject errorResponse) {
                super.onFailure(statusCode, headers, throwable, errorResponse);
                Log.w(TAG,errorResponse.toString());
            }

            // ----New Overridden method
            @Override
            public boolean getUseSynchronousMode() {
                return false;
            }

        });
    }

    public static void loginCheck(Context con, String id, String orgno, ResponseHandlerInterface handler) throws UnsupportedEncodingException {
        Log.w(TAG,"check Login Vegas");

        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss", Locale.getDefault());
        String currentDateTime = sdf.format(new Date());
        SmartFiPreference.setSfCheckTime(currentDateTime);

        String url = Constants.bcnc.BASE_URL+"/api/v1/dev/checkTNH";
        StringEntity jsonEntity = null;
        String encodedId = URLEncoder.encode(id, java.nio.charset.StandardCharsets.UTF_8.toString());
        String encodedNo = URLEncoder.encode(orgno, java.nio.charset.StandardCharsets.UTF_8.toString());
        Log.w(TAG,"Vegas : "+ encodedId + " / " + encodedNo);

        JSONObject jsonParams = new JSONObject();
        try {
            jsonParams.put("id", encodedId);
            jsonParams.put("orgno", encodedNo);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        try {
            jsonEntity = new StringEntity(jsonParams.toString());
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
//        client.addHeader("Accept", "application/json");
        client.post(con,url,jsonEntity,"application/json",handler);
    }

    public static boolean checkTokenValidity(){

        Long startTime = Long.parseLong(SmartFiPreference.getSfTokenTime(getContext()));
        Long currentTime = System.currentTimeMillis();
        Long difference = currentTime - startTime;
//        Log.v(TAG,"currentTime::"+currentTime);
//        Log.v(TAG,"startTime::"+startTime);
        Log.v(TAG,"DIF::"+difference/60000);

//        if(difference > 1){
        if(difference > 25*60*1000){
//            autoLogin();
            return false;
        }
        return true;
    }

    public static boolean isNetworkConnected() {
        ConnectivityManager cm = (ConnectivityManager) mContext.getSystemService(Context.CONNECTIVITY_SERVICE);

        return cm.getActiveNetworkInfo() != null && cm.getActiveNetworkInfo().isConnected();
    }

    public static void loginDoctorKeeper(String username, String password, final JsonHttpResponseHandler responseHandler) {

        HashMap<String, String> params = new HashMap<String, String>();
        params.put("id", username);
        params.put("pwd", password);
        JsonObjectRequest request = new JsonObjectRequest(getAbsoluteUrl2("/user/login"), new JSONObject(params),
                response -> {
                    Log.i(TAG, "response : " + response);
                    try {
                        if (response.has("token")) {
                            mAcccessToken = URLDecoder.decode(response.getString("token"));
                            SmartFiPreference.setDoctorId(getActivity(),username);
                            SmartFiPreference.setSfDoctorPw(getActivity(),password);
                            SmartFiPreference.setHospitalId(getActivity(),username);
                            SmartFiPreference.setSfToken(getActivity(),mAcccessToken);
                            Log.i(TAG, "mAcccessToken : " + mAcccessToken);
                            responseHandler.onSuccess(200, null, response.toString());
//                                JSONArray boards = response.getJSONArray("boards");
//                                for(int i=0;i<boards.length();i++){
//                                    JSONObject board = boards.getJSONObject(i);
//                                    if(board.get("type").toString().equals("hospital")){
//                                        boardId = board.get("id").toString();
//
////                                        Log.i(TAG,"Board Id : =========" + boardId);
//                                        break;
//                                    }
//                                }
                        }else{
                            responseHandler.onSuccess(400, null, response.toString());
                        }
//                        Log.i(TAG, "Response:%n %s" + response.toString());
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.i(TAG, "Error: " + error.getMessage());
                responseHandler.onFailure(0, null, error.getLocalizedMessage(), null);
            }
        }) {
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                Map<String, String> params = new HashMap<String, String>();
                params.put("X-Madamfive-APIKey", mAPIKey);
                return params;
            }
        };

        request.setRetryPolicy(new DefaultRetryPolicy(10000,
                DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
        VolleySingleton.getInstance(mContext).addToRequestQueue(request);

    }

    public static void searchPatient(Context con, String searchByName, String searchByChart, ResponseHandlerInterface responseHandler){
        String url = Constants.vegas.BASE_URL+"/patient?";
        StringEntity jsonEntity = null;
        RequestParams requestParams = new RequestParams();
        String token = SmartFiPreference.getSfToken(BcncAPI.getContext());
        JSONObject jsonParams = new JSONObject();
        try {
            jsonParams.put("CUSTNAME", searchByName);
            jsonParams.put("CUSTNO", searchByChart);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        try {
            Log.v(TAG,jsonParams.toString());
            jsonEntity = new StringEntity(jsonParams.toString());
            Log.v(TAG,jsonEntity.toString());
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        requestParams.put("filter",jsonParams);
        requestParams.put("offset","0");
        requestParams.put("limit","100");

        Log.v(TAG,url+requestParams);

        client.addHeader("Accept", "application/json");
        client.addHeader("Content-Type", "application/json");

        Log.v(TAG,"patient search token : " + token);
        client.addHeader(HttpHeaders.AUTHORIZATION, "Bearer "+token);

        client.get(con, url, requestParams ,responseHandler);

    }

    public static void getDoctorList(String name, String dno, JsonHttpResponseHandler handler){
        log.i(TAG,"getDoctorList");
        String url = Constants.bcnc.BASE_URL + "/api/v1/doctor/search?";
        StringEntity jsonEntity = null;
        String hospitalId = SmartFiPreference.getHospitalId(getContext());
        String token = SmartFiPreference.getSfToken(getContext());

        RequestParams params = new RequestParams();
        params.put("id", hospitalId);
        if(!name.isEmpty()) {
            params.put("name", name);
        }
        if(!dno.isEmpty()) {
            params.put("dno", dno);
        }

        log.w(TAG,params.toString());
        client.addHeader("X-Auth-Token", token);
        client.get(getActivity(), url, params,handler);
    }

    public static void insertDoctor(Context con, String name, String dno, JsonHttpResponseHandler responseHandler){
        log.i(TAG, "insertDoctor:::" + name + ":::" + dno);
        String hospitalId = SmartFiPreference.getHospitalId(getContext());
        String token = SmartFiPreference.getSfToken(getContext());
        String url = Constants.bcnc.BASE_URL + "/api/v1/doctor/create";
        StringEntity jsonEntityUTF8;
        JSONObject jsonParams = new JSONObject();
        try {
            jsonParams.put("id", hospitalId);
            jsonParams.put("name", name);
            jsonParams.put("dno", dno);
        } catch (JSONException e) {
            e.printStackTrace();
            log.w(TAG,e+"");
        }
        jsonEntityUTF8 = new StringEntity(jsonParams.toString(), org.apache.http.protocol.HTTP.UTF_8);

        client.addHeader("Accept", "application/json");
        client.addHeader("Content-Type", "application/json");
        client.addHeader("X-Auth-Token", token);
        client.post(con, url, jsonEntityUTF8, "application/json",responseHandler);

    }


//    public static void getImageLists(Context con, JsonHttpResponseHandler handler) {
//        String url = Constants.Storage.BASE_URL;
//        String hostipalId = SmartFiPreference.getHospitalId(getContext());
//        final String urlTarget = url + "/" + hostipalId + "/?limit=1000&format=json";
//        String token = SmartFiPreference.getSfToken(getContext());
//        log.i(TAG, "url:::" + urlTarget);
//
//        client.addHeader("X-Auth-Token", token);
//        client.get(con, urlTarget, handler);
//
//    }


    public static void login(String username, String password, final JsonHttpResponseHandler responseHandler) {

        HashMap<String, String> params = new HashMap<String, String>();
        params.put("username", username);
        params.put("password", password);
        params.put("fetchBoards", "true");
        JsonObjectRequest request = new JsonObjectRequest(getAbsoluteUrl("/login"), new JSONObject(params),
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        try {
                            if (response.has("accessToken")) {
                                mAcccessToken = URLDecoder.decode(response.getString("accessToken"));
                                SmartFiPreference.setSfToken(getActivity(),mAcccessToken);
//                                write_mAcccessToken(mAcccessToken);
//                                Log.i(TAG, "mAcccessToken : " + mAcccessToken);
                                responseHandler.onSuccess(200, null, response.toString());
                                JSONArray boards = response.getJSONArray("boards");
                                for(int i=0;i<boards.length();i++){
                                    JSONObject board = boards.getJSONObject(i);
                                    if(board.get("type").toString().equals("hospital")){
                                        boardId = board.get("id").toString();
                                        SmartFiPreference.setHospitalId(getActivity(),boardId);
//                                        write_boardId(boardId);
//                                        Log.i(TAG,"Board Id : =========" + boardId);
                                        break;
                                    }
                                }
                            }else{
                                responseHandler.onSuccess(400, null, response.toString());
                            }
//                            Log.i(TAG, "Response:%n %s" + response.toString(4));
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.i(TAG, "Error: " + error.getMessage());
                responseHandler.onFailure(0, null, error.getLocalizedMessage(), null);
            }
        }) {
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                Map<String, String> params = new HashMap<String, String>();
                params.put("X-Madamfive-APIKey", mAPIKey);
                return params;
            }
        };

        request.setRetryPolicy(new DefaultRetryPolicy(10000,
                DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
        VolleySingleton.getInstance(mContext).addToRequestQueue(request);

    }

    /**
     * 파일 패스 기반의 업로드
     * @param fileName
     * @param cameraKind
     * @param responseHandler
     * @throws FileNotFoundException
     * @throws IOException
     */
    public static void createPost(String fileName, String cameraKind, JsonHttpResponseHandler responseHandler) throws FileNotFoundException,
            IOException {

        byte[] buffer = new byte[4096];
        BufferedInputStream bis = new BufferedInputStream(new FileInputStream(fileName));
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        int bytes = 0;
        while ((bytes = bis.read(buffer, 0, buffer.length)) > 0) {
            baos.write(buffer, 0, bytes);
        }
        createPost(baos, cameraKind, responseHandler);
        baos.close();
        bis.close();

    }

    public void createPost(Bitmap bitmap, String cameraKind, JsonHttpResponseHandler responseHandler) {

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos);

        createPost(baos, cameraKind, responseHandler);

    }

    public static void createPost(ByteArrayOutputStream baos, String cameraKind, JsonHttpResponseHandler responseHandler) {
        final byte[] imageBytes = baos.toByteArray();
        Long photoModelId = 0L;
        createPost(imageBytes, cameraKind, photoModelId, responseHandler);
    }

    public static void showLoginDialog() {

        FragmentTransaction changelogTx = BcncAPI.getActivity().getFragmentManager().beginTransaction();
        LoginDialogFragment loginDialogFragment = LoginDialogFragment.newInstance();
        changelogTx.add(loginDialogFragment, "로그인");
        changelogTx.commit();

    }
    public static void createPost(final byte[] imageBytes, final String cameraKind, final Long photoModelId, final JsonHttpResponseHandler responseHandler) {

        mAcccessToken = getAccessToken();
        boardId = getBoardId();

        final Map<String, String> params = new HashMap<String, String>();

        final String name;
        String categoryId, chartNumber;

        if(photoModelId == 0L){
            name = SmartFiPreference.getSfPatientName(getActivity());
            categoryId = SmartFiPreference.getPatientId(getActivity());
            chartNumber = SmartFiPreference.getPatientChart(getActivity());
            chartNumber = chartNumber.replace("++++++",""); //환자 차트 번호
            chartNumber.trim();
        }else{
            PhotoModel pm = PhotoModelService.getPhotoModel(photoModelId);
            name = pm.getCustName();
            categoryId = pm.getCategoryId();
            chartNumber = pm.getCustNo();
        }

        params.put("title", cameraKind);
        params.put("type", "smartfi");
        params.put("content", URLEncoder.encode(chartNumber));
        params.put("accessToken", mAcccessToken);
        params.put("boardId", boardId);
        params.put("categories[]", categoryId);
        params.put("currency", URLEncoder.encode(name));

        JSONObject attachmentJson = new JSONObject();
        final String fileName = UUID.randomUUID().toString();

        try {
            attachmentJson.put("guid", fileName);
            if(cameraKind.equals("Video")){
                attachmentJson.put("fileType", "video/mp4");
            }else{
                attachmentJson.put("fileType", "image/jpeg");
            }
            attachmentJson.put("fileName", fileName);
            attachmentJson.put("type", "none.ko");
            params.put("attachments[]", URLEncoder.encode(attachmentJson.toString()));

        } catch (JSONException e) {
            Log.i("m5API",e.toString());
        }

        JSONObject userDataJson = new JSONObject();
        try {
            if(selectedDoctor!=null) {
//                userDataJson.put("doctorName", URLEncoder.encode(name));
                userDataJson.put("doctorName", URLEncoder.encode(selectedDoctor.get("name")));
                userDataJson.put("doctorNumber", URLEncoder.encode(selectedDoctor.get("doctorNumber")));
            }
            userDataJson.put("patient", URLEncoder.encode(name));
            params.put("userData", userDataJson.toString());
        } catch (JSONException e) {
            Log.i("m5API",e.toString());
        }
        Log.i(TAG,"Start Upload ====>>> "+getAbsoluteUrl2("boards/" + boardId+"/posts"));
        VolleyMultipartRequest request = new VolleyMultipartRequest(Request.Method.POST, getAbsoluteUrl("boards/" + boardId+"/posts"),
                new Response.Listener<NetworkResponse>() {
                    @Override
                    public void onResponse(NetworkResponse response) {
                        String rsData = new String(response.data);
                        Log.i(TAG, "Response: " + rsData);
                        Log.i(TAG, "Response code: " + response.statusCode);
                        int rsCode;
                        try {
                            JSONObject jObject = new JSONObject(rsData);
                            JSONObject headerObject = jObject.getJSONObject("header");

                            rsCode = headerObject.getInt("code");
                            Log.d(TAG, "rsCode : " + rsCode);

                            if(response.statusCode!=200) {
                                Log.d(TAG, "업로드 요청 실패");
                                Toast toast = Toast.makeText(getActivity(), "업로드 실패 서버에러", Toast.LENGTH_LONG);
                                toast.setGravity(Gravity.CENTER, 0, -100);
                                toast.show();
                            }

                            if (rsCode==1201) {
                                Log.d(TAG, "토큰 만료");
                                Toast toast = Toast.makeText(getActivity(), "로그인필요", Toast.LENGTH_LONG);
                                toast.setGravity(Gravity.CENTER, 0, -100);
                                toast.show();
                                showLoginDialog();
                            } else if (rsCode==200) {
                                Log.d(TAG, "업로드 성공");
                                Toast toast = Toast.makeText(getActivity(), "업로드 성공", Toast.LENGTH_LONG);
                                toast.setGravity(Gravity.CENTER, 0, -100);
                                toast.show();
                            } else {
                                Log.d(TAG, "업로드 실패");
                                Toast toast = Toast.makeText(getActivity(), "네트워크 등의 문제로 업로드 실패", Toast.LENGTH_LONG);
                                toast.setGravity(Gravity.CENTER, 0, -100);
                                toast.show();
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }






//                        if (response.statusCode==200) {
//                            Toast toast = Toast.makeText(getActivity(), "업로드 성공", Toast.LENGTH_LONG);
//                            toast.setGravity(Gravity.CENTER, 0, -100);
//                            toast.show();
//                        } else {
//                            Toast toast = Toast.makeText(getActivity(), "업로드 실패", Toast.LENGTH_LONG);
//                            toast.setGravity(Gravity.CENTER, 0, -100);
//                            toast.show();
//                        }
                        JSONObject resultJson = null;
                        try {
                            resultJson = new JSONObject(rsData);
                            responseHandler.onSuccess(200, null, resultJson);
                        } catch (JSONException e) {
                            Log.i(TAG,e.toString());
                            responseHandler.onSuccess(501, null, resultJson);
                        }
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                responseHandler.onFailure(200, null, error.getLocalizedMessage(), null);
//                error.printStackTrace();
            }
        }) {
            @Override
            protected Map<String, String> getParams() {
                return params;
            }

            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                Map<String, String> params = new HashMap<String, String>();
                params.put("X-Madamfive-APIKey", mAPIKey);
                return params;
            }

            @Override
            protected Map<String, DataPart> getByteData() {

                Map<String, DataPart> ImageParams = new HashMap<String, DataPart>();
                if(cameraKind.equals("Video")){
                    ImageParams.put("files[]", new DataPart(fileName, imageBytes, "video/mp4"));
                }else{
                    ImageParams.put("files[]", new DataPart(fileName, imageBytes, "image/jpeg"));
                }

                return ImageParams;
            }
        };

//        request.setRetryPolicy(new DefaultRetryPolicy(10 * 1000, 1, 1.0f));
        request.setRetryPolicy(new DefaultRetryPolicy(0, DefaultRetryPolicy.DEFAULT_MAX_RETRIES, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
        VolleySingleton.getInstance(mContext).addToRequestQueue(request);

        countDownTimer.cancel();
        countDownTimer.start();

    }



    public static void getImageURL (String page, final JsonHttpResponseHandler responseHandler){
        Log.i(TAG, "getImageURL");
//        if (mAcccessToken==null) {
//           return;
//        }
        mAcccessToken = getAccessToken();
        boardId = getBoardId();

        String queryString = "type=smartfi&fetchTotalCount=true&orderDirection=desc&mode=all";
        queryString = queryString+"&limit=50&page="+page+"&accessToken="+URLEncoder.encode(mAcccessToken);
        String relativeURL = "boards/"+boardId+"/posts?"+queryString;

        Log.i("URL=====", getAbsoluteUrl(relativeURL).toString());

        JsonObjectRequest request = new JsonObjectRequest(getAbsoluteUrl(relativeURL), null,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        try {

                            if (response.has("total")) {
                                responseHandler.onSuccess(200, null, response.toString());
                            }else{
                                responseHandler.onSuccess(400, null, response.toString());
                            }
//                            Log.i(TAG, "Response:%n %s" + response.get("total").toString());
                        } catch (Exception e) {
//                            e.printStackTrace();
                        }
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.i(TAG, "Error: " + error.getMessage());
                responseHandler.onFailure(0, null, error.getLocalizedMessage(), null);
            }
        }) {
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                Map<String, String> params = new HashMap<String, String>();
                params.put("X-Madamfive-APIKey", mAPIKey);
                return params;
            }

        };

        request.setRetryPolicy(new DefaultRetryPolicy(10000,
                DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
        VolleySingleton.getInstance(mContext).addToRequestQueue(request);

    }


//    public static void getImageURL (String page, final JsonHttpResponseHandler responseHandler){
//        Log.i(TAG, "getImageURL");
////        if (mAcccessToken==null) {
////           return;
////        }
//        mAcccessToken = getAccessToken();
//        boardId = getBoardId();
//
//        String queryString = "type=smartfi&fetchTotalCount=true&orderDirection=desc&mode=all";
//        queryString = queryString+"&limit=50&page="+page+"&accessToken="+URLEncoder.encode(mAcccessToken);
//        String relativeURL = "boards/"+boardId+"/posts?"+queryString;
//
//        Log.i("URL=====", getAbsoluteUrl(relativeURL).toString());
//
//        JsonObjectRequest request = new JsonObjectRequest(getAbsoluteUrl(relativeURL), null,
//                new Response.Listener<JSONObject>() {
//                    @Override
//                    public void onResponse(JSONObject response) {
//                        try {
//
//                            if (response.has("total")) {
//                                responseHandler.onSuccess(200, null, response.toString());
//                            }else{
//                                responseHandler.onSuccess(400, null, response.toString());
//                            }
////                            Log.i(TAG, "Response:%n %s" + response.get("total").toString());
//                        } catch (Exception e) {
////                            e.printStackTrace();
//                        }
//                    }
//                }, new Response.ErrorListener() {
//            @Override
//            public void onErrorResponse(VolleyError error) {
//                Log.i(TAG, "Error: " + error.getMessage());
//                responseHandler.onFailure(0, null, error.getLocalizedMessage(), null);
//            }
//        }) {
//            @Override
//            public Map<String, String> getHeaders() throws AuthFailureError {
//                Map<String, String> params = new HashMap<String, String>();
//                params.put("X-Madamfive-APIKey", mAPIKey);
//                return params;
//            }
//
//        };
//
//        request.setRetryPolicy(new DefaultRetryPolicy(10000,
//                DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
//                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
//        VolleySingleton.getInstance(mContext).addToRequestQueue(request);
//
//    }

    public static void getPatientList(String name, String chartNo, JsonHttpResponseHandler handler){
        String url = Constants.bcnc.BASE_URL + "/api/v1/patient/search?";
        StringEntity jsonEntity = null;
        String hospitalId = SmartFiPreference.getHospitalId(getContext());
        String token = SmartFiPreference.getSfToken(getContext());

        RequestParams params = new RequestParams();
        params.put("id", hospitalId);
        if(!name.isEmpty()) {
            params.put("name", name);
        }
        if(!chartNo.isEmpty()) {
            params.put("chno", chartNo);
        }

        log.w(TAG,params.toString());
        client.addHeader("X-Auth-Token", token);
        client.get(getContext(), url, params,handler);
    }

    public static void searchPatient (String searchName,String searchChart, final JsonHttpResponseHandler responseHandler){

        mAcccessToken = getAccessToken();
        boardId = getBoardId();

        String queryString = "";
        if(searchChart.isEmpty()){
            queryString = "type=patient&keyword="+URLEncoder.encode(searchName);
        }else{
            queryString = "type=patient&keyword="+URLEncoder.encode(searchName)+"&parentId="+URLEncoder.encode(searchChart);
        }
//        queryString = queryString+"&accessToken="+URLEncoder.encode(mAcccessToken);
        queryString = queryString+"&accessToken=" + mAcccessToken;
        String relativeURL = "boards/" + boardId + "/categories/search?" + queryString;
        if(searchName.isEmpty() && searchChart.isEmpty()) {
//            relativeURL = "boards/" + boardId + "/categories/search?limit=10&" + queryString;
            Log.i(TAG, "keyword is Empty");
            return;
        }
        Log.i("URL=====", getAbsoluteUrl(relativeURL).toString());
        JsonObjectRequest request = new JsonObjectRequest(getAbsoluteUrl(relativeURL), null,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        try {
                            responseHandler.onSuccess(200, null, response.toString());
                            Log.i(TAG, "Response:%n %s" + response.toString());

                        } catch (Exception e) {
                            Log.i(TAG, e.toString());
                        }
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.i(TAG, "Error: " + error.getMessage());
                responseHandler.onFailure(0, null, error.getLocalizedMessage(), null);
            }
        }) {
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                Map<String, String> params = new HashMap<String, String>();
                params.put("X-Madamfive-APIKey", mAPIKey);
                return params;
            }
        };

        request.setRetryPolicy(new DefaultRetryPolicy(10000,
                DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
        VolleySingleton.getInstance(mContext).addToRequestQueue(request);

    }

    public static void searchDoctor (final JsonHttpResponseHandler responseHandler){

        mAcccessToken = getAccessToken();
        boardId = getBoardId();

        String relativeURL = "https://dashboard.doctorkeeper.com/v1/boards/"+boardId+"/posts?limit=1000&accessToken="+URLEncoder.encode(mAcccessToken);

        SSLConnect ssl = new SSLConnect();
        ssl.postHttps(relativeURL,1000,1000);

        JsonObjectRequest request = new JsonObjectRequest(relativeURL, null,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        try {
                            responseHandler.onSuccess(200, null, response.toString());
                            Log.i(TAG, "Response:%n %s" + response.toString());
                        } catch (Exception e) {
                            Log.i(TAG,"Errr:::"+e.toString());
                        }
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.i(TAG, "Error: " + error.getMessage());
                responseHandler.onFailure(0, null, error.getLocalizedMessage(), null);
            }
        }) {
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                Map<String, String> params = new HashMap<String, String>();
                params.put("X-Madamfive-APIKey", mAPIKey);
                return params;
            }
        };

        request.setRetryPolicy(new DefaultRetryPolicy(10000,
                DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
        VolleySingleton.getInstance(mContext).addToRequestQueue(request);

    }

    public static void insertPatient(Context con, String name, String chno, JsonHttpResponseHandler responseHandler){
        log.i(TAG, "insertPatient:::" + name + ":::" + chno);
        String hospitalId = SmartFiPreference.getHospitalId(getContext());
        String token = SmartFiPreference.getSfToken(getContext());
        String url = Constants.bcnc.BASE_URL + "/api/v1/patient/create";
        StringEntity jsonEntityUTF8;
        JSONObject jsonParams = new JSONObject();
        try {
            jsonParams.put("id", hospitalId);
            jsonParams.put("name", name);
            jsonParams.put("chno", chno);
        } catch (JSONException e) {
            e.printStackTrace();
            log.w(TAG,e+"");
        }
        jsonEntityUTF8 = new StringEntity(jsonParams.toString(), org.apache.http.protocol.HTTP.UTF_8);

        client.addHeader("Accept", "application/json");
        client.addHeader("Content-Type", "application/json");
        client.addHeader("X-Auth-Token", token);
        client.post(con, url, jsonEntityUTF8, "application/json",responseHandler);

    }

//    public static void insertPatient (String name, String chartNumber, final JsonHttpResponseHandler responseHandler){
//
//        mAcccessToken = getAccessToken();
//        boardId = getBoardId();
//
//        String queryString = "?name="+URLEncoder.encode(name)+"&code="+URLEncoder.encode(chartNumber);
//        queryString = queryString+"&parentId="+URLEncoder.encode(chartNumber)+"&description=";
//        queryString = queryString+"&checkParentId=true"+"&type=patient&published=true";
//        queryString = queryString+"&accessToken="+URLEncoder.encode(mAcccessToken);
//
//        String relativeURL = "boards/"+boardId+"/categories"+queryString;
//
//        Log.i("URL=====", getAbsoluteUrl(relativeURL).toString());
//
//        JsonObjectRequest request = new JsonObjectRequest(Request.Method.POST,getAbsoluteUrl(relativeURL), null,
//                new Response.Listener<JSONObject>() {
//                    @Override
//                    public void onResponse(JSONObject response) {
//                        try {
//                            responseHandler.onSuccess(200, null, response.toString());
//                            Log.i(TAG, "Response:%n %s" + response.toString());
//
//                        } catch (Exception e) {
//                            e.printStackTrace();
//                        }
//
////                        callBack.onSuccess(imageInfoList);
//                    }
//                }, new Response.ErrorListener() {
//            @Override
//            public void onErrorResponse(VolleyError error) {
//                Log.i(TAG, "Error: " + error.getMessage());
//                responseHandler.onFailure(0, null, error.getLocalizedMessage(), null);
//            }
//        }) {
//            @Override
//            public Map<String, String> getHeaders() throws AuthFailureError {
//                Map<String, String> params = new HashMap<String, String>();
//                params.put("X-Madamfive-APIKey", mAPIKey);
//                return params;
//            }
//
//        };
//
//        request.setRetryPolicy(new DefaultRetryPolicy(10000,
//                DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
//                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
//        VolleySingleton.getInstance(mContext).addToRequestQueue(request);
//
//    }

    public static void deleteImage() {

        File myDir = getActivity().getExternalFilesDir(Environment.getExternalStorageState());
        if(myDir.exists()&&myDir.isDirectory()){
            File[] files = myDir.listFiles();
            int numberOfFiles = files.length;
            Arrays.sort(files, new Comparator() {
                @Override
                public int compare(Object o1, Object o2) {
                    if(((File)o1).lastModified() > ((File)o2).lastModified()) {
                        return -1;
                    }else if(((File)o1).lastModified() < ((File)o2).lastModified()){
                        return  +1;
                    }else {
                        return 0;
                    }
                }
            });
            for(int i=20;i<numberOfFiles;i++){
                if(files[i].isFile()==true){
                    Log.i(TAG,"Delete File:"+files[i]);
                    files[i].delete();
                }
            }
        }
    }

    public static void deletePhotoModelList(){

        ArrayList<PhotoModel> list = PhotoModelService.findImageListOld();
        Log.i(TAG,"List Length"+list.size());
//        for (int i = 0; i < list.size(); i++) {
//            PhotoModel ph = list.get(i);
//            Long id = ph.getId();
//            Log.i(TAG, "photoModel id:" + id.toString());
//        }

        int listSize = list.size();
        if(list.size() > 20){
            for(int i=0;i<list.size();i++){
                PhotoModel ph = list.get(i);
                Long id = ph.getId();
                PhotoModelService.deletePhotoModel(id);
                listSize = listSize - 1;
                Log.i(TAG,"Delete photoModel id:"+id.toString());
//                Log.i(TAG,"Delete photoModel i:"+i);
//                Log.i(TAG,"List Length"+list.size());
                if(listSize == 20) break;
            }
        }

    }

    public static boolean getNetworkStatus(Context con){

        ConnectivityManager connectivityManager = (ConnectivityManager) con.getSystemService(CONNECTIVITY_SERVICE);
        try{
            NetworkInfo mobile = connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_MOBILE);
            if(mobile.isConnected()){
                return true;
            }
        }catch(Exception e){
            Log.i(TAG,"mobile info :"+e.toString());
        }
        NetworkInfo wifi = connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
        if(wifi.isConnected()){
            return true;
        }else{
            return false;
        }

    }


}
