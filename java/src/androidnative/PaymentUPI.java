package androidnative;

import android.app.Activity;
import android.os.Build;
import android.content.Intent;
import android.util.Log;
import android.net.Uri;
// import android.view.View;
// import android.view.Window;
// import android.view.WindowManager;
// import android.widget.Toast;
// import android.database.Cursor;
// import android.database.sqlite.SQLiteException ;
// import android.graphics.Bitmap;
// import android.graphics.BitmapFactory ;
// import android.provider.MediaStore;
// import android.provider.MediaStore.Files ;



import org.qtproject.qt5.android.QtNative;

import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;

import java.net.URL ;



import java.net.MalformedURLException;
import java.io.IOException;


public class PaymentUPI {

    private static final String TAG = "androidnative.Payment.UPI";
    public  static final String PAYMENT_UPI_REQUEST  = "androidnative.Payment.UPIRequest";
    public  static final String PAYMENT_UPI_RESPONSE = "androidnative.Payment.UPIResponse";


    // rand : generate with  'openssl rand -hex 2' and prefix 0x
    public static final int PAYMENT_UPI_ACTION = 0x9cae ;


    static {
        SystemDispatcher.addListener(new SystemDispatcher.Listener() {
            public void onDispatched(String type, Map message) {
                Log.d(TAG, "onDispatched type is:" + type);
                if (type.equals(PAYMENT_UPI_REQUEST)) {
                    initiatePaymentUPI(message);
                    } else if (type.equals(SystemDispatcher.ACTIVITY_RESULT_MESSAGE)) {
                    onActivityResult(message);
                    }
                }
            });
        }


// Thanks to: https://github.com/Pro-Grammerr/Android-payment-using-UPI
static void initiatePaymentUPI (Map message) {

    Activity activity = org.qtproject.qt5.android.QtNative.activity();

    String upiId     = (String)  message.get("pa"); // Payment address
    String name      = (String)  message.get("pn"); // Payment name
    String note      = (String)  message.get("tn"); // Transaction Notes
    String amount    = (String)  message.get("am"); // Amount
    String currency  = (String)  message.get("cu"); // Currency

    Uri uri = Uri.parse("upi://pay").buildUpon()
    .appendQueryParameter("pa", upiId)
    .appendQueryParameter("pn", name)
    .appendQueryParameter("tn", note)
    .appendQueryParameter("am", amount)
    .appendQueryParameter("cu", currency)
    .build();


    Intent theIntent = new Intent(Intent.ACTION_VIEW);
    theIntent.setData(uri);
    Intent chooser = Intent.createChooser(theIntent, "Pay with");

    if(null != chooser.resolveActivity(activity.getPackageManager())) {
        activity.startActivityForResult(chooser, PAYMENT_UPI_ACTION);
        } else {
								Map reply = new HashMap();
								reply.put("status","FAILED" );
								reply.put("reference_no", "" );
								reply.put("txnId", "" );
								reply.put("status_message", "No UPI App found" );
								reply.put("full_response", "" );
								SystemDispatcher.dispatch(PAYMENT_UPI_RESPONSE,reply);
        }

    }

static private void onActivityResult(Map message) {
    int resultCode  = (Integer) message.get("resultCode");
    int requestCode = (Integer) message.get("requestCode");
    Intent data     = (Intent)  message.get("data");


    if ( requestCode != PAYMENT_UPI_ACTION ) {
        Log.d(TAG, "request code is NOT PAYMENT_UPI_ACTION , i do not handle that." );
        return;
        }
    Log.d(TAG, "request code is PAYMENT_UPI_ACTION , hence proceeding" );

    if ((Activity.RESULT_OK == resultCode) || (resultCode == 11)) {
        if (data != null) {
            String trxt = data.getStringExtra("response");
            Log.d(TAG, "onActivityResult: " + trxt);
            ArrayList<String> dataList = new ArrayList<>();
            dataList.add(trxt);
            upiPaymentDataOperation(dataList);
            } else {
            Log.d(TAG , "onActivityResult: " + "Return data is null");
            ArrayList<String> dataList = new ArrayList<>();
            dataList.add("nothing");
            upiPaymentDataOperation(dataList);
            }
        } else {
        Log.d(TAG, "onActivityResult: " + "Return data is null"); //when user simply back without payment
        ArrayList<String> dataList = new ArrayList<>();
        dataList.add("nothing");
        upiPaymentDataOperation(dataList);
        }

    }


// Thanks to: https://github.com/Pro-Grammerr/Android-payment-using-UPI
// Code lifted from there and modified to to send reply to QML

static void upiPaymentDataOperation(ArrayList<String> data) {


    Activity activity = org.qtproject.qt5.android.QtNative.activity();
    Map reply = new HashMap();


    String str = data.get(0);
    Log.d(TAG, "upiPaymentDataOperation: "+str);
    String paymentCancel = "";
    if(str == null) str = "discard";
    String status = "";
    String approvalRefNo = "";
    String txnId = "";
    String fullResponse = str;
    String response[] = str.split("&");
    for (int i = 0; i < response.length; i++) {
        String equalStr[] = response[i].split("=");
        if(equalStr.length >= 2) {
            if (equalStr[0].toLowerCase().equals("Status".toLowerCase())) {
                status = equalStr[1].toLowerCase();
                }
            else if (
            equalStr[0].toLowerCase().equals("ApprovalRefNo".toLowerCase())
            || equalStr[0].toLowerCase().equals("txnRef".toLowerCase())
            ) {
                approvalRefNo = equalStr[1];
                }
            else if (equalStr[0].toLowerCase().equals("txnId".toLowerCase()) ) {
                txnId = equalStr[1];
                }
            }
        else {
            paymentCancel = "Payment cancelled by user.";
            }
        }

    if (status.equals("success")) {
        //Code to handle successful transaction here.
        Log.d(TAG, "UPI responseStr: "+approvalRefNo);
        reply.put("status","SUCCESS" );
        reply.put("reference_no", approvalRefNo );
        reply.put("txnId", txnId );
        reply.put("status_message", "Transaction successful" );
        reply.put("full_response", fullResponse );
        }
    else if("Payment cancelled by user.".equals(paymentCancel)) {

        reply.put("status","CANCELLED" );
        reply.put("reference_no", "" );
        reply.put("txnId", "" );
        reply.put("status_message", "Transaction canceled" );
        reply.put("full_response", fullResponse );
        }
    else {
        reply.put("status","FAILED" );
        reply.put("reference_no", "" );
        reply.put("txnId", "" );
        reply.put("status_message", "Transaction failed.Please try again." );
        reply.put("full_response", fullResponse );
        }

    SystemDispatcher.dispatch(PAYMENT_UPI_RESPONSE,reply);

    }
}
