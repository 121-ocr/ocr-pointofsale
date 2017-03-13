package ocr.pointofsale.returnbill;

import java.util.List;

import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonObject;
import ocr.common.handler.SampleCDOBillBaseQueryHandler;
import otocloud.common.ActionURI;
import otocloud.framework.app.function.ActionDescriptor;
import otocloud.framework.app.function.AppActivityImpl;
import otocloud.framework.core.HandlerDescriptor;

/**
 * 退货单查询
 * @author pcitc
 *
 */
public class ReturnBillQueryHandler extends SampleCDOBillBaseQueryHandler{
	
	public ReturnBillQueryHandler(AppActivityImpl appActivity) {
		super(appActivity);
	}

	// 此action的入口地址
	@Override
	public String getEventAddress() {
		// TODO Auto-generated method stub
		return ReturnBillConstant.QUERY_ADDRESS;
	}

	/**
	 * 要查询的单据状态
	 * 
	 * @return
	 */
	public List<String> getStatus(JsonObject msgBody) {	
		return msgBody.getJsonArray("query_status").getList();
	}	
	
	/**
	 * 此action的自描述元数据
	 */
	@Override
	public ActionDescriptor getActionDesc() {

		ActionDescriptor actionDescriptor = super.getActionDesc();
		HandlerDescriptor handlerDescriptor = actionDescriptor.getHandlerDescriptor();

		ActionURI uri = new ActionURI(getEventAddress(), HttpMethod.POST);
		handlerDescriptor.setRestApiURI(uri);

		return actionDescriptor;
	}

}