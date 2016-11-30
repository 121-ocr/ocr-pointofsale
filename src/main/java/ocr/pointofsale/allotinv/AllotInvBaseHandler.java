package ocr.pointofsale.allotinv;

import io.vertx.core.MultiMap;
import io.vertx.core.json.JsonObject;
import otocloud.common.ActionContextTransfomer;
import otocloud.framework.app.function.ActionHandlerImpl;
import otocloud.framework.app.function.AppActivityImpl;
import otocloud.framework.core.OtoCloudBusMessage;

/**
 * 补货调拨入库单创建操作
 * 
 * @author wanghw
 *
 */
public class AllotInvBaseHandler extends ActionHandlerImpl<JsonObject> {

	public AllotInvBaseHandler(AppActivityImpl appActivity) {
		super(appActivity);
		// TODO Auto-generated constructor stub
	}

	@Override
	public void handle(OtoCloudBusMessage<JsonObject> msg) {
		MultiMap headerMap = msg.headers();

		JsonObject allotInv_model = msg.body();
		
		String boId = allotInv_model.getString("bo_id");
		//如果没有boid，则调用单据号生成规则生成一个单据号
		//TODO
    	String partnerAcct = allotInv_model.getJsonObject("restocking_warehouse").getJsonObject("owner_org").getString("account"); //交易单据一般要记录协作方
		// 当前操作人信息
		JsonObject actor = ActionContextTransfomer.fromMessageHeaderToActor(headerMap);

		// 记录事实对象（业务数据），会根据ActionDescriptor定义的状态机自动进行状态变化，并发出状态变化业务事件
		// 自动查找数据源，自动进行分表处理
		this.recordFactData(appActivity.getBizObjectType(), allotInv_model, boId, actor, partnerAcct, null, result -> {
			if (result.succeeded()) {
				msg.reply("ok");
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
		return null;
	}

}
