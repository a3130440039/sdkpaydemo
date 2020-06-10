package com.youxibi.czddz;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import com.alipay.sdk.app.PayTask;
import com.tencent.mm.opensdk.modelmsg.SendAuth;
import com.tencent.mm.opensdk.modelmsg.SendMessageToWX;
import com.tencent.mm.opensdk.modelmsg.WXMediaMessage;
import com.tencent.mm.opensdk.modelmsg.WXWebpageObject;
import com.tencent.mm.opensdk.openapi.IWXAPI;
import com.tencent.mm.opensdk.openapi.WXAPIFactory;
import com.youxibi.czddz.alipay.PayResult;
import com.youxibi.czddz.alipay.RequestAliPay;
import com.youxibi.czddz.wechat.RequestWXPay;

import java.net.URL;
import java.net.URLEncoder;
import java.util.Map;


public class MainActivity extends Activity {
    public static final String APP_ID = "wxb56c786c3922f135";
    public static final String APP_SECRECT = "0231cf55fe2c95af26ca9c8afca32a04";
    private IWXAPI api;
    private EditText orderInfo;
   // private EditText amount;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        api = WXAPIFactory.createWXAPI(this, APP_ID, false);
        if (!api.isWXAppInstalled()) {
            Toast.makeText(this, "请先安装微信", Toast.LENGTH_SHORT).show();
        }
        api.registerApp(APP_ID); // 将该app注册到微信
    }

    public IWXAPI getWXAPI() {
        return api;
    }

    public void onAliPayClick(View view) {
        new RequestAliPay(this).execute("http://139.199.176.70/alipay?total_amount=0.01"); // 支付1分钱
    }

    public void onAliSDKClick(View view) throws Exception {
        final Activity activity =this;
        final EditText orderInfo = findViewById(R.id.orderInfo);
        Runnable payRunnable = new Runnable() {
            @Override
            public void run() {
                PayTask alipay = new PayTask(activity);
                Map<String, String> result = alipay.payV2(String.valueOf(orderInfo.getText().toString()), true);
                PayResult payResult = new PayResult(result);
                /**
                 对于支付结果，请商户依赖服务端的异步通知结果。同步通知结果，仅作为支付结束的通知。
                 */
                // String resultInfo = payResult.getResult();// 同步返回需要验证的信息
                final String resultStatus = payResult.getResultStatus();
                Log.i("RequestAliPay", "result status: " + resultStatus);
                activity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        // 判断resultStatus 为9000则代表支付成功
                        if (TextUtils.equals(resultStatus, "9000")) {
                            // 该笔订单是否真实支付成功，需要依赖服务端的异步通知。
                            Toast.makeText(activity, "支付成功", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(activity, "支付失败", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
            }
        };
        Thread payThread = new Thread(payRunnable);
        payThread.start();
    }

    public void onWXPayClick(View view) {
        new RequestWXPay(this).execute("http://czddz.shenzhouxing.com:8084/wxpay?total_fee=1&account_id=4368191"); // 支付1分钱
    }

    public void onWeChatLoginClick(View view) {
        final SendAuth.Req req = new SendAuth.Req();
        req.scope = "snsapi_userinfo";
        req.state = "none";
        api.sendReq(req);
    }

    public void onWeChatSendToFriend(View view) {
        shareWebpage("http://www.baidu.com/", "这是标题", "这是描述", SendMessageToWX.Req.WXSceneSession);
    }

    public void onWeChatSendToTimeline(View view) {
        shareWebpage("http://www.baidu.com/", "这是标题", "这是描述", SendMessageToWX.Req.WXSceneTimeline);
    }

    public void shareWebpage(String url, String title, String desc, int scene) {
        WXWebpageObject webpage = new WXWebpageObject();
        webpage.webpageUrl = url;
        WXMediaMessage msg = new WXMediaMessage(webpage);
        msg.title = title;
        msg.description = desc;
        Bitmap bmp = BitmapFactory.decodeResource(getResources(), R.mipmap.icon);
        Bitmap thumbBmp = Bitmap.createScaledBitmap(bmp, 150, 150, true);
        bmp.recycle();
        msg.thumbData = Util.bmpToByteArray(thumbBmp, true);

        SendMessageToWX.Req req = new SendMessageToWX.Req();
        req.transaction = buildTransaction("webpage");
        req.message = msg;
        req.scene = scene;
        api.sendReq(req);
    }

    private String buildTransaction(final String type) {
        return (type == null) ? String.valueOf(System.currentTimeMillis()) : type + System.currentTimeMillis();
    }
}
