package com.dxiang.mring3.request;

import org.ksoap2.serialization.SoapObject;

import com.dxiang.mring3.utils.Commons;
import com.dxiang.mring3.utils.FusionCode;
import com.dxiang.mring3.utils.UserVariable;

import android.os.Handler;
import android.util.Log;

public class QryToneGrpRequest extends Request {
	private final static String TAG = "QryToneGrpRequest";

	/**
	 * 设置handler处理响应
	 * 
	 * @param handler
	 */
	public QryToneGrpRequest(Handler handler) {
		setHandler(handler);
	}

	/**
	 * 发送请求
	 * 
	 * @param showNum
	 * @param pageNo
	 */
	public void sendQryToneGrpRequest(int showNum, int pageNo) {
		String methodName = "QryToneGrp";
		String soapAction = Commons.SOAP_NAMESPACE + "/" + methodName;
		SoapObject rpc1 = new SoapObject(Commons.SOAP_NAMESPACE, methodName);
		// inaccessInfo
		SoapObject rpc2 = new SoapObject(Commons.SOAP_NAMESPACE, "");
		rpc2.addProperty("callNumber", UserVariable.CALLNUMBER);
		rpc2.addProperty("showNum", showNum);
		rpc2.addProperty("pageNo", pageNo);
		addDefaultProperty(rpc2);
		rpc1.addProperty("QryToneGrpEvt", rpc2);
		this.interfaceUrl = Commons.SOAP_URL + "/UserToneMan";
		Log.i(TAG, interfaceUrl);
		this.sendRequest(rpc1, FusionCode.CONNECT_TYPE_WEBSERVICE,
				FusionCode.REQUEST_QRYGRP, soapAction);
	}
}
