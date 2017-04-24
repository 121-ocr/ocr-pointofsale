package ocr.pointofsale.posprice;

import java.util.ArrayList;
import java.util.List;

import otocloud.framework.app.function.AppActivityImpl;
import otocloud.framework.core.OtoCloudEventDescriptor;
import otocloud.framework.core.OtoCloudEventHandlerRegistry;

/**
 * 门店代销价格组件
 * @author wanghw
 *
 */
public class POSPriceComponent extends AppActivityImpl {

	//业务活动组件名
	@Override
	public String getName() {
		// TODO Auto-generated method stub
		return "posprice";
	}
	
	//业务活动组件要处理的核心业务对象
	@Override
	public String getBizObjectType() {
		// TODO Auto-generated method stub
		return "bc_consignment_prices";
	}


	//发布此业务活动对外暴露的业务事件
	@Override
	public List<OtoCloudEventDescriptor> exposeOutboundBizEventsDesc() {
		// TODO Auto-generated method stub
		return null;
	}


	//业务活动组件中的业务功能
	@Override
	public List<OtoCloudEventHandlerRegistry> registerEventHandlers() {
		// TODO Auto-generated method stub
		List<OtoCloudEventHandlerRegistry> ret = new ArrayList<OtoCloudEventHandlerRegistry>();

		POSPriceCreateHandler priceCreateHandler = new POSPriceCreateHandler(this);
		ret.add(priceCreateHandler);
		
		POSPriceQueryHandler priceQueryHandler = new POSPriceQueryHandler(this);
		ret.add(priceQueryHandler);
		
		return ret;
	}

}
