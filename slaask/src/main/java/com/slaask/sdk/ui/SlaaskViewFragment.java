package com.slaask.sdk.ui;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v4.app.Fragment;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.webkit.*;
import com.slaask.sdk.R;
import com.slaask.sdk.Slaask;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedList;

public class SlaaskViewFragment extends Fragment {
    private static final String TAG = SlaaskViewFragment.class.getSimpleName();

    public static final int INPUT_FILE_REQUEST_CODE = 1;

    static WebView mWebView;
    private ValueCallback<Uri[]> mFilePathCallback;
    private String mCameraPhotoPath;

    private static LinkedList<String> commandQueue = new LinkedList<String>();

    public static boolean isLoaded = false;

    static String color = "blue";

    public SlaaskViewFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.slaask_view, container, false);

        mWebView = rootView.findViewById(R.id.slaask_view_webview);

        setUpWebViewDefaults(mWebView);

        if (savedInstanceState != null) {
            mWebView.restoreState(savedInstanceState);
        }

        mWebView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);

                isLoaded = true;
                flushQueue();
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                if (url.startsWith("mailto")) {
                    handleMailToLink(url);
                    return true;
                }

                return false;
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                String url = "";
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
                    url = request.getUrl().toString();
                }

                if (url.startsWith("mailto")) {
                    handleMailToLink(url);
                    return true;
                }

                if (url.startsWith("tel:")) {
                    handleTelToLink(url);
                    return true;
                }

                if (url.startsWith("slaask:")) {
                    handleSlaaskToLink(url);
                    return true;
                }

                handleUrlToLink(url);
                return true;
            }
        });

        mWebView.setWebChromeClient(new WebChromeClient() {
            public boolean onShowFileChooser(
                    WebView webView, ValueCallback<Uri[]> filePathCallback,
                    WebChromeClient.FileChooserParams fileChooserParams) {
                if (mFilePathCallback != null) {
                    mFilePathCallback.onReceiveValue(null);
                }
                mFilePathCallback = filePathCallback;

                Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                if (takePictureIntent.resolveActivity(getActivity().getPackageManager()) != null) {
                    File photoFile = null;
                    try {
                        photoFile = createImageFile();
                        takePictureIntent.putExtra("PhotoPath", mCameraPhotoPath);
                    } catch (IOException ex) {
                        Log.e(TAG, "Unable to create Image File", ex);
                    }

                    if (photoFile != null) {
                        mCameraPhotoPath = "file:" + photoFile.getAbsolutePath();
                        takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT,
                                Uri.fromFile(photoFile));
                    } else {
                        takePictureIntent = null;
                    }
                }

                Intent contentSelectionIntent = new Intent(Intent.ACTION_GET_CONTENT);
                contentSelectionIntent.addCategory(Intent.CATEGORY_OPENABLE);
                contentSelectionIntent.setType("image/*");

                Intent[] intentArray;
                if (takePictureIntent != null) {
                    intentArray = new Intent[]{takePictureIntent};
                } else {
                    intentArray = new Intent[0];
                }

                Intent chooserIntent = new Intent(Intent.ACTION_CHOOSER);
                chooserIntent.putExtra(Intent.EXTRA_INTENT, contentSelectionIntent);
                chooserIntent.putExtra(Intent.EXTRA_TITLE, "Image Chooser");
                chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, intentArray);

                startActivityForResult(chooserIntent, INPUT_FILE_REQUEST_CODE);

                return true;
            }
        });

        mWebView.loadUrl("https://xeno.app/sdk-views/android.html");

        load();
        return rootView;
    }

    private File createImageFile() throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES);
        File imageFile = File.createTempFile(
                imageFileName,
                ".jpg",
                storageDir
        );
        return imageFile;
    }

    public void setColor(String color) {
        this.color = color;
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    private void setUpWebViewDefaults(WebView webView) {
        WebSettings settings = webView.getSettings();

        settings.setJavaScriptEnabled(true);

        settings.setUseWideViewPort(true);
        settings.setLoadWithOverviewMode(true);
        settings.setDatabaseEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setBuiltInZoomControls(true);
        settings.setDisplayZoomControls(false);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            WebView.setWebContentsDebuggingEnabled(true);
        }

        mWebView.addJavascriptInterface(new MyJavaScriptInterface(), "JSInterface");
        mWebView.setWebViewClient(new WebViewClient());
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode != INPUT_FILE_REQUEST_CODE || mFilePathCallback == null) {
            super.onActivityResult(requestCode, resultCode, data);
            return;
        }

        Uri[] results = null;

        // Check that the response is a good one
        if (resultCode == Activity.RESULT_OK) {
            if (data == null) {
                // If there is not data, then we may have taken a photo
                if (mCameraPhotoPath != null) {
                    results = new Uri[]{Uri.parse(mCameraPhotoPath)};
                }
            } else {
                String dataString = data.getDataString();
                if (dataString != null) {
                    results = new Uri[]{Uri.parse(dataString)};
                }
            }
        }

        mFilePathCallback.onReceiveValue(results);
        mFilePathCallback = null;
    }

    @Override
    public void onDetach() {
        isLoaded = false;
        super.onDetach();
    }

    protected void handleSlaaskToLink(String url) {
        if (url.contains("closeButtonPressed")) {
            getActivity().finish();
            getActivity().overridePendingTransition(R.anim.nothing, R.anim.slide_out_down);

        }
    }

    protected void handleUrlToLink(String url) {
        Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
        startActivity(browserIntent);
    }

    protected void handleTelToLink(String url) {
        Intent intent = new Intent(Intent.ACTION_DIAL);
        intent.setData(Uri.parse(url));
        startActivity(intent);
    }

    protected void handleMailToLink(String url) {
        // Initialize a new intent which action is send
        Intent intent = new Intent(Intent.ACTION_SEND);

        // For only email app handle this intent
        intent.setData(Uri.parse("mailto:"));

        intent.setType("plain/text");

        // Empty the text view
        // Extract the email address from mailto url
        String to = url.split("[:?]")[1];
        if (!TextUtils.isEmpty(to)) {
            intent.putExtra(Intent.EXTRA_EMAIL, new String[]{to});
        }


        // Extract the subject
        if (url.contains("subject=")) {
            String subject = url.split("subject=")[1];
            if (!TextUtils.isEmpty(subject)) {
                intent.putExtra(Intent.EXTRA_SUBJECT, subject);
            }
        }

        // Extract the body
        if (url.contains("body=")) {
            String body = url.split("body=")[1];
            if (!TextUtils.isEmpty(body)) {
                body = body.split("&")[0];
                // Encode the body text
                try {
                    body = URLDecoder.decode(body, "UTF-8");
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
                // Put the mail body into intent
                intent.putExtra(Intent.EXTRA_TEXT, body);
            }
        }

        startActivity(intent);
    }

    static void callJavascript(String script) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT) {
            mWebView.evaluateJavascript(script, null);
        } else {
            mWebView.loadUrl("javascript:" + script);
        }
    }

    static void flushQueue() {
        for (String script : commandQueue) {
            callJavascript(script);
        }
        commandQueue.clear();
    }

    public static void load() {
        execute("document.getElementById('android-lds-dual-ring').style.setProperty('color', '" + color + "');");
        execute("window._slaaskSettings.key = \"" + Slaask.getInstance().getApiKey() + "\";");
        execute("window._slaaskSettings.options = { native_sdk: true }");
        execute("window._slaaskSettings.identify = function() { return " + Slaask.getIdentity().build() + " }");
        execute("var slaaskLoaderScript = document.createElement('script'); slaaskLoaderScript.src = 'https://cdn.slaask.com/chat_loader.js?t=" + Slaask.getInstance().getApiKey() + "'; document.head.appendChild(slaaskLoaderScript);");
    }

    public static void execute(String script) {
        commandQueue.add(script);
        if (isLoaded) {
            flushQueue();
        }
    }


    public void goBack() {
        execute("if (typeof _slaask === 'undefined' || !_slaask.goBackToPreviousView()) { document.location.href = 'slaask://closeButtonPressed' }");
    }

    class MyJavaScriptInterface {
        @JavascriptInterface
        public void onChatLoaded(final String color) {
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    getActivity().getWindow().clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
                    getActivity().getWindow().addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
                    getActivity().getWindow().setStatusBarColor(Color.parseColor(color));
                }
            });
        }
    }
}

