package ocr.pointofsale.replenishment;

import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonObject;
import otocloud.common.ActionURI;
import otocloud.framework.app.function.ActionDescriptor;
import otocloud.framework.app.function.AppActivityImpl;
import otocloud.framework.app.function.CDOHandlerImpl;
import otocloud.framework.common.CallContextSchema;
import otocloud.framework.core.CommandMessage;
import otocloud.framework.core.HandlerDescriptor;

/**
 * 补货单通知创建操作
 * 
 * @author wanghw
 *
 */
public class ReplenishmentCreateHandler extends CDOHandlerImpl<JsonObject> {

	public ReplenishmentCreateHandler(AppActivityImpl appActivity) {
		super(appActivity);
		// TODO Auto-generated constructor stub
	}

	/**
	 * corecorp_setting.setting
	 */
	@Override
	public String getEventAddress() {
		return ReplenishmentConstant.CREATE_ADDRESS;
	}

	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public ActionDescriptor getActionDesc() {

		ActionDescriptor actionDescriptor = super.getActionDesc();
		HandlerDescriptor handlerDescriptor = actionDescriptor.getHandlerDescriptor();
				
		ActionURI uri = new ActionURI(ReplenishmentConstant.CREATE_ADDRESS, HttpMethod.POST);
		handlerDescriptor.setRestApiURI(uri);
		
		//状态变化定义
/*		BizStateSwitchDesc bizStateSwitchDesc = new BizStateSwitchDesc(BizRootType.BIZ_OBJECT, 
				null, ReplenishmentConstant.CREATE_STATUS);
		bizStateSwitchDesc.setWebExpose(true); //是否向web端发布事件		
		actionDescriptor.setBizStateSwitch(bizStateSwitchDesc);*/
		
		return actionDescriptor;
	}

	@Override
	public void handle(CommandMessage<JsonObject> msg) {
		JsonObject bo = msg.getContent();
		
		String stubBoId = bo.getString("bo_id");
		String partnerAcct = bo.getString("from_account");
		JsonObject stubBo = this.buildStubForCDO(bo, stubBoId, partnerAcct);
		JsonObject actor = bo.getJsonObject("from_actor");
		
		String initState = bo.getString("current_state");
		
		//按业务单元隔离
		String bizUnit = msg.getCallContext().getString(CallContextSchema.BIZ_UNIT_ID);		
		
		recordFactData(bizUnit, appActivity.getBizObjectType(), stubBo, stubBoId, 
				null, initState, true, false, actor, null, result->{
			if (result.succeeded()) {				
				msg.reply(stubBo);				
			}else{
				Throwable errThrowable = result.cause();
				String errMsgString = errThrowable.getMessage();
				appActivity.getLogger().error(errMsgString, errThrowable);
				msg.fail(100, errMsgString);
			}
    	});
	}
	

}
