package enhancedForLoopUsage;

import java.util.Iterator;
import java.util.List;

public class GoodFindEnhancedForLoopVariableModificationTest {
    void modifyCopyElementOfCollection() {
        List<Integer> list = List.of(13, 14, 15);
        boolean first = true;

        for (final Integer i: list) {
            Integer item = i;
            if (first) {
                first = false;
                item = 99;
            }
            System.out.println(" New item: " + item);
        }
    }

    void readOnlyAccessToCollection() {
        List<String> names = List.of("Alice", "Bob", "Charlie");

        for (String name : names) {
            System.out.println("Name: " + name);
        }
    }

    void modifyObjectPropertyWithoutChangingCollection() {
        List<Person> people = List.of(new Person("Alice"), new Person("Bob"));

        for (Person person : people) {
            person.name = "Updated " + person.name;
        }
    }

    void modifyNestedCollection() {
        List<List<Integer>> nestedList = List.of(
                List.of(1, 2, 3),
                List.of(4, 5, 6)
        );

        for (List<Integer> sublist : nestedList) {
            sublist.set(0, sublist.get(0) + 10);
        }
    }

    void arrayIndexForLoop() {
        char[] characters = {'a', 'b', 'c'};
        int size = characters.length;
        for (int i = 0; i < size; i++) {
            int character = characters[i];
            character = 'd';
        }
    }

    void collectionIteratorLoop() {
        List<String> names = List.of("Bob", "Marie", "Anne");
        for (Iterator<String> i = names.iterator(); i.hasNext();) {
            String name = (String) i.next();

            name = "Adam";
        }
    }
}

class Person {
    String name;

    Person(String name) {
        this.name = name;
    }
}
