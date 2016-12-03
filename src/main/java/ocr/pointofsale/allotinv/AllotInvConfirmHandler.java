package ocr.pointofsale.allotinv;

import java.util.ArrayList;
import java.util.List;

import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonObject;
import ocr.pointofsale.base.POSBaseHandler;
import otocloud.common.ActionURI;
import otocloud.framework.app.function.ActionDescriptor;
import otocloud.framework.app.function.AppActivityImpl;
import otocloud.framework.app.function.BizRootType;
import otocloud.framework.app.function.BizStateSwitchDesc;
import otocloud.framework.core.ApiParameterDescriptor;
import otocloud.framework.core.HandlerDescriptor;

/**
 * 补货调拨入库单确认操作
 * @author wanghw
 *
 */
public class AllotInvConfirmHandler extends POSBaseHandler{

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
	
	public String getPartnerAcct(JsonObject bo) {
		String partnerAcct = bo.getJsonObject("restocking_warehouse").getJsonObject("owner_org").getString("account"); 
		return partnerAcct;
	}	

	/**
	 * {@inheritDoc}
	 */
	@Override
	public ActionDescriptor getActionDesc() {

		ActionDescriptor actionDescriptor = super.getActionDesc();
		HandlerDescriptor handlerDescriptor = actionDescriptor.getHandlerDescriptor();

		// 外部访问url定义
		ActionURI uri = new ActionURI("confirm", HttpMethod.POST);
		handlerDescriptor.setRestApiURI(uri);

		// 状态变化定义
		BizStateSwitchDesc bizStateSwitchDesc = new BizStateSwitchDesc(BizRootType.BIZ_OBJECT, "created", "created");
		bizStateSwitchDesc.setWebExpose(true); // 是否向web端发布事件
		actionDescriptor.setBizStateSwitch(bizStateSwitchDesc);

		return actionDescriptor;
	}

}
