package ocr.pointofsale.allotinv;

import io.vertx.core.MultiMap;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonObject;
import otocloud.common.ActionContextTransfomer;
import otocloud.common.ActionURI;
import otocloud.framework.app.function.ActionDescriptor;
import otocloud.framework.app.function.ActionHandlerImpl;
import otocloud.framework.app.function.AppActivityImpl;
import otocloud.framework.app.function.BizRootType;
import otocloud.framework.app.function.BizStateSwitchDesc;
import otocloud.framework.core.HandlerDescriptor;
import otocloud.framework.core.OtoCloudBusMessage;

/**
 * 补货调拨入库单创建操作
 * 
 * @author wanghw
 *
 */
public class AllotInvCreatHandler extends ActionHandlerImpl<JsonObject> {

	public static final String ADDRESS = "creat";

	public AllotInvCreatHandler(AppActivityImpl appActivity) {
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
	public void handle(OtoCloudBusMessage<JsonObject> msg) {
		MultiMap headerMap = msg.headers();

		JsonObject allotInv_model = msg.body();
		
		//转化为业务实体结构
		JsonObject allotInv_entity = JsonModel2EntityUtil.convert(allotInv_model);

		// 当前操作人信息
		JsonObject actor = ActionContextTransfomer.fromMessageHeaderToActor(headerMap);

		// 记录事实对象（业务数据），会根据ActionDescriptor定义的状态机自动进行状态变化，并发出状态变化业务事件
		// 自动查找数据源，自动进行分表处理
		this.recordFactData(appActivity.getBizObjectType(), allotInv_entity, null, actor, null, null, result -> {
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

	/**
	 * {@inheritDoc}
	 */
	@Override
	public ActionDescriptor getActionDesc() {

		ActionDescriptor actionDescriptor = super.getActionDesc();
		HandlerDescriptor handlerDescriptor = actionDescriptor.getHandlerDescriptor();
		// handlerDescriptor.setMessageFormat("command");

		// 参数
		/*
		 * List<ApiParameterDescriptor> paramsDesc = new
		 * ArrayList<ApiParameterDescriptor>(); paramsDesc.add(new
		 * ApiParameterDescriptor("targetacc","")); paramsDesc.add(new
		 * ApiParameterDescriptor("soid",""));
		 * 
		 * actionDescriptor.getHandlerDescriptor().setParamsDesc(paramsDesc);
		 */

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
