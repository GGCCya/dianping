

import com.hmdp.HmDianPingApplication;
import com.hmdp.service.impl.ShopServiceImpl;
import com.hmdp.utils.RedisIdWorker;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import javax.annotation.Resource;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@SpringBootTest(classes = HmDianPingApplication.class)
public class test2 {
    @Resource
    private ShopServiceImpl shopService;



    @Test
    public void test1() throws InterruptedException {
        shopService.saveShopToRedis(1L,10L);
    }




    @Resource
    private RedisIdWorker redisIdWorker;

    private ExecutorService es = Executors.newFixedThreadPool(500);
  //线性池测试
    @Test
    public void testRedisId() throws InterruptedException {
        CountDownLatch cd = new CountDownLatch(300);
        Runnable task = () ->{
            for (int i = 0; i < 100; i++) {
                long shop = redisIdWorker.nextId("shop");
                System.out.println("shop = " + shop);
            }
            cd.countDown();
        };

        long begin = System.currentTimeMillis();

        for (int i = 0; i < 300; i++) {
            es.submit(task);
        }
        cd.await();
        long end = System.currentTimeMillis();

        System.out.println(end - begin);
    }


}
