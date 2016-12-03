package ocr.pointofsale.allotinv;

import ocr.common.handler.SampleBillBaseQueryHandler;
import otocloud.framework.app.function.AppActivityImpl;

/**
 * 查询补货入库单
 * 
 * @author wanghw
 *
 */
public class AllotInvQueryHandler extends SampleBillBaseQueryHandler {

	public static final String ADDRESS = "getAllotInv";

	public AllotInvQueryHandler(AppActivityImpl appActivity) {
		super(appActivity);
		// TODO Auto-generated constructor stub
	}

	// 此action的入口地址
	@Override
	public String getEventAddress() {
		// TODO Auto-generated method stub
		return ADDRESS;
	}

	/**
	 * 要查询的单据状态
	 * 
	 * @return
	 */
	@Override
	public String getStatus() {
		// TODO Auto-generated method stub
		return "created";
	}

}
