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
 * 退货单提交
 * @author pcitc
 *
 */
public class ReturnBillCommitHandler extends SampleCDOBillBaseHandler{
	
	public ReturnBillCommitHandler(AppActivityImpl appActivity) {
		super(appActivity);
	}

	//此action的入口地址
	@Override
	public String getEventAddress() {
		return ReturnBillConstant.COMMIT_ADDRESS;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public ActionDescriptor getActionDesc() {

		ActionDescriptor actionDescriptor = super.getActionDesc();
		HandlerDescriptor handlerDescriptor = actionDescriptor.getHandlerDescriptor();
				
		ActionURI uri = new ActionURI(ReturnBillConstant.COMMIT_ADDRESS, HttpMethod.POST);
		handlerDescriptor.setRestApiURI(uri);
		
		//状态变化定义
		BizStateSwitchDesc bizStateSwitchDesc = new BizStateSwitchDesc(BizRootType.BIZ_OBJECT, 
				ReturnBillConstant.CREATE_STATUS, ReturnBillConstant.COMMIT_ADDRESS);
		bizStateSwitchDesc.setWebExpose(true); //是否向web端发布事件		
		actionDescriptor.setBizStateSwitch(bizStateSwitchDesc);
		
		return actionDescriptor;
	}	
}
