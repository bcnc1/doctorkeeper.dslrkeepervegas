package com.doctorkeeper.dslrkeeper2022.view.cloud;

import android.app.FragmentTransaction;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import androidx.annotation.Nullable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.GridView;
import android.widget.TextView;
import android.widget.Toast;

import com.loopj.android.http.JsonHttpResponseHandler;
import com.doctorkeeper.dslrkeeper2022.R;
import com.doctorkeeper.dslrkeeper2022.API.BcncAPI;
import com.doctorkeeper.dslrkeeper2022.view.BaseFragment;

import org.apache.commons.io.comparator.LastModifiedFileComparator;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;

import butterknife.BindView;
import butterknife.ButterKnife;
import cz.msebera.android.httpclient.Header;

import android.annotation.SuppressLint;


public class CloudGalleryFragment extends BaseFragment implements AdapterView.OnItemClickListener, AbsListView.OnScrollListener{

    private final Handler handler = new Handler();

    @SuppressLint("NonConstantResourceId")
    @BindView(R.id.cloud_list)
    GridView galleryView;

    private CloudGalleryAdapter cloudGalleryAdapter;

    private SimpleDateFormat formatParser;
    private int currentScrollState;
    HashMap<String,String> pictureMap;

    @SuppressLint("NonConstantResourceId")
    @BindView(R.id.cloud_empty_textview)
    TextView emptyView;

    private ArrayList<HashMap<String,String>> imageInfoList;
    private final String TAG = CloudGalleryFragment.class.getSimpleName();

    public static CloudGalleryFragment newInstance() {
        CloudGalleryFragment f = new CloudGalleryFragment();
        return f;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.cloud_gallery_frag, container, false);
        ButterKnife.bind(this, view);

        cloudGalleryAdapter = new CloudGalleryAdapter(getActivity());
        getImagesList();
        enableUi(true);
        return view;
    }

    public void enableUi(final boolean enabled) {
        galleryView.setEnabled(enabled);
//        Log.i(TAG, "Cloud enableUi..." + enabled);
        if (getActivity()==null)
            return;

        (getActivity()).runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (cloudGalleryAdapter==null)
                    return;

                cloudGalleryAdapter.setItems(imageInfoList);
                galleryView.setAdapter(cloudGalleryAdapter);
            }
        });
        galleryView.setOnItemClickListener(this);
    }

    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {

        pictureMap = new HashMap<>();
        pictureMap = cloudGalleryAdapter.getItem(position);

//        if(pictureMap.get("cameraKind") == "Video"){
//            Toast.makeText(getActivity(), "비디오파일은 미리보기가 제공되지 않습니다!", Toast.LENGTH_SHORT).show();
//        } else{
        String imageUrl = pictureMap.get("fullPath");
        String imageGuid = "";

        FragmentTransaction ft = getFragmentManager().beginTransaction();
        ft.replace(R.id.cloud_detail_container, CloudPictureFragment.newInstance(cloudGalleryAdapter.getItemHandle(position), imageUrl, imageGuid), null);
        ft.addToBackStack(null);
        ft.commit();
//        }

    }

    @Override
    public void onScrollStateChanged(AbsListView view, int scrollState) {
        currentScrollState = scrollState;

        if (scrollState == AbsListView.OnScrollListener.SCROLL_STATE_IDLE) {
            for (int i = 0; i < galleryView.getChildCount(); ++i) {
                View child = view.getChildAt(i);
                if (child == null) {
                    continue;
                }
                CloudGalleryAdapter.ViewHolder holder = (CloudGalleryAdapter.ViewHolder) child.getTag();
                if (!holder.done) {
                    holder.done = true;
//                        camera.retrieveImageInfo(this, holder.objectHandle);
                }
            }
        }
    }

    @Override
    public void onScroll(AbsListView absListView, int i, int i1, int i2) {
    }

    private void getImagesList(){

        imageInfoList = new ArrayList<HashMap<String, String>>();
//        String path = MadamfiveAPI.getActivity().getExternalFilesDir(Environment.getExternalStorageState();
        Log.d("Files", "Path1: " + BcncAPI.getActivity().getExternalFilesDir(Environment.getExternalStorageState())+File.separator);
        File directory = new File(BcncAPI.getActivity().getExternalFilesDir(Environment.getExternalStorageState())+File.separator);
        File[] files = directory.listFiles();
        Arrays.sort(files, LastModifiedFileComparator.LASTMODIFIED_COMPARATOR);
//        Log.d("Files", "Size: "+ files.length);
        for (int i = 0; i < files.length; i++)
        {
            Log.d("Files", "fullPath:" + files[i].getAbsolutePath());
            Log.d("Files", "FileName:" + files[i].getName());
            String fileName = files[i].getName();
            if(fileName.indexOf("jpg")>0){
                HashMap<String,String> imageInfo = new HashMap<>();
                imageInfo.put("fullPath", files[i].getAbsolutePath());
                imageInfo.put("uploadDate", files[i].lastModified()+"");
                imageInfoList.add(0,imageInfo);
            }
        }
        Log.i("List in CloudFragment",imageInfoList.size()+"");
    }


}
