package EnhancedForLoopUsage;

import java.util.Arrays;
import java.util.List;

public class BadEnhanceForLoopUsageCheckTest {

    void modifyCollectionWithEnhancedForLoop() {
        List<Integer> list = Arrays.asList(new Integer[]{13, 14, 15});
        boolean first = true;

        System.out.println("Processing list...");
        for (Integer i : list) {
            if (first) {
                first = false;
                i = new Integer(99);
            }
            System.out.println(" New item: " + i);
        }

        System.out.println("Modified list?");
        for (
                Integer i : list) {
            System.out.println("List item: " + i);
        }
    }

    void modifyElementDirectly() {
        List<String> names = Arrays.asList("Alice", "Bob", "Charlie");

        for (String name : names) {
            name = "Modified"; // Reassigning loop variable
        }
    }

    void modifyArrayListDirectly() {
        List<Integer> numbers = Arrays.asList(1, 2, 3);

        for (Integer number : numbers) {
            number += 10; // Modifying the loop variable
        }
    }

    void modifyPrimitiveArray() {
        int[] numbers = {1, 2, 3, 4};

        for (int number : numbers) {
            number = number + 10;
            System.out.println("Modified number: " + number);
        }
    }
}
