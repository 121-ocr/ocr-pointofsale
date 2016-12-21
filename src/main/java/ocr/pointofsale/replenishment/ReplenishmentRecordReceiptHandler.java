package ocr.pointofsale.replenishment;

import java.util.HashMap;
import java.util.Map;

import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import otocloud.common.ActionContextTransfomer;
import otocloud.common.ActionURI;
import otocloud.framework.app.common.BizRoleDirection;
import otocloud.framework.app.function.ActionDescriptor;
import otocloud.framework.app.function.AppActivityImpl;
import otocloud.framework.app.function.CDOHandlerImpl;
import otocloud.framework.core.HandlerDescriptor;
import otocloud.framework.core.OtoCloudBusMessage;

/**
 * 补货单通知创建操作
 * 
 * @author wanghw
 *
 */
public class ReplenishmentRecordReceiptHandler extends CDOHandlerImpl<JsonObject> {

	public ReplenishmentRecordReceiptHandler(AppActivityImpl appActivity) {
		super(appActivity);
		// TODO Auto-generated constructor stub
	}

	/**
	 * corecorp_setting.setting
	 */
	@Override
	public String getEventAddress() {
		return ReplenishmentConstant.RECEIPT_ADDRESS;
	}

	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public ActionDescriptor getActionDesc() {

		ActionDescriptor actionDescriptor = super.getActionDesc();
		HandlerDescriptor handlerDescriptor = actionDescriptor.getHandlerDescriptor();
				
		ActionURI uri = new ActionURI(ReplenishmentConstant.RECEIPT_ADDRESS, HttpMethod.POST);
		handlerDescriptor.setRestApiURI(uri);
		
		//状态变化定义
/*		BizStateSwitchDesc bizStateSwitchDesc = new BizStateSwitchDesc(BizRootType.BIZ_OBJECT, 
				null, ReplenishmentConstant.CREATE_STATUS);
		bizStateSwitchDesc.setWebExpose(true); //是否向web端发布事件		
		actionDescriptor.setBizStateSwitch(bizStateSwitchDesc);*/
		
		return actionDescriptor;
	}

	@Override
	public void handle(OtoCloudBusMessage<JsonObject> msg) {
		JsonObject body = msg.body();
		JsonObject shipment = body.getJsonObject("bo");
		
		String partnerAcct = body.getString("from_account");	
		String replenishmentsId = shipment.getString("replenishments_id");

		
    	//当前操作人信息
    	JsonObject actor = ActionContextTransfomer.fromMessageHeaderToActor(msg.headers()); 
		
		this.queryLatestCDO(BizRoleDirection.TO, partnerAcct, appActivity.getBizObjectType(), replenishmentsId, null, next->{
			if(next.succeeded()){
				JsonObject replenishmentBo = next.result();
				JsonObject replenishment = replenishmentBo.getJsonObject("bo");
				String currentStatus = replenishmentBo.getString("current_state");
				
				//String repStatus = replenishmentBo.getString("current_state");
				
		   		Map<String,JsonObject> rep_b2Shipment_b = new HashMap<>();//key:补货单表体code；value:发货单表体
	    		JsonArray shipment_b_list = shipment.getJsonArray("details");
	    		for (Object object : shipment_b_list) {
	    			JsonObject detail = (JsonObject)object;
	    			String key = detail.getString("rep_detail_code");
	    			rep_b2Shipment_b.put(key, detail);
	    		}
	    		
    		
	    		JsonArray replenishment_b = replenishment.getJsonArray("details");
	    		for (int i=0; i<replenishment_b.size(); i++) {
	    			JsonObject detail = (JsonObject)replenishment_b.getJsonObject(i);
	    			String detailCode = detail.getString("detail_code");
	    			if(!rep_b2Shipment_b.containsKey(detailCode)){
	    				continue;
	    			}
	    			
	    			if(!detail.containsKey("shipments")){
	    				continue;
	    			}
	    			JsonArray replenishment_s = detail.getJsonArray("shipments");
	    			if (replenishment_s == null || replenishment_s.isEmpty()) {
	    				continue;
	    			}
		    		
	    			for (int j=0; j<replenishment_s.size(); j++) {
	    			
	    				JsonObject detail_s = (JsonObject)replenishment_s.getJsonObject(j);
	    				String shipCode = detail_s.getString("ship_code");
	    				if (!shipCode.equals(shipment.getString("bo_id"))) {
	    					continue;
	    				}
	    				
	    				detail_s.put("accept_completed", true);
	    				
			    		// 修改发货通知的标识
			    		JsonObject queryJs = new JsonObject().put("bo_id", replenishmentsId)
			    				.put("bo.details.detail_code", detailCode)
			    				.put("bo.details.shipments.ship_code", shipCode);
			    		
			    		String acceptQuantityPath = "bo.details." + i + ".shipments." + j + ".accept_quantity";
			    		String rejectQuantityPath = "bo.details." + i + ".shipments." + j + ".reject_quantity";
			    		String acceptCompletePath = "bo.details." + i + ".shipments." + j + ".accept_completed";
			    		String actorQuantityPath = "bo.details." + i + ".shipments." + j + ".accept_actor";
			    				
			    		JsonObject accept_info = rep_b2Shipment_b.get(detail.getString("detail_code")).getJsonObject("accept_info");
			    		JsonObject updateJs = new JsonObject().put(acceptQuantityPath, accept_info.getValue("accept_quantity"))
			    								.put(rejectQuantityPath, accept_info.getValue("reject_quantity"))
			    								.put(acceptCompletePath, true)
			    								.put(actorQuantityPath, accept_info.getValue("accept_actor"));		
			    		
		    		
			    		this.updateCDO(BizRoleDirection.TO, partnerAcct, appActivity.getBizObjectType(), 
			    				queryJs, updateJs, currentStatus, actor, cdoRet->{			    					
			    					if(cdoRet.succeeded()){
			    						
			    					}else{
			    						Throwable errThrowable = next.cause();
			    						String errMsgString = errThrowable.getMessage();
			    						appActivity.getLogger().error(errMsgString, errThrowable);
			    						
			    					}	
			    				});
		    		}
	    		}
	    		
	    		Boolean isCompleted = true;
	    		for (int i=0; i<replenishment_b.size(); i++) {
	    			JsonObject detail = (JsonObject)replenishment_b.getJsonObject(i);	    			

	    			if(!detail.containsKey("shipments")){
	    				isCompleted = false; //有未發貨的數據
	    				continue;
	    			}
	    			JsonArray replenishment_s = detail.getJsonArray("shipments");
	    			if (replenishment_s == null || replenishment_s.isEmpty()) {
	    				isCompleted = false; //有未發貨的數據
	    				continue;
	    			}			    		
	    			for (int j=0; j<replenishment_s.size(); j++) {
	    			
	    				JsonObject detail_s = (JsonObject)replenishment_s.getJsonObject(j);
	    				
    					if(!detail_s.containsKey("accept_completed")){
    						isCompleted = false;
    					}else{
    						Object valueObj = detail_s.getValue("accept_completed");
    						if(valueObj == null){
    							isCompleted = false;
    						}else{
    							Boolean value = (Boolean)valueObj;
    							if(!value){
    								isCompleted = false;
    							}
    						}
    					}

	    			}
	    			
	    		}
	    		
	    		if(isCompleted){
	    			ReplenishmentCompleteHandler replenishmentCompleteHandler = new ReplenishmentCompleteHandler(this.appActivity);
	    			replenishmentCompleteHandler.processComplete(replenishmentsId, partnerAcct, actor, ret->{
	    				
	    				if (ret.succeeded()) {
	    					msg.reply("ok");						
	    				}else{
	    					Throwable errThrowable = ret.cause();
	    					String errMsgString = errThrowable.getMessage();
	    					appActivity.getLogger().error(errMsgString, errThrowable);
	    					msg.fail(100, errMsgString);	    				}
	    				
	    			});

	    		}else{
	    			msg.reply("ok");
	    		}
				
			}else{
				Throwable errThrowable = next.cause();
				String errMsgString = errThrowable.getMessage();
				appActivity.getLogger().error(errMsgString, errThrowable);
				msg.fail(100, errMsgString);
			}	
			
		});

	}
	

}
