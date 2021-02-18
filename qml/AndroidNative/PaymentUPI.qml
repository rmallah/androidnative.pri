import QtQuick 2.12
import AndroidNative 1.0

Item {

    property string m_PAYMENT_UPI_REQUEST: "androidnative.Payment.UPIRequest" ;
    property string m_PAYMENT_UPI_RESPONSE: "androidnative.Payment.UPIResponse" ;

    signal upiResponse(var response ) ;


    function initiateUPI(params) {
        console.log("called initiateUPI with params:" + JSON.stringify(params));
        var args = {};
        if (params === undefined ) {
            params={};
        }

        var arglist = [ 'pa' , 'pn' , 'tn' , 'am' , 'cu' ];

        arglist.map (
                    function (name) {
                        if (params[name] !== undefined )
                            args[name] = params[name];
                    }
                    );

        args["title"] = args["title"] || "UPI Payment";
        SystemDispatcher.dispatch( m_PAYMENT_UPI_REQUEST , args );
    }

    Component.onCompleted: {
        // load the Java class Util in package androidnative
        SystemDispatcher.loadClass("androidnative.PaymentUPI");
    }

    Connections {
        target: SystemDispatcher
        onDispatched: {
            if (type === m_PAYMENT_UPI_RESPONSE) {
                var response={};
                response["status"] = message.status;
                response["reference_no"] = message.reference_no;
                response["status_message"] = message.status_message;
                response["full_response"] = message.full_response;
                response["txn_id"] = message.txnId;
                console.debug("about to emit signal upiResponse(response):" + JSON.stringify(response));
                upiResponse (response);
            }
        }
    }
}
