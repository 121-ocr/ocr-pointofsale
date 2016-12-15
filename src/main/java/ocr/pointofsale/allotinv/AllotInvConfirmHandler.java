package ocr.pointofsale.allotinv;

import java.util.ArrayList;
import java.util.List;

import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.impl.CompositeFutureImpl;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import otocloud.common.ActionURI;
import otocloud.framework.app.function.ActionDescriptor;
import otocloud.framework.app.function.ActionHandlerImpl;
import otocloud.framework.app.function.AppActivityImpl;
import otocloud.framework.app.function.BizRootType;
import otocloud.framework.app.function.BizStateSwitchDesc;
import otocloud.framework.core.HandlerDescriptor;
import otocloud.framework.core.OtoCloudBusMessage;

/**
 * 确认收货操作 0，更新收货通知 1， 更新补货单 2， 创建价格表 3， 更新现存量
 * 
 * @author wanghw
 *
 */
public class AllotInvConfirmHandler extends ActionHandlerImpl<JsonObject> {

	public static final String ADDRESS = "confirm";

	public AllotInvConfirmHandler(AppActivityImpl appActivity) {
		super(appActivity);
	}

	/**
	 * corecorp_setting.setting
	 */
	@Override
	public String getEventAddress() {
		return ADDRESS;
	}

	@Override
	public void handle(OtoCloudBusMessage<JsonObject> msg) {
		List<Future> futures = new ArrayList<>();
		// 更新收货通知
		JsonObject accept = msg.body().getJsonObject("accept");
		Future<JsonObject> acceptFuture = Future.future();
		futures.add(acceptFuture);
		completeAccept(accept, acceptFuture);
		// 更新补货单
		JsonObject replenishment = msg.body().getJsonObject("replenishment");
		// 创建价格表
		JsonArray prices = getPricesObject(replenishment);
		Future<JsonObject> priceFuture = Future.future();
		futures.add(priceFuture);
		createPrices(prices, priceFuture);
		// 更新现存量
		JsonArray invOnhand = getInvOnhandObject(replenishment);
		Future<JsonObject> invOnhandFuture = Future.future();
		futures.add(invOnhandFuture);
		createInvOnhand(invOnhand, invOnhandFuture);

		// 组合
		JsonArray ret = new JsonArray();
		CompositeFuture.join(futures).setHandler(ar -> {
			CompositeFutureImpl comFutures = (CompositeFutureImpl) ar;
			if (comFutures.size() > 0) {
				for (int i = 0; i < comFutures.size(); i++) {
					if (comFutures.failed(i)) {
						Throwable errThrowable = comFutures.cause();
						String errMsgString = errThrowable.getMessage();
						appActivity.getLogger().error(errMsgString, errThrowable);
						msg.fail(100, errMsgString);
					}
				}
			}
			// 更新补货单			
			updateReplenishment(replenishment,msg);
		});
	}

	/**
	 * 保存现存量
	 * @param prices
	 * @param invOnhandFuture
	 */
	private void createInvOnhand(JsonArray invOnhand, Future<JsonObject> invOnhandFuture) {
		String from_account = this.appActivity.getAppInstContext().getAccount();
		String invSrvName = this.appActivity.getDependencies().getJsonObject("inventorycenter_service")
				.getString("service_name", "");
		String getWarehouseAddress = from_account + "." + invSrvName + "." + "stockonhand-mgr.batchcreate";
		this.appActivity.getEventBus().send(getWarehouseAddress, invOnhand, invRet -> {
			if (invRet.succeeded()) {
				invOnhandFuture.complete();
			} else {
				Throwable errThrowable = invRet.cause();
				String errMsgString = errThrowable.getMessage();
				appActivity.getLogger().error(errMsgString, errThrowable);
				invOnhandFuture.fail(invRet.cause());
			}
		});		
	}

	/**
	 * 获取现存量VO
	 * @param replenishment
	 * @return
	 */
	private JsonArray getInvOnhandObject(JsonObject replenishment) {
		JsonArray paramList = new JsonArray();
		for (Object detail : replenishment.getJsonArray("details")) {
			JsonObject param = new JsonObject();
			JsonObject detailO = (JsonObject) detail;
			param.put("warehouses", replenishment.getJsonObject("target_warehouse"));
			param.put("goods", detailO.getJsonObject("goods"));
			param.put("sku", detailO.getJsonObject("goods").getString("product_sku_code"));
			param.put("invbatchcode", detailO.getString("invbatchcode"));
			param.put("warehousecode", replenishment.getJsonObject("target_warehouse").getString("code"));
			Double conhandnum = 0.0;
			JsonArray replenishment_s = detailO.getJsonArray("shipments");
			if(replenishment_s == null || replenishment_s.isEmpty()){
				continue;
			}
			for (Object object2 : replenishment_s) {
				JsonObject detail_s = (JsonObject) object2;
				if(detail_s.getBoolean("accept_completed")){
					conhandnum = conhandnum.doubleValue() + Double.valueOf(detailO.getString("accept_quantity")).doubleValue();
				}				
			}
			param.put("onhandnum", conhandnum.toString());
			param.put("goodaccount", detailO.getJsonObject("goods").getString("account"));
			
			if(conhandnum >0){
				paramList.add(param);
			}			
		}
		return paramList;
	}

	/**
	 * 保存价格表
	 * 
	 * @param prices
	 * @param priceFuture
	 */
	private void createPrices(JsonArray prices, Future<JsonObject> priceFuture) {
		String from_account = this.appActivity.getAppInstContext().getAccount();
		// 创建门店价格表
		String getPriceAddress = from_account + "." + this.appActivity.getService().getRealServiceName()
				+ ".posprice.create";
		this.appActivity.getEventBus().send(getPriceAddress, prices, invRet -> {
			if (invRet.succeeded()) {
				priceFuture.complete();
			} else {
				Throwable errThrowable = invRet.cause();
				String errMsgString = errThrowable.getMessage();
				appActivity.getLogger().error(errMsgString, errThrowable);
				priceFuture.fail(invRet.cause());
			}
		});
	}

	/**
	 * 通过补货单构造价格单
	 * 
	 * @param replenishment
	 * @return
	 */
	private JsonArray getPricesObject(JsonObject replenishment) {
		// 保存门店价格表
		JsonArray prices = new JsonArray();
		// 构建prices
		JsonArray details = replenishment.getJsonArray("details");
		//TODO 需要判断已经存在的价格表不需要再保存了
		
		for (Object detail : details) {
			JsonObject detail_obj = (JsonObject) detail;
			JsonObject price = new JsonObject();

			price.put("goods", detail_obj.getJsonObject("goods"));
			price.put("invbatchcode", detail_obj.getString("invbatchcode"));
			price.put("supply_price", detail_obj.getJsonObject("supply_price"));
			price.put("retail_price", detail_obj.getJsonObject("retail_price"));
			price.put("commission", detail_obj.getJsonObject("commission"));

			prices.add(price);
		}
		return prices;
	}

	/**
	 * 更新补货单
	 * 
	 * @param replenishment
	 * @param msg 
	 * @param replenishmentFuture
	 */
	private void updateReplenishment(JsonObject replenishment, OtoCloudBusMessage<JsonObject> msg) {
		String from_account = this.appActivity.getAppInstContext().getAccount();
		String invSrvName = this.appActivity.getDependencies().getJsonObject("salescenter_service")
				.getString("service_name", "");
		String getReplenishmentAddress = from_account + "." + invSrvName + "." + "channel-restocking.update4accept";
		//修改收货通知的标识
		JsonArray replenishment_b = replenishment.getJsonArray("details");
		for (Object object : replenishment_b) {
			JsonObject detail = (JsonObject) object;
			JsonArray replenishment_s = detail.getJsonArray("shipments");
			if(replenishment_s == null || replenishment_s.isEmpty()){
				continue;
			}
			for (Object object2 : replenishment_s) {
				JsonObject detail_s = (JsonObject) object2;
				if(detail_s.getBoolean("accept_completed")){
					continue;
				}
				detail_s.put("accept_completed", true);
			}
		}
		this.appActivity.getEventBus().send(getReplenishmentAddress, replenishment, ret -> {
			if (ret.succeeded()) {
				msg.reply(ret.result());
			} else {
				Throwable errThrowable = ret.cause();
				String errMsgString = errThrowable.getMessage();
				appActivity.getLogger().error(errMsgString, errThrowable);
				msg.fail(100,errMsgString);
			}
		});
	}

	/**
	 * 更新收货通知
	 * 
	 * @param accept
	 * @param acceptFuture
	 */
	private void completeAccept(JsonObject accept, Future<JsonObject> acceptFuture) {
		String from_account = this.appActivity.getAppInstContext().getAccount();
		// 按照分页条件查询收货通知
		String getAcceptAddress = from_account + "." + this.appActivity.getService().getRealServiceName()
				+ ".accept.accept";
		this.appActivity.getEventBus().send(getAcceptAddress, accept, invRet -> {
			if (invRet.succeeded()) {
				acceptFuture.complete((JsonObject) invRet.result());
			} else {
				Throwable errThrowable = invRet.cause();
				String errMsgString = errThrowable.getMessage();
				appActivity.getLogger().error(errMsgString, errThrowable);
				acceptFuture.fail(errMsgString);
			}
		});

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

		// 状态变化定义
		BizStateSwitchDesc bizStateSwitchDesc = new BizStateSwitchDesc(BizRootType.BIZ_OBJECT, "created", "confirmed");
		bizStateSwitchDesc.setWebExpose(true); // 是否向web端发布事件
		actionDescriptor.setBizStateSwitch(bizStateSwitchDesc);

		return actionDescriptor;
	}

}
