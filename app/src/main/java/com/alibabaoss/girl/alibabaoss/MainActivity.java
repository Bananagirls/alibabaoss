package com.alibabaoss.girl.alibabaoss;

import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.alibaba.sdk.android.oss.ClientConfiguration;
import com.alibaba.sdk.android.oss.ClientException;
import com.alibaba.sdk.android.oss.OSS;
import com.alibaba.sdk.android.oss.OSSClient;
import com.alibaba.sdk.android.oss.ServiceException;
import com.alibaba.sdk.android.oss.callback.OSSCompletedCallback;
import com.alibaba.sdk.android.oss.callback.OSSProgressCallback;
import com.alibaba.sdk.android.oss.common.auth.OSSCredentialProvider;
import com.alibaba.sdk.android.oss.common.auth.OSSFederationCredentialProvider;
import com.alibaba.sdk.android.oss.common.auth.OSSFederationToken;
import com.alibaba.sdk.android.oss.common.auth.OSSPlainTextAKSKCredentialProvider;
import com.alibaba.sdk.android.oss.internal.OSSAsyncTask;
import com.alibaba.sdk.android.oss.model.ObjectMetadata;
import com.alibaba.sdk.android.oss.model.PutObjectRequest;
import com.alibaba.sdk.android.oss.model.PutObjectResult;
import com.google.android.gms.appindexing.Action;
import com.google.android.gms.appindexing.AppIndex;
import com.google.android.gms.common.api.GoogleApiClient;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class MainActivity extends AppCompatActivity {
    private static final String endpoint = "http://oss-cn-shanghai.aliyuncs.com";
    private static final String accessKeyId = "*******";
    private static final String accessKeySecret = "********";
    private static final String bucketName = "*******";

    private OSSClient oss;
    private String picturePath;//上传文件路径
    private String name;


    private void initoss() {
        OSSCredentialProvider credentialProvider = new OSSPlainTextAKSKCredentialProvider(accessKeyId, accessKeySecret);
// 服务器获取token
//  OSSCredentialProvider credentialProvider = new OSSFederationCredentialProvider() {
//            @Override
//            public OSSFederationToken getFederationToken() {
//                String stsJson;
//                OkHttpClient client = new OkHttpClient();
//                Request request = new Request.Builder().url(endpoint).build();
//                try {
//                    Response response = client.newCall(request).execute();
//                    if (response.isSuccessful()) {
//                        stsJson = response.body().string();
//                    } else {
//                        throw new IOException("Unexpected code " + response);
//                    }
//                } catch (IOException e) {
//                    e.printStackTrace();
//                    Log.e("GetSTSTokenFail", e.toString());
//                    return null;
//                }
//
//                try {
//                    JSONObject jsonObjs = new JSONObject(stsJson);
//                    String ak = jsonObjs.getString("AccessKeyId");
//                    String sk = jsonObjs.getString("AccessKeySecret");
//                    String token = jsonObjs.getString("SecurityToken");
//                    String expiration = jsonObjs.getString("Expiration");
//                    return new OSSFederationToken(ak, sk, token, expiration);
//                } catch (JSONException e) {
//                    Log.e("GetSTSTokenFail", e.toString());
//                    e.printStackTrace();
//                    return null;
//                }
//            }
//        };
//        ClientConfiguration conf = new ClientConfiguration();
//        conf.setConnectionTimeout(15 * 1000); // 连接超时，默认15秒
//        conf.setSocketTimeout(15 * 1000); // socket超时，默认15秒
//        conf.setMaxConcurrentRequest(5); // 最大并发请求书，默认5个
//        conf.setMaxErrorRetry(2); // 失败后最大重试次数，默认2次
//        oss = new OSSClient(getApplicationContext(), endpoint, credentialProvider, conf);
        oss = new OSSClient(getApplicationContext(), endpoint, credentialProvider);
    }

    private TextView tv_upload;
    private TextView tv_chooseimg;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initoss();
        initView();
        initListener();

    }

    private void initListener() {
        tv_upload.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                upload();
            }
        });
        /**
         * 选择图片
         */
        tv_chooseimg.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent i = new Intent(
                        Intent.ACTION_PICK,
                        android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);

                startActivityForResult(i, 0);
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 0) {
            Uri selectedImage = data.getData();
            String[] filePathColumn = {MediaStore.Images.Media.DATA};

            Cursor cursor = getContentResolver().query(selectedImage,
                    filePathColumn, null, null, null);
            cursor.moveToFirst();

            int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
            picturePath = cursor.getString(columnIndex);
            Log.d("PickPicture", picturePath);
            cursor.close();
            TextView tv = (TextView) findViewById(R.id.tv_name);
            tv.setText("路径" + picturePath);
            EditText et = (EditText) findViewById(R.id.ev_name);
            if (!TextUtils.isEmpty(et.getText().toString())) {
                name = et.getText().toString();
            } else {
                name = "文件未命名";
            }

        }
    }

    private void upload() {
        // 构造上传请求
        PutObjectRequest put = new PutObjectRequest(bucketName, name, picturePath);
        //设置上传时header的值
        ObjectMetadata objectMetadata = new ObjectMetadata();
        objectMetadata.setContentType("在Web服务中定义文件的类型，决定以什么形式、什么编码读取这个文件");
        objectMetadata.setCacheControl("");
        objectMetadata.setContentLength(512);
        //md5 加密
        objectMetadata.setContentMD5("");
        put.setMetadata(objectMetadata);
        // 异步上传时可以设置进度回调
        put.setProgressCallback(new OSSProgressCallback<PutObjectRequest>() {
            @Override
            public void onProgress(PutObjectRequest request, long currentSize, long totalSize) {
                Log.d("PutObject", "currentSize: " + currentSize + " totalSize: " + totalSize);
            }
        });
        //异步上传 可在主线程中更新UI可在主线程
        OSSAsyncTask task = oss.asyncPutObject(put, new OSSCompletedCallback<PutObjectRequest, PutObjectResult>() {
            @Override
            public void onSuccess(PutObjectRequest request, PutObjectResult result) {
                Log.d("PutObject", "UploadSuccess");
                Toast.makeText(MainActivity.this, "上传成功", Toast.LENGTH_LONG).show();
            }

            @Override
            public void onFailure(PutObjectRequest request, ClientException clientExcepion, ServiceException serviceException) {
                Toast.makeText(MainActivity.this, "上传失败", Toast.LENGTH_LONG).show();

                // 请求异常
                if (clientExcepion != null) {
                    // 本地异常如网络异常等
                    clientExcepion.printStackTrace();
                }
                if (serviceException != null) {
                    // 服务异常
                    Log.e("ErrorCode", serviceException.getErrorCode());
                    Log.e("RequestId", serviceException.getRequestId());
                    Log.e("HostId", serviceException.getHostId());
                    Log.e("RawMessage", serviceException.getRawMessage());
                }
            }
        });

// task.cancel(); // 可以取消任务

// task.waitUntilFinished(); // 可以等待直到任务完成
    }

    private void initView() {
        tv_upload = (TextView) findViewById(R.id.tv_upload);
        tv_chooseimg = (TextView) findViewById(R.id.tv_chooseimg);
    }


}
