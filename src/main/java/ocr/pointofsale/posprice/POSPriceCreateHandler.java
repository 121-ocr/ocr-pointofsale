package ocr.pointofsale.posprice;

import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.mongo.UpdateOptions;
import otocloud.common.ActionURI;
import otocloud.framework.app.function.ActionDescriptor;
import otocloud.framework.app.function.ActionHandlerImpl;
import otocloud.framework.app.function.AppActivityImpl;
import otocloud.framework.common.CallContextSchema;
import otocloud.framework.core.CommandMessage;
import otocloud.framework.core.HandlerDescriptor;

/**
 * 门店代销价格创建操作
 * 
 * @author wanghw
 *
 */
public class POSPriceCreateHandler extends ActionHandlerImpl<JsonObject> {

	public POSPriceCreateHandler(AppActivityImpl appActivity) {
		super(appActivity);
	}

	/**
	 * corecorp_setting.setting
	 */
	@Override
	public String getEventAddress() {
		return POSPriceConstant.CREATE_ADDRESS;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public ActionDescriptor getActionDesc() {

		ActionDescriptor actionDescriptor = super.getActionDesc();
		HandlerDescriptor handlerDescriptor = actionDescriptor.getHandlerDescriptor();

		// 外部访问url定义
		ActionURI uri = new ActionURI(getEventAddress(), HttpMethod.POST);
		handlerDescriptor.setRestApiURI(uri);

/*		// 状态变化定义
		BizStateSwitchDesc bizStateSwitchDesc = new BizStateSwitchDesc(BizRootType.BIZ_OBJECT, null, POSPriceConstant.CREATE_STATUS);
		bizStateSwitchDesc.setWebExpose(true); // 是否向web端发布事件
		actionDescriptor.setBizStateSwitch(bizStateSwitchDesc);
*/
		return actionDescriptor;
	}

	@Override
	public void handle(CommandMessage<JsonObject> msg) {
		
		JsonObject body = msg.getContent();
		
		JsonObject query = body.getJsonObject("query");
		JsonObject update = body.getJsonObject("update");
		
		UpdateOptions opt = new UpdateOptions();
		opt.setUpsert(true);
		
		//String account = this.appActivity.getAppInstContext().getAccount();
		
		//按业务单元隔离
		String bizUnit = msg.getCallContext().getLong(CallContextSchema.BIZ_UNIT_ID).toString();		
		query = this.buildQueryForMongo(query, bizUnit);
		
		this.appActivity.getAppDatasource().getMongoClient().updateCollectionWithOptions(appActivity.getDBTableName(this.appActivity.getBizObjectType()), 
				query, update, opt, resultHandler->{
					
					if (resultHandler.succeeded()) {
						msg.reply(resultHandler.result().toJson());
					} else {
						Throwable errThrowable = resultHandler.cause();
						String errMsgString = errThrowable.getMessage();
						appActivity.getLogger().error(errMsgString, errThrowable);
						msg.fail(100, errMsgString);
					}
					
				});
		
		
		
		
		
	}

}
