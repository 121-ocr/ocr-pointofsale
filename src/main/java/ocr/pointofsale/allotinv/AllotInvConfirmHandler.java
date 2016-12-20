package ocr.pointofsale.allotinv;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
 * 确认收货操作 0，更新发货单完成  1， 更新补货单收货量  2， 创建价格表 3， 更新现存量
 * 
 * @author wanghw
 *
 */
public class AllotInvConfirmHandler extends ActionHandlerImpl<JsonObject> {

	public AllotInvConfirmHandler(AppActivityImpl appActivity) {
		super(appActivity);
	}

	/**
	 * corecorp_setting.setting
	 */
	@Override
	public String getEventAddress() {
		return AllotInvConstant.CONFIRM_ADDRESS;
	}

	@Override
	public void handle(OtoCloudBusMessage<JsonObject> msg) {
		List<Future> futures = new ArrayList<>();
		// 更新收货通知
		JsonObject body = msg.body();
		JsonObject replenishment = body.getJsonObject("replenishment");
		JsonObject shipment = body.getJsonObject("shipment");
		JsonObject replenishmentBo = replenishment.getJsonObject("bo");
		JsonObject shipmentBo = shipment.getJsonObject("bo");
		
		//设置发货单完成状态
		Future<JsonObject> acceptFuture = Future.future();
		futures.add(acceptFuture);
		completeShipment(shipment, acceptFuture);
		// 创建价格表
		Future<JsonObject> priceFuture = Future.future();
		futures.add(priceFuture);
		createPrices(replenishmentBo, priceFuture);
		// 更新现存量
		JsonArray invOnhand = getInvOnhandObject(shipmentBo);
		Future<JsonObject> invOnhandFuture = Future.future();
		futures.add(invOnhandFuture);
		createInvOnhand(invOnhand, invOnhandFuture);
/*		// 更新发货单
		Future<JsonObject> shipmentFuture = Future.future();
		futures.add(shipmentFuture);
		completeShipment(shipmentBo, shipmentFuture);*/

		// 组合
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
			updateReplenishment(shipment,msg);
		});
	}

	/**
	 * 更新发货单
	 * @param shipment
	 * @param shipmentFuture
	 */
	private void completeShipment(JsonObject shipment, Future<JsonObject> shipmentFuture) {
		String account = this.appActivity.getAppInstContext().getAccount();
		String srvName = this.appActivity.getService().getRealServiceName();
		/*String invSrvName = this.appActivity.getDependencies().getJsonObject("salescenter_service")
				.getString("service_name", "");*/
		String updateShipmentAddress = account + "." + srvName + "." + "shipment-mgr.complete";
		
		this.appActivity.getEventBus().send(updateShipmentAddress, shipment, ret -> {
			if (ret.succeeded()) {
				shipmentFuture.complete();
			} else {
				Throwable errThrowable = ret.cause();
				String errMsgString = errThrowable.getMessage();
				appActivity.getLogger().error(errMsgString, errThrowable);
				shipmentFuture.fail(errMsgString);
			}
		});		
	}

	/**
	 * 保存现存量
	 * 
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
	 * 
	 * @param replenishment
	 * @return
	 */
	private JsonArray getInvOnhandObject(JsonObject shipment) {
		JsonArray paramList = new JsonArray();
		for (Object detail : shipment.getJsonArray("details")) {
			JsonObject param = new JsonObject();
			JsonObject detailO = (JsonObject) detail;
			param.put("warehouses", shipment.getJsonObject("target_warehouse"));
			param.put("goods", detailO.getJsonObject("goods"));
			param.put("sku", detailO.getJsonObject("goods").getString("product_sku_code"));
			param.put("invbatchcode", detailO.getString("invbatchcode"));
			param.put("warehousecode", shipment.getJsonObject("target_warehouse").getString("code"));
			param.put("status", "IN");
			param.put("biz_data_type", "bp_shipment");
			param.put("bo_id", shipment.getString("bo_id"));
			param.put("goodaccount", detailO.getJsonObject("goods").getString("account"));
		
			param.put("onhandnum", detailO.getJsonObject("accept_info").getValue("accept_quantity"));
			
			paramList.add(param);
		}
		return paramList;
	}

	/**
	 * 保存价格表
	 * 
	 * @param replenishment
	 * @return
	 */
	private void createPrices(JsonObject replenishment, Future<JsonObject> priceFuture) {
		JsonArray details = replenishment.getJsonArray("details");
		JsonObject query_con = new JsonObject();
		JsonArray query_item = new JsonArray();
		query_con.put("$or", query_item);
		// 首先按照sku+货主+批次查询已存在的价格
		for (int i = 0; i < details.size(); i++) {
			JsonObject detail_obj = (JsonObject) details.getValue(i);
			JsonObject temp = new JsonObject();
			temp.put("goods.product_sku_code", detail_obj.getJsonObject("goods").getString("product_sku_code"));
			temp.put("invbatchcode", detail_obj.getString("invbatchcode"));
			temp.put("goods.account", detail_obj.getJsonObject("goods").getString("account"));

			query_item.add(temp);
		}
		String from_account = this.appActivity.getAppInstContext().getAccount();
		// 创建门店价格表
		String getPriceAddress = from_account + "." + this.appActivity.getService().getRealServiceName()
				+ ".posprice.query";

		this.appActivity.getEventBus().send(getPriceAddress, query_con, invRet -> {
			if (invRet.succeeded()) {
				// 已存在的价格
				JsonObject data = (JsonObject) invRet.result().body();
				JsonArray datas = data.getJsonArray("result");
				// 构建prices
				JsonArray prices = new JsonArray();
				for (Object detail : details) {
					JsonObject detail_obj = (JsonObject) detail;
					JsonObject price = new JsonObject();

					price.put("goods", detail_obj.getJsonObject("goods"));
					price.put("invbatchcode", detail_obj.getString("invbatchcode"));
					price.put("supply_price", detail_obj.getJsonObject("supply_price"));
					price.put("retail_price", detail_obj.getJsonObject("retail_price"));
					price.put("commission", detail_obj.getJsonObject("commission"));

					boolean isExist = false;
					for (Object obj : datas) {
						JsonObject temp = (JsonObject) obj;
						if (price.getJsonObject("goods").getString("product_sku_code")
								.equals(temp.getJsonObject("goods").getString("product_sku_code"))
								&& price.getJsonObject("goods").getString("account")
										.equals(temp.getJsonObject("goods").getString("account"))
								&& price.getString("invbatchcode").equals(temp.getString("invbatchcode"))) {
							isExist = true;
							break;
						}
					}
					if (isExist) {
						continue;
					}
					prices.add(price);					
				}
				if(prices == null || prices.isEmpty()){
					priceFuture.complete();
					return;
				}
				// 创建门店价格表
				String createPriceAddress = from_account + "." + this.appActivity.getService().getRealServiceName()
						+ ".posprice.create";
				this.appActivity.getEventBus().send(createPriceAddress, prices, ret -> {
					if (ret.succeeded()) {
						priceFuture.complete();
					} else {
						Throwable errThrowable = ret.cause();
						String errMsgString = errThrowable.getMessage();
						appActivity.getLogger().error(errMsgString, errThrowable);
						priceFuture.fail(ret.cause());
					}
				});
			}
		});
	}

	/**
	 * 更新补货单
	 * 
	 * @param replenishment
	 * @param shipment 
	 * @param msg
	 * @param replenishmentFuture
	 */
	private void updateReplenishment(JsonObject shipment, OtoCloudBusMessage<JsonObject> msg) {
		String from_account = this.appActivity.getAppInstContext().getAccount();
		String srvName = this.appActivity.getService().getRealServiceName();
		String replenishmentAddress = from_account + "." + srvName + "." + "replenishment-mgr.record-receipt";

		this.appActivity.getEventBus().send(replenishmentAddress, shipment, ret -> {
			if (ret.succeeded()) {
				msg.reply(ret.result().body());
			} else {
				Throwable errThrowable = ret.cause();
				String errMsgString = errThrowable.getMessage();
				appActivity.getLogger().error(errMsgString, errThrowable);
				msg.fail(100, errMsgString);
			}
		});
	}

	/**
	 * 更新收货通知
	 * 
	 * @param accept
	 * @param acceptFuture
	 */
/*	private void completeAccept(JsonObject accept, Future<JsonObject> acceptFuture) {
		String from_account = this.appActivity.getAppInstContext().getAccount();
		// 按照分页条件查询收货通知
		String getAcceptAddress = from_account + "." + this.appActivity.getService().getRealServiceName()
				+ ".accept.accept";
		this.appActivity.getEventBus().send(getAcceptAddress, accept, invRet -> {
			if (invRet.succeeded()) {
				acceptFuture.complete((JsonObject) invRet.result().body());
			} else {
				Throwable errThrowable = invRet.cause();
				String errMsgString = errThrowable.getMessage();
				appActivity.getLogger().error(errMsgString, errThrowable);
				acceptFuture.fail(errMsgString);
			}
		});

	}*/

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
		BizStateSwitchDesc bizStateSwitchDesc = new BizStateSwitchDesc(BizRootType.BIZ_OBJECT, AllotInvConstant.CREATE_STATUS, AllotInvConstant.CONFIRM_STATUS);
		bizStateSwitchDesc.setWebExpose(true); // 是否向web端发布事件
		actionDescriptor.setBizStateSwitch(bizStateSwitchDesc);

		return actionDescriptor;
	}

}
