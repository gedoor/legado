package me.ag2s.epublib.domain;



import me.ag2s.epublib.util.StringUtil;

import java.io.Serializable;

/**
 * Represents one of the authors of the book
 *
 * @author paul
 */
public class Author implements Serializable {

    private static final long serialVersionUID = 6663408501416574200L;

    private String firstname;
    private String lastname;
    private Relator relator = Relator.AUTHOR;

    public Author(String singleName) {
        this("", singleName);
    }

    public Author(String firstname, String lastname) {
        this.firstname = firstname;
        this.lastname = lastname;
    }

    public String getFirstname() {
        return firstname;
    }

    public void setFirstname(String firstname) {
        this.firstname = firstname;
    }

    public String getLastname() {
        return lastname;
    }

    public void setLastname(String lastname) {
        this.lastname = lastname;
    }


    @Override
    @SuppressWarnings("NullableProblems")
    public String toString() {
        return this.lastname + ", " + this.firstname;
    }

    public int hashCode() {
        return StringUtil.hashCode(firstname, lastname);
    }

    public boolean equals(Object authorObject) {
        if (!(authorObject instanceof Author)) {
            return false;
        }
        Author other = (Author) authorObject;
        return StringUtil.equals(firstname, other.firstname)
                && StringUtil.equals(lastname, other.lastname);
    }

    /**
     * 设置贡献者的角色
     *
     * @param code 角色编号
     */

    public void setRole(String code) {
        Relator result = Relator.byCode(code);
        if (result == null) {
            result = Relator.AUTHOR;
        }
        this.relator = result;
    }

    public Relator getRelator() {
        return relator;
    }


    public void setRelator(Relator relator) {
        this.relator = relator;
    }
}
