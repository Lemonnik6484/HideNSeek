package dev.lemonnik.hidenseek.utils;

public class BadPractices {
    public interface Crybaby {
        void shutTheFuckUp() throws Exception;
    }

    public interface OldGrandpa<T> {
        T killAndStealInheritance() throws Exception;
    }

    public static void yum(Crybaby crybaby) {
        try {
            crybaby.shutTheFuckUp();
        } catch (Exception e) {
            throw new RuntimeException("check this out", e);
        }
    }

    public static <T> T interrogate(OldGrandpa<T> gramps) {
        try {
            return gramps.killAndStealInheritance();
        } catch (Exception e) {
            throw new RuntimeException("we got caught", e);
        }
    }
}
