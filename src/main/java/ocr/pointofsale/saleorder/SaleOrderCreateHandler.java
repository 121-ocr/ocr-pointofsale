package ocr.pointofsale.saleorder;

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

/**
 * 零售单创建操作
 * 
 * @author wanghw
 *
 */
public class SaleOrderCreateHandler extends SampleBillBaseHandler {

	public static final String ADDRESS = "create";

	public SaleOrderCreateHandler(AppActivityImpl appActivity) {
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

	/**
	 * 单据保存后处理
	 * 
	 * @param bo
	 * @param future
	 */
	protected void afterProcess(JsonObject bo, Future<JsonObject> future) {
		String from_account = this.appActivity.getAppInstContext().getAccount();
		JsonArray paramList = new JsonArray();
		for (Object detail : bo.getJsonArray("detail")) {
			JsonObject param = new JsonObject();
			JsonObject detailO = (JsonObject) detail;
			param.put("warehouses", bo.getJsonObject("warehouse"));
			param.put("goods", detailO.getJsonObject("goods"));
			param.put("sku", detailO.getJsonObject("goods").getString("product_sku_code"));
			param.put("invbatchcode", detailO.getString("batch_code"));
			param.put("warehousecode", bo.getJsonObject("warehouse").getString("code"));
			param.put("onhandnum", "-" + detailO.getString("quantity"));
			param.put("goodaccount", detailO.getJsonObject("goods").getString("account"));

			paramList.add(param);
		}
		// 增加现存量，调用现存量的接口
		String invSrvName = this.appActivity.getDependencies().getJsonObject("inventorycenter_service")
				.getString("service_name", "");
		String getWarehouseAddress = from_account + "." + invSrvName + "." + "stockonhand-mgr.batchcreate";
		this.appActivity.getEventBus().send(getWarehouseAddress, paramList, invRet -> {
			if (invRet.succeeded()) {
				future.complete(bo);
			} else {
				future.fail(invRet.cause());
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
		ActionURI uri = new ActionURI("create", HttpMethod.POST);
		handlerDescriptor.setRestApiURI(uri);

		// 状态变化定义
		BizStateSwitchDesc bizStateSwitchDesc = new BizStateSwitchDesc(BizRootType.BIZ_OBJECT, null, "created");
		bizStateSwitchDesc.setWebExpose(true); // 是否向web端发布事件
		actionDescriptor.setBizStateSwitch(bizStateSwitchDesc);

		return actionDescriptor;
	}

}
