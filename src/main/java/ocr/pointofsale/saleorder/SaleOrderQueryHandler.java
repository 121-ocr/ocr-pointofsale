package ocr.pointofsale.saleorder;

import io.vertx.core.json.JsonObject;
import ocr.common.handler.SampleBillBaseQueryHandler;
import otocloud.framework.app.function.AppActivityImpl;

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
