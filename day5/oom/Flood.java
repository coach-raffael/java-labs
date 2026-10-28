// Module 17, section 17.2: fill the heap on purpose and read the OutOfMemoryError.
// Run with a small heap and watch the count:   java -Xmx32m Flood.java
// Then with the default heap (a quarter of your RAM):   java Flood.java
// Every Medium holds a few strings; the list keeps them all alive, so the garbage collector cannot free anything.
import java.util.ArrayList;
import java.util.List;

record Medium(int id, String title, String author, String isbn) {
}

void main() {
    List<Medium> all = new ArrayList<>();
    long maxHeap = Runtime.getRuntime().maxMemory() / (1024 * 1024);
    IO.println("max heap: " + maxHeap + " MB");
    int i = 0;
    while (true) {
        i++;
        all.add(new Medium(i, "Title number " + i, "Author number " + i, "978" + i));
        if (i % 100_000 == 0) {
            IO.println(i + " media in memory, heap used " + (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / (1024 * 1024) + " MB");
        }
    }
}
