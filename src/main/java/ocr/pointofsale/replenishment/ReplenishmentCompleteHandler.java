package ocr.pointofsale.replenishment;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonObject;
import otocloud.common.ActionContextTransfomer;
import otocloud.common.ActionURI;
import otocloud.framework.app.common.BizRoleDirection;
import otocloud.framework.app.function.ActionDescriptor;
import otocloud.framework.app.function.AppActivityImpl;
import otocloud.framework.app.function.BizRootType;
import otocloud.framework.app.function.BizStateSwitchDesc;
import otocloud.framework.app.function.CDOHandlerImpl;
import otocloud.framework.core.CommandMessage;
import otocloud.framework.core.HandlerDescriptor;

/**
 * 补货单通知创建操作
 * 
 * @author wanghw
 *
 */
public class ReplenishmentCompleteHandler extends CDOHandlerImpl<JsonObject> {

	public ReplenishmentCompleteHandler(AppActivityImpl appActivity) {
		super(appActivity);
		// TODO Auto-generated constructor stub
	}

	/**
	 * corecorp_setting.setting
	 */
	@Override
	public String getEventAddress() {
		return ReplenishmentConstant.COMPLETED_ADDRESS;
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
		BizStateSwitchDesc bizStateSwitchDesc = new BizStateSwitchDesc(BizRootType.BIZ_OBJECT, 
				null, ReplenishmentConstant.COMPLETED_STATUS);
		bizStateSwitchDesc.setWebExpose(true); //是否向web端发布事件		
		actionDescriptor.setBizStateSwitch(bizStateSwitchDesc);
		
		return actionDescriptor;
	}

	@Override
	public void handle(CommandMessage<JsonObject> msg) {
		JsonObject bo = msg.getContent();
		
		String boId = bo.getString("bo_id");
		String partnerAcct = bo.getString("from_account");
		
		JsonObject actor = ActionContextTransfomer.fromMessageHeaderToActor(msg.headers()); 
		
		this.processComplete(boId, partnerAcct, actor, result->{
			if (result.succeeded()) {
				msg.reply("ok");						
			}else{
				Throwable errThrowable = result.cause();
				msg.fail(100, errThrowable.getMessage());
			}
    	});
	}
	
	public void processComplete(String boId, String partnerAcct, JsonObject actor, Handler<AsyncResult<String>> ret){
		
		Future<String> retFuture = Future.future();
		retFuture.setHandler(ret);
		
		this.recordFactData(null, appActivity.getBizObjectType(), null,
				boId, actor, null, result->{
			if (result.succeeded()) {				
				
				this.recordCDO(null, BizRoleDirection.TO, partnerAcct, appActivity.getBizObjectType(), 
				null, boId, actor, next->{
					if (next.succeeded()) {			
						retFuture.complete("ok");
					}else{
						Throwable errThrowable = next.cause();
						String errMsgString = errThrowable.getMessage();
						appActivity.getLogger().error(errMsgString, errThrowable);
						retFuture.fail(errThrowable);
					}
				});			
						
			}else{
				Throwable errThrowable = result.cause();
				String errMsgString = errThrowable.getMessage();
				appActivity.getLogger().error(errMsgString, errThrowable);
				retFuture.fail(errThrowable);
			}
    	});
	}
	

}
