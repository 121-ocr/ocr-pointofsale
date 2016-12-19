package ocr.pointofsale;

import java.util.ArrayList;
import java.util.List;

import ocr.pointofsale.accept.AcceptComponent;
import ocr.pointofsale.allotinv.AllotInvComponent;
import ocr.pointofsale.posprice.POSPriceComponent;
import ocr.pointofsale.replenishment.ReplenishmentComponent;
import ocr.pointofsale.saleorder.SaleOrderComponent;
import ocr.pointofsale.shipment.ShipmentComponent;
import otocloud.framework.app.engine.AppServiceImpl;
import otocloud.framework.app.engine.WebServer;
import otocloud.framework.app.function.AppActivity;
import otocloud.framework.app.function.AppInitActivityImpl;

/**
 * 门店系统微服务
 * @author wanghw
 *
 */
public class PointOfSaleService extends AppServiceImpl
{

	//创建服务初始化组件
	@Override
	public AppInitActivityImpl createAppInitActivity() {		
		return null;
	}

	//创建租户级web server
	@Override
	public WebServer createWebServer() {
		// TODO Auto-generated method stub
		return null;
	}

	//创建服务内的业务活动组件
	@Override
	public List<AppActivity> createBizActivities() {
		List<AppActivity> retActivities = new ArrayList<>();
		
		AllotInvComponent allotInvCom = new AllotInvComponent();
		retActivities.add(allotInvCom);
		
		SaleOrderComponent saleOrderCom = new SaleOrderComponent();
		retActivities.add(saleOrderCom);
		
		POSPriceComponent priceCom = new POSPriceComponent();
		retActivities.add(priceCom);
		
		ReplenishmentComponent replenishmentComponent = new ReplenishmentComponent();
		retActivities.add(replenishmentComponent);
		
		ShipmentComponent shipmentComponent = new ShipmentComponent();
		retActivities.add(shipmentComponent);
		
		return retActivities;
	}
}
