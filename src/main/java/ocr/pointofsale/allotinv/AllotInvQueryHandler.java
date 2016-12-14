package ocr.pointofsale.allotinv;

import java.util.ArrayList;
import java.util.List;

import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.impl.CompositeFutureImpl;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import ocr.common.handler.SampleBillBaseQueryHandler;
import otocloud.common.ActionURI;
import otocloud.framework.app.function.ActionDescriptor;
import otocloud.framework.app.function.AppActivityImpl;
import otocloud.framework.core.HandlerDescriptor;
import otocloud.framework.core.OtoCloudBusMessage;

/**
 * 查询待收货的渠道补货单
 * 
 * @author wanghw
 *
 */
public class AllotInvQueryHandler extends SampleBillBaseQueryHandler {

	public static final String ADDRESS = "getall";

	public AllotInvQueryHandler(AppActivityImpl appActivity) {
		super(appActivity);
		// TODO Auto-generated constructor stub
	}

	// 此action的入口地址
	@Override
	public String getEventAddress() {
		// TODO Auto-generated method stub
		return ADDRESS;
	}

	/**
	 * 1，查询待收货的收货通知 2，根据bo_id列表查询补货单
	 */
	@Override
	public void handle(OtoCloudBusMessage<JsonObject> msg) {
		String from_account = this.appActivity.getAppInstContext().getAccount();
		// 按照分页条件查询收货通知
		String getAcceptAddress = from_account + "." + this.appActivity.getService().getRealServiceName()
				+ ".accept.query";
		JsonObject queryParams = msg.body();		
		this.appActivity.getEventBus().send(getAcceptAddress, queryParams, invRet -> {
			if (invRet.succeeded()) {
				JsonObject data = (JsonObject) invRet.result();
				//得到收货通知
				JsonArray datas = data.getJsonArray("datas");
				List<Future> futures = new ArrayList<>();
				for (Object obj : datas) {
					JsonObject allData = new JsonObject();//包含收货通知和补货单
					JsonObject accept = (JsonObject)obj;
					allData.put("accept", accept);
					String replenishments_id = accept.getString("replenishments_id");
					Future<JsonObject> repRelationFuture = Future.future();
					futures.add(repRelationFuture);
					//根据id查询补货单
					String invSrvName = this.appActivity.getDependencies().getJsonObject("salescenter_service")
							.getString("service_name", "");
					String getReplenishmentAddress = from_account + "." + invSrvName + "." + "channel-restocking.query4accept";
					JsonObject queryParam = new JsonObject();
					queryParam.put("bo_id", replenishments_id);
					this.appActivity.getEventBus().send(getReplenishmentAddress, queryParam, ret -> {
						if (ret.succeeded()) {
							allData.put("replenishment", ret.result());
							repRelationFuture.complete(allData);
						} else {
							Throwable errThrowable = invRet.cause();
							String errMsgString = errThrowable.getMessage();
							appActivity.getLogger().error(errMsgString, errThrowable);	
							repRelationFuture.fail(ret.cause());
						}
					});
				}
				//组合
				JsonArray ret = new JsonArray();
				CompositeFuture.join(futures).setHandler(ar -> {
					CompositeFutureImpl comFutures = (CompositeFutureImpl) ar;
					if (comFutures.size() > 0) {
						for (int i = 0; i < comFutures.size(); i++) {
							if (comFutures.succeeded(i)) {
								JsonObject document = comFutures.result(i);
								ret.add(document);
							}
						}
					}
					msg.reply(ret);
				});
			} else {
				Throwable errThrowable = invRet.cause();
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

		ActionURI uri = new ActionURI(getEventAddress(), HttpMethod.POST);
		handlerDescriptor.setRestApiURI(uri);

		return actionDescriptor;
	}
}
