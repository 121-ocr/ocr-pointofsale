package ocr.pointofsale.saleorder;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import ocr.common.handler.SampleBillBaseQueryHandler;
import ocr.pointofsale.shift.ShiftConstant;
import otocloud.framework.app.function.AppActivityImpl;
import otocloud.framework.common.CallContextSchema;
import otocloud.framework.core.CommandMessage;

/**
 * 
 * 汇总交班销售单的实收金额，用于pos系统场景
 * 
 * SQL:sum {销售订单.实收金额} from {销售订单} where {根据传入值：开始时间、结束时间, 租户id,收银台号}
 * 
 */
public class SumActualNumBySaleOrderHandler extends SampleBillBaseQueryHandler {

	public SumActualNumBySaleOrderHandler(AppActivityImpl appActivity) {
		super(appActivity);
	}

	// 此action的入口地址
	@Override
	public String getEventAddress() {
		return ShiftConstant.QUERYSUM_ADDRESS;
	}
	
	@Override
	public String getBizUnit(CommandMessage<JsonObject> msg){
/*		JsonObject session = msg.getSession();
		boolean is_global_bu =  session.getBoolean(CallContextSchema.IS_GLOBAL_BU, true);
*/		
		//按业务单元隔离
		String bizUnit = msg.getCallContext().getString(CallContextSchema.BIZ_UNIT_ID);		
		return 	bizUnit;
	}

	@Override
	public void handle(CommandMessage<JsonObject> msg) {
		JsonObject params = msg.getContent();
		querySaleOrderGroup(params, ret -> {
			if (ret.succeeded()) {
				msg.reply(ret.result());
			} else {
				Throwable errThrowable = ret.cause();
				String errMsgString = errThrowable.getMessage();
				msg.fail(100, errMsgString);
			}
		});
	}

	/**
	 * 
	 * 
	 * 输入参数：租户id,开始时间,结束时间，收银台号
	 *  { query: 
	 *    {   account: "3",
	 *  	  sale_date_from: "2017-3-27 09:30:00", 
	 *        sale_date_to:"2017-3-27 12:30:00",
	 *        deskNo:"01"
	 *     }
	 * }
	 * 
	 * db.bp_saleorder_3.aggregate(
		   [
		      {
		          $match : { "deskNo": "$deskNo",sale_date_to: { $gt: "$sale_date_to" },,sale_date_from: { $lt: "$sale_date_from" }}
		      },
		      {
		        $group : {
		           _id :"$deskNo",          
		           totalnum: {$sum: "$totalIncome" } //实收数量汇总
		        }
		      }
		   ]
		)	
	 * 
	 * @param params
	 * @param next
	 */
	public void querySaleOrderGroup(JsonObject params, Handler<AsyncResult<JsonArray>> next) {

		Future<JsonArray> future = Future.future();
		future.setHandler(next);

	
		JsonObject matchObj = new JsonObject().put("$match", getMatchValue(params));

		JsonObject groupObj = new JsonObject().put("$group", getGroupComputeFields());

		JsonArray piplelineArray = new JsonArray();
		piplelineArray.add(matchObj).add(groupObj);

		JsonObject command = new JsonObject()
				.put("aggregate", appActivity.getDBTableName(appActivity.getBizObjectType()))
				.put("pipeline", piplelineArray);

		appActivity.getAppDatasource().getMongoClient().runCommand("aggregate", command, result -> {
			if (result.succeeded()) {
				JsonArray stockOnHandRet = result.result().getJsonArray("result");
				future.complete(stockOnHandRet);
			} else {
				Throwable errThrowable = result.cause();
				String errMsgString = errThrowable.getMessage();
				appActivity.getLogger().error(errMsgString, errThrowable);
				future.fail(errThrowable);
			}
		});

	}

	private JsonObject getGroupComputeFields() {
		JsonObject groupComputeFields = new JsonObject().put("_id", "$deskNo").put("totalIncome",
				new JsonObject().put("$sum", "$totalIncome"));
		return groupComputeFields;
	}

	private JsonObject getMatchValue(JsonObject params) {
		JsonObject matcheOjbs = new JsonObject();
		matcheOjbs = new JsonObject().put("deskNo", params.getString("deskNo"))
				.put("sale_date_to", new JsonObject().put("$gt", params.getString("sale_date_to")))
				.put("sale_date_from", new JsonObject().put("$lt", params.getString("sale_date_from")));
		return matcheOjbs;
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
