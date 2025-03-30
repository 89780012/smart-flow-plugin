package com.smart.utils;

/**
 * 元组工具类,支持2个或3个参数的元组
 */
public class Tuple {
    
    /**
     * 二元组类
     */
    public static class Tuple2<T1, T2> {
        private final T1 first;
        private final T2 second;

        public Tuple2(T1 first, T2 second) {
            this.first = first;
            this.second = second;
        }

        public T1 getFirst() {
            return first;
        }

        public T2 getSecond() {
            return second;
        }

        @Override
        public String toString() {
            return "(" + first + ", " + second + ")";
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Tuple2<?, ?> tuple2 = (Tuple2<?, ?>) o;
            return (first == null ? tuple2.first == null : first.equals(tuple2.first)) &&
                   (second == null ? tuple2.second == null : second.equals(tuple2.second));
        }

        @Override
        public int hashCode() {
            int result = first != null ? first.hashCode() : 0;
            result = 31 * result + (second != null ? second.hashCode() : 0);
            return result;
        }
    }

    /**
     * 三元组类
     */
    public static class Tuple3<T1, T2, T3> {
        private final T1 first;
        private final T2 second;
        private final T3 third;

        public Tuple3(T1 first, T2 second, T3 third) {
            this.first = first;
            this.second = second;
            this.third = third;
        }

        public T1 getFirst() {
            return first;
        }

        public T2 getSecond() {
            return second;
        }

        public T3 getThird() {
            return third;
        }

        @Override
        public String toString() {
            return "(" + first + ", " + second + ", " + third + ")";
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Tuple3<?, ?, ?> tuple3 = (Tuple3<?, ?, ?>) o;
            return (first == null ? tuple3.first == null : first.equals(tuple3.first)) &&
                   (second == null ? tuple3.second == null : second.equals(tuple3.second)) &&
                   (third == null ? tuple3.third == null : third.equals(tuple3.third));
        }

        @Override
        public int hashCode() {
            int result = first != null ? first.hashCode() : 0;
            result = 31 * result + (second != null ? second.hashCode() : 0);
            result = 31 * result + (third != null ? third.hashCode() : 0);
            return result;
        }
    }

    /**
     * 创建二元组的工厂方法
     */
    public static <T1, T2> Tuple2<T1, T2> of(T1 first, T2 second) {
        return new Tuple2<>(first, second);
    }

    /**
     * 创建三元组的工厂方法
     */
    public static <T1, T2, T3> Tuple3<T1, T2, T3> of(T1 first, T2 second, T3 third) {
        return new Tuple3<>(first, second, third);
    }
}
