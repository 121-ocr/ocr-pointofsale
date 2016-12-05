package ocr.pointofsale.posprice;

import ocr.common.handler.SampleDocQueryHandler;
import otocloud.framework.app.function.AppActivityImpl;

/**
 * 查询门店代销价格表
 * 
 * @author wanghw
 *
 */
public class POSPriceQueryHandler extends SampleDocQueryHandler {

	public static final String ADDRESS = "getall";

	public POSPriceQueryHandler(AppActivityImpl appActivity) {
		super(appActivity);
		// TODO Auto-generated constructor stub
	}

	@Override
	public String getEventAddress() {
		// TODO Auto-generated method stub
		return ADDRESS;
	}



}
