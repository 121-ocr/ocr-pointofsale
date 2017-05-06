package ocr.pointofsale.returnbill;

import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonObject;
import ocr.common.handler.SampleCDOBillBaseHandler;
import otocloud.common.ActionURI;
import otocloud.framework.app.function.ActionDescriptor;
import otocloud.framework.app.function.AppActivityImpl;
import otocloud.framework.app.function.BizRootType;
import otocloud.framework.app.function.BizStateSwitchDesc;
import otocloud.framework.common.CallContextSchema;
import otocloud.framework.core.CommandMessage;
import otocloud.framework.core.HandlerDescriptor;

/**
 * 退货单修改操作
 * 
 * @author wanghw
 *
 */
public class ReturnBillUpdateHandler extends SampleCDOBillBaseHandler {

	public ReturnBillUpdateHandler(AppActivityImpl appActivity) {
		super(appActivity);
		// TODO Auto-generated constructor stub
	}

	/**
	 * corecorp_setting.setting
	 */
	@Override
	public String getEventAddress() {
		return ReturnBillConstant.UPDATE_ADDRESS;
	}
	
	@Override
	public String getBizUnit(CommandMessage<JsonObject> msg){
/*		JsonObject session = msg.getSession();
		boolean is_global_bu =  session.getBoolean(CallContextSchema.IS_GLOBAL_BU, true);
*/		
		//按业务单元隔离
		String bizUnit = msg.getCallContext().getLong(CallContextSchema.BIZ_UNIT_ID).toString();		
		return 	bizUnit;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public ActionDescriptor getActionDesc() {

		ActionDescriptor actionDescriptor = super.getActionDesc();
		HandlerDescriptor handlerDescriptor = actionDescriptor.getHandlerDescriptor();

		ActionURI uri = new ActionURI(ReturnBillConstant.UPDATE_ADDRESS, HttpMethod.POST);
		handlerDescriptor.setRestApiURI(uri);

		// 状态变化定义
		BizStateSwitchDesc bizStateSwitchDesc = new BizStateSwitchDesc(BizRootType.BIZ_OBJECT,
				ReturnBillConstant.CREATE_STATUS, ReturnBillConstant.CREATE_STATUS);
		bizStateSwitchDesc.setWebExpose(true); // 是否向web端发布事件
		actionDescriptor.setBizStateSwitch(bizStateSwitchDesc);

		return actionDescriptor;
	}

}
