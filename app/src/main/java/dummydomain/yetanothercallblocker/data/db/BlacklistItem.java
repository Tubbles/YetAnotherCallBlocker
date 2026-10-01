package dummydomain.yetanothercallblocker.data.db;

import org.greenrobot.greendao.annotation.Entity;
import org.greenrobot.greendao.annotation.Generated;
import org.greenrobot.greendao.annotation.Id;
import org.greenrobot.greendao.annotation.Index;
import org.greenrobot.greendao.annotation.NotNull;

import java.util.Date;

@Entity
public class BlacklistItem {

    @Id
    private Long id;

    private String name;

    @Index
    @NotNull
    private String pattern;

    @NotNull
    private Date creationDate;

    @NotNull
    private boolean invalid;

    @NotNull
    private int numberOfCalls = 0;
    private Date lastCallDate;

    @NotNull
    private boolean allow = false;

    @NotNull
    private int position = 0;

    public BlacklistItem() {}

    public BlacklistItem(String name, String pattern) {
        this(null, name, pattern, new Date(), false, 0, null, false, 0);
    }

    @Generated
    public BlacklistItem(Long id, String name, @NotNull String pattern,
                         @NotNull Date creationDate, boolean invalid, int numberOfCalls,
                         Date lastCallDate, boolean allow, int position) {
        this.id = id;
        this.name = name;
        this.pattern = pattern;
        this.creationDate = creationDate;
        this.invalid = invalid;
        this.numberOfCalls = numberOfCalls;
        this.lastCallDate = lastCallDate;
        this.allow = allow;
        this.position = position;
    }

    public Long getId() {
        return this.id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPattern() {
        return this.pattern;
    }

    public void setPattern(String pattern) {
        this.pattern = pattern;
    }

    public Date getCreationDate() {
        return this.creationDate;
    }

    public void setCreationDate(Date creationDate) {
        this.creationDate = creationDate;
    }

    public boolean getInvalid() {
        return this.invalid;
    }

    public void setInvalid(boolean invalid) {
        this.invalid = invalid;
    }

    public Date getLastCallDate() {
        return this.lastCallDate;
    }

    public void setLastCallDate(Date lastCallDate) {
        this.lastCallDate = lastCallDate;
    }

    public int getNumberOfCalls() {
        return this.numberOfCalls;
    }

    public void setNumberOfCalls(int numberOfCalls) {
        this.numberOfCalls = numberOfCalls;
    }

    public boolean getAllow() {
        return this.allow;
    }

    public void setAllow(boolean allow) {
        this.allow = allow;
    }

    public int getPosition() {
        return this.position;
    }

    public void setPosition(int position) {
        this.position = position;
    }

    @Override
    public String toString() {
        return "BlacklistItem{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", pattern='" + pattern + '\'' +
                ", allow=" + allow +
                ", position=" + position +
                '}';
    }

}
