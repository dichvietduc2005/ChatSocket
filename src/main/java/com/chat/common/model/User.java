package com.chat.common.model;

import java.io.Serializable;

/**
 * Lớp đại diện cho người dùng trong hệ thống.
 * Định danh qua Tên và Email (không dùng mật khẩu).
 */
public class User implements Serializable {
    private static final long serialVersionUID = 1L;

    private String name;
    private String email;
    private String avatarUrl; // URL ảnh đại diện (sẽ tải qua HTTP)

    public User(String name, String email) {
        this.name = name;
        this.email = email;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }

    public void setAvatarUrl(String avatarUrl) {
        this.avatarUrl = avatarUrl;
    }

    @Override
    public String toString() {
        return name + " <" + email + ">";
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null || getClass() != obj.getClass())
            return false;
        User user = (User) obj;
        return email.equals(user.email);
    }

    @Override
    public int hashCode() {
        return email.hashCode();
    }
}
