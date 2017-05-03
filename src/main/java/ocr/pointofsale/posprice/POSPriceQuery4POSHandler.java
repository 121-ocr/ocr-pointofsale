package ocr.pointofsale.posprice;

import java.util.ArrayList;
import java.util.List;

import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;
import ocr.common.handler.SampleDocQueryHandler;
import otocloud.framework.app.function.AppActivityImpl;
import otocloud.framework.core.OtoCloudBusMessage;

/**
 * 为收银系统提供的查询商品的方法
 * 
 * @author wanghw
 *
 */
public class POSPriceQuery4POSHandler extends SampleDocQueryHandler {

	public POSPriceQuery4POSHandler(AppActivityImpl appActivity) {
		super(appActivity);
		// TODO Auto-generated constructor stub
	}

	// 处理器
	@Override
	public void handle(OtoCloudBusMessage<JsonObject> msg) {
		
		String from_account = this.appActivity.getAppInstContext().getAccount();
		JsonObject query = msg.body();

		appActivity.getAppDatasource().getMongoClient().find(appActivity.getDBTableName(appActivity.getBizObjectType()),
				query,
				result -> {
					if (result.succeeded()) {
						List<JsonObject> priceList = result.result();
						List<Future> futureList = new ArrayList<>();
						List<JsonObject> resultVOList = new ArrayList<>();
						for (JsonObject priceRet : priceList) {
							Future future = Future.future();
							futureList.add(future);
							
							JsonObject param = new JsonObject("{\"goods\": {\"product_sku_code\":\""+priceRet.getJsonObject("goods").getString("product_sku_code")+"\","
									+ " \"invbatchcode\":\""+priceRet.getString("invbatchcode")+"\"}");
							String invSrvName = this.appActivity.getDependencies().getJsonObject("inventorycenter_service")
									.getString("service_name", "");
							String getWarehouseAddress = from_account + "." + invSrvName + "." + "stockonhand-mgr.commonquery";
							this.appActivity.getEventBus().send(getWarehouseAddress, param, invRet -> {
								if (invRet.succeeded()) {
									JsonObject onhandRetObj = (JsonObject)invRet.result().body();
									JsonObject onhand = onhandRetObj.getJsonArray("result").getJsonObject(0);
									JsonObject resultVO = new JsonObject();
									resultVO.put("goods", priceRet.getJsonObject("goods"));
									resultVO.put("invbatchcode", priceRet.getString("invbatchcode"));
									resultVO.put("retail_price", priceRet.getJsonObject("retail_price"));
									resultVO.put("warehouse", onhand.getJsonObject("warehouses"));
									resultVO.put("discount", priceRet.getDouble("discount"));
									
									resultVOList.add(resultVO);
									future.complete();
								} else {
									future.fail(invRet.cause());
								}
							});
						}	
						CompositeFuture.join(futureList).setHandler(ar -> {
							msg.reply(resultVOList);
						});						
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
		return POSPriceConstant.QUERY4POS_ADDRESS;
	}

}
