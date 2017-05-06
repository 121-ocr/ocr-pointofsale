package ocr.pointofsale.shipment;

import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.impl.CompositeFutureImpl;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.util.ArrayList;
import java.util.List;

import otocloud.common.ActionURI;
import otocloud.framework.app.function.ActionDescriptor;
import otocloud.framework.app.function.AppActivityImpl;
import otocloud.framework.app.function.CDOHandlerImpl;
import otocloud.framework.common.CallContextSchema;
import otocloud.framework.core.CommandMessage;
import otocloud.framework.core.HandlerDescriptor;

/**
 * 发货单创建操作
 * 
 * @author wanghw
 *
 */
public class ShipmentBatchCreateHandler extends CDOHandlerImpl<JsonArray> {

	public ShipmentBatchCreateHandler(AppActivityImpl appActivity) {
		super(appActivity);
		// TODO Auto-generated constructor stub
	}

	/**
	 * corecorp_setting.setting
	 */
	@Override
	public String getEventAddress() {
		return ShipmentConstant.BATCH_CREATE_ADDRESS;
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public ActionDescriptor getActionDesc() {

		ActionDescriptor actionDescriptor = super.getActionDesc();
		HandlerDescriptor handlerDescriptor = actionDescriptor.getHandlerDescriptor();
				
		ActionURI uri = new ActionURI(ShipmentConstant.CREATE_ADDRESS, HttpMethod.POST);
		handlerDescriptor.setRestApiURI(uri);
		
		//状态变化定义
/*		BizStateSwitchDesc bizStateSwitchDesc = new BizStateSwitchDesc(BizRootType.BIZ_OBJECT, 
				null, ShipmentConstant.CREATE_STATUS);
		bizStateSwitchDesc.setWebExpose(true); //是否向web端发布事件		
		actionDescriptor.setBizStateSwitch(bizStateSwitchDesc);*/
		
		return actionDescriptor;
	}
	
	@Override
	public JsonObject buildStubForCDO(JsonObject factData, String boId, String partnerAcct) {
	    //记录最后状态事实对象
    	JsonObject insert = new JsonObject();

    	insert.put("partner", partnerAcct);
    	insert.put("replenishments_id", factData.getString("replenishments_id"));
    	insert.put("bo_id", boId);
    	
    	return insert;
	}

	@Override
	public void handle(CommandMessage<JsonArray> msg) {
		JsonArray bos = msg.getContent();
		
		String bizUnit = msg.getCallContext().getLong(CallContextSchema.BIZ_UNIT_ID).toString();		
		
		List<Future> futures = new ArrayList<Future>();
		for(Object item: bos){
			Future<JsonObject> itemFuture = Future.future();
			futures.add(itemFuture);			
			JsonObject bo = (JsonObject)item;
			
			String stubBoId = bo.getString("bo_id");
			String partnerAcct = bo.getJsonObject("restocking_warehouse").getString("account");
			JsonObject stubBo = this.buildStubForCDO(bo, stubBoId, partnerAcct);
			//JsonObject actor = bo.getJsonObject("from_actor");
			
			String initState = bo.getString("current_state");
			
			recordFactData(bizUnit, appActivity.getBizObjectType(), stubBo, stubBoId, 
					null, initState, true, false, null, null, result->{
				if (result.succeeded()) {				
					itemFuture.complete(stubBo);
				}else{
					Throwable errThrowable = result.cause();
					String errMsgString = errThrowable.getMessage();
					appActivity.getLogger().error(errMsgString, errThrowable);					
					itemFuture.fail(errThrowable);
				}
	    	});
			
		}
		
		CompositeFuture.join(futures).setHandler(ar -> {
			JsonArray retList = new JsonArray();
			CompositeFutureImpl comFutures = (CompositeFutureImpl)ar;
			if(comFutures.size() > 0){										
				for(int i=0;i<comFutures.size();i++){
					if(comFutures.succeeded(i)){
						JsonObject stubBo = comFutures.result(i);
						retList.add(stubBo);
					}
				}
			}
			
			msg.reply(retList);
		});

	}

}
