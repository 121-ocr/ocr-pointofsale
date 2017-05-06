package ocr.pointofsale.posprice;

import io.vertx.core.json.JsonObject;
import ocr.common.handler.SampleDocQueryHandler;
import otocloud.framework.app.function.AppActivityImpl;
import otocloud.framework.common.CallContextSchema;
import otocloud.framework.core.CommandMessage;

/**
 * 查询门店代销价格表
 * 
 * @author wanghw
 *
 */
public class POSPriceQueryHandler extends SampleDocQueryHandler {

	public POSPriceQueryHandler(AppActivityImpl appActivity) {
		super(appActivity);
		// TODO Auto-generated constructor stub
	}

	// 处理器
	@Override
	public void handle(CommandMessage<JsonObject> msg) {
		
		JsonObject query = msg.getContent();
		
		//按业务单元隔离
		String bizUnit = msg.getCallContext().getLong(CallContextSchema.BIZ_UNIT_ID).toString();		
		query = this.buildQueryForMongo(query, bizUnit);

		appActivity.getAppDatasource().getMongoClient().find(appActivity.getDBTableName(appActivity.getBizObjectType()),
				query,
				result -> {
					if (result.succeeded()) {
						msg.reply(result.result());
					} else {
						Throwable errThrowable = result.cause();
						String errMsgString = errThrowable.getMessage();
						appActivity.getLogger().error(errMsgString, errThrowable);
						msg.fail(100, errMsgString);
					}
				});
	}

	@Override
	public String getEventAddress() {
		// TODO Auto-generated method stub
		return POSPriceConstant.QUERY_ADDRESS;
	}

}
