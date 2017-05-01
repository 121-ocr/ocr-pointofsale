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
import otocloud.framework.app.common.BizRoleDirection;
import otocloud.framework.app.function.ActionDescriptor;
import otocloud.framework.app.function.AppActivityImpl;
import otocloud.framework.app.function.CDOHandlerImpl;
import otocloud.framework.common.CallContextSchema;
import otocloud.framework.core.CommandMessage;
import otocloud.framework.core.HandlerDescriptor;

/**
 * TODO: 已签收发货单查询
 * @date 2016年11月15日
 * @author lijing
 */
public class QueryShipmentCompletedHandler extends CDOHandlerImpl<JsonObject> {
	
	public static final String ADDRESS = "find_completed";

	public QueryShipmentCompletedHandler(AppActivityImpl appActivity) {
		super(appActivity);
		// TODO Auto-generated constructor stub
	}

	//此action的入口地址
	@Override
	public String getEventAddress() {
		// TODO Auto-generated method stub
		return ADDRESS;
	}

	//处理器
	@Override
	public void handle(CommandMessage<JsonObject> msg) {
		
		JsonObject queryParams = msg.getContent();
		
		List<String> statusList = new ArrayList<>();
		statusList.add(ShipmentConstant.COMPLETE_STATUS);	    
		
		//PagingOptions pagingObj = PagingOptions.buildPagingOptions(queryParams);
		JsonObject fields = queryParams.getJsonObject("fields");		
		JsonObject queryCond = queryParams.getJsonObject("query");
		JsonObject pagingInfo = queryParams.getJsonObject("paging");
		
		String bizUnit = msg.getCallContext().getString(CallContextSchema.BIZ_UNIT_ID);		
		
		this.queryLatestFactDataList(bizUnit, appActivity.getBizObjectType(), statusList, fields, pagingInfo, queryCond, null, findRet -> {
	        if (findRet.succeeded()) {
				JsonObject retObj = findRet.result();
				JsonArray stubBoList = retObj.getJsonArray("datas");
				if(stubBoList != null && stubBoList.size() > 0){
					List<Future> futures = new ArrayList<Future>();
					//List<JsonObject> retList = new ArrayList<>();
					for(Object item : stubBoList){
						JsonObject stubBo = (JsonObject)item;
						Future<JsonObject> cdoFuture = Future.future();
						futures.add(cdoFuture);							

						JsonObject bo = stubBo.getJsonObject("bo");
						String partner = bo.getString("partner");
						String boId = bo.getString("bo_id");
						
						this.queryLatestCDO(BizRoleDirection.TO, partner, appActivity.getBizObjectType(), 
								boId, null, cdoRet->{
									if (cdoRet.succeeded()) {
										cdoFuture.complete(cdoRet.result());
									}else{
										cdoFuture.fail(cdoRet.cause());
									}									
									
								});
						
					}
					
					CompositeFuture.join(futures).setHandler(ar -> {
						JsonArray retList = new JsonArray();
						CompositeFutureImpl comFutures = (CompositeFutureImpl)ar;
						if(comFutures.size() > 0){										
							for(int i=0;i<comFutures.size();i++){
								if(comFutures.succeeded(i)){
									JsonObject cdo = comFutures.result(i);
									retList.add(cdo);
								}
							}
						}
						
						retObj.put("datas", retList);
						
						msg.reply(retObj);
					});
					
				}else{
					msg.reply(null);
				}
	        	
	        } else {
				Throwable errThrowable = findRet.cause();
				String errMsgString = errThrowable.getMessage();
				appActivity.getLogger().error(errMsgString, errThrowable);
				msg.fail(100, errMsgString);		
	        }

	    });


	}
	

	/**
	 * 此action的自描述元数据
	 */
	@Override
	public ActionDescriptor getActionDesc() {		
		
		ActionDescriptor actionDescriptor = super.getActionDesc();
		HandlerDescriptor handlerDescriptor = actionDescriptor.getHandlerDescriptor();
		//handlerDescriptor.setMessageFormat("command");
		
		//参数
/*		List<ApiParameterDescriptor> paramsDesc = new ArrayList<ApiParameterDescriptor>();
		paramsDesc.add(new ApiParameterDescriptor("targetacc",""));		
		paramsDesc.add(new ApiParameterDescriptor("soid",""));		
		
		actionDescriptor.getHandlerDescriptor().setParamsDesc(paramsDesc);	*/
				
		ActionURI uri = new ActionURI(ADDRESS, HttpMethod.POST);
		handlerDescriptor.setRestApiURI(uri);
		
		return actionDescriptor;
	}
	
	
}
