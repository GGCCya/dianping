import com.hmdp.entity.ShopType;

import java.util.Arrays;
import java.util.List;

public class test1 {
    public static void main(String[] args) {
        // 构建 typeList 示例数据
        List<ShopType> typeList = Arrays.asList(
                new ShopType().setId(1L).setName("Restaurant").setIcon("/icons/rest.png").setSort(1),
                new ShopType().setId(2L).setName("Bookstore").setIcon("/icons/book.png").setSort(2),
                new ShopType().setId(3L).setName("Clothing Store").setIcon("/icons/cloth.png").setSort(3)
        );

        // 遍历并打印 typeList
        //System.out.println("ShopType List:");
        typeList.forEach(System.out::println);
        /*typeList.forEach(shopType -> {
            System.out.println("ID: " + shopType.getId());
            System.out.println("Name: " + shopType.getName());
            System.out.println("Icon: " + shopType.getIcon());
            System.out.println("Sort: " + shopType.getSort());
            System.out.println("-----------------------------");
        })*/
    }
}
