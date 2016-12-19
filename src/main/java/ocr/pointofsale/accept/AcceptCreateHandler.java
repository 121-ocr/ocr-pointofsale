package ocr.pointofsale.accept;

import io.vertx.core.MultiMap;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import otocloud.common.ActionContextTransfomer;
import otocloud.common.ActionURI;
import otocloud.framework.app.function.ActionDescriptor;
import otocloud.framework.app.function.AppActivityImpl;
import otocloud.framework.app.function.BizRootType;
import otocloud.framework.app.function.BizStateSwitchDesc;
import otocloud.framework.app.function.CDOHandlerImpl;
import otocloud.framework.core.HandlerDescriptor;
import otocloud.framework.core.OtoCloudBusMessage;

/**
 * 收货通知创建操作
 * 
 * @author wanghw
 *
 */
public class AcceptCreateHandler extends CDOHandlerImpl<JsonObject> {

	public AcceptCreateHandler(AppActivityImpl appActivity) {
		super(appActivity);
		// TODO Auto-generated constructor stub
	}

	/**
	 * corecorp_setting.setting
	 */
	@Override
	public String getEventAddress() {
		return AcceptConstant.CREATE_ADDRESS;
	}

	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public ActionDescriptor getActionDesc() {

		ActionDescriptor actionDescriptor = super.getActionDesc();
		HandlerDescriptor handlerDescriptor = actionDescriptor.getHandlerDescriptor();
				
		ActionURI uri = new ActionURI(AcceptConstant.CREATE_ADDRESS, HttpMethod.POST);
		handlerDescriptor.setRestApiURI(uri);
		
		//状态变化定义
		BizStateSwitchDesc bizStateSwitchDesc = new BizStateSwitchDesc(BizRootType.BIZ_OBJECT, 
				null, AcceptConstant.CREATE_STATUS);
		bizStateSwitchDesc.setWebExpose(true); //是否向web端发布事件		
		actionDescriptor.setBizStateSwitch(bizStateSwitchDesc);
		
		return actionDescriptor;
	}

	@Override
	public void handle(OtoCloudBusMessage<JsonObject> msg) {
		
		MultiMap headerMap = msg.headers();
		
		JsonObject bo = msg.body();
		
		String replenishmentsId = bo.getString("replenishments_id");

		// 当前操作人信息
		JsonObject actor = ActionContextTransfomer.fromMessageHeaderToActor(headerMap);
		
		//String partnerAcct = bo.getJsonObject("supplier").getString("link_account"); 
		
		if(replenishmentsId != null && !replenishmentsId.isEmpty()){		
			JsonObject queryJson = new JsonObject().put("bo.replenishments_id", replenishmentsId);
			this.getFactDataCount(appActivity.getBizObjectType(), AcceptConstant.CREATE_STATUS,			
					queryJson,  null, resultHandler->{
				if (resultHandler.succeeded()) {
					if(resultHandler.result() > 0){
						//update发货单列表
						JsonArray shipments = bo.getJsonArray("shipments");
						JsonObject update = new JsonObject()
												.put("$addToSet", new JsonObject()
																	.put("bo.shipments", new JsonObject()
																						.put("$each", shipments)));
						
						//JsonObject query = new JsonObject().put("bo_id", boId);						
						this.updateFactData(appActivity.getBizObjectType(), queryJson, update, AcceptConstant.CREATE_STATUS, 
								null, next->{
								if (next.succeeded()) {
									msg.reply(next.result()); 				
								} else {
									Throwable errThrowable = next.cause();
									String errMsgString = errThrowable.getMessage();
									appActivity.getLogger().error(errMsgString, errThrowable);
									msg.fail(100, errMsgString);
								}								
								
							});
						return;
					}
				}
				// 记录事实对象（业务数据），会根据ActionDescriptor定义的状态机自动进行状态变化，并发出状态变化业务事件
				// 自动查找数据源，自动进行分表处理
				this.recordFactData(appActivity.getBizObjectType(), bo, null, actor, null, result -> {
					if (result.succeeded()) {
						msg.reply(bo); //返回BO						
					} else {
						Throwable errThrowable = result.cause();
						String errMsgString = errThrowable.getMessage();
						appActivity.getLogger().error(errMsgString, errThrowable);
						msg.fail(100, errMsgString);
					}
				});
			});
		}else{
			// 记录事实对象（业务数据），会根据ActionDescriptor定义的状态机自动进行状态变化，并发出状态变化业务事件
			// 自动查找数据源，自动进行分表处理
			this.recordFactData(appActivity.getBizObjectType(), bo, null, actor, null, result -> {
				if (result.succeeded()) {
					msg.reply(bo); //返回BO						
				} else {
					Throwable errThrowable = result.cause();
					String errMsgString = errThrowable.getMessage();
					appActivity.getLogger().error(errMsgString, errThrowable);
					msg.fail(100, errMsgString);
				}
			});	
		}
		
	}

}
