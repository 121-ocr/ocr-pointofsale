package ocr.pointofsale.shipment;

import io.vertx.core.MultiMap;
import io.vertx.core.eventbus.DeliveryOptions;
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
 * 发货单完成操作
 * 
 * @author wanghw
 *
 */
public class ShipmentCompleteHandler extends CDOHandlerImpl<JsonObject> {

	public ShipmentCompleteHandler(AppActivityImpl appActivity) {
		super(appActivity);
		// TODO Auto-generated constructor stub
	}

	/**
	 * corecorp_setting.setting
	 */
	@Override
	public String getEventAddress() {
		return ShipmentConstant.COMPLETE_ADDRESS;
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public ActionDescriptor getActionDesc() {

		ActionDescriptor actionDescriptor = super.getActionDesc();
		HandlerDescriptor handlerDescriptor = actionDescriptor.getHandlerDescriptor();
				
		ActionURI uri = new ActionURI(getEventAddress(), HttpMethod.POST);
		handlerDescriptor.setRestApiURI(uri);
		
		//状态变化定义
		BizStateSwitchDesc bizStateSwitchDesc = new BizStateSwitchDesc(BizRootType.BIZ_OBJECT, 
				ShipmentConstant.CREATE_STATUS, ShipmentConstant.COMPLETE_STATUS);
		bizStateSwitchDesc.setWebExpose(true); //是否向web端发布事件		
		actionDescriptor.setBizStateSwitch(bizStateSwitchDesc);
		
		return actionDescriptor;
	}

	@Override
	public void handle(CommandMessage<JsonObject> msg) {
		MultiMap headerMap = msg.headers();
		
		JsonObject body = msg.getContent();
		JsonObject shipmentBo = body.getJsonObject("bo");
		
    	String boId = body.getString("bo_id");
    	
    	String partnerAcct = body.getString("from_account");	

    	
    	//当前操作人信息
    	JsonObject actor = ActionContextTransfomer.fromMessageHeaderToActor(headerMap); 
    	
    	   	
    	//记录事实对象（业务数据），会根据ActionDescriptor定义的状态机自动进行状态变化，并发出状态变化业务事件
    	//自动查找数据源，自动进行分表处理
    	this.recordCDO(null, BizRoleDirection.TO, partnerAcct, appActivity.getBizObjectType(), shipmentBo, boId, actor, 
    			cdoResult->{
    		if (cdoResult.succeeded()) {	
    			String stubBoId = shipmentBo.getString("bo_id");
    			JsonObject stubBo = this.buildStubForCDO(shipmentBo, stubBoId, partnerAcct);
    			
    	    	this.recordFactData(null, appActivity.getBizObjectType(), stubBo, stubBoId, actor, null, result->{
    				if (result.succeeded()) {				
    					msg.reply(shipmentBo);
    				} else {
    					Throwable errThrowable = result.cause();
    					String errMsgString = errThrowable.getMessage();
    					appActivity.getLogger().error(errMsgString, errThrowable);
    					msg.fail(100, errMsgString);		
    				}

    	    	});
    	    	
    			//通知供应方更新发货状态
    			String scSrvName = this.appActivity.getDependencies().getJsonObject("salescenter_service")
    					.getString("service_name", "");
    			String scAddress = partnerAcct + "." + scSrvName + "." + "shipment.complete";
    			DeliveryOptions options = new DeliveryOptions();
    			options.setHeaders(headerMap);
    			this.appActivity.getEventBus().send(scAddress, body, options, invRet -> {
    				if (invRet.succeeded()) {
    					
    				} else {
    					Throwable errThrowable = invRet.cause();
    					String errMsgString = errThrowable.getMessage();
    					appActivity.getLogger().error(errMsgString, errThrowable);    					
    				}
    			});

    		}else{
				Throwable errThrowable = cdoResult.cause();
				String errMsgString = errThrowable.getMessage();
				appActivity.getLogger().error(errMsgString, errThrowable);
				msg.fail(100, errMsgString);		

    		}
    	});    	


	}

}

