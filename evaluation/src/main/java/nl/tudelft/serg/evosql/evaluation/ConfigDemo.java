package nl.tudelft.serg.evosql.evaluation;


import in2test.util.Wrap;

public class ConfigDemo {
    protected static boolean PLATFORM_J2SE = true;
    protected static boolean PLATFORM_IKVM = false;
    protected static boolean PLATFORM_NET = false;
    public static String IN2TEST_PROJECT_ROOT = ".\\";

    public ConfigDemo() {
    }

    public static void setLanguage(String language, String country) {
        Wrap.setDefaultLanguage(language, country);
    }

    public static String getImplementationTitle() {
        String title = Wrap.getPlatformName();
        if (title.equalsIgnoreCase("IKVM.NET OpenJDK")) {
            title = "IKVM";
        }

        return title;
    }

    public static String getImplementationVersion() {
        return Wrap.getPlatformVersion();
    }

    static {
        if (getImplementationTitle().equals("IKVM")) {
            PLATFORM_IKVM = true;
        } else if (getImplementationTitle().equals("NET")) {
            PLATFORM_J2SE = false;
            PLATFORM_NET = true;
        }

        System.err.println(getImplementationTitle() + " " + getImplementationVersion());
        if (PLATFORM_IKVM || PLATFORM_NET) {
            IN2TEST_PROJECT_ROOT = "..\\..\\..\\";
        }

    }
}

