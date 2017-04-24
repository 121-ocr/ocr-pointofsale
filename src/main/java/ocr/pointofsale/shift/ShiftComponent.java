package ocr.pointofsale.shift;

import java.util.ArrayList;
import java.util.List;

import otocloud.framework.app.function.AppActivityImpl;
import otocloud.framework.core.OtoCloudEventDescriptor;
import otocloud.framework.core.OtoCloudEventHandlerRegistry;

/**
 * 交接班 组件
 * 
 * 2017年3月27日
 *
 */
public class ShiftComponent extends AppActivityImpl {

	// 业务活动组件名
	@Override
	public String getName() {
		return "shift";
	}

	// 业务活动组件要处理的核心业务对象
	@Override
	public String getBizObjectType() {
		return "bp_shift";
	}


	// 发布此业务活动对外暴露的业务事件
	@Override
	public List<OtoCloudEventDescriptor> exposeOutboundBizEventsDesc() {
		return null;
	}

	// 业务活动组件中的业务功能
	@Override
	public List<OtoCloudEventHandlerRegistry> registerEventHandlers() {

		List<OtoCloudEventHandlerRegistry> ret = new ArrayList<OtoCloudEventHandlerRegistry>();
		
		ShiftCreateHandler shiftCreateHandler = new ShiftCreateHandler(this);
		ret.add(shiftCreateHandler);

		ShiftQueryHandler shiftQueryHandler = new ShiftQueryHandler(this);
		ret.add(shiftQueryHandler);

		return ret;
	}

}
