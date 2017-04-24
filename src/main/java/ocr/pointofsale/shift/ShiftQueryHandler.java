package ocr.pointofsale.shift;

import io.vertx.core.json.JsonObject;
import io.vertx.ext.mongo.FindOptions;

import java.util.List;

import ocr.common.handler.SampleBillBaseQueryHandler;
import otocloud.framework.app.function.AppActivityImpl;
import otocloud.framework.core.CommandMessage;

/**
 * 
 * 交班查询
 * 
 */
public class ShiftQueryHandler extends SampleBillBaseQueryHandler {

	public ShiftQueryHandler(AppActivityImpl appActivity) {
		super(appActivity);
	}

	// 此action的入口地址
	@Override
	public String getEventAddress() {
		return ShiftConstant.Query_ADDRESS;
	}

	// 处理器
	//根据条件查询交班信息
	@Override
	public void handle(CommandMessage<JsonObject> msg) {

		JsonObject query = msg.getContent();
		
		appActivity.getAppDatasource().getMongoClient().findWithOptions(
				appActivity.getDBTableName(this.appActivity.getBizObjectType()), query, new FindOptions(), findRet -> {
					if (findRet.succeeded()) {
						List<JsonObject> retObj = findRet.result();
						if (retObj != null && retObj.size() > 0) {
							msg.reply(retObj.get(0));
						} else {
							msg.reply(null);
						}
					} else {
						Throwable err = findRet.cause();
						String errMsg = err.getMessage();
						appActivity.getLogger().error(errMsg, err);
						msg.fail(500, errMsg);
					}

				});

	}

	/**
	 * 要查询的单据状态
	 * 
	 * @return
	 */
	@Override
	public String getStatus(JsonObject msgBody) {
		return ShiftConstant.CREATE_STATUS;
	}

}
