package ocr.pointofsale.base;

import io.vertx.core.MultiMap;
import io.vertx.core.json.JsonObject;
import otocloud.common.ActionContextTransfomer;
import otocloud.framework.app.function.ActionHandlerImpl;
import otocloud.framework.app.function.AppActivityImpl;
import otocloud.framework.core.OtoCloudBusMessage;

/**
 * 单据操作基类
 * 
 * @author wanghw
 *
 */
public class POSBaseHandler extends ActionHandlerImpl<JsonObject> {

	public POSBaseHandler(AppActivityImpl appActivity) {
		super(appActivity);
		// TODO Auto-generated constructor stub
	}

	@Override
	public void handle(OtoCloudBusMessage<JsonObject> msg) {
		MultiMap headerMap = msg.headers();

		JsonObject bo = msg.body();
		
		String boId = bo.getString("bo_id");
		//如果没有boid，则调用单据号生成规则生成一个单据号
		//TODO
		//交易单据一般要记录协作方
    	String partnerAcct = getPartnerAcct(bo);
		// 当前操作人信息
		JsonObject actor = ActionContextTransfomer.fromMessageHeaderToActor(headerMap);

		// 记录事实对象（业务数据），会根据ActionDescriptor定义的状态机自动进行状态变化，并发出状态变化业务事件
		// 自动查找数据源，自动进行分表处理
		this.recordFactData(appActivity.getBizObjectType(), bo, boId, actor, partnerAcct, null, result -> {
			if (result.succeeded()) {
				msg.reply(bo); //返回BO
			} else {
				Throwable errThrowable = result.cause();
				String errMsgString = errThrowable.getMessage();
				appActivity.getLogger().error(errMsgString, errThrowable);
				msg.fail(100, errMsgString);
			}

		});
	}

	public String getPartnerAcct(JsonObject bo) {
		return null;
	}	

	@Override
	public String getEventAddress() {
		// TODO Auto-generated method stub
		return null;
	}

}
