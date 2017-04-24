package ocr.pointofsale.returnbill;

import java.util.ArrayList;
import java.util.List;

import otocloud.framework.app.function.AppActivityImpl;
import otocloud.framework.core.OtoCloudEventDescriptor;
import otocloud.framework.core.OtoCloudEventHandlerRegistry;

/**
 * 退货单组件
 * @author wanghw
 *
 */
public class ReturnBillComponent extends AppActivityImpl {

	//业务活动组件名
	@Override
	public String getName() {
		// TODO Auto-generated method stub
		return "returnbill";
	}
	
	//业务活动组件要处理的核心业务对象
	@Override
	public String getBizObjectType() {
		// TODO Auto-generated method stub
		return "bp_returnbill";
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

		ReturnBillCreateHandler replenishmentCreateHandler = new ReturnBillCreateHandler(this);
		ret.add(replenishmentCreateHandler);
		
		ReturnBillQueryHandler replenishmentQueryHandler = new ReturnBillQueryHandler(this);
		ret.add(replenishmentQueryHandler);	
		
		ReturnBillCompleteHandler returnBillCompleteHandler = new ReturnBillCompleteHandler(this);
		ret.add(returnBillCompleteHandler);
		
		ReturnBillCommitHandler returnBillCommitHandler = new ReturnBillCommitHandler(this);
		ret.add(returnBillCommitHandler);
	
		return ret;
	}

}
