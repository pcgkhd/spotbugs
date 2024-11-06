package enhancedForLoopUsage;

import java.util.Arrays;
import java.util.List;

public class GoodEnhanceForLoopUsageCheckTest {
    void modifyCopyElementOfCollection() {
        List<Integer> list = Arrays.asList(new Integer[]{13, 14, 15});
        boolean first = true;

        for (final Integer i: list) {
            Integer item = i;
            if (first) {
                first = false;
                item = new Integer(99);
            }
            System.out.println(" New item: " + item);
        }
    }

    void readOnlyAccessToCollection() {
        List<String> names = Arrays.asList("Alice", "Bob", "Charlie");

        for (String name : names) {
            System.out.println("Name: " + name);
        }
    }

    void modifyObjectPropertyWithoutChangingCollection() {
        List<Person> people = Arrays.asList(new Person("Alice"), new Person("Bob"));

        for (Person person : people) {
            person.name = "Updated " + person.name;
        }
    }

    void modifyNestedCollection() {
        List<List<Integer>> nestedList = Arrays.asList(
                Arrays.asList(1, 2, 3),
                Arrays.asList(4, 5, 6)
        );

        for (List<Integer> sublist : nestedList) {
            sublist.set(0, sublist.get(0) + 10);
        }
    }
}

class Person {
    String name;

    Person(String name) {
        this.name = name;
    }
}
