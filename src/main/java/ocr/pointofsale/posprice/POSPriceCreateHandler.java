package ocr.pointofsale.posprice;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import ocr.common.handler.SampleDocBaseHandler;
import otocloud.common.ActionURI;
import otocloud.framework.app.function.ActionDescriptor;
import otocloud.framework.app.function.AppActivityImpl;
import otocloud.framework.app.function.BizRootType;
import otocloud.framework.app.function.BizStateSwitchDesc;
import otocloud.framework.core.HandlerDescriptor;

/**
 * 门店代销价格创建操作
 * 
 * @author wanghw
 *
 */
public class POSPriceCreateHandler extends SampleDocBaseHandler {

	public static final String ADDRESS = "create";

	public POSPriceCreateHandler(AppActivityImpl appActivity) {
		super(appActivity);
	}

	/**
	 * corecorp_setting.setting
	 */
	@Override
	public String getEventAddress() {
		return ADDRESS;
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
		BizStateSwitchDesc bizStateSwitchDesc = new BizStateSwitchDesc(BizRootType.BIZ_OBJECT, null, "created");
		bizStateSwitchDesc.setWebExpose(true); // 是否向web端发布事件
		actionDescriptor.setBizStateSwitch(bizStateSwitchDesc);

		return actionDescriptor;
	}

	/**
	 * 供补货入库单调用的接口
	 * 
	 * @param prices
	 */
	public void ceatePrice(JsonArray prices, Handler<AsyncResult<JsonArray>> next) {
		Future<JsonArray> future = Future.future();
		future.setHandler(next);
		String acctId = this.appActivity.getAppInstContext().getAccount();
		for (Object settingInfo : prices) {
			((JsonObject) settingInfo).put("account", acctId);
		}
		// 记录事实对象（业务数据），会根据ActionDescriptor定义的状态机自动进行状态变化，并发出状态变化业务事件
		// 自动查找数据源，自动进行分表处理
		appActivity.getAppDatasource().getMongoClient_oto().save(appActivity.getDBTableName(appActivity.getName()),
				prices, result -> {
					if (result.succeeded()) {
						JsonArray bos = result.result();
						future.complete(bos);
					} else {
						Throwable errThrowable = result.cause();
						String errMsgString = errThrowable.getMessage();
						appActivity.getLogger().error(errMsgString, errThrowable);
						future.fail(errThrowable);
					}
				});
	}
}
