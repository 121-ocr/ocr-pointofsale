package ocr.pointofsale;

import java.util.List;

import otocloud.framework.app.engine.AppService;
import otocloud.framework.app.engine.AppServiceEngineImpl;
import otocloud.framework.core.OtoCloudComponent;

/**
 * 门店系统微服务入口
 * @author wanghw
 *
 */
public class App  extends AppServiceEngineImpl
{

	//创建此APP中租户的应用服务实例时调用
	@Override
	public AppService newAppInstance() {
		return new PointOfSaleService();
	}

	//创建APP全局组件
	@Override
	public List<OtoCloudComponent> createServiceComponents() {
		return null;
	}
	
	
    public static void main( String[] args )
    {
    	App app = new App();

    	AppServiceEngineImpl.internalMain("log4j2.xml",
    										"ocr-pointofsale.json", 
    										app);
    	
    }   

}
