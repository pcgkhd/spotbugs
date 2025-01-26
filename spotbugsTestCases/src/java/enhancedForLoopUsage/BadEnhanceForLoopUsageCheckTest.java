package enhancedForLoopUsage;

import java.util.ArrayList;
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
                i = 99;
            }
            System.out.println(" New item: " + i);
        }

        System.out.println("Modified list?");
        for (Integer i : list) {
            System.out.println("List item: " + i);
        }
    }

    void modifyStringElement() {
        List<String> names = Arrays.asList("Alice", "Bob", "Charlie");

        for (String name : names) {
            name = "Modified";
        }
    }

    void loopWithinLoop() {
        List<List<Integer>> numberLists = Arrays.asList(List.of(1, 2, 4), List.of(1, 2, 4), List.of(1, 2, 3));

        for (List<Integer> numbers : numberLists) {
            for (Integer number : numbers) {
                number = number + 10;
            }
        }
    }

    void notEnhancedLoopWithinEnhancedLoops() {
        List<List<Integer>> numberLists = Arrays.asList(List.of(1, 2, 4), List.of(1, 2, 4), List.of(1, 2, 3));

        for (List<Integer> numbers : numberLists) {
            for (int i = 0; i < 10; i++) {
                for (Integer number : numbers) {
                    number = number + 10;
                }
            }
        }
    }

    void modifyOuterLoopVariable() {
        List<List<Integer>> numberLists = Arrays.asList(List.of(1, 2, 4), List.of(1, 2, 4), List.of(1, 2, 3));

        for (List<Integer> numbers : numberLists) {
            for (int i = 0; i < 10; i++) {
                for (Integer number : numbers) {
                    System.out.println("Modified number: " + number);
                }
            }
            numbers = new ArrayList<>();
        }
    }

    void multipleEnhancedLoopVariableModification() {
        List<List<Integer>> numberLists = Arrays.asList(List.of(1, 2, 4), List.of(1, 2, 4), List.of(1, 2, 3));

        for (List<Integer> numbers : numberLists) {
            for (Integer number : numbers) {
                number = number + 10;
            }
            numbers = new ArrayList<>();
        }
    }

    void arrayEnhancedForLoop() {
        int[] numbers = {1, 2, 3};

        for (int number : numbers) {
            number = 2;
        }
    }

    void charArrayEnhancedForLoop() {
        char[] characters = {'a', 'b', 'c'};

        for (char character : characters) {
            character = 'd';
        }
    }

    void mixedForLoopsWithEnhancedLoops() {
        char[] characters = {'a', 'b', 'c'};
        List<Integer> numbers = List.of(1, 2, 3);

        for (int i = 0; i < 5; i++) {
            i = i + 1;
            for (Integer number : numbers) {
                for (char character : characters) {
                    character = 'd';
                }
                number = 2;
            }
        }
    }
}
