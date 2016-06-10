import java.util.WeakHashMap;

public class PubSub {

    static @FunctionalInterface interface Subscriber<T> {
        public void hereYouGo(T message);
    }

    static class Publisher<T> {
        WeakHashMap<Subscriber<T>, Boolean> subscribers = new WeakHashMap<>();

        public void publish(T importantThing) {
            for (Subscriber<T> s : subscribers.keySet()) {
                s.hereYouGo(importantThing);
            }
        }

        public void addSubscriber(Subscriber<T> subscriber) {
            subscribers.put(subscriber, true);
        }
    }

    static class ClassWithACoupleOfSubscribers {
        public final PubSub.Subscriber<String> prefixedSubscriber;
        public final PubSub.Subscriber<String> baseSubscriber;

        public ClassWithACoupleOfSubscribers(String prefix) {
            this.baseSubscriber = message -> System.out.println(message);
            this.prefixedSubscriber = message -> System.out.println(prefix + message);
        }
    }

    public static void main(String[] args) throws Exception {
        Publisher<String> pub = new Publisher<>();

        //these don't get cleared by GC
        int count =1 ;
        while (count < 10000000) {
            pub.addSubscriber(message -> System.out.println(message));
            pub.addSubscriber(message -> System.out.println(message));
            System.out.println(count);
            count++;
        }

        //this one does
        String prefix = "_";
        pub.addSubscriber(message -> System.err.println(prefix + message));

        //this one does too
        pub.addSubscriber(new Subscriber<String>() {
            @Override
            public void hereYouGo(String message) {
                System.out.println(message.toUpperCase());
            }
        });

        //add in separate method so the ClassWithACoupleOfSubscribers can be GC'd from here
        addSubsWithRefToInstance(pub);

        //GC 10 times just to be sure :-D
        for (int i = 0; i < 10; i++) {
            System.out.println(pub.subscribers.size());
            System.gc();
            Thread.sleep(100);
        }
        //size should be zero, but those first lambdas don't clear :-(
        System.out.println(pub.subscribers.size());
    }

    private static void addSubsWithRefToInstance(Publisher<String> pub) {
        ClassWithACoupleOfSubscribers exampleSubscriber = new ClassWithACoupleOfSubscribers("_");
        //this first one doesn't get cleared,
        pub.addSubscriber(exampleSubscriber.baseSubscriber);
        //but this one does as it uses the prefix from the ExampleSubscriber instance
        pub.addSubscriber(exampleSubscriber.prefixedSubscriber);
    }
}
