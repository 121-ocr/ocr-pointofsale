package ocr.pointofsale.saleorder;

import io.vertx.core.json.JsonObject;
import ocr.common.handler.SampleBillBaseQueryHandler;
import otocloud.framework.app.function.AppActivityImpl;
import otocloud.framework.common.CallContextSchema;
import otocloud.framework.core.CommandMessage;

/**
 * 查询零售单
 * query={"operator":"","salesman":"","deskNo":"","sale_date":{$gte:""},"sale_date":{$lte:""}}
 * 
 * @author wanghw
 *
 */
public class SaleOrderQueryHandler extends SampleBillBaseQueryHandler {

	/**
	 * @param appActivity
	 */
	public SaleOrderQueryHandler(AppActivityImpl appActivity) {
		super(appActivity);
		// TODO Auto-generated constructor stub
	}

	// 此action的入口地址
	@Override
	public String getEventAddress() {
		// TODO Auto-generated method stub
		return SaleOrderConstant.QUERY_ADDRESS;
	}
	
	@Override
	public String getBizUnit(CommandMessage<JsonObject> msg){
/*		JsonObject session = msg.getSession();
		boolean is_global_bu =  session.getBoolean(CallContextSchema.IS_GLOBAL_BU, true);
*/		
		//按业务单元隔离
		String bizUnit = msg.getCallContext().getString(CallContextSchema.BIZ_UNIT_ID);		
		return 	bizUnit;
	}

	/**
	 * 要查询的单据状态
	 * 
	 * @return
	 */
	@Override
	public String getStatus(JsonObject msgBody) {
		// TODO Auto-generated method stub
		return SaleOrderConstant.CREATE_STATUS;
	}

}
