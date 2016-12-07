package ocr.pointofsale.allotinv;

import io.vertx.core.Future;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import ocr.common.handler.SampleBillBaseHandler;
import otocloud.common.ActionURI;
import otocloud.framework.app.function.ActionDescriptor;
import otocloud.framework.app.function.AppActivityImpl;
import otocloud.framework.app.function.BizRootType;
import otocloud.framework.app.function.BizStateSwitchDesc;
import otocloud.framework.core.HandlerDescriptor;
import otocloud.framework.core.OtoCloudBusMessage;

/**
 * 补货调拨入库单确认操作
 * 
 * @author wanghw
 *
 */
public class AllotInvConfirmHandler extends SampleBillBaseHandler {

	public static final String ADDRESS = "confirm";

	public AllotInvConfirmHandler(AppActivityImpl appActivity) {
		super(appActivity);
		// TODO Auto-generated constructor stub
	}

	/**
	 * corecorp_setting.setting
	 */
	@Override
	public String getEventAddress() {
		return ADDRESS;
	}

	@Override
	public String getPartnerAcct(JsonObject bo) {
		String partnerAcct = bo.getJsonObject("restocking_warehouse").getString("account");
		return partnerAcct;
	}

	/**
	 * 输入值校验
	 * 
	 * @param msg
	 * @param future
	 */
	protected void beforeProess(OtoCloudBusMessage<JsonObject> msg, Future<JsonObject> future) {
		future.complete(msg.body());
	}

	/**
	 * 单据保存后处理
	 * 
	 * @param bo
	 * @param future
	 */
	protected void afterProcess(JsonObject bo, Future<JsonObject> future) {
		String from_account = this.appActivity.getAppInstContext().getAccount();
		// 保存门店价格表
		JsonArray prices = new JsonArray();
		// 构建prices
		JsonArray details = bo.getJsonArray("detail");
		for (Object detail : details) {
			JsonObject detail_obj = (JsonObject) detail;
			JsonObject price = new JsonObject();
			
			price.put("goods", detail_obj.getJsonObject("goods"));
			price.put("invbatchcode", detail_obj.getString("batch_code"));
			price.put("supply_price", detail_obj.getJsonObject("supply_price"));
			price.put("retail_price", detail_obj.getJsonObject("retail_price"));
			price.put("commission", detail_obj.getJsonObject("commission"));

			prices.add(price);
		}
		// 创建门店价格表
		String getPriceAddress = from_account +  "." 
				+ this.appActivity.getService().getRealServiceName() + ".posprice.create";
		this.appActivity.getEventBus().send(getPriceAddress, prices, invRet -> {
			if (invRet.succeeded()) {
				future.complete(bo);
			} else {
				Throwable errThrowable = invRet.cause();
				String errMsgString = errThrowable.getMessage();
				appActivity.getLogger().error(errMsgString, errThrowable);				

				future.fail(invRet.cause());
			}
		});
		
		JsonArray paramList = new JsonArray();
		for (Object detail : bo.getJsonArray("detail")) {
			JsonObject param = new JsonObject();
			JsonObject detailO = (JsonObject) detail;
			param.put("warehouses", bo.getJsonObject("warehouse"));
			param.put("goods", detailO.getJsonObject("goods"));
			param.put("sku", detailO.getJsonObject("goods").getString("product_sku_code"));
			param.put("invbatchcode", detailO.getString("batch_code"));
			param.put("warehousecode", bo.getJsonObject("warehouse").getString("code"));
			param.put("onhandnum", detailO.getString("quantity_fact"));
			param.put("goodaccount", detailO.getJsonObject("goods").getString("account"));

			paramList.add(param);
		}
		// 增加现存量，调用现存量的接口
		String invSrvName = this.appActivity.getDependencies().getJsonObject("inventorycenter_service")
				.getString("service_name", "");
		String getWarehouseAddress = from_account + "." + invSrvName + "." + "stockonhand-mgr.batchcreate";
		this.appActivity.getEventBus().send(getWarehouseAddress, paramList, invRet -> {
			if (invRet.succeeded()) {
				//future.complete(bo);
			} else {
				Throwable errThrowable = invRet.cause();
				String errMsgString = errThrowable.getMessage();
				appActivity.getLogger().error(errMsgString, errThrowable);				

				//future.fail(invRet.cause());
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
