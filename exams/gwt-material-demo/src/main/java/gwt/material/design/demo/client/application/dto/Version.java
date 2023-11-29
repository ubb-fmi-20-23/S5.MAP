package gwt.material.design.demo.client.application.dto;

import java.io.Serializable;

/**
 * Version DTO to display the downloadable links for gwt-material, addins , and theme repositories.
 * @author kevzlou7979
 */
public class Version implements Serializable {

    public enum VersionLink{

        // FOR gwt-material Core
        CORE_1_5_SNAPSHOT("https://oss.sonatype.org/content/repositories/snapshots/com/github/gwtmaterialdesign/gwt-material/1.5.0-SNAPSHOT/"),
        CORE_1_4_1("http://mvnrepository.com/artifact/com.github.gwtmaterialdesign/gwt-material/1.4.1"),
        CORE_1_4("http://mvnrepository.com/artifact/com.github.gwtmaterialdesign/gwt-material/1.4"),
        CORE_1_3_3("http://mvnrepository.com/artifact/com.github.gwtmaterialdesign/gwt-material/1.3.3"),
        CORE_1_3_2("http://mvnrepository.com/artifact/com.github.gwtmaterialdesign/gwt-material/1.3.2"),
        CORE_1_3_1("http://mvnrepository.com/artifact/com.github.gwtmaterialdesign/gwt-material/1.3.1"),
        CORE_1_3("http://mvnrepository.com/artifact/com.github.gwtmaterialdesign/gwt-material/1.3"),
        CORE_1_2("http://mvnrepository.com/artifact/com.github.gwtmaterialdesign/gwt-material/1.2"),
        CORE_1("http://mvnrepository.com/artifact/com.github.gwtmaterialdesign/gwt-material/1.0"),

        // FOR gwt-material Themes
        THEME_1_4("http://mvnrepository.com/artifact/com.github.gwtmaterialdesign/gwt-material-themes/1.4"),
        THEME_1_5("https://oss.sonatype.org/content/repositories/snapshots/com/github/gwtmaterialdesign/gwt-material-themes/1.5.0-SNAPSHOT/"),

        // FOR gwt-material Addins
        ADDINS_1_5_SNAPSHOT("https://oss.sonatype.org/content/repositories/snapshots/com/github/gwtmaterialdesign/gwt-material-addins/1.5.0-SNAPSHOT/");

        String name;
        VersionLink(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }

    private String version;
    private String date;
    private String linkCore;
    private String linkAddins;
    private String linkThemes;
    private String color;

    public Version() {}

    public Version(String version, String date, String linkCore, String linkAddins, String linkThemes, String color) {
        this.version = version;
        this.date = date;
        this.linkCore = linkCore;
        this.linkAddins = linkAddins;
        this.linkThemes = linkThemes;
        this.color = color;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getLinkCore() {
        return linkCore;
    }

    public void setLinkCore(String linkCore) {
        this.linkCore = linkCore;
    }

    public String getLinkAddins() {
        return linkAddins;
    }

    public void setLinkAddins(String linkAddins) {
        this.linkAddins = linkAddins;
    }

    public String getLinkThemes() {
        return linkThemes;
    }

    public void setLinkThemes(String linkThemes) {
        this.linkThemes = linkThemes;
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }
}
