package ocr.pointofsale.returnbill;

import io.vertx.core.http.HttpMethod;
import ocr.common.handler.SampleCDOBillBaseHandler;
import otocloud.common.ActionURI;
import otocloud.framework.app.function.ActionDescriptor;
import otocloud.framework.app.function.AppActivityImpl;
import otocloud.framework.app.function.BizRootType;
import otocloud.framework.app.function.BizStateSwitchDesc;
import otocloud.framework.core.HandlerDescriptor;

/**
 * 退货单完成操作
 * 
 * @author wanghw
 *
 */
public class ReturnBillCompleteHandler extends SampleCDOBillBaseHandler{

	public ReturnBillCompleteHandler(AppActivityImpl appActivity) {
		super(appActivity);
	}

	/**
	 * corecorp_setting.setting
	 */
	@Override
	public String getEventAddress() {
		return ReturnBillConstant.COMPLETED_ADDRESS;
	}

	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public ActionDescriptor getActionDesc() {

		ActionDescriptor actionDescriptor = super.getActionDesc();
		HandlerDescriptor handlerDescriptor = actionDescriptor.getHandlerDescriptor();
				
		ActionURI uri = new ActionURI(ReturnBillConstant.CREATE_ADDRESS, HttpMethod.POST);
		handlerDescriptor.setRestApiURI(uri);
		
		//状态变化定义
		BizStateSwitchDesc bizStateSwitchDesc = new BizStateSwitchDesc(BizRootType.BIZ_OBJECT, 
				ReturnBillConstant.COMMIT_STATUS, ReturnBillConstant.COMPLETED_STATUS);
		bizStateSwitchDesc.setWebExpose(true); //是否向web端发布事件		
		actionDescriptor.setBizStateSwitch(bizStateSwitchDesc);
		
		return actionDescriptor;
	}
}
