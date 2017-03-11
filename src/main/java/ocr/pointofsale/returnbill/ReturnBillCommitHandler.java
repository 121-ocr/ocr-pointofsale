package ocr.pointofsale.returnbill;

import io.vertx.core.Future;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import ocr.common.handler.SampleCDOBillBaseHandler;
import otocloud.common.ActionURI;
import otocloud.framework.app.function.ActionDescriptor;
import otocloud.framework.app.function.AppActivityImpl;
import otocloud.framework.app.function.BizRootType;
import otocloud.framework.app.function.BizStateSwitchDesc;
import otocloud.framework.core.HandlerDescriptor;

/**
 * 退货单提交
 * @author pcitc
 *
 */
public class ReturnBillCommitHandler extends SampleCDOBillBaseHandler{
	
	public ReturnBillCommitHandler(AppActivityImpl appActivity) {
		super(appActivity);
	}

	//此action的入口地址
	@Override
	public String getEventAddress() {
		return ReturnBillConstant.COMMIT_ADDRESS;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public ActionDescriptor getActionDesc() {

		ActionDescriptor actionDescriptor = super.getActionDesc();
		HandlerDescriptor handlerDescriptor = actionDescriptor.getHandlerDescriptor();
				
		ActionURI uri = new ActionURI(ReturnBillConstant.COMMIT_ADDRESS, HttpMethod.POST);
		handlerDescriptor.setRestApiURI(uri);
		
		//状态变化定义
		BizStateSwitchDesc bizStateSwitchDesc = new BizStateSwitchDesc(BizRootType.BIZ_OBJECT, 
				ReturnBillConstant.CREATE_STATUS, ReturnBillConstant.COMMIT_ADDRESS);
		bizStateSwitchDesc.setWebExpose(true); //是否向web端发布事件		
		actionDescriptor.setBizStateSwitch(bizStateSwitchDesc);
		
		return actionDescriptor;
	}
	
	@Override
	protected void afterProcess(JsonObject bo, Future<JsonObject> future) {
		String from_account = this.appActivity.getAppInstContext().getAccount();
		JsonArray paramList = new JsonArray();
		for (Object detail : bo.getJsonArray("detail")) {
			JsonObject param = new JsonObject();
			JsonObject detailO = (JsonObject) detail;
			param.put("warehouses", bo.getJsonObject("out_warehouse"));
			param.put("goods", detailO.getJsonObject("goods"));
			param.put("sku", detailO.getJsonObject("goods").getString("product_sku_code"));
			param.put("invbatchcode", detailO.getString("batch_code"));
			param.put("shelf_life", detailO.getString("shelf_life"));
			param.put("warehousecode", bo.getJsonObject("out_warehouse").getString("code"));
			param.put("onhandnum", detailO.getDouble("quantity")*(-1));
			param.put("goodaccount", detailO.getJsonObject("goods").getString("account"));
			param.put("status", "OUT");
			param.put("biz_data_type", "bp_returnbill");
			param.put("bo_id", bo.getString("bo_id"));

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
}
