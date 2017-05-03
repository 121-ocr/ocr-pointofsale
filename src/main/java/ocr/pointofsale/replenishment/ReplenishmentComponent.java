package ocr.pointofsale.replenishment;

import java.util.ArrayList;
import java.util.List;

import otocloud.framework.app.function.AppActivityImpl;
import otocloud.framework.core.OtoCloudEventDescriptor;
import otocloud.framework.core.OtoCloudEventHandlerRegistry;

/**
 * 渠道补货组件
 * @author wanghw
 *
 */
public class ReplenishmentComponent extends AppActivityImpl {

	//业务活动组件名
	@Override
	public String getName() {
		// TODO Auto-generated method stub
		return "replenishment-mgr";
	}
	
	//业务活动组件要处理的核心业务对象
	@Override
	public String getBizObjectType() {
		// TODO Auto-generated method stub
		return "bp_replenishments";
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

		ReplenishmentCreateHandler replenishmentCreateHandler = new ReplenishmentCreateHandler(this);
		ret.add(replenishmentCreateHandler);
		
		ReplenishmentIncomingQueryHandler replenishmentQueryHandler = new ReplenishmentIncomingQueryHandler(this);
		ret.add(replenishmentQueryHandler);	
		
		ReplenishmentRecordReceiptHandler replenishmentRecordReceiptHandler = new ReplenishmentRecordReceiptHandler(this);
		ret.add(replenishmentRecordReceiptHandler);
	
		return ret;
	}

}
